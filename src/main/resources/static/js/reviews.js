(() => {
    "use strict";

    const { ML } = window;
    let movieReviews = [];

    ensureCommentStyles();

    const date = value => value
        ? new Date(value).toLocaleDateString("ko-KR")
        : "날짜 정보 없음";

    function reviewCard(review, showMovie = false) {
        const article = ML.el("article", "review-item");

        if (showMovie) {
            const row = ML.el("a", "review-movie");
            row.href = "/movies/" + Number(review.movieId);

            const img = ML.el("img");
            img.src = review.posterUrl || "/poster/no-poster.svg";
            img.alt = review.movieTitle || "영화 포스터";
            img.loading = "lazy";
            ML.poster(img);

            const title = ML.el("div");
            title.append(
                ML.el("h3", "", review.movieTitle || "영화 정보를 불러올 수 없습니다."),
                ML.el("p", "muted small", "영화 상세 보기 →")
            );

            row.append(img, title);
            article.append(row);
        }

        const top = ML.el("div", "review-top");
        const user = ML.el("div", "review-user");
        const name = review.nickname || (showMovie ? ML.member?.nickname : "관객") || "관객";
        const meta = ML.el("div");

        meta.append(
            ML.el("strong", "", name),
            ML.el(
                "small",
                "",
                date(review.createdAt) + (review.updatedAt ? " · 수정됨" : "")
            )
        );

        user.append(
            ML.el("span", "avatar", name.slice(0, 1)),
            meta
        );

        top.append(
            user,
            ML.el("span", "review-rating", "★ " + review.rating + "/10")
        );

        article.append(
            top,
            ML.el("p", "review-content", review.content)
        );

        if (ML.member?.userId === review.userId) {
            const actions = ML.el("div", "review-actions");

            const edit = ML.el("a", "btn btn-sm btn-ghost", "수정");
            edit.href = "/review?reviewId=" + review.id;
            edit.addEventListener("click", () => {
                edit.href = "/review?reviewId=" + review.id + "&returnUrl=" + ML.reviewReturn();
            });

            const remove = ML.el("button", "btn btn-sm btn-ghost", "삭제");
            remove.type = "button";

            remove.addEventListener("click", async () => {
                if (!await ML.dialog({
                    title: "리뷰를 삭제할까요?",
                    message: "리뷰를 삭제하면 이 리뷰에 달린 댓글도 함께 삭제됩니다.",
                    confirm: "삭제",
                    cancel: "취소"
                })) {
                    return;
                }

                remove.disabled = true;

                try {
                    await ML.request("/api/reviews/" + review.id, {
                        method: "DELETE"
                    });

                    ML.toast("리뷰를 삭제했습니다.");

                    if (showMovie) {
                        loadMyReviews();
                    } else {
                        loadMovieReviews();
                    }
                } catch (error) {
                    ML.handleError(error);
                    remove.disabled = false;
                }
            });

            actions.append(edit, remove);
            article.append(actions);
        }

        const commentSection = createCommentSection(review.id);
        article.append(commentSection);
        loadComments(review.id, commentSection);

        return article;
    }

    function createCommentSection(reviewId) {
        const section = ML.el("section", "review-comments");
        section.dataset.reviewId = String(reviewId);

        const header = ML.el("div", "review-comments-head");
        const title = ML.el("strong", "", "댓글");
        const count = ML.el("span", "review-comment-count", "0");
        header.append(title, count);

        const list = ML.el("div", "review-comment-list");
        list.dataset.commentList = "";

        section.append(header, list);

        if (ML.member) {
            const form = document.createElement("form");
            form.className = "review-comment-form";

            const input = document.createElement("input");
            input.type = "text";
            input.name = "content";
            input.maxLength = 500;
            input.placeholder = "이 리뷰에 댓글을 남겨보세요.";
            input.setAttribute("aria-label", "리뷰 댓글");

            const button = ML.el("button", "review-comment-submit", "등록");
            button.type = "submit";

            form.append(input, button);

            form.addEventListener("submit", async event => {
                event.preventDefault();

                const content = input.value.trim();
                if (!content) {
                    input.focus();
                    return;
                }

                button.disabled = true;

                try {
                    await ML.request("/api/review-comments", {
                        method: "POST",
                        body: new URLSearchParams({
                            reviewId: String(reviewId),
                            content
                        })
                    });

                    input.value = "";
                    await loadComments(reviewId, section);
                } catch (error) {
                    if (error.status === 401) {
                        ML.requireLogin();
                    } else {
                        ML.handleError(error);
                    }
                } finally {
                    button.disabled = false;
                }
            });

            section.append(form);
        } else {
            const loginHint = ML.el("p", "review-comment-login muted small");
            const loginLink = ML.el("a", "", "로그인 후 댓글을 남길 수 있습니다.");
            loginLink.href = "/login?returnUrl=" + encodeURIComponent(location.pathname + location.search + location.hash);
            loginHint.append(loginLink);
            section.append(loginHint);
        }

        return section;
    }

    async function loadComments(reviewId, section) {
        const list = section.querySelector("[data-comment-list]");
        const count = section.querySelector(".review-comment-count");

        if (!list) {
            return;
        }

        try {
            const comments = await ML.request(
                "/api/review-comments?reviewId=" + encodeURIComponent(reviewId)
            );

            const rows = Array.isArray(comments) ? comments : [];
            count.textContent = String(rows.length);
            list.replaceChildren(...rows.map(comment => commentCard(comment, reviewId, section)));

            if (!rows.length) {
                list.append(
                    ML.el("p", "review-comment-empty", "아직 댓글이 없습니다.")
                );
            }
        } catch (error) {
            list.replaceChildren(
                ML.el("p", "review-comment-empty", "댓글을 불러오지 못했습니다.")
            );
        }
    }

    function commentCard(comment, reviewId, section) {
        const item = ML.el("div", "review-comment-item");
        const head = ML.el("div", "review-comment-meta");
        const name = comment.nickname || comment.userId || "회원";

        head.append(
            ML.el("strong", "", name),
            ML.el(
                "span",
                "",
                date(comment.createdAt) + (comment.updatedAt ? " · 수정됨" : "")
            )
        );

        const content = ML.el("p", "review-comment-content", comment.content);
        item.append(head, content);

        if (ML.member?.userId === comment.userId) {
            const actions = ML.el("div", "review-comment-actions");
            const edit = ML.el("button", "", "수정");
            const remove = ML.el("button", "", "삭제");
            edit.type = "button";
            remove.type = "button";

            edit.addEventListener("click", async () => {
                const next = prompt("댓글을 수정해 주세요.", comment.content);
                if (next === null) {
                    return;
                }

                const trimmed = next.trim();
                if (!trimmed) {
                    ML.toast("댓글 내용을 입력해 주세요.", "error");
                    return;
                }

                try {
                    await ML.request("/api/review-comments/" + comment.id, {
                        method: "PUT",
                        body: new URLSearchParams({ content: trimmed })
                    });
                    await loadComments(reviewId, section);
                } catch (error) {
                    ML.handleError(error);
                }
            });

            remove.addEventListener("click", async () => {
                if (!await ML.dialog({
                    title: "댓글을 삭제할까요?",
                    message: "삭제한 댓글은 복구할 수 없습니다.",
                    confirm: "삭제",
                    cancel: "취소"
                })) {
                    return;
                }

                try {
                    await ML.request("/api/review-comments/" + comment.id, {
                        method: "DELETE"
                    });
                    await loadComments(reviewId, section);
                } catch (error) {
                    ML.handleError(error);
                }
            });

            actions.append(edit, remove);
            item.append(actions);
        }

        return item;
    }

    async function failure(host, error, retry) {
        const empty = ML.empty("리뷰를 불러오지 못했습니다", error.message);
        const button = ML.el("button", "btn", "다시 시도");
        button.type = "button";
        button.addEventListener("click", retry);
        empty.append(button);
        host.replaceChildren(empty);
    }

    function renderMovieReviews() {
        const host = document.querySelector("#movieReviews");
        if (!host) {
            return;
        }

        const sort = document.querySelector("#reviewSort")?.value;
        const list = [...movieReviews];

        if (sort === "high") {
            list.sort((a, b) => b.rating - a.rating);
        }

        if (sort === "low") {
            list.sort((a, b) => a.rating - b.rating);
        }

        host.replaceChildren(...list.map(review => reviewCard(review)));

        if (!list.length) {
            host.append(
                ML.empty(
                    "아직 리뷰가 없습니다",
                    "이 영화의 첫 번째 감상을 남겨 보세요.",
                    "/review?movieId=" + document.querySelector("[data-movie-id]").dataset.movieId,
                    "첫 리뷰 남기기"
                )
            );
        }

        const own = movieReviews.find(review => review.userId === ML.member?.userId);
        const link = document.querySelector("[data-review-link]");

        if (link) {
            link.href = own
                ? "/review?reviewId=" + own.id
                : "/review?movieId=" + document.querySelector("[data-movie-id]").dataset.movieId;

            link.onclick = () => {
                link.href += (link.href.includes("?") ? "&" : "?") + "returnUrl=" + ML.reviewReturn();
            };

            link.textContent = own ? "내 리뷰 수정하기" : "리뷰 작성하기 ↗";
        }
    }

    async function loadMovieReviews() {
        const host = document.querySelector("#movieReviews");
        if (!host) {
            return;
        }

        try {
            const data = await ML.request(
                "/api/reviews?movieId=" + document.querySelector("[data-movie-id]").dataset.movieId
            );

            await ML.ready;
            movieReviews = data.reviews || [];

            document.querySelectorAll("[data-review-count]").forEach(element => {
                element.textContent = String(data.reviewCount || 0);
            });

            document.querySelector("[data-average-rating]").textContent = data.reviewCount
                ? Number(data.averageRating).toFixed(1)
                : "—";

            renderMovieReviews();
            ML.restoreReviewScroll();
        } catch (error) {
            failure(host, error, loadMovieReviews);
        }
    }

    async function loadMyReviews() {
        const host = document.querySelector("#myReviews");
        if (!host) {
            return;
        }

        try {
            const list = await ML.request("/api/reviews/my");
            await ML.ready;

            host.replaceChildren(...list.map(review => reviewCard(review, true)));
            ML.restoreReviewScroll();

            if (!list.length) {
                host.append(
                    ML.empty(
                        "아직 작성한 리뷰가 없습니다",
                        "기억에 남는 영화에 별점과 감상을 남겨 보세요.",
                        "/review",
                        "리뷰 작성하기"
                    )
                );
            }
        } catch (error) {
            if (error.status === 401) {
                ML.requireLogin();
            }
            failure(host, error, loadMyReviews);
        }
    }

    document.querySelector("#reviewSort")?.addEventListener("change", renderMovieReviews);

    const form = document.querySelector("#reviewForm");

    if (form) {
        const content = form.querySelector("#reviewContent");
        const error = form.querySelector("#reviewError");
        const count = () => {
            document.querySelector("#characterCount").textContent = String(content.value.length);
        };

        count();
        content.addEventListener("input", count);

        form.addEventListener("submit", async event => {
            event.preventDefault();
            error.textContent = "";

            const rating = new FormData(form).get("rating");

            if (!rating) {
                error.textContent = "별점을 선택해 주세요.";
                return;
            }

            if (!content.value.trim()) {
                error.textContent = "리뷰 내용을 입력해 주세요.";
                content.focus();
                return;
            }

            const button = form.querySelector("[type=submit]");
            button.disabled = true;

            try {
                const id = form.dataset.reviewId;

                await ML.request("/api/reviews" + (id ? "/" + id : ""), {
                    method: id ? "PUT" : "POST",
                    body: new URLSearchParams({
                        movieId: form.dataset.movieId,
                        rating,
                        content: content.value.trim()
                    })
                });

                await ML.dialog({
                    title: id ? "리뷰를 수정했습니다" : "리뷰를 등록했습니다",
                    message: "소중한 감상을 남겨 주셔서 감사합니다."
                });

                const back = ML.safeReturn(
                    new URLSearchParams(location.search).get("returnUrl"),
                    "/movies/" + form.dataset.movieId + "#reviews"
                );

                location.href = back;
            } catch (e) {
                error.textContent = e.message;

                if (e.status === 401) {
                    ML.requireLogin();
                }
            } finally {
                button.disabled = false;
            }
        });
    }

    function ensureCommentStyles() {
        if (document.getElementById("reviewCommentStyles")) {
            return;
        }

        const style = document.createElement("style");
        style.id = "reviewCommentStyles";
        style.textContent = `
            .review-comments {
                margin-top: 18px;
                padding-top: 16px;
                border-top: 1px solid var(--line);
            }

            .review-comments-head {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 12px;
                color: var(--text);
                font-size: 14px;
            }

            .review-comment-count {
                display: inline-flex;
                min-width: 24px;
                height: 24px;
                align-items: center;
                justify-content: center;
                padding: 0 7px;
                border-radius: 999px;
                background: var(--red-soft);
                color: var(--red);
                font-size: 12px;
                font-weight: 800;
            }

            .review-comment-list {
                display: grid;
                gap: 9px;
            }

            .review-comment-item {
                position: relative;
                padding: 12px 14px;
                border: 1px solid var(--line);
                border-radius: 9px;
                background: var(--panel-2);
            }

            .review-comment-meta {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 5px;
            }

            .review-comment-meta strong {
                color: var(--text);
                font-size: 13px;
            }

            .review-comment-meta span {
                color: var(--muted);
                font-size: 11px;
            }

            .review-comment-content {
                margin: 0 !important;
                color: var(--text);
                font-size: 13px;
                line-height: 1.65;
                white-space: pre-wrap;
                overflow-wrap: anywhere;
            }

            .review-comment-actions {
                display: flex;
                justify-content: flex-end;
                gap: 8px;
                margin-top: 8px;
            }

            .review-comment-actions button {
                border: 0;
                background: transparent;
                color: var(--muted);
                font-size: 11px;
                cursor: pointer;
            }

            .review-comment-actions button:hover {
                color: var(--red);
            }

            .review-comment-form {
                display: grid;
                grid-template-columns: minmax(0, 1fr) auto;
                gap: 8px;
                margin-top: 12px;
            }

            .review-comment-form input {
                min-width: 0;
                height: 42px;
                padding: 0 12px;
                border: 1px solid var(--line);
                border-radius: 8px;
                background: var(--panel);
                color: var(--text);
                outline: none;
            }

            .review-comment-form input:focus {
                border-color: var(--red);
                box-shadow: 0 0 0 3px var(--red-soft);
            }

            .review-comment-submit {
                min-width: 70px;
                border: 1px solid var(--red);
                border-radius: 8px;
                background: var(--red);
                color: #ffffff;
                font-weight: 800;
                cursor: pointer;
            }

            .review-comment-submit:disabled {
                opacity: 0.5;
                cursor: wait;
            }

            .review-comment-empty,
            .review-comment-login {
                margin: 0 !important;
                color: var(--muted);
                font-size: 12px;
            }

            .review-comment-login {
                margin-top: 10px !important;
            }

            .review-comment-login a {
                color: var(--red) !important;
            }
        `;

        document.head.appendChild(style);
    }

    loadMovieReviews();
    loadMyReviews();
})();
