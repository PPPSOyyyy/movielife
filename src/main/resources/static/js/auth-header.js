document.addEventListener("DOMContentLoaded", () => {
    updateActiveNav();
    updateAuthHeader();
});


// ==================================================
// 현재 페이지 메뉴 빨간 밑줄 표시
// ==================================================
function updateActiveNav() {

    const path = window.location.pathname;

    let activeNav = "";

    if (path === "/") {
        activeNav = "home";
    }
    else if (
        path === "/movies/domestic"
        || path.startsWith("/movies/domestic/")
    ) {
        activeNav = "domestic";
    }
    else if (
        path === "/movies/foreign"
        || path.startsWith("/movies/foreign/")
    ) {
        activeNav = "foreign";
    }
    else if (
        path === "/movies"
        || (
            path.startsWith("/movies/")
            && !path.startsWith("/movies/domestic")
            && !path.startsWith("/movies/foreign")
        )
    ) {
        activeNav = "movies";
    }
    else if (
        path === "/recommend"
        || path === "/movie-recommend"
    ) {
        activeNav = "recommend";
    }
    else if (path.startsWith("/ott")) {
        activeNav = "ott";
    }
    else if (
        path.startsWith("/mypage")
        || path.startsWith("/profile")
        || path.startsWith("/favorites")
        || path.startsWith("/my-reviews")
    ) {
        activeNav = "mypage";
    }

    document
        .querySelectorAll(".ml-nav a[data-nav]")
        .forEach(link => {

            link.classList.remove("active");

            if (
                link.dataset.nav === activeNav
            ) {
                link.classList.add("active");
            }

        });
}


// ==================================================
// 로그인 상태에 따라 공통 헤더 변경
// ==================================================
async function updateAuthHeader() {

    const authArea =
        document.querySelector(".ml-auth");

    if (!authArea) {
        return;
    }

    try {

        const response =
            await fetch(
                "/api/members/me",
                {
                    method: "GET",
                    credentials: "same-origin",
                    headers: {
                        "Accept": "application/json"
                    }
                }
            );

        // ==========================================
        // 비로그인 상태
        // ==========================================
        if (!response.ok) {

            showGuestHeader(authArea);

            return;
        }

        // ==========================================
        // 로그인 상태
        // ==========================================
        const member =
            await response.json();

        showLoginHeader(
            authArea,
            member
        );

    }
    catch (error) {

        console.error(
            "로그인 상태 확인 실패:",
            error
        );

        showGuestHeader(authArea);
    }
}


// ==================================================
// 비로그인 헤더
// ==================================================
function showGuestHeader(authArea) {

    const currentUrl =
        window.location.pathname
        + window.location.search;

    authArea.innerHTML = `
        <a
            href="/login?returnUrl=${encodeURIComponent(currentUrl)}"
            data-login-link
        >
            로그인
        </a>

        <a
            href="/signup"
            class="signup-link"
        >
            회원가입
        </a>
    `;
}


// ==================================================
// 로그인 헤더
// ==================================================
function showLoginHeader(
    authArea,
    member
) {

    authArea.innerHTML = "";

    // 닉네임
    const nickname =
        document.createElement("span");

    nickname.className =
        "auth-nickname";

    nickname.textContent =
        member.nickname + "님";

    authArea.appendChild(
        nickname
    );


    // 관리자 계정
    if (member.userId === "admin") {

        const adminLink =
            document.createElement("a");

        adminLink.href =
            "/admin";

        adminLink.className =
            "auth-admin-link";

        adminLink.textContent =
            "관리자";

        authArea.appendChild(
            adminLink
        );
    }


    // 로그아웃
    const logoutButton =
        document.createElement("button");

    logoutButton.type =
        "button";

    logoutButton.className =
        "auth-logout-button";

    logoutButton.textContent =
        "로그아웃";

    logoutButton.addEventListener(
        "click",
        logout
    );

    authArea.appendChild(
        logoutButton
    );
}


// ==================================================
// 로그아웃
// 현재 페이지 유지
// ==================================================
async function logout() {

    try {

        const response =
            await fetch(
                "/api/members/logout",
                {
                    method: "POST",
                    credentials: "same-origin"
                }
            );

        if (!response.ok) {

            alert(
                "로그아웃 처리 중 오류가 발생했습니다."
            );

            return;
        }

        window.location.reload();

    }
    catch (error) {

        console.error(
            error
        );

        alert(
            "로그아웃 처리 중 오류가 발생했습니다."
        );
    }
}