/* Presentation behavior only. Authentication and data mutations stay in ml-core/account. */
(() => {
  'use strict';
  document.addEventListener('DOMContentLoaded', () => {
    const path = location.pathname;
    const tab = new URLSearchParams(location.search).get('tab') || 'all';
    document.querySelectorAll('.browse-nav a').forEach(link => {
      const url = new URL(link.href, location.href);
      const selected = url.pathname === path && (url.searchParams.get('tab') || 'all') === tab;
      link.classList.toggle('active', selected);
      if (selected) link.setAttribute('aria-current', 'page');
    });
    const sidebar = document.querySelector('.browse-sidebar');
    if (sidebar) {
      const mobile = matchMedia('(max-width:800px)');
      const resize = () => { sidebar.open = !mobile.matches; };
      resize();
      mobile.addEventListener('change', resize);
    }
    document.querySelectorAll('[data-ott-section]').forEach(section => {
      const heading = section.querySelector('[data-ott-heading]');
      if (!heading) return;
      const update = () => {
        const selected = section.querySelector('[data-provider][aria-pressed="true"]');
        heading.textContent = (selected?.textContent.trim() || '') + ' 제공 영화';
      };
      new MutationObserver(update).observe(section.querySelector('.provider-tabs'), {
        attributes:true, subtree:true, attributeFilter:['aria-pressed']
      });
      update();
    });
    const tabs = [...document.querySelectorAll('[data-account-tab]')];
    const select = button => {
      tabs.forEach(item => {
        const active = item === button;
        item.setAttribute('aria-selected', String(active));
        item.tabIndex = active ? 0 : -1;
        item.classList.toggle('active', active);
        document.getElementById(item.dataset.accountTab).hidden = !active;
      });
    };
    if(location.hash==='#reviewsPanel'){const reviewTab=tabs.find(b=>b.dataset.accountTab==='reviewsPanel');if(reviewTab)select(reviewTab);}
    tabs.forEach((button, index) => {
      button.addEventListener('click', () => select(button));
      button.addEventListener('keydown', event => {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        let next = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 :
          (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
        select(tabs[next]); tabs[next].focus();
      });
    });
    document.addEventListener('ml:favorites-changed', event => {
      const grid = document.getElementById('accountFavorites');
      if (!grid) return;
      grid.querySelectorAll('[data-favorite]').forEach(button => {
        if (button.dataset.favorite === event.detail.id && !event.detail.active) button.closest('.movie-card').remove();
      });
      const count = document.getElementById('favoriteStat');
      if (count) count.textContent = window.ML.favorites.size;
      const empty = document.getElementById('accountFavoritesEmpty');
      empty.hidden = !!grid.children.length;
      if (!grid.children.length && window.ML.favorites.size) {
        empty.querySelector('h3').textContent = '찜 목록에 더 많은 영화가 있습니다';
        empty.querySelector('p').textContent = '전체 목록에서 나머지 영화를 확인해 보세요.';
        const link = empty.querySelector('a'); link.href='/favorite-movies'; link.textContent='찜 목록 전체보기';
      }
    });
    // Keep the original recommendation choice interaction accessible.
    document.querySelectorAll('.choice[data-group]').forEach(button => {
      button.setAttribute('aria-pressed', String(button.classList.contains('selected')));
      button.addEventListener('click', () => {
        document.querySelectorAll('.choice[data-group]').forEach(item => {
          item.setAttribute('aria-pressed', String(item.classList.contains('selected')));
        });
      });
    });
  });
})();
