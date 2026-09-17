# movieLife 추천 비교·통합 및 보라색 디자인

2026-09-16. 비교 대상은 팀원 `movielife.zip`의 실제 src 안 추천 파일 5개와 앞서 만든 `MovieLife_Upgrade21.zip`입니다. 통합본은 제가 만든 21개 기능의 프로젝트를 기준으로 수정했습니다. 팀원 ZIP 전체를 덮어쓰지 않았습니다.

## 결론

기존 공통 MovieService, MovieDto, 영화 상세, 찜, 회원관리, 관리자, 10점 리뷰와 테마를 유지하고, 팀원 버전의 장르·기분 복수 선택과 회원 평균 평점 표시를 반영했습니다. Gemini API를 사용하지 않는 TMDB 조건 기반 추천입니다. AI가 취향을 학습하는 서비스로 표시하지 않습니다.

## 5개 파일 비교

| 팀원 파일 | 실제 코드에서 확인한 내용 | 통합 결정 |
|---|---|---|
| AiRecommendService.java | 이름은 AI지만 실제 호출은 TMDB Discover입니다. rating 매개변수는 사용하지 않습니다. 영화 제목을 쉼표 문자열로 돌려주며 API 실패 시 고정 영화 6개를 반환합니다. | 파일을 추가하지 않았습니다. 기존 MovieService의 TMDB 호출·캐시·한국 관람등급 처리를 재사용하고 ID와 데이터를 그대로 전달합니다. 실패는 오류로 알립니다. |
| RecommendDto.java | 자체 별점 siteRating이 있고 상세 화면을 위한 필드도 중복 선언합니다. | 공통 MovieDto를 상속하는 작은 DTO로 정리했습니다. 기존 필드와 영화 ID를 보존하고 nullable siteRating만 추가했습니다. |
| RecommendService.java | 별도 RestTemplate과 코드에 직접 적힌 키로 TMDB를 호출합니다. 추천 제목마다 재검색 후 첫 결과를 골라 DB 별점을 조회합니다. | 키·TMDB 연결은 기존 MovieService 설정을 재사용합니다. 제목 재검색을 없애 동명 영화가 바뀌는 문제를 피하고 DB 평균을 한 번의 집계 쿼리로 조회합니다. |
| RecommendController.java | /movie-recommend가 기존 HomeController와 겹칩니다. /detail은 RecommendDto만 넣어 공통 상세 화면을 렌더링합니다. 4.0점 문자열만 판정하지만 화면은 4점입니다. 주석은 TMDB 기준인데 실제 비교는 siteRating입니다. | 기존 HomeController와 RecommendationController의 URL 구조를 유지합니다. 새 RecommendController를 중복 등록하지 않습니다. 필터는 TMDB 10점, 회원 평균은 별도 표시로 구분합니다. |
| movie-recommend.html | 장르·기분 복수 선택, 자체 별점 표시가 장점입니다. 화면 전체 이동, 별도 찜 fetch, 독립 스타일·로딩 UI를 사용합니다. | 기존 공통 레이아웃·추천 화면을 유지하며 복수 선택과 두 평점 표시를 반영했습니다. 기존 ML.card/ML.syncFavorites로 상세·찜 동작을 재사용합니다. |

팀원 버전의 조건별 HttpSession 캐시는 만료·개수 제한이 없고 DB 별점 변경이 즉시 반영되지 않을 수 있어 도입하지 않았습니다. 기존 MovieService의 TMDB 캐시는 유지하고, 회원 평균은 추천 조회 때 DB에서 다시 집계합니다.

## 통합 후 동작

- /recommend와 /movie-recommend에서 같은 추천 화면을 엽니다.
- 장르·기분은 여러 개 선택할 수 있고 다시 누르면 해제됩니다. 평점은 하나만 선택합니다.
- 선택한 장르·기분의 장르 ID는 중복 제거 후 모두 만족하는 AND 조건으로 처리합니다. 조건이 많으면 결과가 적어질 수 있다는 안내를 표시했습니다. 아무것도 선택하지 않으면 임의의 액션 장르를 강제하지 않습니다.
- 기분 매핑은 기존 버전을 유지합니다: 웃음=코미디, 감동=드라마, 설렘=로맨스, 긴장=스릴러, 생각=미스터리, 편안함=가족. 기분을 분석하는 AI가 아닌 명시적인 규칙입니다.
- 화면 평점 선택은 TMDB 8점 이상·9점 이상·상관없어요이며 모두 10점 기준입니다. 서버는 6·7도 검증된 값으로 지원합니다.
- TMDB 투표 200개 이상인 영화의 평점 내림차순입니다. 기존 성인·한국 청소년 관람불가·등급 미확인 제외 처리를 그대로 거칩니다.
- 카드에는 TMDB 평점과 회원 평균을 각각 /10으로 표시합니다. 리뷰가 없으면 '아직 평가 없음'이며 0점 영화로 취급하지 않습니다. 회원 평균이 낮거나 없어도 TMDB 필터 결과에서 임의 제외하지 않습니다.
- 영화 제목/포스터는 기존 /movies/{id} 상세 페이지로 이동합니다. 찜은 공통 로그인 확인·상태 동기화를 사용합니다.
- API 오류 때 고정 영화나 가짜 성공 결과를 보여주지 않습니다. 조건 불일치 빈 결과와 통신 실패 안내를 구분합니다.

TMDB의 쉼표 AND 및 평점·투표 수 필터는 공식 문서를 확인했습니다: [TMDB Discover Movie](https://developer.themoviedb.org/reference/discover-movie).

## 보라색 디자인

라이트는 흰색 카드와 아주 밝은 배경(#FAF9FC), 깊은 보라색 포인트(#7040C0)입니다. 다크는 거의 검정인 배경(#141219), 밝은 라벤더 포인트(#BFA3FF)입니다. 검정·흰색은 테마별 배경이며 보라색이 공통 브랜드 색입니다.

로고·메뉴·버튼·필터 선택·회원가입·회원정보·관리자까지 동일하게 적용했습니다. 빨간 19 표시와 오류·삭제 등 의미가 있는 경고색은 유지했습니다. 시스템/라이트/다크 및 저장된 선택값도 유지했습니다. 기본값은 라이트입니다.

버튼 글자 대비 계산: 라이트 흰 글자/보라 버튼 6.60:1, 다크 짙은 글자/라벤더 버튼 7.87:1. 작은 글자가 흐려지지 않도록 라이트 포인트를 진하게 골랐습니다. 실제 브라우저 화면 캡처 검증은 수행하지 못했습니다.

## 이번 수정 파일

- src/main/java/com/yse/dev/Controller/RecommendationController.java: 기존 API 유지, 추천 서비스 호출·입력 오류 처리.
- src/main/java/com/yse/dev/Repository/ReviewRepository.java: 영화별 회원 평균 일괄 집계 쿼리.
- src/main/resources/templates/movie-recommend.html: 복수 선택·평점 구분·설명·명시적 data-value.
- src/main/resources/static/js/recommendations.js: 복수 선택, 공통 카드, 두 평점 표시, 로딩/실패 처리.
- src/main/resources/static/css/app.css, theme.css, auth-mint.css, admin-mint.css: 보라색 및 라이트/다크 색상 통일. 기존 파일명·클래스는 호환성을 위해 유지.
- src/main/resources/static/js/theme.js, ml-core.js: 테마 배경 메타 색상과 기존 대화상자 기본색 변경. 인증/찜 동작 로직 유지.
- src/main/resources/templates/fragments/layout.html, admin.html: 수정된 CSS/JS 캐시 버전 갱신.
- docs/CHANGELOG_KO.md, docs/CHANGELOG_KO.html, README_UPGRADE_KO.md: 최신 변경 안내.

## 이번 새 파일

- src/main/java/com/yse/dev/Service/RecommendService.java: 조건 검증·TMDB 연결 재사용·회원 평균 조합.
- src/main/java/com/yse/dev/DTO/RecommendDto.java: 공통 영화 DTO + 회원 평균.
- src/test/java/com/yse/dev/verification/RecommendationCheck.java, RecommendationTest.java: 추천 서비스·컨트롤러 검증.
- tools/verify-recommendations.cjs: 복수 선택·평점 구분·오류 상태 복구 검증.
- docs/RECOMMEND_MERGE_KO.md: 이 비교·통합 문서.

이번 추천/디자인 변경으로 DB 컬럼은 추가되지 않았습니다. 앞선 21개 기능에서 추가한 회원 복구 정보와 review.rating_scale 및 기존 5점 리뷰의 10점 환산은 그대로 포함되어 있습니다. 따라서 처음 업그레이드하는 DB는 기존 실행 안내대로 백업 후 실행합니다.

## 점검 결과와 제한

- 첨부 실행 JAR의 의존성을 이용한 전체 Java 소스 호환 컴파일 통과. 임시 사본에서 Lombok이 생성할 접근자/생성자를 펼친 방식으로, 실제 Gradle/Lombok 빌드 통과와는 다릅니다.
- 추천 서비스·컨트롤러 검증 23개, 추천 JS 검증 13개 통과. TMDB/DB는 테스트 대역을 사용했습니다.
- 기존 회원·활동·필터·10점 리뷰 검사 53개, 기존 클라이언트 검사 28개 통과.
- 실제 Thymeleaf 엔진 렌더링 24건, MySQL 엔티티 메타데이터 기반 JPQL 구문 검사 4건 통과. 실제 MySQL 데이터 조회 검증은 아닙니다.
- 네트워크/실행 환경 제한으로 Gradle test 및 MySQL·TMDB 연결 서버 기동, 실제 브라우저에서의 클릭·반응형 검증은 완료하지 못했습니다. '실제 실행 확인 완료'로 전달하면 안 됩니다.

## 사용자 PC에서 열고 확인하기

1. 기존 서버를 종료하고 DB를 백업합니다. 이번 ZIP을 새 폴더에 풀고 movielife 폴더를 STS Existing Gradle Project로 가져옵니다.
2. Java 17과 본인 DB/TMDB 설정을 확인하고 Gradle Refresh 후 MovieApplication을 실행합니다. HTML 파일을 직접 열어 실행하는 프로젝트가 아닙니다.
3. /recommend에서 장르 두 개와 기분을 선택·해제하고 추천을 확인합니다. 8점/9점 이상 선택 시 TMDB 평점을 확인합니다.
4. 영화 상세에서 10점 리뷰를 등록하고 추천을 다시 조회해 회원 평균이 바뀌는지 확인합니다. 리뷰 없는 영화는 '아직 평가 없음'인지 확인합니다.
5. 추천 카드 찜 → 마이페이지 확인 → 찜 해제, 비로그인 찜 → 로그인 안내, 포스터 → 기존 상세를 확인합니다.
6. 홈·회원가입·추천·마이페이지·관리자에서 라이트/다크를 각각 선택하고 새로고침합니다. 시스템 모드는 OS 테마를 변경해 확인합니다.
7. Windows에서 gradlew.bat test, Node 사용 시 node tools/verify-client.cjs 및 node tools/verify-recommendations.cjs를 실행할 수 있습니다.

팀원에게는 '추천 5개 파일을 그대로 넣으면 URL과 상세 DTO가 겹쳐서 기존 프로젝트 구조를 기준으로 통합했고, 복수 선택과 회원 평균 표시를 살렸다. 오프라인 검증은 통과했지만 실제 DB/TMDB 연결 실행은 PC에서 최종 확인해야 한다'고 설명하시면 됩니다.
