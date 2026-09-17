# 9월 17일 추가 8개 수정

기준은 MovieLife_Fix0917.zip입니다. 아래 내용이 이전 문서보다 우선합니다.

1. 배우 페이지: 완전한 패키지 이름으로 작성된 RequestParam에 이름 지정이 빠져 있었습니다. page/role/certification을 명시했습니다. 출연진 링크는 role=cast로 연결하며 감독 링크는 기존 role=director를 유지합니다. TMDB 실제 출연작/감독작 ID로 기존 영화 상세를 엽니다.
2. 보안 질문·답변: 회원가입 HTML에 서버의 실제 보안 질문 5개를 직접 렌더링합니다. 별도 질문 API 요청이 실패해도 선택할 수 있습니다. 입력한 답변은 보이는 텍스트로 변경했습니다. DB의 해시 저장 및 복구 검증 방식은 유지합니다. 추가로 API에서 질문이 도착해도 선택지가 중복되지 않습니다.
3. 여백: 회원가입·로그인 공통 영역은 최대 1000px 안에서 안내와 폼을 위쪽에 나란히 배치하고 모바일은 한 열로 표시합니다. 회원가입 폼이 화면 맨 오른쪽에 떨어지고 안내가 아래로 내려가던 문제를 수정했습니다. 라이트 헤더 로그인 글자·국가 필터 선택창 대비도 개선했습니다.
4. OTT: 요청한 넷플릭스·티빙·Apple TV/TV+·디즈니+·웨이브·왓챠·Google Play Movies·쿠팡플레이 브랜드만, TMDB watch_region=KR 응답에 실제로 포함될 때 표시합니다. 해외 전문 OTT 및 넷플릭스 광고형 중복 버튼은 제외합니다. 없는 서비스 ID는 만들지 않습니다. 쿠팡플레이가 현재 실데이터에 있는지는 이 환경에서 확정하지 못했습니다. KR 응답에 Coupang Play가 있으면 자동 표시됩니다. 구독/대여/구매는 제공 영화마다 다를 수 있습니다.
5. 카드 정렬: 실제 카드 개수와 화면 폭에 따라 열 수를 조정하고 동적 OTT 로딩·화면 크기 변경 시 다시 계산합니다. 마지막 줄에 1~2개만 남는 경우를 줄이도록 처리했습니다. 모든 화면 폭과 모든 작품 수에서 완전한 행을 보장할 수는 없습니다. 실제 영화 수가 적거나 나누어떨어지지 않으면 일부 빈칸이 남으며 숨김·중복·가짜 카드로 채우지 않습니다. 포스터는 같은 비율로 맞추고 OTT 선택 버튼도 짧은 행으로 정리했습니다.
6. 전체 영화 최신순: 국내·해외뿐 아니라 전체 영화에도 primary_release_date.lte=오늘 조건을 적용했습니다. 2099년 등 미래 개봉일이 최신순 상단을 차지하지 않습니다. TMDB 개봉일 정보 기준이며 개봉 예정 전용 목록은 기존 미래 개봉일 범위를 유지합니다.
7. 연도: 시작/종료 숫자 입력 UI를 없애고 전체·2020년대·2010년대…1950년대·1950년 이전 드롭다운 하나로 정리했습니다. 기존 연도 URL 매개변수는 호환하며 새 UI를 선택하면 해당 연도 범위를 적용합니다.
8. 마지막 옵션: 첨부 화면의 'TMDB 성인 콘텐츠 (한국 19세 등급과 별개)' 항목을 제거했습니다. 한국 청소년 관람불가(19) 필터는 유지합니다. 이전 tmdb-adult 탐색 URL도 일반 기본 필터로 정규화합니다.

## 검증과 제한

추가 검사 16개(출연작/감독작·OTT 실제 응답 선별·미래 날짜 제외·개봉 예정 유지·연도·인물 요청 이름) 통과. 전체 Java 호환 컴파일, 기존 서비스 53개, 추천 서비스 23개, 클라이언트 30개, Thymeleaf 렌더링 24건, JPQL 구문 4건 통과했습니다. Java 호환 컴파일은 임시 Lombok 확장 사본과 첨부 JAR 의존성을 사용한 방식입니다. 외부 TMDB/DB 연결은 테스트 대역을 사용한 항목이 있으며 정식 Gradle 빌드·실제 서버 기동·브라우저 시각 검증은 사용자 PC에서 필요합니다.

TMDB 제공처 공식 근거: https://developer.themoviedb.org/reference/watch-providers-movie-list — watch_region으로 국가별 응답을 제한할 수 있습니다. 이 응답에서 요청한 브랜드만 다시 선별합니다.

## 적용

기존 서버 종료 → ZIP 새 폴더에 압축 해제 → STS Existing Gradle Project로 movielife 가져오기 → Gradle Refresh 및 Project Clean → MovieApplication 실행 → Ctrl+Shift+R.

배우 포스터 클릭 → 출연작 클릭, 회원가입 질문 선택 및 답변 확인, /movies 최신순, 연도 드롭다운, OTT 버튼 목록과 더보기, 라이트/다크 전환을 확인해 주세요. 이번 수정은 Entity·DB 컬럼을 변경하지 않습니다. 앞선 버전의 10점 리뷰와 기존 기능은 포함됩니다.

## 수정 파일

- src/main/java/com/yse/dev/Controller/CatalogAdvice.java
- src/main/java/com/yse/dev/Controller/PersonController.java
- src/main/java/com/yse/dev/DTO/CatalogFilter.java
- src/main/java/com/yse/dev/Service/CatalogService.java
- src/main/java/com/yse/dev/Service/MovieService.java
- src/main/resources/static/css/theme.css
- src/main/resources/static/js/account-management.js
- src/main/resources/templates/admin.html
- src/main/resources/templates/country-movies.html
- src/main/resources/templates/fragments/catalog-filters.html
- src/main/resources/templates/fragments/layout.html
- src/main/resources/templates/movie-detail.html
- src/main/resources/templates/movie-list.html
- src/main/resources/templates/signup.html
- src/test/java/com/yse/dev/verification/TemplateCheck.java

## 새 파일

- docs/REFINE_0917_KO.md
- src/main/resources/static/js/balanced-grid.js
- src/test/java/com/yse/dev/verification/RefinementCheck.java
- src/test/java/com/yse/dev/verification/RefinementTest.java