> 최신: 로딩 안정화·성인/19등급 제외·OTT 링크·TMDB 속도·배우 검색·레드/블랙 테마 수정은 docs/STABILIZE_0917_KO.md를 먼저 확인하세요.

> 최신 5열/소개/로그인 링크 수정: docs/FIVE_COLUMNS_KO.md를 먼저 확인하세요.

> 최신 추가 8개 수정은 docs/REFINE_0917_KO.md를 먼저 확인하세요.

> 최신 수정본: docs/FIX_0917_KO.md의 5가지 수정 및 적용 안내를 먼저 확인하세요. Project Clean 후 서버를 재시작해 주세요.

> 최신판: 보라색 라이트/다크 + 팀원 추천 복수 선택·회원 평균 통합. 비교 문서: docs/RECOMMEND_MERGE_KO.md. 추천 검사 23개, 추천 JS 검사 13개를 추가로 통과했습니다.

# movieLife 수정본 실행 안내

이 프로젝트는 최신 업로드 소스를 바탕으로 1~20번과 추가 요청한 리뷰 10점 만점을 반영했습니다.

1. 기존 서버를 종료하고 MySQL DB를 백업합니다. 최초 실행 시 기존 리뷰 점수가 5점에서 10점으로 한 번 환산됩니다.
2. STS에서 이 폴더(`build.gradle`이 있는 `movielife`)를 Existing Gradle Project로 가져옵니다.
3. Java 17을 선택하고 Gradle Refresh를 합니다. 기존 application.properties 설정은 보존했습니다.
4. MovieApplication을 Spring Boot App으로 실행합니다.
5. http://localhost:8080 을 엽니다. HTML 더블클릭으로 실행하는 정적 모형이 아닙니다.

- 모든 변경/새 파일, 1~21번 동작과 테스트, DB 컬럼: `docs/CHANGELOG_KO.html`을 브라우저로 여세요.
- 기존 회원도 로그인 가능하며 회원정보 관리(`/profile`)에서 복구 정보를 등록할 수 있습니다.
- 관리자 변경 비밀번호를 재시작 때 덮어쓰지 않습니다.
- 헤더의 ◐ 선택창: 시스템 / 라이트 / 다크. 기본은 라이트입니다. 색상은 강조 #D92234 · 검정 #000000 기준입니다.
- 신규 리뷰는 1~10점 정수. 기존 4점은 8점, 5점은 10점으로 환산합니다.

## 테스트

Windows: `gradlew.bat test`

macOS/Linux: `./gradlew test`

Node 사용 가능 시: `node tools/verify-client.cjs`

작업 환경에서는 Gradle 다운로드 제한으로 정식 Gradle test를 완료하지 못했습니다. 첨부 의존성으로 소스 호환 컴파일, 서비스 53개, 클라이언트 28개, Thymeleaf 24건, JPA 쿼리 4건 검증을 수행했습니다. 실제 MySQL/TMDB 연결 및 브라우저 실사용 검증은 별도 필요합니다. 상세 범위는 변경사항 문서를 확인하세요.
