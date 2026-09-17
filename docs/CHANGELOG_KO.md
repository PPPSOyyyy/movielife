> 9월 17일 최신 수정: [FIX_0917_KO.md](FIX_0917_KO.md). 일반 탐색의 등급 미확인 처리·페이지 크기·5점 필터는 최신 문서를 우선합니다.

> 최신 추천 통합·보라색 디자인 비교는 [RECOMMEND_MERGE_KO.md](RECOMMEND_MERGE_KO.md)를 먼저 확인하세요. 아래는 기존 1~21번 기능 설명입니다.

# movieLife 변경사항 정리 — 1~21번

기준 파일: `MovieLife_Mint_Project (2).zip`의 실제 Spring Boot 프로젝트. 추가 요청한 리뷰 10점 만점까지 포함했습니다.

**상태: 21개 항목의 구현을 반영했습니다. 실제 MySQL·TMDB·브라우저를 연결한 최종 통합 확인은 사용자 실행 환경에서 필요합니다.**

## 먼저 실행하는 방법

1. 기존 서버를 종료하고 현재 사용 중인 MySQL DB를 백업해 주세요. 최초 실행 때 기존 리뷰 점수가 환산됩니다.
2. ZIP을 풀고 `movielife` 폴더를 STS의 `File → Import → Gradle → Existing Gradle Project`로 가져옵니다. `build.gradle`이 있는 폴더를 선택합니다.
3. Java 17을 사용하고 Gradle 프로젝트를 새로고침합니다. 기존 `application.properties`의 DB/TMDB 설정은 그대로 보존했습니다. 본인 환경의 연결 값이 맞는지 확인합니다.
4. `MovieApplication.java`를 Spring Boot App으로 실행하고 브라우저에서 `http://localhost:8080`을 엽니다. 이전 서버와 같은 포트를 동시에 사용하지 마세요.
5. 로그인 → 마이페이지 → 회원정보 관리에서 기존 계정의 이름·이메일·보안 질문을 등록할 수 있습니다. 이메일/복구 답변 저장에는 현재 비밀번호가 필요합니다.
6. 관리자 화면은 로그인한 계정이 기존과 동일한 `admin`일 때 `/admin`에서 엽니다. 기존 관리자 비밀번호를 보존합니다.

이 ZIP은 사진/정적 모형이 아닌 전체 Spring Boot 소스입니다. HTML 파일을 더블클릭하는 방식이 아니라 서버를 실행해서 이용합니다.

## 검증 결과와 범위

| 구분 | 결과 | 범위 |
|---|---|---|
| Java 소스 호환 컴파일 | 통과 | 첨부 실행 JAR의 실제 의존성을 사용했습니다. Lombok 다운로드가 막혀 임시 검증 복사본에 동일한 접근자·생성자를 생성했습니다. 정식 Gradle/Lombok 빌드 결과와 구분해야 합니다. |
| 서비스/회귀 검증 | 53개 통과 | 실제 서비스 로직·BCrypt·복구 세션·오답/만료/재사용·필터·등급·기존 찜/리뷰 권한·10점 평균. 저장소/외부 API는 테스트 더블 사용. |
| Thymeleaf 렌더링 | 24건 통과 | 주요 화면 19개, 관리자 개별 목록 3개, 인물만 검색 결과, 비회원 빈 홈. 실제 Thymeleaf/SpringEL 엔진 사용. |
| JPA 쿼리 의미 검증 | 3건 통과 | MySQL dialect의 Entity 메타데이터로 JPQL 파싱·의미 검증. DB에 실제 SQL을 실행한 검증은 아님. |
| 클라이언트 | 28개 통과 | JS 구문, 테마 기본/저장/시스템/저장소 차단, 내부 복귀 주소, 리뷰 스크롤 복원. |
| 정식 Gradle test / MySQL 통합 / 라이브 TMDB | 실행 불가·미검증 | 환경의 외부 네트워크 제한으로 Gradle 배포판을 받을 수 없었습니다. 실제 DB에는 접속하지 않았습니다. |
| 브라우저 픽셀·터치·실사용 검증 | 미실행 | 실행 가능한 브라우저가 없어 실제 화면 캡처·반응형 픽셀 검증은 못 했습니다. CSS/템플릿/테마 동작 검증과 구분합니다. |

테스트 코드는 `src/test/java/com/yse/dev/verification/`에 포함했습니다. 프로젝트 폴더에서 Windows는 `gradlew.bat test`, macOS/Linux는 `./gradlew test`를 실행합니다. Node가 있다면 `node tools/verify-client.cjs`도 실행할 수 있습니다. 테스트용 샘플은 실제 API·DB를 대체하지 않습니다.

## 기능별 변경사항

아래 Java 경로는 `src/main/java/com/yse/dev/`, HTML은 `src/main/resources/templates/`, JS/CSS는 `src/main/resources/static/` 아래입니다. 공통 스타일·템플릿 변경은 여러 기능에 적용됩니다.

### 1번 - 구현 완료: 아이디 찾기 / 비밀번호 찾기

**수정·추가 파일:** Controller/AccountController.java, AccountPageController.java, MemberController.java / Service/MemberService.java, RecoveryRules.java, RecoveryLimiter.java / account-recovery.html, login.html / account-management.js

**동작 방식:** 이름+정규화한 이메일로 일치하는 회원의 아이디를 마스킹하여 표시합니다. 불일치는 안내문을 표시합니다. 비밀번호는 아이디 → 선택한 보안 질문 → 답변 확인 → 새 비밀번호/확인 순서입니다. 5분 유효·1회 사용 세션 권한과 기존 비밀번호 해시 일치 여부를 검증합니다. 답변은 NFKC 정규화·앞뒤 공백 제거·영문 소문자화 후 SHA-256+BCrypt로 저장하며, 새 비밀번호는 기존 BCrypt 방식을 그대로 사용합니다. 15분 동안 IP 30회·아이디 10회의 복구 요청 제한을 적용합니다.

**테스트 방법:** 가입한 정보로 아이디 찾기, 불일치, 오답, 인증 없이 재설정, 만료된 인증, 재설정 권한 재사용을 확인합니다.

### 2번 - 구현 완료: 회원가입 복구 정보와 이메일 중복확인

**수정·추가 파일:** Entity/Member.java / DTO/MemberDto.java / Repository/MemberRepository.java / Service/MemberService.java, RecoveryRules.java / signup.html / account.js, account-management.js

**동작 방식:** 신규 가입에는 이름·이메일·선택형 보안 질문·답변이 필수입니다. 아이디·닉네임 중복확인은 유지하고 이메일 중복확인을 추가했습니다. 이메일은 앞뒤 공백을 제거하고 소문자로 저장하며 서버 검증과 DB unique 제약을 함께 적용합니다. 기존 회원의 새 컬럼은 NULL을 허용하므로 기존 로그인은 유지됩니다.

**테스트 방법:** 신규 가입 성공, 빈 이름/이메일/질문/답변, 아이디·닉네임·이메일 중복, 중복확인 후 입력값 변경을 확인합니다.

### 3번 - 구현 완료: 개인정보 / 비밀번호 / 회원탈퇴 분리

**수정·추가 파일:** DTO/AccountDto.java / Controller/AccountController.java / Service/MemberService.java / profile.html, mypage.html / account-management.js, account.js

**동작 방식:** 기존 /profile URL을 회원정보 관리 화면으로 재사용합니다. 개인정보 저장과 비밀번호 변경은 별도 폼·API입니다. 닉네임만 수정할 때 비밀번호를 요구하지 않습니다. 이메일·복구 답변 변경에는 현재 비밀번호가 필요합니다. 비밀번호 변경은 현재 비밀번호와 새 비밀번호 확인을 검증합니다. 탈퇴는 기존 비밀번호 확인·회원 소유 찜/리뷰 삭제 기능을 유지합니다.

**테스트 방법:** 닉네임만 수정 후 비밀번호로 계속 로그인되는지, 현재 비밀번호 오답, 새 비밀번호 불일치, 탈퇴 후 관련 데이터 삭제를 확인합니다.

### 4번 - 구현 완료: 로그인 성공 안내 제거 / 이전 화면 복귀

**수정·추가 파일:** Controller/MemberController.java / account.js, ml-core.js

**동작 방식:** 로그인 성공 응답의 안내 문구를 비웠으며 성공 팝업 없이 기존 returnUrl로 돌아갑니다. 실패 안내는 로그인 폼 안에 표시합니다. 복귀 주소는 같은 사이트의 절대 경로만 허용하고 외부 주소·이중 슬래시·역슬래시를 차단했습니다.

**테스트 방법:** 영화 상세/목록/마이페이지 진입 직전 로그인 후 복귀, 로그인 오답의 빨간 안내, 외부 returnUrl 차단을 확인합니다.

### 5번 - 구현 완료: 대형 화면 여백 / 카드 크기

**수정·추가 파일:** theme.css

**동작 방식:** 기존 .shell과 헤더 구조를 유지하면서 최대 폭을 1760px로 확대하고 화면에 따른 좌우 여백을 적용했습니다. 카드 그리드는 폭에 맞춰 열 수를 늘리며 카드 최대 폭은 250px로 제한합니다. 모바일은 2열, 중간 화면은 3열을 유지합니다.

**테스트 방법:** 노트북 1366px, 데스크톱 1920px, 큰 모니터 2560px, 모바일 390px에서 가로 넘침·포스터 비율을 확인합니다.

### 6번 - 구현 완료: TMDB 별점 필터 세분화

**수정·추가 파일:** DTO/CatalogFilter.java / Service/CatalogService.java, CatalogModel.java / fragments/catalog-filters.html, movie-list.html, country-movies.html

**동작 방식:** TMDB vote_average는 기존과 동일하게 10점 기준입니다. 전체·5점 미만·6/7/8/9점 이상을 제공합니다. 5점 미만은 vote_average.lte=4.999와 1표 이상 조건을 사용해 무투표 0점을 제외합니다. 추천순의 200표 조건은 유지합니다. 기존 minRating URL도 지원합니다.

**테스트 방법:** 5.0점 영화가 5점 미만에 포함되지 않는지, 8점 이상과 장르·연도·OTT 복합 필터를 확인합니다.

### 7번 - 구현 완료: 오래된 개봉 연도 검색

**수정·추가 파일:** DTO/CatalogFilter.java / Service/CatalogService.java, CatalogModel.java / fragments/catalog-filters.html

**동작 방식:** 1870년부터 현재 연도+5까지 시작·종료 연도를 입력할 수 있습니다. 비어 있는 쪽은 제한을 두지 않습니다. 순서를 거꾸로 입력하면 정렬하여 적용합니다. 기존 year URL은 유지하며 단일 연도 범위로 표시합니다.

**테스트 방법:** 1930~1979, 종료연도만 1959, 시작=종료, 거꾸로 입력, 기존 year 링크를 확인합니다.

### 8번 - 구현 완료: 실제 KR OTT 제공처 확대

**수정·추가 파일:** Service/MovieService.java / Controller/CatalogAdvice.java, HomeController.java / ott.html, fragments/catalog-filters.html / catalog.js

**동작 방식:** 화면에 고정한 5개 버튼을 /watch/providers/movie?watch_region=KR 응답 목록으로 교체했습니다. TMDB에서 실제 반환한 이름·ID만 표시합니다. 특정 서비스가 응답에 없으면 임의로 추가하지 않습니다. 제공 범위는 구독·무료·광고·대여·구매를 포함하고 화면에 이를 표기합니다. 영화별 KR 제공 정보로 목록을 조회합니다. 기존 /api/ott-movies 배열 응답은 유지하고, 프런트에는 다음 페이지 여부를 전달하는 /api/ott-page를 추가했습니다.

**테스트 방법:** KR 응답에 실제 있는 제공처만 표시되는지, 홈에서 선택한 OTT의 전체 보기 연결, 더 보기, API 장애 시 오류 안내를 확인합니다.

### 9번 - 구현 완료: 국내·해외 최신순 / 인기순 / 추천순

**수정·추가 파일:** Controller/TeamPageController.java / Service/CatalogService.java, CatalogModel.java / DTO/CatalogFilter.java / country-movies.html, fragments/catalog-filters.html

**동작 방식:** 선택 순서는 최신순 → 인기순 → 추천순이고 국내·해외 기본값은 최신순입니다. 최신순은 TMDB primary_release_date.desc이며 미래 개봉작을 제외합니다. 인기순은 popularity.desc, 추천순은 200표 이상인 영화의 vote_average.desc입니다. 지역별 서비스 평점과 TMDB 평점을 임의로 혼합하지 않았습니다. 국가·필터·정렬은 페이지 이동에도 유지됩니다.

**테스트 방법:** 국내/해외 첫 진입 기본값, 정렬 전환, 국가와 연도 필터를 유지한 다음 페이지 이동을 확인합니다.

### 10번 - 구현 완료: 성인 필터 의미 분리

**수정·추가 파일:** Service/KoreanCertification.java, CatalogService.java, MovieService.java / DTO/CatalogFilter.java / fragments/catalog-filters.html

**동작 방식:** 한국 청소년 관람불가(19)와 TMDB 성인 콘텐츠를 별도 항목으로 제공합니다. KR release_dates의 18/19만 한국 19로 정규화합니다. TMDB adult 속성은 한국 관람등급으로 추정하지 않습니다. 한국 등급의 실제 API 코드는 /certification/movie/list의 KR 목록에서 조회합니다. include_adult는 포함 허용이며 성인 전용 검색이 아니므로 TMDB 성인 항목은 응답의 adult=true를 다시 걸러 표시합니다.

**테스트 방법:** KR 19 필터와 TMDB 성인 필터의 결과가 같은 의미로 표시되지 않는지, 일반 목록에서 제외되는지 확인합니다.

### 11번 - 구현 완료: 빨간 원형 19 배지

**수정·추가 파일:** DTO/MovieDto.java, MovieDetailDto.java, PersonDetailDto.java / Service/KoreanCertification.java, MovieService.java, PersonService.java / fragments/layout.html, movie-detail.html, person-detail.html, index.html / ml-core.js, experience.js, theme.css

**동작 방식:** 한국 등급을 실제로 18/19로 확인한 영화에만 빨간 원·흰색 19 배지를 표시합니다. 공통 영화 카드·상세 포스터·인물 필모그래피·홈의 개인 찜 카드에 반영했습니다. TMDB adult=true라는 이유만으로 한국 19 표시를 붙이지 않습니다. 제한상영가는 별도 등급으로 처리합니다.

**테스트 방법:** 일반 영화에는 19가 없는지, KR 18/19 영화의 상세와 카드에서 표시되는지, 찜 자동 전환 때 배지가 함께 바뀌는지 확인합니다.

### 12번 - 구현 완료: 기본 인기·추천의 성인 콘텐츠 제외

**수정·추가 파일:** Service/MovieService.java, CatalogModel.java, KoreanCertification.java / Controller/RecommendationController.java / movie-recommend.html, recommendations.js

**동작 방식:** 인기·홈·OTT·자동 추천에서는 TMDB adult=true, KR 19/제한상영가, KR 등급 미확인 작품을 제외합니다. 기본 영화 탐색도 미확인 등급을 제외하며, 사용자는 등급 정보 없음 항목을 명시적으로 선택해 찾을 수 있습니다. 성인 작품은 명시적인 성인 필터로 조회합니다. 사용자가 이미 저장한 개인 찜·리뷰·직접 상세 URL은 보존합니다. 업로드된 추천 화면은 고정 예시였으므로 장르·기분·평점 선택을 실제 TMDB Discover API에 연결했습니다.

**테스트 방법:** 기본 인기·추천에 성인/미확인 등급이 없는지, 명시적 필터로 조회되는지, 기존 찜·리뷰가 삭제되지 않는지 확인합니다.

### 13번 - 구현 완료: 영화·배우 통합 검색 / 인물 상세

**수정·추가 파일:** Controller/MovieController.java, PersonController.java / Service/PersonService.java, MovieService.java / DTO/PersonDetailDto.java / movie-list.html, person-detail.html, fragments/layout.html

**동작 방식:** 상단 검색어로 영화 검색과 Person 검색을 각각 호출하여 영화와 배우·감독 결과를 구분합니다. 인물 클릭은 기존 /people/{id}를 사용합니다. 한국어 인물 정보와 영어 응답의 이름을 조회하며, 한국어 소개가 없으면 영어 소개를 사용합니다. 영화·인물 검색 페이지 수를 함께 고려합니다.

**테스트 방법:** 배우 이름 검색, 인물만 결과가 있는 검색, 프로필/영문 이름/정보 없음 처리, 필모그래피 포스터의 영화 상세 링크를 확인합니다.

### 14번 - 구현 완료: 감독 클릭 / 감독 필모그래피

**수정·추가 파일:** Service/MovieService.java, PersonService.java / Controller/PersonController.java / DTO/MovieDetailDto.java, PersonDetailDto.java / movie-detail.html, person-detail.html

**동작 방식:** Credits crew의 job=Director를 출연진 앞에 표시합니다. 여러 감독도 각각 노출합니다. 감독 링크는 /people/{id}?role=director로 이동합니다. 인물 페이지의 출연작·감독작 선택은 동일한 Person 서비스와 화면을 재사용합니다. 필모그래피는 개봉일 최신순, 24개 원본 항목 단위로 페이지를 이동합니다.

**테스트 방법:** 공동 감독, 감독 정보 없음, 인물의 출연작↔감독작 전환, 다음 페이지와 영화 상세 이동을 확인합니다.

### 15번 - 구현 완료: 출연진 확대 / 가로 스크롤

**수정·추가 파일:** Service/MovieService.java / movie-detail.html / experience.js, theme.css

**동작 방식:** Credits 출연진 제한을 8명에서 최대 60명으로 확대했습니다. 기존 인물 카드 디자인을 유지하면서 가로 스크롤·마우스 드래그·좌우 화살표 키·모바일 터치를 지원합니다.

**테스트 방법:** 출연진이 많은 영화에서 오른쪽 스크롤, 드래그 중 잘못 클릭되지 않는지, 드래그하지 않은 정상 인물 클릭을 확인합니다.

### 16번 - 구현 완료: 한국 관람등급 필터

**수정·추가 파일:** Service/KoreanCertification.java, MovieService.java, CatalogService.java / fragments/catalog-filters.html

**동작 방식:** 전체(성인 제외)·전체관람가·12·15·19·정보 없음 항목을 제공합니다. 12/15는 해당 등급을 뜻하며 최소 연령 이상인 모든 등급을 합산하는 조건이 아닙니다. KR 개봉정보에 등급이 여러 개이면 가장 엄격한 알려진 등급을 사용합니다. 정보가 없거나 조회에 실패하면 UNKNOWN으로 처리하여 전체관람가로 오인하지 않습니다.

**테스트 방법:** 미국 R 등급을 한국 19로 변환하지 않는지, KR 여러 등급 중 상위 등급이 선택되는지, 미확인 필터를 확인합니다.

### 17번 - 구현 완료: 홈 개인 찜 영화 자동 전환

**수정·추가 파일:** Controller/HomeController.java / Service/MovieService.java / index.html / experience.js

**동작 방식:** 로그인 사용자의 실제 Favorite 데이터를 읽어 7초마다 다음 작품을 표시합니다. 1개이면 고정하고 비어 있으면 기존 안내를 유지합니다. 다음·자동 전환 정지 버튼을 제공합니다. 탭이 숨겨졌거나 카드에 마우스/키보드 초점이 있으면 자동 이동을 멈춥니다. 동작 줄이기 설정에서는 자동 전환을 정지한 상태로 시작합니다. 네트워크 오류 시 마지막 정상 카드를 유지합니다.

**테스트 방법:** 찜 0/1/여러 개, 정지/다음, 비로그인, 찜 취소, 19 배지 전환을 확인합니다.

### 18번 - 구현 완료: 리뷰 수정 후 시작 화면 복귀

**수정·추가 파일:** reviews.js, account.js, ml-core.js, design.js

**동작 방식:** 마이페이지·내 리뷰·상세에서 수정 링크를 클릭할 때 원래 경로와 스크롤 위치를 기록합니다. 완료 후 검증된 내부 returnUrl로 돌아가며 마이페이지의 최근 리뷰 탭을 선택하고 저장한 스크롤을 복원합니다. 직접 편집 URL로 들어왔으면 기존 영화 상세 리뷰 영역으로 돌아갑니다.

**테스트 방법:** 마이페이지 최근 리뷰 → 수정 → 저장, 내 리뷰 목록 → 수정, 상세 → 수정, 외부 returnUrl 차단을 확인합니다.

### 19번 - 구현 완료: 관리자 최신순 기본 정렬

**수정·추가 파일:** Controller/AdminController.java / Service/AdminAccountInitializer.java / admin.html, theme.css

**동작 방식:** 회원은 생성일 컬럼이 없어 ID 내림차순을 사용합니다. 리뷰·찜은 createdAt 내림차순, 동률이면 ID 내림차순입니다. 기존 관리자 view 분기와 조회·삭제·권한 검사를 유지했습니다. 기존 관리자 비밀번호를 서버 시작 때 기본값으로 재설정하던 초기화 코드도 수정하여 최초 계정 생성 때만 비밀번호를 설정합니다.

**테스트 방법:** 관리자 네 메뉴가 각각 다른 목록인지, 최신 항목 우선, 삭제 후 해당 목록 복귀, 일반 회원 접근 제한, 재시작 후 관리자 변경 비밀번호 유지를 확인합니다.

### 20번 - 구현 완료: 전체 라이트 / 다크 / 시스템 테마

**수정·추가 파일:** theme.css, theme.js / fragments/layout.html, admin.html / 관련 템플릿 캐시 버전

**동작 방식:** 최초 기본값은 흰색·보라 라이트입니다. 헤더의 작은 ◐ 선택창에서 시스템·라이트·다크를 고릅니다. 선택값을 localStorage의 movielife-theme에 저장하며 시스템 모드에서는 prefers-color-scheme 변경을 즉시 반영합니다. 로그인·회원가입·목록·상세·인물·OTT·마이페이지·관리자에 공통 토큰을 적용했습니다. localStorage 사용이 막혀도 현재 화면의 테마 선택은 작동합니다.

**테스트 방법:** 첫 방문 라이트, 다크 선택 후 새로고침/재접속, 시스템 모드에서 OS 테마 변경, 관리자 이동, 모바일 헤더·폼 대비를 확인합니다.

### 21번 - 구현 완료: 추가 요청: 회원 리뷰·별점 10점 만점

**수정·추가 파일:** Entity/Review.java / Repository/ReviewRepository.java / Service/ReviewService.java, ReviewRatingMigration.java / review.html, movie-detail.html, mypage.html, admin.html / reviews.js, account.js, theme.css

**동작 방식:** 리뷰 작성·수정은 1~10점 정수입니다. 회원 평균·마이페이지·관리자 표시도 10점 기준으로 맞췄습니다. review.rating_scale을 추가하고 기존 rating_scale=NULL인 1~5점 행만 rating×2, rating_scale=10으로 한 번에 변경합니다. 재시작해도 10점 표시가 있는 행은 다시 환산하지 않습니다. 새 리뷰는 처음부터 rating_scale=10입니다. 기존 내용·작성일·수정일·ID는 유지합니다. 6~10점인데 배율 표시가 없는 등 해석이 모호한 데이터가 발견되면 자동 추정하지 않고 시작을 중단합니다.

**테스트 방법:** 기존 1/4/5점이 2/8/10점으로 환산되는지, 두 번째 재시작 때 값이 유지되는지, 새 1/9/10점 저장과 0/11점 거절, 8점·10점 평균이 9점인지 확인합니다.

## DB 변경과 데이터 보존

| 테이블 | 새 컬럼 | 타입·제약 | 기존 데이터 |
|---|---|---|---|
| member | name | varchar(80), NULL 허용 | 자동으로 임의 이름을 채우지 않습니다. |
| member | email | varchar(254), UNIQUE, NULL 허용 | 기존 행은 NULL. 신규 가입 필수, 저장 시 소문자 정규화. |
| member | security_question | varchar(30), NULL 허용 | 질문 코드(school/pet/place/nickname/teacher). 기존 행은 NULL. |
| member | security_answer_hash | varchar(100), NULL 허용 | 답변 해시. 원문을 저장하거나 API 응답으로 보내지 않습니다. |
| review | rating_scale | integer, NULL 허용 | 기존 NULL 행은 최초 실행 시 5점→10점 환산 후 10. 새 행은 10. |

`member.password`, `review.rating`의 기존 타입과 테이블 이름·기본 키·찜/리뷰 연결 키는 유지했습니다. 회원 생성일 컬럼은 추가하지 않았습니다. 기존 `spring.jpa.hibernate.ddl-auto=update` 설정에 따라 컬럼을 추가하고, `ReviewRatingMigration`이 트랜잭션 안에서 환산합니다. 이 문서의 SQL을 별도로 중복 실행할 필요는 없습니다.

**리뷰 환산:** 기존 1→2, 2→4, 3→6, 4→8, 5→10. 내용·ID·작성일·수정일은 그대로입니다. 서버 재시작 후에도 점수는 유지됩니다. 기존 프로젝트가 원래 1~5점 정수라는 것을 소스에서 확인한 뒤 적용했습니다. 이미 별도 방식으로 10점 데이터가 섞여 있다면 최초 실행 전에 데이터를 확인해야 합니다.

운영 서버에서 구버전과 신버전을 동시에 실행하지 마세요. 롤백은 소스만 이전 버전으로 바꾸지 말고 백업한 DB와 함께 진행해야 합니다. 새 10점 리뷰에는 홀수 점수도 있으므로 일괄 나누기 2로 되돌리면 정보가 손실됩니다.

최초 실행 후 MySQL에서 다음 읽기 전용 조회로 확인할 수 있습니다.

```sql
SELECT COUNT(*) AS total_reviews,
       SUM(rating_scale = 10) AS ten_point_reviews,
       MIN(rating) AS min_rating, MAX(rating) AS max_rating
FROM review;

SELECT id, rating, rating_scale
FROM review
WHERE rating_scale IS NULL OR rating_scale <> 10
   OR rating NOT BETWEEN 1 AND 10;
```

두 번째 조회 결과는 0행이어야 합니다. DB schema 권한이 없거나 `ddl-auto=validate/none`으로 바꾼 환경에서는 위 컬럼·이메일 고유 인덱스를 DBA가 먼저 추가해야 합니다.

## TMDB와 우리 DB의 역할

| 담당 | 처리 내용 |
|---|---|
| MySQL/JPA | 회원·이름·이메일·복구 답변 해시·로그인 비밀번호·찜·회원 리뷰·10점 별점·평균·관리자 데이터 |
| TMDB | 영화 제목/포스터/정보/평점·인기도·개봉일·장르·국가·KR 관람등급·OTT 제공 정보·배우/감독/필모그래피 |
| 브라우저 | 테마 선택 저장·7초 찜 전환·리뷰 복귀 스크롤·출연진 드래그 |

## API 제약과 대체 처리

- **KR 관람등급의 누락:** TMDB는 모든 영화의 한국 등급을 보장하지 않습니다. 없는 정보를 성인 또는 전체관람가로 추정하지 않습니다. 자동 영역과 기본 탐색에서는 미확인 작품을 제외하고, 탐색의 “한국 등급 정보 없음”을 직접 선택하면 별도로 조회할 수 있습니다. 직접 선택한 인물 필모그래피·개인 찜은 저장/열람 기능을 유지합니다.
- **서로 다른 개봉본 등급:** 한국 등급이 여러 개면 가장 높은 알려진 등급을 사용합니다. 현재 상영 중인 특정 판본의 공식 심의 정보와 항상 같다고 단정하지 않습니다.
- **성인 전용 API 없음:** `include_adult=true`는 성인을 포함하도록 허용하는 옵션입니다. 해당 필터를 선택한 경우 결과의 adult 값을 다시 확인합니다. 한국 19세 필터는 별도로 KR release_dates를 검증합니다. [TMDB Discover 문서](https://developer.themoviedb.org/reference/discover-movie), [Release Dates 문서](https://developer.themoviedb.org/reference/movie-release-dates), [Certification 목록](https://developer.themoviedb.org/reference/certification-movie-list).
- **페이지가 적게 보이거나 비는 경우:** 등급 확인은 TMDB 페이지를 받은 후 적용합니다. 따라서 원본 총 검색 건수/페이지 수와 실제 표시 수가 다를 수 있습니다. 빈 페이지여도 다음 페이지로 이동할 수 있습니다. 인물·성인 전용 결과도 같은 제한을 안내합니다.
- **제목 검색 + 필터:** TMDB Search는 Discover의 복합 필터/정렬을 그대로 제공하지 않습니다. 제목 검색은 TMDB 관련도순을 유지하고 영화 결과에 선택한 장르·점수·연도·제공처·관람등급을 현재 페이지 단위로 적용합니다. 인물 검색 결과에는 영화 필터를 적용하지 않습니다.
- **OTT 제공처:** 실제 KR 목록을 런타임에 조회하므로 서비스의 등장/누락은 TMDB 데이터에 따라 달라집니다. 이번 환경에서 Coupang Play 등의 현재 등재 여부를 라이브 확인한 것은 아닙니다. 구독과 대여/구매가 섞여 있으므로 화면에 제공 유형 범위를 명시했습니다. [TMDB Watch Providers 문서](https://developer.themoviedb.org/reference/watch-providers-movie-list).
- **인물 정보:** 한국어 소개가 없으면 영어 소개를 사용하고 그것도 없으면 정보 없음으로 표시합니다. 인물 이름은 영어 응답을 따로 확인합니다. [TMDB Person 문서](https://developer.themoviedb.org/reference/person-details), [Person Movie Credits](https://developer.themoviedb.org/reference/person-movie-credits).
- **추천 기준:** 팀 DB의 별점은 평가 건수가 적고 TMDB와 모집단이 달라 숫자를 임의로 합치지 않았습니다. 추천순은 TMDB 평점+최소 투표 200개 조건입니다. 기분 선택은 해당 기분과 연결한 장르를 추가하는 방식이며 AI 추천으로 표시하지 않습니다.
- **복구 제한 저장:** 요청 횟수 제한과 복구 인증은 현재 단일 서버 메모리·HttpSession 기준입니다. 서버 재시작 시 횟수 제한은 초기화됩니다. 다중 서버 환경에서는 공유 저장소가 필요합니다. 비밀번호 재설정은 해당 복구 세션을 종료하며, 다른 브라우저의 기존 로그인 세션을 모두 찾아 종료하는 기능은 추가하지 않았습니다.

## 단계별 실사용 확인 순서

1. 계정: 기존 로그인/로그아웃 → 신규 가입/중복 → ID 찾기 → 비밀번호 복구 → 회원정보/비밀번호/탈퇴.
2. 탐색: 5점 미만/9점 이상 → 1980년 이전 → KR 등급과 정보 없음 → 복합 필터/페이지 이동.
3. 국가·인기: 국내·해외 최신순 → 인기순/추천순 → 성인 제외 → 명시적 성인 필터.
4. 인물: 제목/배우 검색 → 인물 상세 → 감독작/출연작 → 출연진 스크롤 → 영화 상세.
5. 홈·리뷰: 찜 0/1/여러 개 → 정지/다음 → 리뷰 수정 복귀/스크롤 → 찜·리뷰 CRUD.
6. 관리자: 대시보드/회원/리뷰/찜 각각 진입 → 최신순 → 삭제·권한 → 재시작 후 비밀번호 보존.
7. 테마·반응형: 시스템/라이트/다크 → 새로고침/재접속 → 모든 주요 화면 → 390/768/1366/1920/2560px 폭.
8. 10점 전환: DB 백업 → 최초 실행 점수×2 확인 → 재시작 동일성 → 1~10점 작성/수정 → 평균/관리자 표시.

## 수정한 기존 파일

- `README.md`: 기존 안내 유지, 최신 실행 문서 링크 추가.

- `src/main/java/com/yse/dev/Controller/AdminController.java`
- `src/main/java/com/yse/dev/Controller/ApiExceptionHandler.java`
- `src/main/java/com/yse/dev/Controller/HomeController.java`
- `src/main/java/com/yse/dev/Controller/MemberController.java`
- `src/main/java/com/yse/dev/Controller/MovieController.java`
- `src/main/java/com/yse/dev/Controller/PersonController.java`
- `src/main/java/com/yse/dev/Controller/TeamPageController.java`
- `src/main/java/com/yse/dev/DTO/MemberDto.java`
- `src/main/java/com/yse/dev/DTO/MovieDetailDto.java`
- `src/main/java/com/yse/dev/DTO/MovieDto.java`
- `src/main/java/com/yse/dev/DTO/PersonDetailDto.java`
- `src/main/java/com/yse/dev/Entity/Member.java`
- `src/main/java/com/yse/dev/Entity/Review.java`
- `src/main/java/com/yse/dev/Repository/MemberRepository.java`
- `src/main/java/com/yse/dev/Repository/ReviewRepository.java`
- `src/main/java/com/yse/dev/Service/AdminAccountInitializer.java`
- `src/main/java/com/yse/dev/Service/MemberService.java`
- `src/main/java/com/yse/dev/Service/MovieService.java`
- `src/main/java/com/yse/dev/Service/PersonService.java`
- `src/main/java/com/yse/dev/Service/ReviewService.java`
- `src/main/resources/static/js/account.js`
- `src/main/resources/static/js/catalog.js`
- `src/main/resources/static/js/design.js`
- `src/main/resources/static/js/ml-core.js`
- `src/main/resources/static/js/reviews.js`
- `src/main/resources/templates/admin.html`
- `src/main/resources/templates/country-movie.html`
- `src/main/resources/templates/country-movies.html`
- `src/main/resources/templates/error.html`
- `src/main/resources/templates/favorite-movies.html`
- `src/main/resources/templates/fragments/layout.html`
- `src/main/resources/templates/index.html`
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/movie-detail.html`
- `src/main/resources/templates/movie-list.html`
- `src/main/resources/templates/movie-recommend.html`
- `src/main/resources/templates/my-reviews.html`
- `src/main/resources/templates/mypage.html`
- `src/main/resources/templates/ott.html`
- `src/main/resources/templates/person-detail.html`
- `src/main/resources/templates/popular.html`
- `src/main/resources/templates/profile.html`
- `src/main/resources/templates/review.html`
- `src/main/resources/templates/signup.html`
- `src/main/resources/templates/team-placeholder.html`

## 새로 만든 파일

- `src/main/java/com/yse/dev/Controller/AccountController.java`
- `src/main/java/com/yse/dev/Controller/AccountPageController.java`
- `src/main/java/com/yse/dev/Controller/CatalogAdvice.java`
- `src/main/java/com/yse/dev/Controller/RecommendationController.java`
- `src/main/java/com/yse/dev/DTO/AccountDto.java`
- `src/main/java/com/yse/dev/DTO/CatalogFilter.java`
- `src/main/java/com/yse/dev/Service/CatalogModel.java`
- `src/main/java/com/yse/dev/Service/CatalogService.java`
- `src/main/java/com/yse/dev/Service/KoreanCertification.java`
- `src/main/java/com/yse/dev/Service/RecoveryLimiter.java`
- `src/main/java/com/yse/dev/Service/RecoveryRules.java`
- `src/main/java/com/yse/dev/Service/ReviewRatingMigration.java`
- `src/main/resources/static/css/theme.css`
- `src/main/resources/static/js/account-management.js`
- `src/main/resources/static/js/experience.js`
- `src/main/resources/static/js/recommendations.js`
- `src/main/resources/static/js/theme.js`
- `src/main/resources/templates/account-recovery.html`
- `src/main/resources/templates/fragments/catalog-filters.html`
- `src/test/java/com/yse/dev/verification/QueryCheck.java`
- `src/test/java/com/yse/dev/verification/TemplateAndQueryTest.java`
- `src/test/java/com/yse/dev/verification/TemplateCheck.java`
- `src/test/java/com/yse/dev/verification/UpgradeVerification.java`
- `src/test/java/com/yse/dev/verification/UpgradeVerificationTest.java`
- `tools/verify-client.cjs`
- `docs/CHANGELOG_KO.md`, `docs/CHANGELOG_KO.html`: 이 변경사항 문서.
- `README_UPGRADE_KO.md`: 실행·DB 환산 안내.

이전 ZIP 안의 과거 디자인 미리보기·중복 배포본·컴파일 캐시·예전 실행 JAR은 신규 ZIP에 넣지 않았습니다. 실제 프로젝트 소스·리소스·Gradle Wrapper·기존 README·설정은 유지했습니다. 새 소스로 빌드한 결과를 사용해야 하므로 구버전 JAR을 함께 제공하지 않습니다.
