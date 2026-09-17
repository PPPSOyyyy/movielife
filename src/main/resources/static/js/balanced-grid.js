(() => {
'use strict';
// Five cards per desktop row. A short final row becomes a five-card viewport
// containing the last complete row plus the remainder, with no duplicate cards.
function install(grid){
 const observer=new MutationObserver(records=>{if(records.some(r=>r.target===grid||r.target.classList?.contains("movie-tail-track")))rebuild();});
 function rebuild(){
  observer.disconnect();
  const previous=grid.querySelector(':scope > .movie-tail');
  if(previous){const cards=[...previous.querySelector('.movie-tail-track').children];previous.replaceWith(...cards);}
  const cards=[...grid.children].filter(x=>x.matches('.movie-card,.person-movie-card'));
  if(cards.length>5&&cards.length%5!==0){
   const count=5+cards.length%5,tail=document.createElement('section'),track=document.createElement('div'),controls=document.createElement('div');
   tail.className='movie-tail';track.className='movie-tail-track';track.tabIndex=0;track.setAttribute('aria-label','나머지 영화, 좌우로 이동');controls.className='movie-tail-controls';
   for(const card of cards.slice(-count))track.append(card);
   for(const [label,direction] of [['이전 영화',-1],['다음 영화',1]]){
    const button=document.createElement('button');button.type='button';button.className='btn btn-sm';button.textContent=label;
    button.addEventListener('click',()=>track.scrollBy({left:direction*track.clientWidth,behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'}));controls.append(button);
   }
   tail.append(track,controls);grid.append(tail);
  }
  observer.observe(grid,{childList:true,subtree:true});
 }
 rebuild();
}
document.querySelectorAll('.movie-grid,.person-movie-grid').forEach(install);
})();
