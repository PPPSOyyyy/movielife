(() => {
'use strict'; const key='movielife-theme',root=document.documentElement,system=matchMedia('(prefers-color-scheme: dark)');
let preference='light';try{const v=localStorage.getItem(key);if(['system','light','dark'].includes(v))preference=v;}catch{}
function apply(){const mode=preference==='system'?(system.matches?'dark':'light'):preference;root.dataset.theme=mode;root.style.colorScheme=mode;document.querySelectorAll('[data-theme-select]').forEach(s=>s.value=preference);const meta=document.querySelector('meta[name="theme-color"]');if(meta)meta.content=mode==='dark'?'#000000':'#f7f7f7';}
apply();system.addEventListener('change',()=>{if(preference==='system')apply();});
window.addEventListener('storage',e=>{if(e.key===key){preference=['system','light','dark'].includes(e.newValue)?e.newValue:'light';apply();}});
document.addEventListener('DOMContentLoaded',()=>{apply();document.querySelectorAll('[data-theme-select]').forEach(s=>s.addEventListener('change',()=>{preference=s.value;try{localStorage.setItem(key,preference);}catch{}apply();}));});
})();
