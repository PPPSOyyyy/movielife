# 변경 내역

첨부된 `movielife(3).zip`을 기준으로 수정했습니다. 이 파일은 실행 완료를 보증하는 배포 기록이 아니라 수정 내용과 검증 범위를 설명하는 안내입니다.

## 화면

- 어두운 배경과 빨간 포인트를 유지하고 여백, 글자 크기, 버튼, 입력창, 카드와 안내창을 통일했습니다.
- 공통 헤더/검색창을 연결했습니다. 작은 화면의 메뉴는 줄바꿈 없이 가로로 이동합니다.
- 홈: 오늘의 영화, 주목받는 영화, 베스트 영화, 실제 찜한 영화, OTT 목록으로 정리했습니다.
- 영화: 전체/평점 높은 영화/개봉 예정 탭, 장르·평점·연도·OTT 필터, 검색 결과와 페이지 이동을 정리했습니다.
- 제목 검색과 전체 영화 필터를 구분했습니다. 제목 검색 중에는 필터를 숨겨 적용되지 않는 조건을 표시하지 않으며, 검색을 초기화하면 필터를 사용할 수 있습니다.
- 상세: TMDB 10점과 회원 리뷰 5점 평점을 구분하고, 줄거리·출연·시청 정보·리뷰를 배치했습니다.
- 찜: 카드와 상세에서 같은 상태를 표시하고, 내 찜 목록에서 취소하면 즉시 카드와 개수를 갱신합니다.
- 리뷰: 최신/별점 높은/낮은 순 정렬, 작성·수정·삭제, 글자 수 표시, 내 리뷰와 빈 상태를 정리했습니다.
- 회원: 입력칸 아래 오류, 비밀번호 보기, 아이디·닉네임 중복확인, 확인 버튼으로 마무리하는 가입 안내, 프로필/비밀번호 수정, 탈퇴 비밀번호 확인을 정리했습니다.
- 마이페이지: 찜 수, 리뷰 수, 내가 준 평균 별점, 최근 감상과 계정 관리를 표시합니다. 별점은 기존 구조처럼 리뷰와 함께 저장됩니다.
- 기본 포스터를 세로 비율 SVG로 추가해 포스터가 없거나 이미지가 깨져도 카드 비율을 유지합니다. 기존 PNG는 보존했습니다.

## 동작 보완

- 로그인 성공 시 기존 페이지로 복귀하며 성공 경고창을 띄우지 않습니다. 외부 사이트로 향하는 복귀 주소는 차단합니다.
- 로그아웃 후 공개 화면은 현재 위치를 유지하고, 로그인 전용 화면에서는 홈으로 이동합니다.
- 회원가입과 프로필 수정에 서버 입력 검증을 추가했습니다. 기존 BCrypt/평문 계정의 로그인 후 암호화 처리는 유지했습니다.
- 로그인 성공 시 세션 ID를 재발급하고 세션 ID 디버그 로그를 제거했습니다. 쿠키의 HttpOnly/SameSite 설정을 명시했습니다.
- 찜 추가·취소 시 회원 행 잠금을 사용합니다. 반복 추가 요청은 기존 찜을 반환하고, 이미 삭제된 찜의 취소도 성공으로 처리합니다.
- 한 회원이 한 영화에 새 리뷰를 여러 번 등록하지 않도록 막고, 기존 리뷰가 있으면 수정 화면으로 연결합니다. 기존 중복 데이터는 임의로 삭제하지 않습니다.
- 탈퇴 시 계정의 찜과 리뷰를 함께 삭제하여 이후 같은 아이디를 재사용해도 예전 활동이 남지 않게 했습니다.
- 리뷰 내용은 DOM의 textContent로 표시해 사용자 입력을 HTML로 실행하지 않습니다.
- 영화 요청에 연결/응답 제한 시간을 두고, 공개 영화 데이터만 5분·최대 200건 캐시합니다.
- 영화 상세의 출연진·관람등급·시청 정보 요청은 [TMDB append_to_response](https://developer.themoviedb.org/docs/append-to-response) 방식으로 묶었습니다.
- 개봉 예정은 [TMDB 지역별 개봉 필터](https://developer.themoviedb.org/docs/region-support)를 사용해 한국 극장 개봉일 기준 내일~6개월 뒤를 조회합니다. 재개봉이 포함될 수 있으며 제공 데이터에 따라 달라집니다.
- 기존 임의의 개봉 예정 페이지 수 계산을 없애고 API 페이지 수를 사용합니다. 잘못된 페이지를 보정하고 검색 조건을 페이지 이동에 유지합니다.
- 외부 영화 조회 실패는 빈 목록과 구분해 재시도 안내를 표시합니다.

## 팀원 담당 영역

- 국내영화 `/movies/domestic`, 해외영화 `/movies/foreign`: 실제 조회 로직은 추가하지 않고 준비 중 안내만 연결했습니다.
- 담당자가 구현할 때 `TeamPageController.java`의 해당 매핑을 삭제하거나 교체하세요. 주소가 중복되면 Spring 실행 시 오류가 납니다.
- 영화추천 `movie-recommend.html`: 본문/추천/찜 예시 스크립트는 보존했습니다. 공통 헤더 연결, CSS 참조, 본문 바로가기용 id만 수정했습니다. 추천 알고리즘은 이번 작업에 포함되지 않습니다.
- 기존 `/popular`, `/movie-list`, `/movie-recommend` 주소도 유지했습니다.
- 공통 헤더는 `templates/fragments/layout.html`, 디자인은 `static/css/app.css`에 있습니다.

## 확인한 범위와 남은 실행 확인

완료:
- JDK의 Java 17 파서로 main/test Java 소스 29개 문법 확인.
- 공통/회원/OTT/리뷰 JS 문법 확인.
- 실제 MemberValidation을 컴파일하여 정상·오류·길이·BCrypt 바이트 한도 등 18건 실행.
- 공통 JS를 실행하여 외부 복귀 주소 차단, API 성공/실패 응답, 텍스트 처리 등 21건 확인.
- 템플릿 16개: 중복 id, 정적 파일 존재, 공통 fragment 연결, 추천 화면 변경 범위 확인.

미완료:
- 전체 Gradle 빌드와 JUnit 실행: 이 환경에서 Gradle 배포본 다운로드 불가.
- 실제 Thymeleaf 렌더링, MySQL 트랜잭션/동시 요청, TMDB 실데이터 통합 검증.
- 데스크톱/모바일 브라우저의 실제 렌더링과 클릭 테스트: 로컬 화면 접근 정책 제한.

디자인 미리보기는 샘플 데이터를 채운 정적 HTML입니다. Spring Boot 템플릿 엔진이나 실제 API로 실행한 결과가 아닙니다.

실행 가능한 컴퓨터에서 `gradlew.bat test`를 먼저 실행한 뒤 아래 순서를 확인해 주세요.
1. 홈 → 상단 검색 → 필터 → 2페이지 이동 → 영화 상세.
2. 비회원 찜 → 로그인 안내 → 취소. 로그인 후 같은 영화로 복귀.
3. 카드에서 찜 → 상세 하트 상태 → 마이페이지 찜 수 → 내 찜 목록에서 취소.
4. 리뷰 등록 → 본인 리뷰 수정 → 삭제 → 평균/개수 반영. 다른 회원의 리뷰에는 수정/삭제가 없어야 합니다.
5. 회원가입: 중복확인 후 값을 바꾸면 다시 확인해야 하며, 완료 안내에서 확인을 눌러 로그인 화면으로 이동합니다.
6. 프로필: 닉네임 변경, 잘못된 현재 비밀번호 거절, 새 비밀번호로 로그인.
7. 별도로 만든 시험용 계정으로 탈퇴 후 해당 계정의 찜과 리뷰가 제거되는지 확인.
8. 휴대폰 폭에서 상단 메뉴 가로 이동, 검색창, 카드, 리뷰 입력창과 모달 확인.

## 변경 파일 목록

| 파일 | 구분 |
| --- | --- |
| `README.md` | 수정 |
| `docs/preview-detail.html` | 추가 |
| `docs/preview-home.html` | 추가 |
| `docs/preview-login.html` | 추가 |
| `docs/preview-movies.html` | 추가 |
| `docs/preview-mypage.html` | 추가 |
| `docs/preview-signup.html` | 추가 |
| `scripts/check-client.cjs` | 추가 |
| `src/main/java/com/yse/dev/Controller/ApiExceptionHandler.java` | 추가 |
| `src/main/java/com/yse/dev/Controller/HomeController.java` | 수정 |
| `src/main/java/com/yse/dev/Controller/MemberController.java` | 수정 |
| `src/main/java/com/yse/dev/Controller/MovieController.java` | 수정 |
| `src/main/java/com/yse/dev/Controller/MoviePageExceptionHandler.java` | 추가 |
| `src/main/java/com/yse/dev/Controller/ReviewPageController.java` | 수정 |
| `src/main/java/com/yse/dev/Controller/TeamPageController.java` | 추가 |
| `src/main/java/com/yse/dev/DTO/MovieDetailDto.java` | 수정 |
| `src/main/java/com/yse/dev/DTO/MovieDto.java` | 수정 |
| `src/main/java/com/yse/dev/Repository/FavoriteRepository.java` | 수정 |
| `src/main/java/com/yse/dev/Repository/MemberRepository.java` | 수정 |
| `src/main/java/com/yse/dev/Repository/ReviewRepository.java` | 수정 |
| `src/main/java/com/yse/dev/Service/FavoriteService.java` | 수정 |
| `src/main/java/com/yse/dev/Service/MemberService.java` | 수정 |
| `src/main/java/com/yse/dev/Service/MemberValidation.java` | 추가 |
| `src/main/java/com/yse/dev/Service/MovieService.java` | 수정 |
| `src/main/java/com/yse/dev/Service/ReviewService.java` | 수정 |
| `src/main/resources/application.properties` | 수정 |
| `src/main/resources/static/css/app.css` | 추가 |
| `src/main/resources/static/js/account.js` | 추가 |
| `src/main/resources/static/js/auth-header.js` | 수정 |
| `src/main/resources/static/js/catalog.js` | 추가 |
| `src/main/resources/static/js/reviews.js` | 추가 |
| `src/main/resources/static/poster/no-poster.svg` | 추가 |
| `src/main/resources/templates/error.html` | 추가 |
| `src/main/resources/templates/favorite-movies.html` | 수정 |
| `src/main/resources/templates/fragments/layout.html` | 추가 |
| `src/main/resources/templates/index.html` | 수정 |
| `src/main/resources/templates/login.html` | 수정 |
| `src/main/resources/templates/movie-detail.html` | 수정 |
| `src/main/resources/templates/movie-list.html` | 수정 |
| `src/main/resources/templates/movie-recommend.html` | 수정 |
| `src/main/resources/templates/my-reviews.html` | 수정 |
| `src/main/resources/templates/mypage.html` | 수정 |
| `src/main/resources/templates/ott.html` | 수정 |
| `src/main/resources/templates/popular.html` | 수정 |
| `src/main/resources/templates/profile.html` | 수정 |
| `src/main/resources/templates/review.html` | 수정 |
| `src/main/resources/templates/signup.html` | 수정 |
| `src/main/resources/templates/team-placeholder.html` | 추가 |
| `src/test/java/com/yse/dev/Service/ActivityServiceTest.java` | 추가 |
| `src/test/java/com/yse/dev/Service/MemberValidationTest.java` | 추가 |
| `먼저읽어주세요.txt` | 추가 |

`SOURCE_DIFF.patch`에는 실행 소스와 테스트의 전체 변경 내용이 있습니다. 바로 사용하실 때는 패치를 적용하지 않고 동봉된 전체 소스를 사용하면 됩니다.
