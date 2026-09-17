# 9월 17일 확인된 5가지 문제 수정

기준: 앞서 전달한 MovieLife_Purple_Recommend21.zip. 보라색 테마, 기존 회원/찜/리뷰/관리자 기능과 10점 리뷰를 유지했습니다.

## 1. 영화가 적게 나오거나 요청이 실패하는 문제

기존 탐색 목록은 TMDB 한 페이지를 받아 한국 관람등급이 없는 영화까지 일괄 제외했습니다. 이번에는 일반 탐색에서 등급 미확인 작품을 표시하되 카드에 '한국 관람등급 정보 없음'을 표시합니다. TMDB adult 및 확인된 한국 청소년 관람불가 작품은 기존처럼 기본 목록에서 제외합니다. 홈·자동추천·인기영화 전용 화면의 엄격한 기본 제외 정책은 유지합니다. 등급 미확인은 안전한 등급을 확인했다는 뜻이 아닙니다.

일부 요청에는 서버의 매개변수 이름 지정이 빠져 있었습니다. 이메일뿐 아니라 영화 상세의 movieId, 추천의 genre/mood/rating, 홈 찜의 index, OTT의 provider/page를 명시했습니다. 이 문제로 상세나 OTT·추천 요청이 실패하는 경로도 수정했습니다.

## 2. 포스터 위 정렬 버튼

전체 영화·국내영화·해외영화의 포스터 바로 위에 'TMDB 기준 / 최신순 / 인기순 / 별점순' 버튼을 추가했습니다. 선택한 버튼은 보라색으로 강조됩니다. 기존 장르·국가·평점·연도·OTT 조건을 유지하고 정렬을 바꾸면 첫 페이지로 돌아갑니다. 왼쪽 정렬 선택창도 유지합니다.

최신순은 개봉일, 인기순은 TMDB popularity, 별점순은 투표 200개 이상 영화의 TMDB 평점 내림차순입니다. 별점순의 기존 URL 값 recommended는 호환성을 위해 유지합니다. 제목 검색은 TMDB 관련도순이므로 정렬 버튼이 비활성화됩니다.

## 3. 영화 둘러보기 표시 수 확대

한 화면에서 TMDB 4페이지, 최대 80개 후보를 묶어 표시합니다. 같은 영화 ID는 중복 제거합니다. 1화면은 원본 1~4페이지, 2화면은 5~8페이지이므로 다음 페이지에서 같은 범위를 재조회하지 않습니다. 페이지 수는 이 묶음 기준으로 다시 계산했습니다. 인물 검색도 같은 묶음 기준을 사용합니다.

후보 80개 중 성인 영화 2개인 검증 데이터에서는 78개가 표시되는 것을 확인했습니다. 실제로 조건을 만족하는 영화가 충분하지 않거나 마지막 페이지·강한 필터·부분 API 실패인 경우에는 30개 미만일 수 있습니다. 가짜 영화나 중복 카드로 개수를 채우지 않습니다. 화면에 현재 표시 수와 검색 후보 수를 구분해 보여줍니다. 추가 페이지 요청이 실패해도 이미 받은 목록은 유지하고 부분 실패 안내를 표시합니다.

## 4. 5점 선택 누락

'5점 이상'과 '5점대 (5점 이상~6점 미만)'를 추가했습니다. 이제 전체 평점 / 5점 미만 / 5점대 / 5점 이상 / 6·7·8·9점 이상을 선택할 수 있습니다. TMDB 10점 기준이며 제목 검색의 후처리에도 같은 경계를 적용했습니다. 5.0은 5점대에 포함되고 6.0은 포함되지 않습니다.

## 5. 이메일 중복확인 오류

스크린샷의 'Name for argument ... parameter name information not available ... -parameters'는 이메일 형식 오류가 아니라 서버 요청 연결 오류입니다. AccountController의 email 매개변수에 이름을 명시하고 build.gradle에도 -parameters를 지정했습니다. STS가 해당 옵션 없이 컴파일하더라도 요청을 해석하도록 처리했습니다.

## 검사 결과

- 전체 Java 소스 호환 컴파일 통과. 기존과 같이 첨부 JAR 의존성과 임시 Lombok 확장 사본을 사용했으며 정식 Gradle 빌드 통과를 의미하지 않습니다.
- 이번 오류 재현·목록 묶음·5점 필터·정렬·부분 실패 검사 19개 통과. 실제 MovieService의 변환·성인 제외 로직을 사용하고 외부 TMDB 데이터는 테스트 대역으로 제공했습니다.
- AccountController를 Java 매개변수 이름 정보 없이 별도로 컴파일한 뒤 실제 Spring RequestParamMethodArgumentResolver로 이메일 문자열이 전달되는 것을 확인했습니다. 실제 MySQL 중복 조회까지 수행한 것은 아닙니다.
- 기존 서비스 검사 53개, 추천 검사 23개, 클라이언트 검사 29개, 추천 JS 검사 13개, Thymeleaf 렌더링 24건, JPQL 구문 검사 4건 통과.
- 실제 사용자 PC의 DB·TMDB 연결 서버 기동 및 브라우저 클릭 검증은 아직 필요합니다. 여러 페이지의 등급을 확인하므로 캐시가 없는 첫 조회는 시간이 더 걸릴 수 있습니다.

## 적용 방법

1. 실행 중인 Spring Boot 서버를 종료합니다.
2. 전체 ZIP을 새 폴더에 풀고 build.gradle이 있는 movielife 폴더를 STS의 Existing Gradle Project로 가져옵니다. 이전 버전과 같은 포트로 동시에 실행하지 않습니다.
3. Java 17 및 기존 DB/TMDB 연결 설정을 확인하고 Gradle Refresh 후 Project → Clean을 실행합니다.
4. MovieApplication을 Spring Boot App으로 실행합니다.
5. 브라우저에서 Ctrl+Shift+R로 새로고침합니다. CSS/JS 캐시 버전도 갱신했습니다.
6. 회원가입 이메일 중복확인 → 전체/국내/해외 영화 → 정렬 버튼 → 5점대 → 다음 페이지 → 상세 → 찜/리뷰 순서로 확인합니다.

이번 수정 자체는 Entity·DB 컬럼·기존 DB 데이터를 변경하지 않습니다. 다만 이전 21개 기능 업그레이드를 아직 적용하지 않은 DB에는 기존 문서의 최초 10점 리뷰 환산 안내가 여전히 적용됩니다.

## 수정한 파일

- build.gradle
- src/main/java/com/yse/dev/Controller/AccountController.java
- src/main/java/com/yse/dev/Controller/HomeController.java
- src/main/java/com/yse/dev/Controller/MovieController.java
- src/main/java/com/yse/dev/Controller/RecommendationController.java
- src/main/java/com/yse/dev/Service/CatalogModel.java
- src/main/java/com/yse/dev/Service/CatalogService.java
- src/main/resources/static/css/theme.css
- src/main/resources/templates/admin.html
- src/main/resources/templates/country-movies.html
- src/main/resources/templates/fragments/catalog-filters.html
- src/main/resources/templates/fragments/layout.html
- src/main/resources/templates/movie-list.html

## 새 파일

- docs/FIX_0917_KO.md
- src/main/resources/static/js/catalog-controls.js
- src/test/java/com/yse/dev/verification/ReportedFixCheck.java
- src/test/java/com/yse/dev/verification/ReportedFixTest.java
