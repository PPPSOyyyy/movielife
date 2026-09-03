document.addEventListener(
    "DOMContentLoaded",
    updateAuthHeader
);


async function updateAuthHeader() {

    /*
     * 현재 보고 있는 페이지 주소
     * 로그인 성공 후 다시 이 화면으로 돌아오기 위해 사용
     */

    const currentUrl =
        window.location.pathname
        + window.location.search
        + window.location.hash;


    /*
     * 로그인 버튼에 현재 페이지 주소 넣기
     *
     * 예:
     * /popular
     * ↓
     * /login?returnUrl=/popular
     */

    document
        .querySelectorAll("a.login-button")
        .forEach(loginLink => {

            loginLink.href =
                "/login?returnUrl="
                + encodeURIComponent(currentUrl);

        });


    const headerRightList =
        document.querySelectorAll(
            ".header-right"
        );


    if (headerRightList.length === 0) {

        return;

    }


    try {

        /*
         * 현재 로그인한 회원 확인
         */

        const response = await fetch(
            "/api/members/me",
            {
                headers: {
                    "Accept": "application/json"
                }
            }
        );


        /*
         * 로그인하지 않은 상태라면
         * 기존 로그인 / 회원가입 버튼 그대로 사용
         */

        if (!response.ok) {

            return;

        }


        const member =
            await response.json();


        addAuthHeaderStyle();


        /*
         * 로그인 상태라면
         * 닉네임 + 로그아웃 버튼으로 변경
         */

        headerRightList.forEach(
            headerRight => {

                headerRight.replaceChildren();

                headerRight.style.display =
                    "flex";


                const nickname =
                    document.createElement(
                        "span"
                    );


                nickname.className =
                    "auth-nickname";


                nickname.textContent =
                    member.nickname + "님";


                const logoutButton =
                    document.createElement(
                        "button"
                    );


                logoutButton.type =
                    "button";


                logoutButton.className =
                    "auth-logout-button";


                logoutButton.textContent =
                    "로그아웃";


                logoutButton.addEventListener(
                    "click",
                    logoutFromHeader
                );


                headerRight.append(
                    nickname,
                    logoutButton
                );

            }
        );


    } catch (error) {

        console.error(
            "로그인 상태 확인 실패",
            error
        );

    }

}



function addAuthHeaderStyle() {

    /*
     * 스타일 중복 추가 방지
     */

    if (
        document.getElementById(
            "auth-header-style"
        )
    ) {

        return;

    }


    const style =
        document.createElement(
            "style"
        );


    style.id =
        "auth-header-style";


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


    document.head.appendChild(
        style
    );

}



/* =========================================
   로그아웃
========================================= */

async function logoutFromHeader() {

    try {

        /*
         * 로그아웃하기 전에
         * 현재 보고 있는 페이지 저장
         */

        const currentUrl =
            window.location.pathname
            + window.location.search
            + window.location.hash;


        const response = await fetch(
            "/api/members/logout",
            {
                method: "POST"
            }
        );


        if (!response.ok) {

            alert(
                "로그아웃 처리 중 오류가 발생했습니다."
            );

            return;

        }


        /*
         * 로그아웃 성공
         *
         * 홈으로 이동하지 않고
         * 현재 보고 있던 페이지 다시 열기
         */

        window.location.href =
            currentUrl;


    } catch (error) {

        alert(
            "로그아웃 처리 중 오류가 발생했습니다."
        );

    }

}