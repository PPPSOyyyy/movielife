(() => {
  'use strict';
  const {ML}=window;
  const $=selector=>document.querySelector(selector);
  const validPassword=value=>value.length>=8 && value.length<=50 && new TextEncoder().encode(value).length<=72 && /[A-Za-z]/.test(value) && /[0-9]/.test(value) && /[^A-Za-z0-9\s]/.test(value) && !/\s/.test(value);
  const passwordMessage='비밀번호는 영문, 숫자, 특수문자를 포함해 공백 없이 8~50자로 입력해 주세요. (최대 72바이트)';
  const returnUrl=ML.returnUrl();
  document.querySelectorAll('[data-password-toggle]').forEach(button=>button.addEventListener('click',()=>{
    const input=document.getElementById(button.dataset.passwordToggle);
    const show=input.type==='password';
    input.type=show?'text':'password';
    button.textContent=show?'숨김':'보기';
    button.setAttribute('aria-label',show?'비밀번호 숨기기':'비밀번호 표시');
  }));
  if($('#signupReturnLink'))$('#signupReturnLink').href='/signup?returnUrl='+encodeURIComponent(returnUrl);
  if($('#loginReturnLink'))$('#loginReturnLink').href='/login?returnUrl='+encodeURIComponent(returnUrl);
  $('#loginForm')?.addEventListener('submit',async event=>{
    event.preventDefault();
    const button=event.target.querySelector('[type=submit]');
    const error=$('#loginError');
    error.textContent='';
    button.disabled=true;
    try{await ML.request('/api/members/login',ML.json('POST',{userId:$('#loginUserId').value.trim(),password:$('#loginPassword').value}));
    await ML.request('/api/members/me');
    location.href=returnUrl;
    }
    catch(e){error.textContent=e.message;
    $('#loginPassword').setAttribute('aria-invalid','true');
    }
    finally{button.disabled=false;
    }
  });
  $('#loginPassword')?.addEventListener('input',()=>$('#loginPassword').removeAttribute('aria-invalid'));
  const signup=$('#signupForm');
  if(signup){
    const checked={userId:null,nickname:null};
    const fields={userId:$('#signupUserId'),nickname:$('#signupNickname')};
    const patterns={userId:/^[A-Za-z0-9_]{4,20}$/,nickname:/^[가-힣A-Za-z0-9_]{2,20}$/};
    for(const key of Object.keys(fields))fields[key].addEventListener('input',()=>{checked[key]=null;$('#'+key+'Check').textContent='';});
    document.querySelectorAll('[data-check]').forEach(button=>button.addEventListener('click',async()=>{
      const key=button.dataset.check,input=fields[key],hint=$('#'+key+'Check'),value=input.value.trim();
      input.value=value;
      checked[key]=null;
      if(!patterns[key].test(value)){hint.textContent=key==='userId'?'아이디 입력 형식을 확인해 주세요.':'닉네임 입력 형식을 확인해 주세요.';
      hint.style.color='#ff8194';
      input.reportValidity();
      return;
      }
      button.disabled=true;
      try{
        const duplicate=await ML.request('/api/members/check-'+(key==='userId'?'userid':'nickname')+'?'+key+'='+encodeURIComponent(value));
        if(input.value.trim()!==value)return;
        checked[key]=duplicate?null:value;
        hint.textContent=duplicate?'이미 사용 중입니다.':'사용할 수 있습니다.';
        hint.style.color=duplicate?'#ff8194':'#8be0b9';
      }catch(e){hint.textContent=e.message;
      hint.style.color='#ff8194';
      }
      finally{button.disabled=false;
      }
    }));
    signup.addEventListener('submit',async event=>{
      event.preventDefault();
      const error=$('#signupError');
      error.textContent='';
      if(checked.userId!==fields.userId.value.trim()||checked.nickname!==fields.nickname.value.trim()){error.textContent='아이디와 닉네임 중복확인을 완료해 주세요.';
      return;
      }
      const password=$('#signupPassword').value;
      if(!validPassword(password)){error.textContent=passwordMessage;
      return;
      }
      if(password!==$('#signupPasswordConfirm').value){error.textContent='비밀번호가 일치하지 않습니다.';
      return;
      }
      const button=signup.querySelector('[type=submit]');
      button.disabled=true;
      try{
        await ML.request('/api/members/signup',ML.json('POST',{userId:fields.userId.value.trim(),nickname:fields.nickname.value.trim(),password}));
        // 자동 이동하지 않고 사용자가 확인을 누른 뒤 이동합니다.
        await ML.dialog({title:'회원가입이 완료되었습니다',message:'movieLife에 오신 것을 환영합니다.\n로그인하고 나만의 영화 기록을 시작해 보세요.'});
        location.href='/login?returnUrl='+encodeURIComponent(returnUrl);
      }catch(e){error.textContent=e.message;
      }
      finally{button.disabled=false;
      }
    });
  }
  if(document.querySelector('[data-account-page], [data-profile-page]')){
    ML.ready.then(member=>{
      if(!member){ML.requireLogin();
      return;
      }
      document.querySelectorAll('[data-nickname]').forEach(e=>e.textContent=member.nickname);
      document.querySelectorAll('[data-user-id]').forEach(e=>e.textContent='@'+member.userId);
      document.querySelectorAll('[data-avatar]').forEach(e=>e.textContent=member.nickname.slice(0,1));
      if($('#profileUserId')){$('#profileUserId').value=member.userId;
      $('#profileNickname').value=member.nickname;
      }
    });
  }
  $('#profileForm')?.addEventListener('submit',async event=>{
    event.preventDefault();
    const form=event.target,error=$('#profileError');
    error.textContent='';
    const password=$('#newPassword').value, confirmation=$('#newPasswordConfirm').value,current=$('#currentPassword').value;
    if(password||confirmation||current){
      if(!current){error.textContent='현재 비밀번호를 입력해 주세요.';
      return;
      }
      if(!validPassword(password)){error.textContent=passwordMessage;
      return;
      }
      if(password!==confirmation){error.textContent='새 비밀번호가 일치하지 않습니다.';
      return;
      }
    }
    const button=form.querySelector('[type=submit]');
    button.disabled=true;
    try{
      await ML.request('/api/members/profile',ML.json('PUT',{nickname:$('#profileNickname').value.trim(),currentPassword:current,password}));
      await ML.dialog({title:'프로필을 수정했습니다',message:'변경한 정보가 반영되었습니다.'});
      location.href='/mypage';
    }catch(e){error.textContent=e.message;
    if(e.status===401)ML.requireLogin();
    }
    finally{button.disabled=false;
    }
  });
  async function activity(){
    if(!$('#recentReviews'))return;
    const member=await ML.ready;
    if(!member)return;
    const results=await Promise.allSettled([ML.request('/api/favorites/my'),ML.request('/api/reviews/my')]);
    if(results[0].status==='fulfilled')$('#favoriteStat').textContent=results[0].value.length;
    if(results[1].status==='fulfilled'){
      const reviews=results[1].value;
      $('#reviewStat').textContent=reviews.length;
      $('#ratingStat').textContent=reviews.length?(reviews.reduce((n,r)=>n+Number(r.rating),0)/reviews.length).toFixed(1):'—';
      const recent=$('#recentReviews');
      recent.replaceChildren();
      reviews.slice(0,2).forEach(review=>{
        const a=ML.el('a','review-item');
        a.href='/review?reviewId='+review.id;
        const head=ML.el('div','review-top');
        head.append(ML.el('strong','small',review.movieTitle||'영화 정보 없음'),ML.el('span','review-rating','★ '+review.rating+' / 5'));
        a.append(head,ML.el('p','review-content',review.content.length>110?review.content.slice(0,110)+'…':review.content));
        recent.append(a);
      });
      if(!reviews.length)recent.append(ML.empty('아직 남긴 감상이 없습니다','영화 한 편에 나만의 별점과 리뷰를 남겨 보세요.','/review','리뷰 작성하기'));
    }else $('#recentReviews').replaceChildren(ML.empty('기록을 불러오지 못했습니다','잠시 후 다시 시도해 주세요.'));
    if(results.some(r=>r.status==='rejected')){
      $('#statsError').textContent='일부 활동 정보를 불러오지 못했습니다. ';
      const retry=ML.el('button','btn btn-sm','다시 시도');
      retry.type='button';
      retry.addEventListener('click',()=>{$('#statsError').replaceChildren();activity();});
      $('#statsError').append(retry);
    }
  }
  activity();
  const dialog=$('#withdrawDialog');
  $('#withdrawOpen')?.addEventListener('click',()=>{$('#withdrawForm').reset();$('#withdrawError').textContent='';dialog.showModal();});
  document.querySelectorAll('[data-close-dialog]').forEach(button=>button.addEventListener('click',()=>button.closest('dialog').close()));
  $('#withdrawForm')?.addEventListener('submit',async event=>{
    event.preventDefault();
    const button=event.target.querySelector('[type=submit]');
    button.disabled=true;
    $('#withdrawError').textContent='';
    try{
      await ML.request('/api/members/withdraw',ML.json('DELETE',{password:$('#withdrawPassword').value}));
      dialog.close();
      await ML.dialog({title:'회원탈퇴가 완료되었습니다',message:'이용해 주셔서 감사합니다.'});
      location.href='/';
    }catch(e){$('#withdrawError').textContent=e.message;
    }
    finally{button.disabled=false;
    }
  });
})();
