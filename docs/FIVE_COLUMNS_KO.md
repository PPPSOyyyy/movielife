# 5열 고정·인물 소개·로그인 하단 링크 수정

1. PC 영화 목록은 정렬/필터와 무관하게 5열로 고정했습니다. 자동 열 수 조정은 제거했습니다. 마지막에 1~4개가 남으면 직전 5개와 함께 마지막 행을 좌우 스크롤 영역으로 구성합니다. 한 번에 5개가 보이고 마지막 위치에서도 실제 마지막 5개가 보여 빈칸을 남기지 않습니다. 예: 82편은 일반 그리드 75편 + 마지막 행 7편이며, 마지막 행에서 이전/다음 영화 버튼으로 모두 볼 수 있습니다. 영화 DOM을 복제하거나 삭제하지 않습니다. 결과 자체가 5편 미만이면 실제 개수만 표시합니다. 모바일 720px 이하에서는 가독성을 위해 2열입니다.

2. 한국어 인물 소개가 없을 때 영어 소개를 자동으로 채우던 처리를 제거했습니다. 한국어 본문이 없으면 '등록된 한국어 인물 소개가 없습니다.'로 안내합니다. 번역되지 않은 영어 정보를 한국어로 만든 것처럼 표시하지 않습니다. 인물 필터 라벨/선택창을 위아래로 분리하고 폭·높이·아래 여백을 확보하여 필모그래피 제목과 구분했습니다.

3. 아이디 찾기·비밀번호 찾기 링크를 로그인 버튼 바로 아래로 옮겼습니다. 기존 복구 URL과 동작을 그대로 사용합니다.

## 수정 파일

- src/main/resources/static/js/balanced-grid.js
- src/main/resources/static/css/theme.css
- src/main/resources/templates/fragments/layout.html
- src/main/resources/templates/admin.html (공통 CSS 캐시 버전)
- src/main/resources/templates/login.html
- src/main/resources/templates/person-detail.html
- src/main/java/com/yse/dev/Service/PersonService.java

## 새 파일

- tools/verify-five-grid.cjs: 0/1/5/7/18/20/82편과 동적 추가 시 원래 순서 및 모든 카드 유지 검사
- docs/FIVE_COLUMNS_KO.md: 이 안내

## 확인

카드 구성 검사 20개 통과. 기존 서비스 53개·추천 23개·클라이언트 30개·Thymeleaf 24건·JPQL 4건 검사를 통과했습니다. Java 소스 호환 컴파일을 완료했습니다(임시 Lombok 확장 사본과 첨부 JAR 의존성 사용). 실제 Gradle 빌드·MySQL/TMDB 연결·브라우저 시각 검증은 이 환경에서 완료하지 못했습니다. 카드 검사는 DOM 동작을 재현한 테스트이며 실제 브라우저 스크린샷 검사는 아닙니다.

전체 ZIP을 새 폴더에 풀어 STS에 가져온 다음 Gradle Refresh / Project Clean / 서버 재시작 / Ctrl+Shift+R 순서로 적용하세요. DB 및 Entity는 이번에 변경하지 않았습니다. 기존 10점 리뷰·회원·추천·찜 기능은 유지합니다.
