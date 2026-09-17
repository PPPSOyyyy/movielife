package com.yse.dev.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDetailDto.CastMember;
import com.yse.dev.DTO.MovieDetailDto.OttProvider;
import com.yse.dev.DTO.MovieDto;

import jakarta.annotation.PreDestroy;


/**
 * TMDB 연동 서비스.
 *
 * <ul>
 *   <li>응답 캐시: 목록 5분 / 상세·인물 30분 / 제공처·등급 코드표 24시간, 최대 3,000건 LRU</li>
 *   <li>한국 관람등급 캐시: 영화 ID별 24시간, 최대 20,000건, 파일에 저장해 재시작 후에도 재사용</li>
 *   <li>일시 오류(네트워크·429·5xx)는 1회 자동 재시도</li>
 *   <li>목록 변환: TMDB adult 플래그 작품 제외 + 한국 청소년 관람불가(19) 작품 제외.
 *       등급은 캐시 우선, 없으면 병렬 조회(기한 내), 기한 초과분은 다음 화면부터 반영</li>
 *   <li>영화 상세는 append_to_response 로 한 번에 조회</li>
 * </ul>
 */
@Service
public class MovieService {


    @Value("${tmdb.api.key}")
    private String apiKey;


    @Value("${tmdb.api.base-url}")
    private String baseUrl;


    private final RestTemplate restTemplate;


    // =========================================================
    // 캐시
    // =========================================================

    private static final long LIST_TTL   = 5L * 60 * 1000;          // 목록·검색·디스커버
    private static final long DETAIL_TTL = 30L * 60 * 1000;         // 영화 상세 / 인물
    private static final long STATIC_TTL = 24L * 60 * 60 * 1000;    // 제공처 목록 / 등급 코드표
    private static final long CERT_TTL   = 24L * 60 * 60 * 1000;    // 영화별 한국 관람등급

    private static final int RESPONSE_CACHE_MAX      = 3000;
    private static final int CERTIFICATION_CACHE_MAX = 20000;


    private record CachedResponse(
            long expiresAt,
            Map<String, Object> value
    ) {
    }


    private record CachedCertification(
            long expiresAt,
            String code
    ) {
    }


    private final Map<URI, CachedResponse> cache =
            new LinkedHashMap<>(512, .75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<URI, CachedResponse> eldest) {
                    return size() > RESPONSE_CACHE_MAX;
                }
            };


    private final Map<Long, CachedCertification> certifications =
            new LinkedHashMap<>(1024, .75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, CachedCertification> eldest) {
                    return size() > CERTIFICATION_CACHE_MAX;
                }
            };


    // =========================================================
    // 관람등급 확인 대기 시간
    //
    // 기본 목록: 확인되지 않은 영화의 등급을 병렬로 조회하되 이 시간까지만 기다립니다.
    //            시간 안에 끝나지 않은 영화는 일단 표시하고, 조회는 계속 진행되어 캐시에 저장됩니다.
    // 등급 필터: 정확도가 우선이므로 더 오래 기다립니다.
    // =========================================================

    @Value("${movielife.certification-wait-ms:6000}")
    private long defaultCertificationWaitMillis = 6_000;

    @Value("${movielife.certification-filter-wait-ms:15000}")
    private long filterCertificationWaitMillis = 15_000;


    // =========================================================
    // 스레드 풀
    //
    // fetchPool  : 목록 여러 페이지·등급 확인 등 화면 응답에 필요한 병렬 요청
    //              (고정 24개, 작업은 대기열에 쌓이므로 호출 스레드가 막히지 않음)
    // warmupPool : 화면 응답과 무관한 관람등급 예열·캐시 저장 (낮은 동시성, 가득 차면 버림)
    // =========================================================

    private final ThreadPoolExecutor fetchPool =
            new ThreadPoolExecutor(
                    24, 24,
                    30, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    daemonThreads("tmdb-fetch-")
            );

    {
        fetchPool.allowCoreThreadTimeOut(true);
    }


    private final ThreadPoolExecutor warmupPool =
            new ThreadPoolExecutor(
                    3, 3,
                    30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(500),
                    daemonThreads("tmdb-warmup-"),
                    new ThreadPoolExecutor.AbortPolicy()
            );



    private static ThreadFactory daemonThreads(String prefix) {

        AtomicInteger seq = new AtomicInteger();

        return runnable -> {
            Thread t = new Thread(runnable, prefix + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }


    @PreDestroy
    public void shutdownPools() {
        warmupPool.shutdownNow();
        fetchPool.shutdownNow();
        saveCertificationCache();
    }


    // =========================================================
    // 관람등급 캐시 파일
    //
    // 서버를 재시작해도 이미 확인한 등급을 다시 조회하지 않도록
    // 사용자 홈의 .movielife/certifications.txt 에 저장합니다.
    // (movielife.certification-cache 속성으로 경로 변경 가능, 빈 값이면 저장 안 함)
    // =========================================================

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(MovieService.class);

    private static final int CERTIFICATION_SAVE_EVERY = 50;


    @Value("${movielife.certification-cache:${user.home}/.movielife/certifications.txt}")
    private String certificationCachePath;


    private final AtomicInteger unsavedCertifications =
            new AtomicInteger();


    private final java.util.concurrent.atomic.AtomicBoolean savePending =
            new java.util.concurrent.atomic.AtomicBoolean();


    private java.nio.file.Path certificationCacheFile() {

        if (certificationCachePath == null || certificationCachePath.isBlank()) {
            return null;
        }

        return java.nio.file.Paths.get(certificationCachePath);
    }


    @jakarta.annotation.PostConstruct
    public void loadCertificationCache() {


        java.nio.file.Path file =
                certificationCacheFile();


        if (file == null || !java.nio.file.Files.isRegularFile(file)) {
            return;
        }


        long now =
                System.currentTimeMillis();

        int loaded = 0;


        try {

            List<String> lines =
                    java.nio.file.Files.readAllLines(file, StandardCharsets.UTF_8);


            synchronized (certifications) {

                for (String line : lines) {

                    String[] parts = line.split("\t");

                    if (parts.length < 3) {
                        continue;
                    }

                    try {

                        long id = Long.parseLong(parts[0].trim());
                        long expiresAt = Long.parseLong(parts[2].trim());
                        String code = KoreanCertification.normalize(parts[1].trim());

                        if (expiresAt > now) {
                            certifications.put(id, new CachedCertification(expiresAt, code));
                            loaded++;
                        }

                    } catch (NumberFormatException ignored) {
                        // 손상된 줄은 건너뜁니다.
                    }
                }
            }


            log.info("관람등급 캐시 {}건을 불러왔습니다: {}", loaded, file);

        } catch (IOException e) {

            log.warn("관람등급 캐시를 읽지 못했습니다: {} ({})", file, e.getMessage());
        }
    }


    private void saveCertificationCache() {


        java.nio.file.Path file =
                certificationCacheFile();


        if (file == null) {
            return;
        }


        List<String> lines =
                new ArrayList<>();


        long now =
                System.currentTimeMillis();


        synchronized (certifications) {

            for (Map.Entry<Long, CachedCertification> entry : certifications.entrySet()) {

                if (entry.getValue().expiresAt() > now) {
                    lines.add(entry.getKey() + "\t" + entry.getValue().code() + "\t" + entry.getValue().expiresAt());
                }
            }
        }


        try {

            if (file.getParent() != null) {
                java.nio.file.Files.createDirectories(file.getParent());
            }

            java.nio.file.Path temp =
                    file.resolveSibling(file.getFileName() + ".tmp");

            java.nio.file.Files.write(temp, lines, StandardCharsets.UTF_8);

            java.nio.file.Files.move(
                    temp,
                    file,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            unsavedCertifications.set(0);

        } catch (IOException | RuntimeException e) {

            log.warn("관람등급 캐시를 저장하지 못했습니다: {} ({})", file, e.getMessage());
        }
    }


    /**
     * 새 등급이 일정 수 이상 쌓이면 백그라운드에서 한 번 저장합니다.
     */
    private void scheduleCertificationSave() {


        if (unsavedCertifications.incrementAndGet() < CERTIFICATION_SAVE_EVERY) {
            return;
        }


        if (!savePending.compareAndSet(false, true)) {
            return;
        }


        try {

            warmupPool.execute(() -> {
                try {
                    saveCertificationCache();
                } finally {
                    savePending.set(false);
                }
            });

        } catch (RejectedExecutionException full) {

            savePending.set(false);
        }
    }


    // =========================================================
    // 생성자
    // =========================================================

    public MovieService() {


        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(4))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();


        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);


        factory.setReadTimeout(
                Duration.ofSeconds(8)
        );


        restTemplate =
                new RestTemplate(
                        factory
                );
    }


    // =========================================================
    // TMDB 기본 주소
    // =========================================================

    private UriComponentsBuilder endpoint(
            String path) {


        return UriComponentsBuilder
                .fromUriString(
                        baseUrl + path
                )
                .queryParam(
                        "api_key",
                        apiKey
                )
                .queryParam(
                        "language",
                        "ko-KR"
                );
    }


    // =========================================================
    // 페이지 번호 보정
    // =========================================================

    private int page(
            int value) {


        return Math.max(
                1,
                Math.min(
                        500,
                        value
                )
        );
    }


    // =========================================================
    // 경로별 캐시 유지 시간
    // =========================================================

    private static long ttlFor(String path) {

        if (path.startsWith("/watch/providers")
                || path.startsWith("/certification/")
                || path.startsWith("/genre/")) {
            return STATIC_TTL;
        }

        if (path.startsWith("/movie/") && path.length() > 7 && Character.isDigit(path.charAt(7))) {
            return DETAIL_TTL;
        }

        if (path.startsWith("/person/")) {
            return DETAIL_TTL;
        }

        return LIST_TTL;
    }


    // =========================================================
    // TMDB 요청 (캐시 + 재시도)
    // =========================================================

    private Map<String, Object> request(
            UriComponentsBuilder builder) {

        return request(builder, LIST_TTL);
    }


    /**
     * @param ttl 캐시 유지 시간(ms). 0 이하이면 캐시하지 않습니다.
     */
    private Map<String, Object> request(
            UriComponentsBuilder builder,
            long ttl) {


        URI uri =
                builder
                        .build()
                        .encode()
                        .toUri();


        if (ttl > 0) {

            synchronized (cache) {

                CachedResponse cached = cache.get(uri);

                if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
                    return cached.value();
                }
            }
        }


        Map<String, Object> value =
                fetch(uri);


        if (ttl > 0) {

            synchronized (cache) {

                cache.put(
                        uri,
                        new CachedResponse(
                                System.currentTimeMillis() + ttl,
                                value
                        )
                );
            }
        }


        return value;
    }


    /**
     * 실제 HTTP 호출. 네트워크 오류·429·5xx 는 짧게 기다린 뒤 1회 재시도합니다.
     * 404 등 나머지 4xx 는 즉시 예외를 던집니다.
     * (테스트에서 TMDB 응답을 대체할 수 있도록 protected)
     */
    protected Map<String, Object> fetch(URI uri) {


        RestClientException last = null;


        for (int attempt = 0; attempt < 2; attempt++) {


            if (attempt > 0) {
                pause(last instanceof HttpClientErrorException.TooManyRequests ? 700 : 250);
            }


            try {

                return http(uri);

            } catch (HttpClientErrorException.TooManyRequests
                     | HttpServerErrorException
                     | ResourceAccessException e) {

                last = e;
            }
        }


        throw last;
    }


    /**
     * 재시도 없는 단일 HTTP 호출.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> http(URI uri) {


        Map<String, Object> value =
                restTemplate.getForObject(
                        uri,
                        Map.class
                );


        if (value == null) {

            throw new ResourceAccessException(
                    "TMDB 응답이 비어 있습니다."
            );
        }


        return value;
    }


    private static void pause(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // =========================================================
    // 병렬 실행 도우미
    //
    // 여러 TMDB 요청을 동시에 보내고 순서대로 결과를 돌려줍니다.
    // 작업 하나가 실패하면 원래 예외(RestClientException 등)를 그대로 던집니다.
    // =========================================================

    public <T> List<T> parallel(
            List<Supplier<T>> tasks) {


        if (tasks.isEmpty()) {
            return List.of();
        }


        if (tasks.size() == 1) {
            List<T> single = new ArrayList<>(1);
            single.add(tasks.get(0).get());   // null 결과도 허용
            return single;
        }


        List<CompletableFuture<T>> futures =
                new ArrayList<>();


        for (Supplier<T> task : tasks) {
            futures.add(CompletableFuture.supplyAsync(task, fetchPool));
        }


        List<T> result =
                new ArrayList<>();


        for (CompletableFuture<T> future : futures) {
            result.add(join(future));
        }


        return result;
    }


    private static <T> T join(CompletableFuture<T> future) {

        try {

            return future.join();

        } catch (CompletionException e) {

            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }

            throw new ResourceAccessException(
                    "TMDB 요청에 실패했습니다.",
                    cause instanceof IOException io ? io : new IOException(cause)
            );
        }
    }


    // =========================================================
    // 인기 영화
    // =========================================================

    public Map<String, Object> getPopularMovies(
            int page) {


        return request(
                endpoint(
                        "/movie/popular"
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 영화 검색
    // =========================================================

    public Map<String, Object> searchMovies(
            String query,
            int page) {


        return request(
                endpoint(
                        "/search/movie"
                )
                .queryParam(
                        "query",
                        query
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 장르별 영화
    // =========================================================

    public Map<String, Object> getMoviesByGenre(
            int genreId,
            int page) {


        return getFilteredMovies(
                genreId,
                null,
                null,
                null,
                page
        );
    }


    // =========================================================
    // OTT 영화
    // =========================================================

    public Map<String, Object> getOttMovies(
            Integer provider,
            int page) {


        UriComponentsBuilder builder =
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "watch_region",
                        "KR"
                )
                .queryParam(
                        "with_watch_monetization_types",
                        "flatrate|free|ads|rent|buy"
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                );


        if (
            provider != null
        ) {

            builder.queryParam(
                    "with_watch_providers",
                    provider
            );
        }


        return request(
                builder
        );
    }


    // =========================================================
    // 복합 필터
    // =========================================================

    public Map<String, Object> getFilteredMovies(
            Integer genre,
            Double minRating,
            Integer year,
            Integer provider,
            int page) {


        UriComponentsBuilder builder =
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                );


        if (
            genre != null
        ) {

            builder.queryParam(
                    "with_genres",
                    genre
            );
        }


        if (
            minRating != null &&
            minRating > 0
        ) {

            builder
                    .queryParam(
                            "vote_average.gte",
                            minRating
                    )
                    .queryParam(
                            "vote_count.gte",
                            50
                    );
        }


        if (
            year != null
        ) {

            builder.queryParam(
                    "primary_release_year",
                    year
            );
        }


        if (
            provider != null
        ) {

            builder
                    .queryParam(
                            "watch_region",
                            "KR"
                    )
                    .queryParam(
                            "with_watch_providers",
                            provider
                    )
                    .queryParam(
                            "with_watch_monetization_types",
                            "flatrate"
                    );
        }


        return request(
                builder
        );
    }


    // =========================================================
    // 평점 높은 영화
    // =========================================================

    public Map<String, Object> getTopRatedMovies(
            int page) {


        return request(
                endpoint(
                        "/movie/top_rated"
                )
                .queryParam(
                        "region",
                        "KR"
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 개봉 예정 영화
    // =========================================================

    public Map<String, Object> getUpcomingMovies(
            int page) {


        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                "Asia/Seoul"
                        )
                );


        return request(
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "region",
                        "KR"
                )
                .queryParam(
                        "with_release_type",
                        "3|2"
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "release_date.gte",
                        today.plusDays(1)
                )
                .queryParam(
                        "release_date.lte",
                        today.plusMonths(6)
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 국내 / 해외 영화
    // =========================================================

    public Map<String, Object> getCountryMovies(
            String country,
            Integer genre,
            Double minRating,
            Integer year,
            Integer provider,
            int page) {


        if (
            country == null ||
            !country.matches(
                    "[A-Z]{2}"
            )
        ) {

            throw new IllegalArgumentException(
                    "국가 정보가 올바르지 않습니다."
            );
        }


        UriComponentsBuilder builder =
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "with_origin_country",
                        country
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "include_video",
                        false
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                );


        if (
            genre != null
        ) {

            builder.queryParam(
                    "with_genres",
                    genre
            );
        }


        if (
            minRating != null &&
            minRating > 0
        ) {

            builder
                    .queryParam(
                            "vote_average.gte",
                            minRating
                    )
                    .queryParam(
                            "vote_count.gte",
                            50
                    );
        }


        if (
            year != null
        ) {

            builder.queryParam(
                    "primary_release_year",
                    year
            );
        }


        if (
            provider != null
        ) {

            builder
                    .queryParam(
                            "watch_region",
                            "KR"
                    )
                    .queryParam(
                            "with_watch_providers",
                            provider
                    )
                    .queryParam(
                            "with_watch_monetization_types",
                            "flatrate"
                    );
        }


        return request(
                builder
        );
    }


    // =========================================================
    // 목록 Map → MovieDto
    //
    // - TMDB adult=true 작품은 항상 제외합니다 (성인 콘텐츠 필터).
    // - 관람등급 필터를 선택한 경우에만 영화별 한국 등급을 확인해 걸러냅니다.
    // - 그 외에는 추가 호출 없이 캐시된 등급만 배지에 반영하고,
    //   확인되지 않은 등급은 백그라운드에서 예열합니다.
    // =========================================================

    public List<MovieDto> convertToMovieList(
            Map<String, Object> response) {

        return convertToMovieList(response, "");
    }


    public List<MovieDto> convertToMovieList(
            Map<String, Object> response,
            String certification) {


        String wanted =
                certification == null ? "" : certification.trim();


        List<MovieDto> list =
                new ArrayList<>();


        for (Map<?, ?> raw : objects(response.get("results"))) {


            if (!(raw.get("id") instanceof Number id)) {
                continue;
            }


            // 성인 콘텐츠 제외
            if (Boolean.TRUE.equals(raw.get("adult"))) {
                continue;
            }


            list.add(
                    movieDtoFromMap(raw, id.longValue())
            );
        }


        if (wanted.isBlank()) {

            // 기본 목록: 한국 청소년 관람불가(19)·제한상영가도 제외합니다.
            // 등급은 캐시 → 병렬 조회(기한 내) 순으로 확인하고,
            // 기한 안에 확인되지 않은 영화는 일단 표시하되 조회는 계속되어 다음 화면부터 반영됩니다.
            resolveCertifications(list, defaultCertificationWaitMillis);

            list.removeIf(
                    movie -> movie.isCertificationChecked()
                            && KoreanCertification.adult(movie.getCertificationCode())
            );

        } else {

            resolveCertifications(list, filterCertificationWaitMillis);

            list.removeIf(
                    movie -> !certificationMatches(wanted, movie.getCertificationCode())
            );
        }


        return list;
    }


    private static boolean certificationMatches(
            String wanted,
            String code) {

        if ("19".equals(wanted)) {
            return KoreanCertification.adult(code);
        }

        return wanted.equals(code);
    }


    /**
     * 목록의 관람등급을 확인합니다.
     *
     * 1) 캐시에 있으면 즉시 반영
     * 2) 없으면 병렬로 조회하되 waitMillis 까지만 기다림
     * 3) 기한 안에 끝나지 않은 영화는 '미확인'(UNKNOWN, checked=false)으로 두고
     *    조회 자체는 계속 진행되어 캐시에 저장됨 → 다음 화면부터 반영
     */
    private void resolveCertifications(
            List<MovieDto> list,
            long waitMillis) {


        List<MovieDto> missing =
                new ArrayList<>();


        for (MovieDto movie : list) {


            String cached =
                    cachedCertification(movie.getId());


            if (cached != null) {

                movie.setCertificationCode(cached);
                movie.setCertificationChecked(true);

            } else {

                movie.setCertificationCode("UNKNOWN");
                movie.setCertificationChecked(false);

                missing.add(movie);
            }
        }


        if (missing.isEmpty()) {
            return;
        }


        // 같은 영화가 여러 번 들어 있어도(제목 검색 + 출연작 병합 등) 조회는 한 번만 시작합니다.
        Map<Long, CompletableFuture<String>> lookups =
                new LinkedHashMap<>();


        for (MovieDto movie : missing) {

            lookups.computeIfAbsent(
                    movie.getId(),
                    id -> CompletableFuture.supplyAsync(() -> certification(id), fetchPool)
            );
        }


        List<CompletableFuture<String>> futures =
                new ArrayList<>();


        for (MovieDto movie : missing) {
            futures.add(lookups.get(movie.getId()));
        }


        long deadline =
                System.currentTimeMillis() + waitMillis;


        for (int i = 0; i < missing.size(); i++) {


            MovieDto movie = missing.get(i);


            try {

                long remaining =
                        Math.max(1, deadline - System.currentTimeMillis());

                String code =
                        futures.get(i).get(remaining, TimeUnit.MILLISECONDS);

                // certification() 은 실패 시 캐시 없이 UNKNOWN 을 돌려주므로 캐시 유무로 확인 여부를 판단
                if (cachedCertification(movie.getId()) != null) {
                    movie.setCertificationCode(code);
                    movie.setCertificationChecked(true);
                }

            } catch (java.util.concurrent.TimeoutException slow) {

                // 기한 초과: 조회는 계속 진행되어 캐시에 저장됩니다.

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                return;

            } catch (java.util.concurrent.ExecutionException e) {

                // 등급 확인 실패: 미확인 상태로 표시합니다.
            }
        }
    }



    private String cachedCertification(
            Long movieId) {


        if (movieId == null) {
            return null;
        }


        synchronized (certifications) {

            CachedCertification cached = certifications.get(movieId);

            if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
                return cached.code();
            }
        }


        return null;
    }


    private void storeCertification(
            long movieId,
            String code) {


        synchronized (certifications) {

            certifications.put(
                    movieId,
                    new CachedCertification(
                            System.currentTimeMillis() + CERT_TTL,
                            code
                    )
            );
        }


        scheduleCertificationSave();
    }


    /**
     * 영화 한 편의 한국 관람등급 코드 (ALL / 12 / 15 / 19 / RESTRICTED / UNKNOWN).
     * 실패하면 캐시하지 않고 UNKNOWN 을 돌려줍니다.
     */
    public String certification(
            long movieId) {


        String cached =
                cachedCertification(movieId);


        if (cached != null) {
            return cached;
        }


        try {

            Map<String, Object> releaseDates =
                    request(
                            endpoint("/movie/" + movieId + "/release_dates"),
                            0
                    );


            String code =
                    KoreanCertification.from(releaseDates.get("results"));


            storeCertification(movieId, code);


            return code;

        } catch (RestClientException e) {

            return "UNKNOWN";
        }
    }


    private MovieDto movieDtoFromMap(
            Map<?, ?> movie,
            Long id) {


        MovieDto dto =
                new MovieDto();


        dto.setId(
                id
        );


        dto.setTitle(
                text(
                        movie,
                        "title",
                        "제목 정보 없음"
                )
        );


        dto.setOverview(
                text(
                        movie,
                        "overview",
                        ""
                )
        );


        dto.setPosterPath(
                text(
                        movie,
                        "poster_path",
                        ""
                )
        );


        dto.setBackdropPath(
                text(
                        movie,
                        "backdrop_path",
                        ""
                )
        );


        dto.setReleaseDate(
                text(
                        movie,
                        "release_date",
                        ""
                )
        );


        dto.setVoteAverage(
                number(
                        movie,
                        "vote_average"
                )
                .doubleValue()
        );


        dto.setVoteCount(
                number(
                        movie,
                        "vote_count"
                )
                .intValue()
        );


        dto.setPopularity(
                number(
                        movie,
                        "popularity"
                )
                .doubleValue()
        );


        dto.setAdult(
                Boolean.TRUE.equals(movie.get("adult"))
        );


        List<Integer> genreIds =
                new ArrayList<>();


        if (
            movie.get("genre_ids")
                    instanceof List<?> ids
        ) {


            for (
                Object genreId :
                ids
            ) {


                if (
                    genreId instanceof Number n
                ) {

                    genreIds.add(
                            n.intValue()
                    );
                }
            }
        }


        // 상세 응답(/movie/{id})은 genre_ids 대신 genres 객체 목록을 줍니다.
        if (
            genreIds.isEmpty()
        ) {

            for (Map<?, ?> genre : objects(movie.get("genres"))) {

                if (genre.get("id") instanceof Number n) {
                    genreIds.add(n.intValue());
                }
            }
        }


        dto.setGenreIds(
                genreIds
        );


        dto.setOttProviders(
                new ArrayList<>()
        );


        return dto;
    }


    // =========================================================
    // 영화 제목만 조회 (관리자 페이지 등 간단 조회용)
    // =========================================================

    public String getMovieTitle(
            Long movieId) {


        if (
            movieId == null ||
            movieId <= 0
        ) {

            return "영화 정보 없음";
        }


        Map<String, Object> movie =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                        ),
                        DETAIL_TTL
                );


        return text(
                movie,
                "title",
                "영화 정보 없음"
        );
    }


    // =========================================================
    // 영화 상세
    //
    // 기본정보 + 출연진 + 개봉정보(관람등급) + OTT + 예고편을
    // append_to_response 로 한 번에 받아옵니다.
    // =========================================================

    public MovieDetailDto getMovieDetail(
            Long movieId) {


        if (
            movieId == null ||
            movieId <= 0
        ) {

            throw new IllegalArgumentException(
                    "영화 정보가 올바르지 않습니다."
            );
        }


        Map<String, Object> movie =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                        )
                        .queryParam(
                                "append_to_response",
                                "credits,release_dates,watch/providers,videos"
                        ),
                        DETAIL_TTL
                );


        Map<?, ?> credits =
                map(movie.get("credits"));


        Map<?, ?> releaseDates =
                map(movie.get("release_dates"));


        Map<?, ?> watchProviders =
                map(movie.get("watch/providers"));


        Map<?, ?> videos =
                map(movie.get("videos"));


        MovieDetailDto dto =
                new MovieDetailDto();


        dto.setId(
                movieId
        );


        dto.setTitle(
                text(
                        movie,
                        "title",
                        "제목 정보 없음"
                )
        );


        dto.setOriginalTitle(
                text(
                        movie,
                        "original_title",
                        ""
                )
        );


        dto.setOverview(
                text(
                        movie,
                        "overview",
                        ""
                )
        );


        dto.setPosterPath(
                text(
                        movie,
                        "poster_path",
                        ""
                )
        );


        dto.setBackdropPath(
                text(
                        movie,
                        "backdrop_path",
                        ""
                )
        );


        dto.setReleaseDate(
                text(
                        movie,
                        "release_date",
                        ""
                )
        );


        dto.setVoteAverage(
                number(
                        movie,
                        "vote_average"
                )
                .doubleValue()
        );


        dto.setRuntime(
                number(
                        movie,
                        "runtime"
                )
                .intValue()
        );


        dto.setAdult(
                Boolean.TRUE.equals(movie.get("adult"))
        );


        dto.setGenres(
                names(
                        movie.get(
                                "genres"
                        )
                )
        );


        // =====================================================
        // 제작 국가
        // =====================================================

        List<String> countries =
                new ArrayList<>();


        for (
            Map<?, ?> country :
            objects(
                    movie.get(
                            "production_countries"
                    )
            )
        ) {


            String code =
                    text(
                            country,
                            "iso_3166_1",
                            ""
                    );


            if (
                code.isBlank()
            ) {

                countries.add(
                        text(
                                country,
                                "name",
                                "정보 없음"
                        )
                );

            } else {

                countries.add(
                        new Locale(
                                "",
                                code
                        )
                        .getDisplayCountry(
                                Locale.KOREAN
                        )
                );
            }
        }


        dto.setProductionCountries(
                countries
        );


        // =====================================================
        // 출연진
        // =====================================================

        List<CastMember> castMembers =
                new ArrayList<>();


        for (
            Map<?, ?> cast :
            objects(
                    credits.get(
                            "cast"
                    )
            )
        ) {


            if (
                castMembers.size() >= 60
            ) {

                break;
            }


            Long personId =
                    null;


            if (
                cast.get("id")
                        instanceof Number n
            ) {

                personId =
                        n.longValue();
            }


            castMembers.add(
                    new CastMember(
                            personId,
                            text(
                                    cast,
                                    "name",
                                    "이름 정보 없음"
                            ),
                            text(
                                    cast,
                                    "character",
                                    ""
                            ),
                            text(
                                    cast,
                                    "profile_path",
                                    ""
                            )
                    )
            );
        }


        dto.setDirectors(objects(credits.get("crew")).stream()
            .filter(c->"Director".equals(c.get("job")))
            .map(c->new CastMember(number(c,"id").longValue(),text(c,"name","이름 정보 없음"),"감독",text(c,"profile_path",""))).toList());
        dto.setCastMembers(
                castMembers
        );


        dto.setCast(
                castMembers
                        .stream()
                        .map(
                                CastMember::getName
                        )
                        .toList()
        );


        // =====================================================
        // 감독
        // =====================================================

        dto.setDirector(
                dto.getDirectors()
                        .stream()
                        .map(CastMember::getName)
                        .findFirst()
                        .orElse("정보 없음")
        );


        // =====================================================
        // 관람등급 (상세에서 확인한 등급은 목록 배지용 캐시에도 저장)
        // =====================================================

        String certificationCode =
                KoreanCertification.from(releaseDates.get("results"));


        storeCertification(movieId, certificationCode);


        dto.setCertificationCode(certificationCode);
        dto.setCertification(KoreanCertification.label(certificationCode));


        // =====================================================
        // OTT
        // =====================================================

        List<OttProvider> ottLinks =
                providerLinks(
                        movieId,
                        dto.getTitle(),
                        dto.getOriginalTitle(),
                        watchProviders
                );


        dto.setOttLinks(
                ottLinks
        );


        dto.setOttProviders(
                ottLinks
                        .stream()
                        .map(OttProvider::getName)
                        .toList()
        );


        // =====================================================
        // 예고편
        // =====================================================

        dto.setTrailerKey(
                findTrailerKey(
                        videos
                )
        );


        return dto;
    }


    // =========================================================
    // 예고편 찾기
    // =========================================================

    private String findTrailerKey(
            Object videosValue) {


        Map<?, ?> videosMap =
                map(
                        videosValue
                );


        List<Map<?, ?>> videos =
                objects(
                        videosMap.get(
                                "results"
                        )
                );


        String fallback =
                "";


        for (
            Map<?, ?> video :
            videos
        ) {


            String site =
                    text(
                            video,
                            "site",
                            ""
                    );


            String type =
                    text(
                            video,
                            "type",
                            ""
                    );


            String key =
                    text(
                            video,
                            "key",
                            ""
                    );


            if (
                !"YouTube".equalsIgnoreCase(
                        site
                ) ||
                !"Trailer".equalsIgnoreCase(
                        type
                ) ||
                key.isBlank()
            ) {

                continue;
            }


            if (
                Boolean.TRUE.equals(
                        video.get(
                                "official"
                        )
                )
            ) {

                return key;
            }


            if (
                fallback.isBlank()
            ) {

                fallback =
                        key;
            }
        }


        return fallback;
    }


    // =========================================================
    // 영화 OTT 조회
    // =========================================================

    public List<String> getOttProviders(
            Long movieId) {


        return providerNames(
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                                + "/watch/providers"
                        ),
                        DETAIL_TTL
                )
        );
    }


    // =========================================================
    // OTT 제공처 ID 찾기
    // =========================================================

    public Integer getMovieProviderId(
            String providerName) {


        for (
            Map<?, ?> provider :
            objects(
                    request(
                            endpoint(
                                    "/watch/providers/movie"
                            )
                            .queryParam(
                                    "watch_region",
                                    "KR"
                            ),
                            STATIC_TTL
                    )
                    .get(
                            "results"
                    )
            )
        ) {


            if (
                providerName.equalsIgnoreCase(
                        text(
                                provider,
                                "provider_name",
                                ""
                        )
                )
                &&
                provider.get(
                        "provider_id"
                )
                instanceof Number id
            ) {

                return id.intValue();
            }
        }


        return null;
    }


    // =========================================================
    // OTT 이름 목록
    // =========================================================

    private List<String> providerNames(
            Object value) {


        List<String> result =
                new ArrayList<>();


        for (OttProvider provider : providerLinks(null, "", "", value)) {
            result.add(provider.getName());
        }


        return result;
    }


    // =========================================================
    // OTT 링크
    //
    // TMDB 는 서비스별 작품 딥링크를 제공하지 않으므로
    // 알려진 서비스는 해당 서비스의 작품 검색 페이지로,
    // 그 밖의 서비스는 TMDB 의 국가별 시청 안내 페이지(JustWatch 연동)로 연결합니다.
    // =========================================================

    private static final List<Map.Entry<String, String>> PROVIDER_SEARCH_URLS =
            List.of(
                    Map.entry("netflix",          "https://www.netflix.com/search?q={q}"),
                    Map.entry("watcha",           "https://watcha.com/search?query={q}"),
                    Map.entry("tving",            "https://www.tving.com/search?keyword={q}"),
                    Map.entry("wavve",            "https://www.wavve.com/search?searchWord={q}"),
                    Map.entry("coupangplay",      "https://www.coupangplay.com/search?keyword={q}"),
                    Map.entry("disneyplus",       "https://www.disneyplus.com/ko-kr/search?q={q}"),
                    Map.entry("appletv",          "https://tv.apple.com/kr/search?term={q}"),
                    Map.entry("googleplaymovies", "https://play.google.com/store/search?q={q}&c=movies&hl=ko&gl=KR"),
                    Map.entry("amazonprimevideo", "https://www.primevideo.com/search?phrase={q}"),
                    Map.entry("amazonvideo",      "https://www.primevideo.com/search?phrase={q}"),
                    Map.entry("youtube",          "https://www.youtube.com/results?search_query={q}"),
                    Map.entry("naverstore",       "https://serieson.naver.com/v3/search?query={q}")
            );


    private List<OttProvider> providerLinks(
            Long movieId,
            String title,
            String originalTitle,
            Object value) {


        Map<?, ?> results =
                map(
                        map(
                                value
                        )
                        .get(
                                "results"
                        )
                );


        Map<?, ?> kr =
                map(
                        results.get(
                                "KR"
                        )
                );


        String fallbackLink =
                text(kr, "link", "");


        if (fallbackLink.isBlank() && movieId != null) {
            fallbackLink = "https://www.themoviedb.org/movie/" + movieId + "/watch?locale=KR";
        }


        String query =
                title == null || title.isBlank() ? originalTitle : title;


        String encodedQuery =
                query == null || query.isBlank()
                        ? ""
                        : URLEncoder.encode(query, StandardCharsets.UTF_8);


        Map<String, String> kindLabels =
                new LinkedHashMap<>();

        kindLabels.put("flatrate", "구독");
        kindLabels.put("free",     "무료");
        kindLabels.put("ads",      "광고");
        kindLabels.put("rent",     "대여");
        kindLabels.put("buy",      "구매");


        List<OttProvider> result =
                new ArrayList<>();


        Set<String> seen =
                new HashSet<>();


        for (Map.Entry<String, String> kind : kindLabels.entrySet()) {


            for (
                Map<?, ?> provider :
                objects(
                        kr.get(
                                kind.getKey()
                        )
                )
            ) {


                String name =
                        text(
                                provider,
                                "provider_name",
                                ""
                        );


                if (
                    name.isBlank() ||
                    !seen.add(name)
                ) {

                    continue;
                }


                result.add(
                        new OttProvider(
                                name,
                                text(provider, "logo_path", ""),
                                providerUrl(name, encodedQuery, fallbackLink),
                                kind.getValue()
                        )
                );
            }
        }


        return result;
    }


    private static String providerUrl(
            String providerName,
            String encodedQuery,
            String fallbackLink) {


        String key =
                providerName
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", "");


        if (!encodedQuery.isBlank()) {

            for (Map.Entry<String, String> entry : PROVIDER_SEARCH_URLS) {

                if (key.startsWith(entry.getKey())) {
                    return entry.getValue().replace("{q}", encodedQuery);
                }
            }
        }


        return fallbackLink;
    }


    // =========================================================
    // Map 안전 변환
    // =========================================================

    private Map<?, ?> map(
            Object value) {


        return value
                instanceof Map<?, ?> m
                ? m
                : Map.of();
    }


    // =========================================================
    // List<Map> 안전 변환
    // =========================================================

    private List<Map<?, ?>> objects(
            Object value) {


        List<Map<?, ?>> result =
                new ArrayList<>();


        if (
            value instanceof List<?> list
        ) {


            for (
                Object item :
                list
            ) {


                if (
                    item instanceof Map<?, ?> m
                ) {

                    result.add(
                            m
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // name 목록 추출
    // =========================================================

    private List<String> names(
            Object value) {


        return objects(
                value
        )
        .stream()
        .map(
                item ->
                        text(
                                item,
                                "name",
                                ""
                        )
        )
        .filter(
                name ->
                        !name.isBlank()
        )
        .toList();
    }


    // =========================================================
    // String 값 안전 추출
    // =========================================================

    private String text(
            Map<?, ?> map,
            String key,
            String fallback) {


        Object value =
                map.get(
                        key
                );


        return (
                value == null ||
                value.toString()
                        .isBlank()
        )
                ? fallback
                : value.toString();
    }


    // =========================================================
    // Number 값 안전 추출
    // =========================================================

    private Number number(
            Map<?, ?> map,
            String key) {


        return map.get(
                key
        )
        instanceof Number n
                ? n
                : 0;
    }


    // =========================================================
    // 범용 TMDB 호출 (경로별 캐시 유지 시간 자동 적용)
    // =========================================================

    public Map<String,Object> tmdb(String path,Map<String,?> params) {
        UriComponentsBuilder uri=endpoint(path);
        params.forEach((k,v)->{if(v!=null)uri.replaceQueryParam(k,v);});
        return request(uri, ttlFor(path));
    }


    // =========================================================
    // KR 제공처 목록 (OTT 탭·필터용)
    // =========================================================

    public List<Map<String,Object>> krProviders() {
        List<Map<String,Object>> result=new ArrayList<>();
        Map<String,String> labels=new LinkedHashMap<>();
        labels.put("netflix","넷플릭스");labels.put("tving","티빙");
        labels.put("apple tv","Apple TV");labels.put("apple tv plus","Apple TV+");
        labels.put("disney plus","디즈니+");labels.put("wavve","웨이브");
        labels.put("watcha","왓챠");labels.put("google play movies","Google Play Movies");
        labels.put("coupang play","쿠팡플레이");
        List<Map<?,?>> available=objects(tmdb("/watch/providers/movie",Map.of("watch_region","KR")).get("results"));
        Set<Integer> seen=new HashSet<>();
        for(var brand:labels.entrySet())for(Map<?,?> provider:available) {
            String name=text(provider,"provider_name","").trim().toLowerCase(Locale.ROOT);
            if(name.equals(brand.getKey()) && provider.get("provider_id") instanceof Number id && seen.add(id.intValue()))
                result.add(Map.of("id",id.intValue(),"name",brand.getValue(),"logo",text(provider,"logo_path","")));
        }
        return result;
    }


    // =========================================================
    // 영화 요약 (홈 찜 카드·찜 목록용, 상세보다 가벼움)
    // =========================================================

    public MovieDto getMovieSummary(long id){
        // 기본정보와 관람등급을 한 번에 조회합니다.
        Map<String,Object> raw=tmdb("/movie/"+id,Map.of("append_to_response","release_dates"));
        MovieDto m=movieDtoFromMap(raw,id);
        String code=KoreanCertification.from(map(raw.get("release_dates")).get("results"));
        storeCertification(id,code);
        m.setCertificationCode(code);
        m.setCertificationChecked(true);
        return m;
    }


    // =========================================================
    // 특정 OTT 제공 여부
    // =========================================================

    public boolean hasKrProvider(long movieId,int providerId){
        try {
            Map<?,?> results=map(tmdb("/movie/"+movieId+"/watch/providers",Map.of()).get("results"));
            Map<?,?> kr=map(results.get("KR"));
            for(String kind:List.of("flatrate","free","ads","rent","buy"))for(Map<?,?> p:objects(kr.get(kind)))
                if(p.get("provider_id") instanceof Number n && n.intValue()==providerId)return true;
        }catch(RestClientException e){return false;}
        return false;
    }


    /** Obtain the exact current KR API value (e.g. All vs ALL, 18 vs 19). */
    public String krCertificationValue(String normalized) {
        try {
            Map<?,?> all=map(tmdb("/certification/movie/list",Map.of()).get("certifications"));
            List<String> codes=objects(all.get("KR")).stream().map(c->text(c,"certification",""))
                .filter(code->normalized.equals(KoreanCertification.normalize(code))).sorted().toList();
            return codes.isEmpty()?null:codes.get(0);
        }catch(RestClientException e){return null;}
    }
}
