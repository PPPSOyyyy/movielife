(() => {
    'use strict';

    const { ML } = window;

    document.querySelectorAll('[data-ott-section]').forEach(section => {
        const result = section.querySelector('[data-ott-results]');
        const more = section.querySelector('[data-ott-more]');
        const buttons = [...section.querySelectorAll('[data-provider]')];

        const limit = Number(section.dataset.limit) || 20;

        // 전체 보기로 전달받은 OTT를 처음부터 선택합니다.
        const selected = new URLSearchParams(location.search).get('provider');

        let provider = buttons.some(
            button => button.dataset.provider === selected
        ) ? selected : (buttons[0]?.dataset.provider || '');

        let page = 1;
        let revision = 0;
        let loading = false;

        const seen = new Set();

        async function load(append = false) {
            const requestRevision = ++revision;

            loading = true;

            // 홈에서 선택한 OTT를 전체 보기 링크에도 반영합니다.
            const allLink = section.querySelector('[data-ott-all]');

            if (allLink) {
                allLink.href = '/ott?provider=' + encodeURIComponent(provider);
            }

            if (more) {
                more.disabled = true;
                more.hidden = true;
            }

            buttons.forEach(button => {
                button.setAttribute(
                    'aria-pressed',
                    String(button.dataset.provider === provider)
                );
            });

            if (!append) {
                seen.clear();

                result.replaceChildren(
                    ML.el('p', 'loading', '영화를 불러오는 중입니다.')
                );
            }

            try {
                const response = await ML.request(
                    '/api/ott-page?provider='
                    + encodeURIComponent(provider)
                    + '&page='
                    + page
                );

                const movies=response.movies;
                // 다른 OTT로 전환한 뒤 도착한 이전 응답은 무시합니다.
                if (requestRevision !== revision) {
                    return;
                }

                if (!Array.isArray(movies)) {
                    throw new Error('영화 목록을 불러오지 못했습니다.');
                }

                if (!append) {
                    result.replaceChildren();
                }

                movies.slice(0, limit).forEach(movie => {
                    if (!seen.has(movie.id)) {
                        seen.add(movie.id);
                        result.append(ML.card(movie));
                    }
                });

                if (!seen.size) {
                    result.append(
                        ML.empty(
                            '제공 중인 영화가 없습니다',
                            '다른 OTT를 선택해 보세요.'
                        )
                    );
                }

                if (more) {
                    more.hidden = !response.hasMore || page >= 500;
                }

                await ML.ready;

                if (ML.member) {
                    try {
                        await ML.loadFavorites();
                    } catch {
                        // 찜 상태 조회 실패 시 찜 버튼 클릭에서 재시도합니다.
                    }
                }

                ML.syncFavorites();

            } catch (error) {
                if (requestRevision !== revision) {
                    return;
                }

                if (append) {
                    page = Math.max(1, page - 1);
                    ML.toast(error.message, true);

                    if (more) {
                        more.hidden = false;
                    }

                } else {
                    const empty = ML.empty(
                        '영화를 불러오지 못했습니다',
                        error.message
                    );

                    const retry = ML.el('button', 'btn', '다시 시도');
                    retry.type = 'button';

                    retry.addEventListener('click', () => load());

                    empty.append(retry);
                    result.replaceChildren(empty);
                }

            } finally {
                if (requestRevision === revision) {
                    loading = false;

                    if (more) {
                        more.disabled = false;
                    }
                }
            }
        }

        buttons.forEach(button => {
            button.addEventListener('click', () => {
                if (provider === button.dataset.provider && !loading) {
                    return;
                }

                provider = button.dataset.provider;
                page = 1;

                load();

                // OTT 전체 화면에서는 주소에도 선택한 서비스를 반영합니다.
                if (location.pathname === '/ott') {
                    history.replaceState(
                        null,
                        '',
                        '/ott?provider=' + provider
                    );
                }
            });
        });

        more?.addEventListener('click', () => {
            if (!loading) {
                page++;
                load(true);
            }
        });

        if(buttons.length)load();
        else result.textContent="KR 제공처 정보를 불러오지 못했습니다.";
    });
})();
