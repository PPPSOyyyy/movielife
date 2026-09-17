(() => {
    "use strict";

    const selected = new Set();
    const choices = [...document.querySelectorAll("[data-genre-id]")];
    const count = document.getElementById("selectedCount");
    const error = document.getElementById("preferenceError");
    const save = document.getElementById("savePreference");
    const skip = document.getElementById("skipPreference");

    if (!choices.length || !save) {
        return;
    }

    for (const button of choices) {
        if (button.classList.contains("selected")) {
            selected.add(Number(button.dataset.genreId));
        }

        button.addEventListener("click", () => {
            const id = Number(button.dataset.genreId);

            if (selected.has(id)) {
                selected.delete(id);
                button.classList.remove("selected");
            } else {
                if (selected.size >= 5) {
                    error.textContent = "장르는 최대 5개까지 선택할 수 있습니다.";
                    return;
                }
                selected.add(id);
                button.classList.add("selected");
            }

            error.textContent = "";
            updateCount();
        });
    }

    updateCount();

    save.addEventListener("click", async () => {
        if (selected.size < 3 || selected.size > 5) {
            error.textContent = "좋아하는 장르를 3개 이상 5개 이하로 선택해 주세요.";
            return;
        }

        save.disabled = true;
        error.textContent = "";

        try {
            await ML.request(
                "/api/preferences",
                ML.json("PUT", {
                    genreIds: [...selected],
                    skip: false
                })
            );

            location.href = nextPage();
        } catch (e) {
            if (e.status === 401) {
                ML.requireLogin();
                return;
            }
            error.textContent = e.message;
        } finally {
            save.disabled = false;
        }
    });

    skip?.addEventListener("click", async () => {
        skip.disabled = true;
        error.textContent = "";

        try {
            await ML.request(
                "/api/preferences",
                ML.json("PUT", {
                    genreIds: [],
                    skip: true
                })
            );

            location.href = nextPage();
        } catch (e) {
            if (e.status === 401) {
                ML.requireLogin();
                return;
            }
            error.textContent = e.message;
        } finally {
            skip.disabled = false;
        }
    });

    function updateCount() {
        count.textContent = `${selected.size} / 5`;
    }

    function nextPage() {
        const raw = new URLSearchParams(location.search).get("next");
        return ML.safeReturn(raw, "/recommend");
    }
})();
