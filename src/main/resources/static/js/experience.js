(() => {
'use strict';
document.querySelectorAll('[data-drag-scroll]').forEach(row=>{
 let x=0,left=0,down=false,moved=false;
 row.addEventListener('pointerdown',e=>{if(e.pointerType!=='mouse'||e.button!==0)return;down=true;moved=false;x=e.clientX;left=row.scrollLeft;});
 row.addEventListener('pointermove',e=>{if(!down)return;if(Math.abs(e.clientX-x)>5){moved=true;row.scrollLeft=left-(e.clientX-x);e.preventDefault();}});
 window.addEventListener('pointerup',()=>down=false);row.addEventListener('pointerleave',()=>down=false);
 row.addEventListener('dragstart',e=>e.preventDefault());row.addEventListener('click',e=>{if(moved){e.preventDefault();moved=false;}},true);
 row.addEventListener('keydown',e=>{if(e.key==='ArrowRight'||e.key==='ArrowLeft'){e.preventDefault();row.scrollBy({left:e.key==='ArrowRight'?220:-220,behavior:'smooth'});}});
});
const host=document.getElementById('homeFavorite');if(!host||host.dataset.loggedIn!=='true')return;
const pause=document.getElementById('favoritePause'),next=document.getElementById('favoriteNext');
let index=0,count=0,paused=matchMedia('(prefers-reduced-motion: reduce)').matches,busy=false;
function sync(){pause.textContent=paused?'자동 전환 시작':'자동 전환 정지';pause.setAttribute('aria-pressed',String(paused));}
async function show(at){if(busy)return;busy=true;try{const r=await ML.request('/api/home-favorite?index='+at);count=r.count;pause.hidden=next.hidden=count<2;if(!count){document.getElementById('homeFavoriteBadge').hidden=true;document.getElementById('homeFavoriteTitle').textContent='마음에 드는 영화의 하트를 눌러 담아 보세요.';document.getElementById('homeFavoritePoster').hidden=true;return;}index=r.index;const m=r.movie;document.getElementById('homeFavoriteBadge').hidden=!m.adultsOnly;document.getElementById('homeFavoriteTitle').textContent=m.title;const img=document.getElementById('homeFavoritePoster');img.src=m.posterUrl;img.alt=m.title;img.hidden=false;document.getElementById('homeFavoriteLink').href='/movies/'+m.id;}catch{/* keep last valid card; do not fabricate an empty favorite list */}finally{busy=false;}}
pause.addEventListener('click',()=>{paused=!paused;sync();});next.addEventListener('click',()=>show(index+1));
setInterval(()=>{if(count>1&&!paused&&!document.hidden&&!host.matches(':hover')&&!host.contains(document.activeElement))show(index+1);},7000);
document.addEventListener('ml:favorites-changed',()=>show(0));sync();show(0);
})();
