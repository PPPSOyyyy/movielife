/* 모든 화면의 공통 로그인/메뉴/찜/안내창. 기존 팀원 화면에서도 사용 가능합니다. */
(() => {
  'use strict';
  if (window.ML?.ready) return;
  const ML = window.ML = {};
  ML.member = null;
  ML.favorites = new Set();
  const pendingFavorites = new Set();
  let favoriteLoad = null;
  let favoritesLoaded = false;
  let authFailure = false;
  const protectedPaths = ['/mypage', '/profile', '/favorite-movies', '/my-reviews', '/review'];
  const currentPath = () => location.pathname + location.search + location.hash;
  ML.safeReturn = (value, fallback = '/') => {
    try {
      if (!value || !value.startsWith('/') || value.startsWith('//') || /[\\\u0000-\u001f]/.test(value)) return fallback;
      const url = new URL(value, location.origin);
      if (url.origin !== location.origin || ['/login', '/signup'].includes(url.pathname)) return fallback;
      return url.pathname + url.search + url.hash;
    } catch { return fallback;
    }
  };
  ML.returnUrl = () => ML.safeReturn(new URLSearchParams(location.search).get('returnUrl'));
  ML.el = (tag, className, text) => {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined && text !== null) element.textContent = text;
    return element;
  };
  ML.request = async (url, options = {}) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 18000);
    try {
      const response = await fetch(url, {credentials: 'same-origin', cache: 'no-store', ...options, signal: options.signal || controller.signal});
      const text = await response.text();
      let body = text;
      try { body = text ? JSON.parse(text) : null;
      } catch { /* 기존 API의 문자열 응답도 지원 */ }
      if (!response.ok) {
        const message = typeof body === 'object' && body ? body.message : (typeof body === 'string' && !body.includes('<') ? body : null);
        const error = new Error(message || (response.status === 401 ? '로그인이 필요합니다.' : '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'));
        error.status = response.status;
        throw error;
      }
      if (typeof body === 'string' && /^\s*</.test(body)) throw new Error('API 대신 HTML 화면이 응답했습니다. 실행 중인 서버와 요청 경로를 확인해 주세요.');
      return body;
    } catch (error) {
      if (error.name === 'AbortError') throw new Error('응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.');
      if (error instanceof TypeError) throw new Error('연결을 확인한 뒤 다시 시도해 주세요.');
      throw error;
    } finally { clearTimeout(timer);
    }
  };
  ML.json = (method, body) => ({method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)});
  ML.toast = (message, error = false) => {
    let host = document.querySelector('.toast-container');
    if (!host) { host = ML.el('div','toast-container');
    host.setAttribute('role','status');
    host.setAttribute('aria-live','polite');
    document.body.append(host);
    }
    host.replaceChildren(ML.el('div', 'toast' + (error ? ' error' : ''), message));
    clearTimeout(ML.toastTimer);
    ML.toastTimer = setTimeout(() => host.replaceChildren(), 4000);
  };
  let activeDialog = null;
  ML.dialog = ({title = '안내', message = '', confirm = '확인', cancel = null} = {}) => new Promise(resolve => {
    if (activeDialog) activeDialog.close();
    const previous = document.activeElement;
    const dialog = ML.el('dialog','ml-dialog');
    activeDialog = dialog;
    const heading = ML.el('h2','',title);
    heading.id = 'mlDialogTitle';
    const copy = ML.el('p','',message);
    copy.id = 'mlDialogDescription';
    dialog.setAttribute('aria-labelledby', heading.id);
    dialog.setAttribute('aria-describedby', copy.id);
    const actions = ML.el('div','actions');
    if (cancel) { const button = ML.el('button','btn',cancel);
    button.type='button';
    button.addEventListener('click',()=>dialog.close('cancel'));
    actions.append(button);
    }
    const button = ML.el('button','btn btn-primary',confirm);
    button.type='button';
    button.addEventListener('click',()=>dialog.close('confirm'));
    actions.append(button);
    dialog.append(heading,copy,actions);
    document.body.append(dialog);
    dialog.addEventListener('cancel',event=>{if(!cancel)event.preventDefault();});
    dialog.addEventListener('close',()=>{ const accepted = dialog.returnValue === 'confirm'; dialog.remove(); if (activeDialog === dialog) activeDialog=null; previous?.focus(); resolve(accepted); },{once:true});
    dialog.showModal();
  });
  ML.requireLogin = async (returnUrl = currentPath()) => {
    const ok = await ML.dialog({title:'로그인이 필요합니다',message:'로그인하고 나만의 영화와 감상을 기록해 보세요.',confirm:'로그인',cancel:'취소'});
    if (ok) location.href = '/login?returnUrl=' + encodeURIComponent(ML.safeReturn(returnUrl));
  };
  window.openLoginRequiredModal = ML.requireLogin;
  window.closeLoginRequiredModal = () => activeDialog?.close();
  ML.handleError = error => error.status === 401 ? ML.requireLogin() : ML.toast(error.message, true);
  ML.poster = image => {
    image.addEventListener('error', () => {
      if (image.dataset.fallback) return;
      image.dataset.fallback = 'true';
      image.src = '/poster/no-poster.svg';
    });
    if (image.complete && image.naturalWidth === 0) { image.dataset.fallback='true';
    image.src='/poster/no-poster.svg';
    }
  };
  const updateNav = () => {
    const path = location.pathname;
    let active = path === '/' ? 'home' : 'movies';
    if (path === '/movies/domestic') active = 'domestic';
    else if (path === '/movies/foreign') active = 'foreign';
    else if (['/recommend','/movie-recommend'].includes(path)) active = 'recommend';
    else if (path === '/ott') active = 'ott';
    else if (protectedPaths.includes(path) && path !== '/review') active = 'mypage';
    else if (['/login','/signup'].includes(path)) active = '';
    document.querySelectorAll('[data-nav]').forEach(link => {
      const selected = link.dataset.nav === active;
      link.classList.toggle('active', selected);
      if (selected) link.setAttribute('aria-current','page');
      else link.removeAttribute('aria-current');
    });
    document.querySelectorAll('[data-login-link]').forEach(a => { a.href='/login?returnUrl='+encodeURIComponent(ML.safeReturn(currentPath())); });
  };
  const updateHeader = () => {
    document.querySelectorAll('.ml-auth, .header-right').forEach(host => {
      if (!ML.member) return;
      const profile = ML.el('a','auth-nickname',ML.member.nickname + '님');
      profile.href='/mypage';
      profile.title=ML.member.nickname;
      const logout = ML.el('button','','로그아웃');
      logout.type='button';
      logout.addEventListener('click',()=>ML.logout(logout));
      host.replaceChildren(profile,logout);
    });
  };
  ML.logout = async button => {
    if (button) button.disabled=true;
    try {
      await ML.request('/api/members/logout',{method:'POST'});
      location.href = protectedPaths.includes(location.pathname) ? '/' : currentPath();
    } catch (error) { ML.handleError(error);
    if(button)button.disabled=false;
    }
  };
  window.logoutFromHeader = ML.logout;
  ML.syncFavorites = () => {
    document.querySelectorAll('[data-favorite]').forEach(button => {
      const selected = ML.favorites.has(Number(button.dataset.favorite));
      button.setAttribute('aria-pressed',String(selected));
      button.setAttribute('aria-label',(button.dataset.title ? button.dataset.title + ' ' : '') + (selected ? '찜 취소' : '찜하기'));
      button.textContent = button.hasAttribute('data-full-label') ? (selected ? '♥ 찜한 영화' : '♡ 찜하기') : selected ? '♥' : '♡';
      button.disabled = pendingFavorites.has(Number(button.dataset.favorite));
    });
  };
  ML.loadFavorites = async () => {
    if (!ML.member) return;
    if (favoriteLoad) return favoriteLoad;
    favoriteLoad = ML.request('/api/favorites/my').then(list => {
      ML.favorites = new Set(list.map(item => Number(item.movieId)));
      favoritesLoaded = true;
      ML.syncFavorites();
    }).finally(()=>{favoriteLoad=null;});
    return favoriteLoad;
  };
  const toggleFavorite = async button => {
    await ML.ready;
    if (authFailure) { ML.toast('로그인 상태를 확인하지 못했습니다. 새로고침 후 다시 시도해 주세요.',true);
    return;
    }
    if (!ML.member) { await ML.requireLogin();
    return;
    }
    const id = Number(button.dataset.favorite);
    if (!Number.isSafeInteger(id) || id<=0 || pendingFavorites.has(id)) return;
    pendingFavorites.add(id);
    ML.syncFavorites();
    try {
      if (!favoritesLoaded) await ML.loadFavorites();
      const remove = ML.favorites.has(id);
      await ML.request(remove ? '/api/favorites/'+id : '/api/favorites?movieId='+id,{method:remove?'DELETE':'POST'});
      if (remove) ML.favorites.delete(id);
      else ML.favorites.add(id);
      ML.toast(remove ? '찜한 영화에서 삭제했습니다.' : '찜한 영화에 담았습니다.');
      if (remove && document.querySelector('[data-favorite-page]')) {
        button.closest('.movie-card')?.remove();
        const count=document.querySelectorAll('#favoritesGrid .movie-card').length;
        document.querySelector('[data-favorite-count]').textContent=String(count);
        document.querySelector('#favoritesEmpty').hidden=count>0;
      }
      try { localStorage.setItem('movielife:activity',String(Date.now()));
      } catch { /* 저장소 차단 시에도 찜은 정상 처리 */ }
      document.dispatchEvent(new CustomEvent('ml:favorite-changed',{detail:{id,selected:!remove}}));
    } catch(error) { ML.handleError(error);
    }
    finally { pendingFavorites.delete(id);
    ML.syncFavorites();
    }
  };
  ML.card = movie => {
    const card=ML.el('article','movie-card'), poster=ML.el('div','poster-wrap');
    const link=ML.el('a');
    link.href='/movies/'+Number(movie.id);
    link.setAttribute('aria-label',movie.title+' 상세 보기');
    const image=ML.el('img');
    image.alt=(movie.title || '영화')+' 포스터';
    image.loading='lazy';
    image.width=240;
    image.height=360;
    image.src=movie.posterUrl || '/poster/no-poster.svg';
    ML.poster(image);
    link.append(image);
    const rating=ML.el('span','rating-badge','★ '+(Number.isFinite(Number(movie.voteAverage)) ? Number(movie.voteAverage).toFixed(1) : '—'));
    const favorite=ML.el('button','favorite-toggle','♡');
    favorite.type='button';
    favorite.dataset.favorite=movie.id;
    favorite.dataset.title=movie.title||'';
    favorite.setAttribute('aria-pressed','false');
    favorite.setAttribute('aria-label','찜하기');
    poster.append(link,rating,favorite);
    const info=ML.el('div','movie-info'), heading=ML.el('h3'), title=ML.el('a','',movie.title || '제목 정보 없음');
    title.href=link.href;
    heading.append(title);
    info.append(heading,ML.el('p','',movie.releaseDate||'개봉일 정보 없음'));
    card.append(poster,info);
    return card;
  };
  ML.empty = (title,message,href,label) => {
    const block=ML.el('div','empty-state');
    block.style.gridColumn='1 / -1';
    block.append(ML.el('h3','',title),ML.el('p','',message));
    if(href){const link=ML.el('a','btn',label);
    link.href=href;
    block.append(link);
    }return block;
  };
  ML.init = () => {
    updateNav();
    document.querySelectorAll('img[data-poster]').forEach(ML.poster);
    document.querySelectorAll('form[role=search]').forEach(form=>form.addEventListener('submit',event=>{
      const input=form.querySelector('input[name=query]');
      input.value=input.value.trim();
      if(!input.value){event.preventDefault();
      input.setCustomValidity('영화 제목을 입력해 주세요.');
      input.reportValidity();
      }
    }));
    document.querySelectorAll('form[role=search] input').forEach(input=>input.addEventListener('input',()=>input.setCustomValidity('')));
    document.addEventListener('click',async event=>{
      const button=event.target.closest('[data-favorite]');
      if(button){event.preventDefault();
      await toggleFavorite(button);
      return;
      }
      if(event.target.closest('[data-reload]')){location.reload();
      return;
      }
      const link=event.target.closest('a[href]');
      if(!link || event.ctrlKey || event.metaKey || event.shiftKey || event.button>0) return;
      const url=new URL(link.href,location.origin);
      if(url.origin!==location.origin || !protectedPaths.includes(url.pathname)) return;
      event.preventDefault();
      await ML.ready;
      if(authFailure){ML.toast('로그인 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.',true);
      return;
      }
      if(ML.member) location.href=url.pathname+url.search+url.hash;
      else await ML.requireLogin(url.pathname+url.search+url.hash);
    });
    window.addEventListener('storage', event=>{if(event.key==='movielife:activity' && ML.member)ML.loadFavorites().catch(()=>{});});
  };
  ML.init();
  ML.ready = (async()=>{
    try { ML.member=await ML.request('/api/members/me');
    updateHeader();
    }
    catch(error){if(error.status!==401)authFailure=true;
    }
    if(ML.member && document.querySelector('[data-favorite]')) {
      try { await ML.loadFavorites();
      } catch { /* 클릭 시 재조회하여 잘못된 상태로 추가하지 않습니다. */ }
    }
    return ML.member;
  })();
})();
