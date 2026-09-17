(() => {
'use strict'; const {ML}=window; const $=s=>document.querySelector(s);
const questions=ML.request('/api/members/security-questions').then(q=>{
 document.querySelectorAll('[data-security-questions]').forEach(select=>Object.entries(q).forEach(([k,v])=>{if([...select.options].some(o=>o.value===k))return;const o=document.createElement('option');o.value=k;o.textContent=v;select.append(o);}));
}).catch(()=>{});
function bind(selector,url,method,success,before){const form=$(selector);if(!form)return;form.addEventListener('submit',async e=>{e.preventDefault();const status=form.querySelector('[data-status]'),btn=form.querySelector('button');status.textContent='';btn.disabled=true;try{const data=Object.fromEntries(new FormData(form));if(before)before(data);const result=await ML.request(url,ML.json(method,data));status.textContent=result.message||'';await success?.(result,data);}catch(e){status.textContent=e.message;}finally{btn.disabled=false;}});}
let recoveryId='';
bind('#findIdForm','/api/members/find-id','POST',r=>$('#findIdForm [data-status]').textContent='가입한 아이디: '+r.userId);
bind('#questionForm','/api/members/recovery/question','POST',(r,d)=>{recoveryId=d.userId;$('#recoveryQuestion').textContent=r.question;$('#answerForm').hidden=false;$('#resetForm').hidden=true;$('#answerForm').reset();},()=>{$('#answerForm').hidden=true;$('#resetForm').hidden=true;});
bind('#answerForm','/api/members/recovery/verify','POST',()=>{$('#resetForm').hidden=false;$('#answerForm').hidden=true;},d=>{d.userId=recoveryId;$('#resetForm').hidden=true;});
bind('#resetForm','/api/members/recovery/reset','POST',async()=>{await ML.dialog({title:'비밀번호 변경 완료',message:'새 비밀번호로 로그인해 주세요.'});location.href='/login';});
if($('#personalForm')) ML.ready.then(async m=>{if(!m){ML.requireLogin();return;}await questions;$('#accountUserId').value=m.userId;['name','nickname','email','securityQuestion'].forEach(k=>$('#personalForm').elements[k].value=m[k]||'');});
bind('#personalForm','/api/members/account','PUT');
bind('#passwordForm','/api/members/password','PUT',()=>$('#passwordForm').reset());
})();
