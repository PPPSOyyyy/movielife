document.addEventListener(
    "DOMContentLoaded",
    function () {

        updateAuthHeader();

    }
);



/* =========================================
   로그인 상태 확인 및 헤더 변경
========================================= */

async function updateAuthHeader() {


    const currentUrl =

        window.location.pathname

        + window.location.search

        + window.location.hash;


    const headerRightList =

        document.querySelectorAll(
            ".header-right"
        );


    try {


        const response = await fetch(

            "/api/members/me",

            {

                headers: {

                    "Accept":
                        "application/json"

                }

            }

        );


        /* =========================================
           로그인하지 않은 상태
        ========================================= */

        if (!response.ok) {


            /*
             * 일반 로그인 버튼
             * 로그인 성공 후 현재 페이지로 돌아오기
             */

            document

                .querySelectorAll(
                    "a.login-button"
                )

                .forEach(loginLink => {


                    loginLink.href =

                        "/login?returnUrl="

                        + encodeURIComponent(
                            currentUrl
                        );

                });


            /*
             * 회원 전용 메뉴에 접근 제한 적용
             */

            setupLoginRequiredLinks();


            return;

        }



        /* =========================================
           로그인 상태
        ========================================= */

        const member =

            await response.json();


        addAuthHeaderStyle();


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

                    member.nickname
                    + "님";


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



/* =========================================
   비회원 회원전용 기능 접근 제한
========================================= */

function setupLoginRequiredLinks() {


    /*
     * 마이페이지
     */

    document

        .querySelectorAll(
            'a[href="/mypage"]'
        )

        .forEach(link => {


            link.addEventListener(

                "click",

                function (event) {


                    event.preventDefault();


                    openLoginRequiredModal(
                        "/mypage"
                    );

                }

            );

        });



    /*
     * 찜한 영화 페이지
     */

    document

        .querySelectorAll(
            'a[href="/favorite-movies"]'
        )

        .forEach(link => {


            link.addEventListener(

                "click",

                function (event) {


                    event.preventDefault();


                    openLoginRequiredModal(
                        "/favorite-movies"
                    );

                }

            );

        });



    /*
     * 작성한 리뷰 보기
     */

    document

        .querySelectorAll(
            'a[href="/my-reviews"]'
        )

        .forEach(link => {


            link.addEventListener(

                "click",

                function (event) {


                    event.preventDefault();


                    openLoginRequiredModal(
                        "/my-reviews"
                    );

                }

            );

        });



    /*
     * 리뷰 작성
     */

    document

        .querySelectorAll(
            'a[href="/review"]'
        )

        .forEach(link => {


            link.addEventListener(

                "click",

                function (event) {


                    event.preventDefault();


                    openLoginRequiredModal(
                        "/review"
                    );

                }

            );

        });

}



/* =========================================
   로그인 필요 모달 열기
========================================= */

function openLoginRequiredModal(returnUrl) {


    addLoginRequiredModalStyle();


    let modal =

        document.getElementById(
            "loginRequiredModal"
        );


    /*
     * 모달이 아직 없으면 생성
     */

    if (!modal) {


        modal =

            document.createElement(
                "div"
            );


        modal.id =
            "loginRequiredModal";


        modal.className =
            "login-required-overlay";


        modal.innerHTML = `

            <div class="login-required-modal">

                <div class="login-required-icon">
                    🔒
                </div>


                <div class="login-required-small">
                    LOGIN REQUIRED
                </div>


                <h2>
                    로그인이 필요합니다
                </h2>


                <p>
                    회원 전용 기능입니다.<br>
                    로그인 후 이용해 주세요.
                </p>


                <div class="login-required-buttons">


                    <button
                        type="button"
                        class="login-required-cancel"
                        id="loginRequiredCancel"
                    >
                        취소
                    </button>


                    <button
                        type="button"
                        class="login-required-login"
                        id="loginRequiredLogin"
                    >
                        로그인
                    </button>


                </div>

            </div>

        `;


        document.body.appendChild(
            modal
        );


        /*
         * 배경 클릭하면 닫기
         */

        modal.addEventListener(

            "click",

            function (event) {


                if (
                    event.target === modal
                ) {

                    closeLoginRequiredModal();

                }

            }

        );


        /*
         * 취소
         */

        document

            .getElementById(
                "loginRequiredCancel"
            )

            .addEventListener(

                "click",

                closeLoginRequiredModal

            );

    }



    /*
     * 로그인 버튼 클릭 시
     * 원래 가려던 주소로 돌아오도록 설정
     */

    const loginButton =

        document.getElementById(
            "loginRequiredLogin"
        );


    loginButton.onclick =

        function () {


            window.location.href =

                "/login?returnUrl="

                + encodeURIComponent(
                    returnUrl
                );

        };


    modal.classList.add(
        "show"
    );


    document.body.style.overflow =
        "hidden";

}



/* =========================================
   로그인 필요 모달 닫기
========================================= */

function closeLoginRequiredModal() {


    const modal =

        document.getElementById(
            "loginRequiredModal"
        );


    if (modal) {

        modal.classList.remove(
            "show"
        );

    }


    document.body.style.overflow =
        "";

}



/* =========================================
   로그인 필요 모달 스타일
========================================= */

function addLoginRequiredModalStyle() {


    if (

        document.getElementById(
            "login-required-style"
        )

    ) {

        return;

    }


    const style =

        document.createElement(
            "style"
        );


    style.id =
        "login-required-style";


    style.textContent = `


        .login-required-overlay {

            position: fixed;

            inset: 0;

            z-index: 99999;

            display: none;

            align-items: center;

            justify-content: center;

            padding: 20px;

            background:
                rgba(0, 0, 0, 0.78);

            backdrop-filter:
                blur(5px);

        }


        .login-required-overlay.show {

            display: flex;

        }


        .login-required-modal {

            width: 100%;

            max-width: 390px;

            padding:
                34px 30px 30px;

            border:
                1px solid #303030;

            border-radius: 10px;

            background:
                #111111;

            color:
                #ffffff;

            text-align:
                center;

            box-shadow:
                0 25px 80px
                rgba(0,0,0,0.65);

        }


        .login-required-icon {

            margin-bottom:
                14px;

            font-size:
                30px;

        }


        .login-required-small {

            margin-bottom:
                8px;

            color:
                #D92234;

            font-size:
                10px;

            font-weight:
                700;

            letter-spacing:
                1.8px;

        }


        .login-required-modal h2 {

            margin-bottom:
                12px;

            font-size:
                23px;

            letter-spacing:
                -1px;

        }


        .login-required-modal p {

            margin-bottom:
                27px;

            color:
                #777777;

            font-size:
                12px;

            line-height:
                1.7;

        }


        .login-required-buttons {

            display:
                flex;

            gap:
                10px;

        }


        .login-required-buttons button {

            flex:
                1;

            height:
                44px;

            border-radius:
                5px;

            font-family:
                inherit;

            font-size:
                12px;

            font-weight:
                700;

            cursor:
                pointer;

        }


        .login-required-cancel {

            border:
                1px solid #333333;

            background:
                transparent;

            color:
                #999999;

        }


        .login-required-cancel:hover {

            border-color:
                #555555;

            color:
                #ffffff;

        }


        .login-required-login {

            border:
                1px solid #D92234;

            background:
                #D92234;

            color:
                #ffffff;

        }


        .login-required-login:hover {

            background:
                #ed3043;

            border-color:
                #ed3043;

        }

    `;


    document.head.appendChild(
        style
    );

}



/* =========================================
   로그인된 헤더 스타일
========================================= */

function addAuthHeaderStyle() {


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

            color:
                #ffffff;

            font-size:
                13px;

            font-weight:
                600;

            white-space:
                nowrap;

        }


        .auth-logout-button {

            padding:
                9px 18px;

            border:
                1px solid #444444;

            border-radius:
                5px;

            background:
                transparent;

            color:
                #ffffff;

            font:
                inherit;

            font-size:
                12px;

            cursor:
                pointer;

            transition:
                0.2s;

        }


        .auth-logout-button:hover {

            border-color:
                #D92234;

            color:
                #D92234;

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


        const currentUrl =

            window.location.pathname

            + window.location.search

            + window.location.hash;


        const response = await fetch(

            "/api/members/logout",

            {

                method:
                    "POST"

            }

        );


        if (!response.ok) {


            alert(
                "로그아웃 처리 중 오류가 발생했습니다."
            );


            return;

        }


        /*
         * 로그아웃 후
         * 현재 페이지 유지
         */

        window.location.href =
            currentUrl;


    } catch (error) {


        alert(
            "로그아웃 처리 중 오류가 발생했습니다."
        );

    }

}



/* =========================================
   ESC 키로 로그인 필요 모달 닫기
========================================= */

document.addEventListener(

    "keydown",

    function (event) {


        if (
            event.key === "Escape"
        ) {

            closeLoginRequiredModal();

        }

    }

);