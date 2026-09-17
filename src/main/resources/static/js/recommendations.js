(() => {
    "use strict";

    const button = document.getElementById("recommendButton");
    const list = document.getElementById("recommendationList");
    const loading = document.getElementById("recommendLoading");
    const message = document.getElementById("recommendMessage");
    const stage = document.getElementById("recommendStage");

    const weightPreference = document.getElementById("weightPreference");
    const weightRating = document.getElementById("weightRating");
    const weightFavorite = document.getElementById("weightFavorite");
    const weightView = document.getElementById("weightView");
    const weightTmdb = document.getElementById("weightTmdb");

    if (!button || !list) {
        return;
    }

    document.addEventListener("DOMContentLoaded", loadRecommendations);
    button.addEventListener("click", loadRecommendations);

    async function loadRecommendations() {
        button.disabled = true;
        list.replaceChildren();

        if (loading) {
            loading.style.display = "flex";
        }

        if (stage) {
            stage.textContent = "취향을 분석하고 있어요.";
        }

        try {
            const data = await ML.request("/api/recommendations");

            if (stage) {
                stage.textContent = data.stage || "맞춤 추천";
            }

            if (message) {
                message.textContent = data.message || "movieLife가 고른 추천 영화입니다.";
            }

            renderWeights(data.weights || {});

            const movies = Array.isArray(data.movies) ? data.movies : [];

            if (!movies.length) {
                list.append(
                    ML.empty(
                        "추천할 영화를 찾지 못했습니다",
                        "조금 뒤 다시 추천받기를 눌러주세요.",
                        "/movies",
                        "영화 둘러보기"
                    )
                );
                return;
            }

            for (const movie of movies) {
                const card = ML.card(movie);
                const poster = card.querySelector(".poster-wrap");
                const info = card.querySelector(".movie-info");

                if (poster && typeof movie.voteAverage === "number") {
                    const tmdb = document.createElement("span");
                    tmdb.className = "rating-badge";
                    tmdb.textContent = `TMDB ★ ${movie.voteAverage.toFixed(1)}`;
                    poster.appendChild(tmdb);
                }

                if (info && typeof movie.siteRating === "number") {
                    const rating = document.createElement("p");
                    rating.className = "recommend-member-rating";
                    rating.textContent = `회원 ★ ${movie.siteRating.toFixed(1)}`;
                    info.appendChild(rating);
                }

                list.appendChild(card);
            }

            await ML.ready;

            if (ML.member) {
                try {
                    await ML.loadFavorites();
                    ML.syncFavorites();
                } catch (_) {
                    // 찜 동기화 실패가 추천 결과 표시는 막지 않습니다.
                }
            }

        } catch (e) {
            list.replaceChildren(
                ML.empty(
                    "추천 영화를 불러오지 못했습니다",
                    e.message || "잠시 후 다시 시도해 주세요.",
                    "/movies",
                    "영화 둘러보기"
                )
            );
        } finally {
            button.disabled = false;

            if (loading) {
                loading.style.display = "none";
            }
        }
    }

    function renderWeights(weights) {
        setWeight(weightPreference, weights.initialPreference);
        setWeight(weightRating, weights.rating);
        setWeight(weightFavorite, weights.favorite);
        setWeight(weightView, weights.view);
        setWeight(weightTmdb, weights.tmdb);
    }

    function setWeight(element, value) {
        if (!element) {
            return;
        }

        const number = Number(value);
        element.textContent = Number.isFinite(number) ? `${number}%` : "-";
    }
})();
