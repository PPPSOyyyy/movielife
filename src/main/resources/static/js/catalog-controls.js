(() => {
'use strict';
const form=document.getElementById('catalogFilterForm');if(!form)return;
form.addEventListener('submit',event=>{
  const button=event.submitter;
  if(button?.name==='sort') {
    // Avoid two different sort values (sidebar select + clicked toolbar button).
    const select=form.querySelector('select[name="sort"]');if(select)select.disabled=true;
  }
});
window.addEventListener('pageshow',()=>{
  const select=form.querySelector('select[name="sort"]');
  if(select)select.disabled=!!new URLSearchParams(location.search).get('query');
});
})();
