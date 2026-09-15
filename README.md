# movieLife

Spring Boot / Thymeleaf / MySQL 기반 영화 검색·찜·리뷰 서비스입니다.

적용 방법: [먼저읽어주세요.txt](먼저읽어주세요.txt)  
변경 내역: [docs/CHANGES.md](docs/CHANGES.md)  
디자인 미리보기: [docs/preview-home.html](docs/preview-home.html)

Java 17을 사용합니다. Gradle 및 Spring Boot 버전은 전달받은 프로젝트 설정을 유지했습니다.
DB와 TMDB 설정은 `src/main/resources/application.properties`에서 확인합니다.

```shell
# Windows
gradlew.bat test
gradlew.bat bootRun

# Mac / Linux
bash gradlew test
bash gradlew bootRun
```

실행 후 `http://localhost:8080`에 접속합니다.
