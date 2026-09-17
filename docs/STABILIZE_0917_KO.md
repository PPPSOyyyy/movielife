# 9월 17일 안정화·성능·검색 수정

기준: MovieLife_Fix0917 (5열 고정판). 레이아웃·구조는 바꾸지 않았고, 서버 로직·템플릿의 데이터 바인딩과 색상 토큰만 수정했습니다.

## 원인 진단

- 목록 한 화면(최대 80편)마다 영화별로 `/movie/{id}/release_dates` 를 호출해 한국 관람등급을 확인했습니다. 6개 스레드로 80회를 돌리므로 화면당 2~5초가 걸리고, TMDB 429(요청 과다)가 나면 등급이 UNKNOWN 으로 바뀌어 결과 수가 들쭉날쭉했습니다.
- TMDB 4페이지·인물 검색 4페이지를 순차 호출했습니다.
- 영화 상세는 기본정보·출연진·개봉정보·OTT·예고편 5회를 순차 호출했습니다.
- 응답 캐시가 200건·5분이라 목록 한 화면(80건 이상)만 열어도 캐시가 계속 밀려났습니다.
- 찜 목록/마이페이지는 찜한 영화마다 상세 5회를 순차 호출했습니다. 또 카드 프래그먼트가 요구하는 회원 별점 필드가 상세 DTO에 없어 렌더링이 실패할 수 있었습니다.
- 일시적인 네트워크 오류·429·5xx 에 재시도가 없었습니다.

## 1. 서버 로딩 안정화 / 5. TMDB 속도 최적화

- 목록 변환의 영화별 등급 호출을 캐시·병렬·기한 방식으로 바꿨습니다. 화면당 TMDB 호출: 기존 84회 이상(6스레드 순차 대기) → 영화 4페이지(검색 시 8~12회, 모두 병렬) + 캐시에 없는 영화의 등급 조회만(24스레드 병렬, 재시작 후에도 캐시 유지).
- 한 화면의 영화 4페이지(+검색 시 인물 4페이지, 출연작 4페이지)를 병렬로 요청합니다.
- 영화 상세는 `append_to_response=credits,release_dates,watch/providers,videos` 로 1회만 호출합니다.
- 캐시: 목록 5분 / 상세·인물 30분 / 제공처·등급 코드표 24시간, 최대 3,000건 LRU. 영화별 한국 관람등급은 별도 24시간·20,000건 캐시.
- 네트워크 오류·429·5xx 는 짧게 기다린 뒤 1회 재시도합니다. 404 등은 즉시 오류 처리합니다.
- HTTP 클라이언트를 JDK HttpClient(연결 재사용, HTTP/2) 기반으로 바꿨습니다. 연결 4초 / 읽기 8초.
- 찜 목록/마이페이지는 카드에 필요한 요약 정보만 병렬로 조회하고 회원 별점을 붙입니다.
- 관람등급 확인: 캐시에 있으면 즉시 반영하고, 없는 영화만 24개 스레드로 병렬 조회합니다. 기본 목록은 최대 6초(`movielife.certification-wait-ms`), 등급 필터 선택 시 최대 15초까지만 기다리며, 기한 안에 끝나지 않은 영화는 일단 표시하되 조회는 계속되어 다음 화면부터 반영됩니다. 확인되지 않은 상태에서는 '한국 관람등급 정보 없음'을 표시하지 않습니다.
- 확인한 등급은 `사용자 홈/.movielife/certifications.txt` 에 저장해 서버를 재시작해도 다시 조회하지 않습니다(50건마다·종료 시 저장, `movielife.certification-cache` 속성으로 경로 변경, 빈 값이면 저장 안 함). 상세·찜 목록에서 확인한 등급도 같은 캐시에 들어갑니다.

실측(같은 PC, TMDB 실제 연결): `/movies` 기존 4.1초 → 등급 캐시 없는 첫 조회 0.8~2.3초, 캐시 후 0.05초. 상세 1.3초 → 0.35초(캐시 후 0.02초). 배우 검색 첫 조회 1.0초 → 캐시 후 0.05초.

## 2. 성인 콘텐츠 필터링

- 모든 검색/디스커버 호출에 `include_adult=false` 를 고정했습니다. 청소년 관람불가(19) 필터를 선택해도 TMDB 성인 콘텐츠는 포함하지 않습니다.
- 목록 변환 단계에서 TMDB `adult=true` 작품을 항상 제외합니다(전체/국내/해외/평점/개봉예정/OTT/홈/추천/인물 필모그래피 공통).
- 인물 검색 결과와 배우 출연작에서도 `adult=true` 인 인물·작품을 제외합니다.
- 한국 청소년 관람불가(19)·제한상영가로 확인된 작품도 기본 목록(전체/국내/해외/평점/개봉예정/OTT/홈/인물 필모그래피)에서 제외합니다. 관람등급 필터에서 청소년 관람불가(19)를 직접 선택하면 볼 수 있습니다. 등급이 아직 확인되지 않은 영화(기한 초과·조회 실패)는 그 화면에서만 잠시 보일 수 있고, 다음 화면부터 제외됩니다.

## 4. OTT 링크 연결

- 영화 상세의 OTT 칩이 새 창 링크로 동작합니다. 같은 `cinema-ott-chip` 클래스를 유지해 모양은 동일합니다.
- TMDB 는 서비스별 작품 딥링크를 주지 않으므로 넷플릭스·왓챠·티빙·웨이브·쿠팡플레이·디즈니+·Apple TV·Google Play·Prime Video·YouTube 는 해당 서비스의 작품 검색 페이지(한글 제목)로, 그 밖의 서비스는 TMDB 국가별 시청 안내 페이지(JustWatch 연동)로 연결합니다.
- `MovieDetailDto.ottLinks` 에 이름·로고·링크·구분(구독/무료/광고/대여/구매)을 담고, 기존 `ottProviders`(이름 목록)는 그대로 유지합니다.

## 6. 배우 이름 검색

- 검색어로 인물을 찾은 뒤 검색어와 이름(한글 또는 원어, 띄어쓰기 무시)이 실제로 맞는 인물을 인기순으로 최대 3명 고릅니다.
- 그 인물들이 참여한 영화를 `discover/movie?with_people=` 로 조회해 제목 검색 결과 뒤에 붙입니다. 같은 영화는 한 번만 표시하고, 장르·평점·연도·OTT 상세 필터도 함께 적용됩니다.
- 이름이 맞는 인물이 없으면 추가 호출 없이 기존 제목 검색만 수행합니다. 페이지 수는 제목 검색·인물·출연작 중 가장 큰 값을 씁니다.
- 예: '톰 크루즈' → 인물 카드 + 탑건: 매버릭, 미션 임파서블 시리즈 등 80편(2화면).

## 7. 레드/블랙 테마

- 기준 색: 강조 `#D92234`, 검정 `#000000`. 보라색 계열(`#7040c0`·`#bfa3ff` 등)을 모두 `#D92234` 로 바꾸고, 보라 기운이 섞인 회색(`#faf9fc`·`#292333`·`#141219`·`#201c29` 등)은 주변 테마에 맞는 무채색으로 바꿨습니다.
- 라이트: 배경 `#f7f7f7` / 패널 흰색 / 본문 검정 `#000000` / 보조 `#5c5c5c` / 선 `#dddddd` / 강조 `#D92234`(hover `#b81b2b`, 연한 배경 `#fbe6e8`).
- 다크: 배경 `#000000` / 패널 `#121212`·`#1c1c1c` / 본문 흰색 / 보조 `#b0b0b0` / 선 `#333333` / 강조 `#D92234`(hover `#ff4b5c`, 연한 배경 `#3a0f15`, 포커스 링 `#ff5c6b`).
- 적용 범위: theme.css 토큰, app.css 의 보라 재디자인 블록 리터럴, admin-mint.css, ml-core.js 대화상자 버튼, theme.js·layout.html 의 theme-color 메타와 로고 SVG. 라이트/다크/시스템 선택창과 기본값(라이트)은 그대로입니다.
- CSS/JS 캐시 버전을 `redblack-0917` 로 올렸으므로 서버 재시작 후 Ctrl+Shift+R 한 번이면 됩니다.

## 검사

- Gradle `test` 27개 통과(기존 10개 + 신규 17개). 신규 검사는 실제 MovieService/CatalogService/CatalogModel 을 사용하고 TMDB 응답만 고정 데이터로 대체합니다: adult·19등급 제외와 영화당 1회 조회, 느린 등급 조회의 기한 초과 처리와 다음 화면 반영, 등급 캐시 파일 저장·재시작 후 재사용, 등급 필터 검증, 상세 1회 호출과 OTT 링크, 429/5xx 재시도·404 즉시 실패, 병렬 순서·예외 전파, 배우 검색 병합·성인 인물 제외·중복 제거, 검색+OTT 필터 제공 여부 확인, 2화면=원본 5~8페이지.
- 실제 MySQL·TMDB 연결로 8081 포트에 기동해 홈/전체/국내/해외/평점/개봉예정/OTT/상세/인물/검색/등급 필터 전 경로 200 응답과 브라우저 표시를 확인했습니다.

## 수정 파일

- src/main/java/com/yse/dev/Service/MovieService.java (전면 재작성)
- src/main/java/com/yse/dev/Service/CatalogService.java
- src/main/java/com/yse/dev/Service/CatalogModel.java
- src/main/java/com/yse/dev/Service/PersonService.java
- src/main/java/com/yse/dev/Controller/MovieController.java
- src/main/java/com/yse/dev/Controller/HomeController.java
- src/main/java/com/yse/dev/DTO/MovieDto.java (`certificationChecked`)
- src/main/java/com/yse/dev/DTO/MovieDetailDto.java (`ottLinks`, `OttProvider`, 카드 호환 필드)
- src/main/resources/templates/movie-detail.html (OTT 칩 → 링크)
- src/main/resources/templates/fragments/layout.html ('정보 없음' 표시 조건, theme-color·로고 색)
- src/main/resources/static/css/theme.css, app.css, admin-mint.css (색상 토큰·리터럴)
- src/main/resources/static/js/theme.js, ml-core.js (theme-color·대화상자 색)
- src/main/resources/templates/*.html (CSS/JS 캐시 버전 redblack-0917)
- README_UPGRADE_KO.md

## 새 파일

- docs/STABILIZE_0917_KO.md
- src/test/java/com/yse/dev/Service/TmdbFixture.java
- src/test/java/com/yse/dev/Service/MovieServiceTest.java
- src/test/java/com/yse/dev/Service/CatalogSearchTest.java

## 적용

기존 서버 종료 → Gradle Refresh → Project Clean → MovieApplication 실행 → Ctrl+Shift+R. Entity·DB 컬럼·application.properties 는 변경하지 않았습니다.
