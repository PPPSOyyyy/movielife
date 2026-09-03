document.addEventListener("DOMContentLoaded", updateAuthHeader);

async function updateAuthHeader() {
    const headerRightList =
        document.querySelectorAll(".header-right");

    if (headerRightList.length === 0) {
        return;
    }

    try {
        const response = await fetch("/api/members/me", {
            headers: {
                "Accept": "application/json"
            }
        });

        // 로그인하지 않았다면 기존 로그인·회원가입 버튼 유지
        if (!response.ok) {
            return;
        }

        const member = await response.json();

        addAuthHeaderStyle();

        headerRightList.forEach(headerRight => {
            headerRight.replaceChildren();
            headerRight.style.display = "flex";

            const nickname = document.createElement("span");
            nickname.className = "auth-nickname";
            nickname.textContent = member.nickname + "님";

            const logoutButton = document.createElement("button");
            logoutButton.type = "button";
            logoutButton.className = "auth-logout-button";
            logoutButton.textContent = "로그아웃";
            logoutButton.addEventListener(
                "click",
                logoutFromHeader
            );

            headerRight.append(
                nickname,
                logoutButton
            );
        });

    } catch (error) {
        console.error(
            "로그인 상태 확인 실패",
            error
        );
    }
}

function addAuthHeaderStyle() {
    if (document.getElementById("auth-header-style")) {
        return;
    }

    const style = document.createElement("style");
    style.id = "auth-header-style";

    style.textContent = `
        .auth-nickname {
            color: #ffffff;
            font-size: 13px;
            font-weight: 600;
            white-space: nowrap;
        }

        .auth-logout-button {
            padding: 9px 18px;
            border: 1px solid #444444;
            border-radius: 5px;
            background: transparent;
            color: #ffffff;
            font: inherit;
            font-size: 12px;
            cursor: pointer;
            transition: 0.2s;
        }

        .auth-logout-button:hover {
            border-color: #D92234;
            color: #D92234;
        }
    `;

    document.head.appendChild(style);
}

async function logoutFromHeader() {
    try {
        const response = await fetch(
            "/api/members/logout",
            {
                method: "POST"
            }
        );

        if (!response.ok) {
            alert("로그아웃 처리 중 오류가 발생했습니다.");
            return;
        }

        alert("로그아웃되었습니다.");
        window.location.href = "/";

    } catch (error) {
        alert("로그아웃 처리 중 오류가 발생했습니다.");
    }
}