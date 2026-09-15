(() => {
  'use strict';

  const ML = window.ML = window.ML || {};
  ML.member = null;
  ML.favorites = new Set();

  ML.el = (tag, className = '', text = '') => {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (text !== undefined && text !== null) el.textContent = text;
    return el;
  };

  ML.json = (method, body) => ({
    method,
    headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
    body: JSON.stringify(body)
  });

  ML.request = async (url, options = {}) => {
    const response = await fetch(url, {credentials: 'same-origin', ...options});
    const type = response.headers.get('content-type') || '';
    let data;
    if (type.includes('application/json')) data = await response.json();
    else data = await response.text();
    if (!response.ok) {
      const message = typeof data === 'string' ? data : (data?.message || '요청 처리 중 오류가 발생했습니다.');
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }
    return data;
  };

  ML.returnUrl = () => {
    const value = new URLSearchParams(location.search).get('returnUrl');
    return value && value.startsWith('/') && !value.startsWith('//') ? value : '/';
  };

  ML.ready = ML.request('/api/members/me')
    .then(member => (ML.member = member, member))
    .catch(() => null);

  ML.requireLogin = () => {
    const current = location.pathname + location.search + location.hash;
    location.href = '/login?required=true&returnUrl=' + encodeURIComponent(current);
  };

  ML.toast = (message, isError = false) => {
    let toast = document.getElementById('mlGlobalToast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'mlGlobalToast';
      Object.assign(toast.style, {
        position:'fixed', left:'50%', bottom:'28px', transform:'translateX(-50%)', zIndex:'99999',
        padding:'12px 18px', borderRadius:'10px', background:'#17191f', color:'#fff',
        border:'1px solid #343842', boxShadow:'0 12px 34px rgba(0,0,0,.35)', fontSize:'14px'
      });
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.style.borderColor = isError ? '#d92234' : '#343842';
    toast.hidden = false;
    clearTimeout(ML._toastTimer);
    ML._toastTimer = setTimeout(() => toast.hidden = true, 2400);
  };

  ML.dialog = ({title, message, confirm='확인', cancel}) => new Promise(resolve => {
    const existing = document.getElementById('mlGlobalDialog');
    if (existing) existing.remove();
    const wrap = document.createElement('div');
    wrap.id = 'mlGlobalDialog';
    wrap.innerHTML = `<div class="ml-core-backdrop"><div class="ml-core-dialog"><h3></h3><p></p><div class="ml-core-actions"></div></div></div>`;
    const style = document.createElement('style');
    style.textContent = `.ml-core-backdrop{position:fixed;inset:0;z-index:100000;background:rgba(0,0,0,.72);display:grid;place-items:center;padding:20px}.ml-core-dialog{width:min(420px,100%);background:#12141a;border:1px solid #30343e;border-radius:18px;padding:28px;color:#fff;box-shadow:0 24px 80px rgba(0,0,0,.5)}.ml-core-dialog h3{margin:0 0 10px;font-size:21px}.ml-core-dialog p{margin:0;white-space:pre-line;color:#aeb3bf;line-height:1.65}.ml-core-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}.ml-core-actions button{border:0;border-radius:9px;padding:10px 16px;cursor:pointer}.ml-core-ok{background:#d92234;color:#fff}.ml-core-cancel{background:#292c34;color:#ddd}`;
    wrap.appendChild(style);
    wrap.querySelector('h3').textContent = title || '알림';
    wrap.querySelector('p').textContent = message || '';
    const actions = wrap.querySelector('.ml-core-actions');
    if (cancel) {
      const no = ML.el('button','ml-core-cancel',cancel);
      no.onclick = () => { wrap.remove(); resolve(false); };
      actions.appendChild(no);
    }
    const ok = ML.el('button','ml-core-ok',confirm);
    ok.onclick = () => { wrap.remove(); resolve(true); };
    actions.appendChild(ok);
    document.body.appendChild(wrap);
  });

  ML.handleError = error => {
    if (error?.status === 401) return ML.requireLogin();
    ML.toast(error?.message || '오류가 발생했습니다.', true);
  };

  ML.poster = img => {
    img.addEventListener('error', () => { img.src = '/poster/no-poster.svg'; }, {once:true});
  };

  ML.empty = (title, message, href, linkText) => {
    const box = ML.el('div','empty-state');
    box.append(ML.el('h3','',title), ML.el('p','',message));
    if (href && linkText) { const a=ML.el('a','btn',linkText); a.href=href; box.append(a); }
    return box;
  };

  ML.card = movie => {
    const article=ML.el('article','movie-card');
    const wrap=ML.el('div','poster-wrap');
    const a=document.createElement('a'); a.href='/movies/'+movie.id;
    const img=document.createElement('img'); img.src=movie.posterUrl||'/poster/no-poster.svg'; img.alt=(movie.title||'영화')+' 포스터'; img.loading='lazy'; ML.poster(img);
    a.append(img); wrap.append(a);
    const fav=ML.el('button','favorite-toggle','♡'); fav.type='button'; fav.dataset.favorite=movie.id; fav.dataset.title=movie.title||''; fav.setAttribute('aria-pressed','false'); wrap.append(fav);
    const info=ML.el('div','movie-info'); const h3=document.createElement('h3'); const title=document.createElement('a'); title.href='/movies/'+movie.id; title.textContent=movie.title||'제목 없음'; h3.append(title); info.append(h3,ML.el('p','',movie.releaseDate||'개봉일 정보 없음'));
    article.append(wrap,info); return article;
  };

  ML.loadFavorites = async () => {
    if (!ML.member) { ML.favorites.clear(); return; }
    const list = await ML.request('/api/favorites/my');
    ML.favorites = new Set(list.map(item => String(item.movieId)));
  };

  ML.syncFavorites = () => document.querySelectorAll('[data-favorite]').forEach(button => {
    const active = ML.favorites.has(String(button.dataset.favorite));
    button.textContent = active ? '♥' : '♡';
    button.setAttribute('aria-pressed', String(active));
  });

  document.addEventListener('click', async event => {
    const button = event.target.closest('[data-favorite]');
    if (!button) return;
    event.preventDefault();
    await ML.ready;
    if (!ML.member) return ML.requireLogin();
    const id = String(button.dataset.favorite);
    button.disabled = true;
    try {
      if (ML.favorites.has(id)) {
        await ML.request('/api/favorites/' + encodeURIComponent(id), {method:'DELETE'});
        ML.favorites.delete(id); ML.toast('찜을 취소했습니다.');
      } else {
        await ML.request('/api/favorites?movieId=' + encodeURIComponent(id), {method:'POST'});
        ML.favorites.add(id); ML.toast('찜에 추가했습니다.');
      }
      ML.syncFavorites();
    } catch (e) { ML.handleError(e); }
    finally { button.disabled = false; }
  });

  ML.ready.then(async member => {
    if (!member) return;
    try { await ML.loadFavorites(); ML.syncFavorites(); } catch (_) {}
  });
})();
