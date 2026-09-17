(() => {
  'use strict';

  // movieLife desktop grids use 5 columns.  When filtering/API enrichment leaves
  // a remainder, keep that remainder hidden until enough cards exist to make a
  // complete five-card row.  This is intentionally visual only: cards are not
  // deleted, so OTT/recommendation results can reveal them later when more cards
  // are appended.
  const SELECTORS = [
    '.ml-browse-page .movie-grid',
    '.ml-country-page .movie-grid',
    '#recommendationList.movie-grid',
    '[data-ott-results].movie-grid'
  ].join(',');

  const CARD_SELECTOR = ':scope > .movie-card';
  const HIDDEN_CLASS = 'five-grid-tail-hidden';

  function normalize(grid) {
    const cards = [...grid.querySelectorAll(CARD_SELECTOR)];
    if (!cards.length) return;

    // First make every card available again, then calculate the current tail.
    cards.forEach(card => card.classList.remove(HIDDEN_CLASS));

    // A genuine search with fewer than five results should still show its results.
    // Once there is at least one complete row, never leave a short second/third row.
    if (cards.length < 5) return;

    const visibleCount = Math.floor(cards.length / 5) * 5;
    for (let i = visibleCount; i < cards.length; i++) {
      cards[i].classList.add(HIDDEN_CLASS);
    }
  }

  function install(grid) {
    if (grid.dataset.fiveGridInstalled === 'true') return;
    grid.dataset.fiveGridInstalled = 'true';

    let queued = false;
    const run = () => {
      queued = false;
      normalize(grid);
    };
    const schedule = () => {
      if (queued) return;
      queued = true;
      requestAnimationFrame(run);
    };

    normalize(grid);
    new MutationObserver(schedule).observe(grid, { childList: true });
  }

  function scan(root = document) {
    if (root.matches?.(SELECTORS)) install(root);
    root.querySelectorAll?.(SELECTORS).forEach(install);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => scan());
  } else {
    scan();
  }

  // Handles sections that are rendered/filled after page load.
  new MutationObserver(records => {
    for (const record of records) {
      for (const node of record.addedNodes) {
        if (node.nodeType === Node.ELEMENT_NODE) scan(node);
      }
    }
  }).observe(document.documentElement, { childList: true, subtree: true });
})();
