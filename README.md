# movieLife

Spring Boot / Thymeleaf / MySQL 기반 영화 검색 · 찜 · 별점 · 리뷰 웹 서비스입니다.

## 주요 기능

- 회원가입 / 로그인 / 로그아웃
- 회원정보 수정 / 회원탈퇴
- 영화 검색
- 인기 영화 조회
- 평점 높은 영화 조회
- 개봉 예정 영화 조회
- 국내 / 해외 영화 조회
- 영화 상세정보 조회
- OTT 제공 정보 조회
- 영화 찜 등록 / 취소
- 별점 등록 / 수정
- 리뷰 작성 / 수정 / 삭제
- 마이페이지
- 내가 찜한 영화 조회
- 내가 작성한 리뷰 조회

## 사용 기술

- Java 17
- Spring Boot
- Spring Data JPA
- Thymeleaf
- MySQL
- HTML
- CSS
- JavaScript
- TMDB API

## 프로젝트 구조

```text
src
├─ main
│  ├─ java
│  │  └─ com.yse.dev
│  │     ├─ Controller
│  │     ├─ DTO
│  │     ├─ Entity
│  │     ├─ Repository
│  │     └─ Service
│  │
│  └─ resources
│     ├─ static
│     │  ├─ css
│     │  └─ js
│     ├─ templates
│     └─ application.properties
│
└─ test
