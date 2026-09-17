/* Each admin view is selected by the server (?view=...). JavaScript only enhances search/logout. */
(() => {
 'use strict';
 document.addEventListener('DOMContentLoaded', () => {
  for (const name of ['member','review','favorite']) {
   const input=document.getElementById(name+'Search');
   const rows=document.getElementById(name+'Rows');
   const empty=document.getElementById(name+'Empty');
   if (!input || !rows || !empty) continue;
   input.addEventListener('input',()=>{
    const keyword=input.value.trim().toLocaleLowerCase();let count=0;
    rows.querySelectorAll('tr').forEach(row=>{
     const text=(row.dataset.search || row.textContent).toLocaleLowerCase();
     row.hidden=!text.includes(keyword);if(!row.hidden)count++;
    });empty.hidden=count>0;
   });
  }
  document.getElementById('logoutButton')?.addEventListener('click',async event=>{
   const button=event.currentTarget;button.disabled=true;
   try {
    const response=await fetch('/api/members/logout',{method:'POST',credentials:'same-origin'});
    if(!response.ok)throw new Error('로그아웃 처리 중 오류가 발생했습니다.');
    location.href='/login?returnUrl=/admin';
   } catch(error) {alert(error.message);} finally {button.disabled=false;}
  });
  const params=new URLSearchParams(location.search);
  const messages={memberDeleted:'회원이 삭제되었습니다.',reviewDeleted:'리뷰가 삭제되었습니다.',favoriteDeleted:'찜이 삭제되었습니다.',cannotDeleteAdmin:'관리자 계정은 삭제할 수 없습니다.'};
  const key=Object.keys(messages).find(k=>params.get(k)==='true');
  const toast=document.getElementById('toast');
  if(key && toast){toast.textContent=messages[key];toast.setAttribute('role','status');toast.classList.add('show');setTimeout(()=>toast.classList.remove('show'),3000);}
 });
})();
