(() => {
  'use strict';
  const {ML}=window;
  let movieReviews=[];
  const date=value=>value?new Date(value).toLocaleDateString('ko-KR'):'날짜 정보 없음';
  function reviewCard(review,showMovie=false){
    const article=ML.el('article','review-item');
    if(showMovie){const row=ML.el('a','review-movie');
    row.href='/movies/'+Number(review.movieId);
      const img=ML.el('img');
      img.src=review.posterUrl||'/poster/no-poster.svg';
      img.alt=review.movieTitle||'영화 포스터';
      img.loading='lazy';
      ML.poster(img);
      const title=ML.el('div');
      title.append(ML.el('h3','',review.movieTitle||'영화 정보를 불러올 수 없습니다.'),ML.el('p','muted small','영화 상세 보기 →'));
      row.append(img,title);
      article.append(row);
      }
    const top=ML.el('div','review-top'),user=ML.el('div','review-user');
    const name=review.nickname || (showMovie ? ML.member?.nickname : '관객') || '관객';
    const meta=ML.el('div');
    meta.append(ML.el('strong','',name),ML.el('small','',date(review.createdAt)+(review.updatedAt?' · 수정됨':'')));
    user.append(ML.el('span','avatar',name.slice(0,1)),meta);
    top.append(user,ML.el('span','review-rating','★'.repeat(Math.max(0,Math.min(5,Number(review.rating))))+' '+review.rating+'/5'));
    article.append(top,ML.el('p','review-content',review.content));
    if(ML.member?.userId===review.userId){
      const actions=ML.el('div','review-actions');
      const edit=ML.el('a','btn btn-sm btn-ghost','수정');
      edit.href='/review?reviewId='+review.id;
      const remove=ML.el('button','btn btn-sm btn-ghost','삭제');
      remove.type='button';
      remove.addEventListener('click',async()=>{
        if(!await ML.dialog({title:'리뷰를 삭제할까요?',message:'삭제한 리뷰는 복구할 수 없습니다.',confirm:'삭제',cancel:'취소'}))return;
        remove.disabled=true;
        try{await ML.request('/api/reviews/'+review.id,{method:'DELETE'});
        ML.toast('리뷰를 삭제했습니다.');
        if(showMovie)loadMyReviews();
        else loadMovieReviews();
        }
        catch(error){ML.handleError(error);
        remove.disabled=false;
        }
      });
      actions.append(edit,remove);
      article.append(actions);
    }
    return article;
  }
  async function failure(host,error,retry){
    const empty=ML.empty('리뷰를 불러오지 못했습니다',error.message);
    const button=ML.el('button','btn','다시 시도');
    button.type='button';
    button.addEventListener('click',retry);
    empty.append(button);
    host.replaceChildren(empty);
  }
  function renderMovieReviews(){
    const host=document.querySelector('#movieReviews');
    if(!host)return;
    const sort=document.querySelector('#reviewSort')?.value;
    const list=[...movieReviews];
    if(sort==='high')list.sort((a,b)=>b.rating-a.rating);
    if(sort==='low')list.sort((a,b)=>a.rating-b.rating);
    host.replaceChildren(...list.map(r=>reviewCard(r)));
    if(!list.length)host.append(ML.empty('아직 리뷰가 없습니다','이 영화의 첫 번째 감상을 남겨 보세요.','/review?movieId='+document.querySelector('[data-movie-id]').dataset.movieId,'첫 리뷰 남기기'));
    const own=movieReviews.find(r=>r.userId===ML.member?.userId);
    const link=document.querySelector('[data-review-link]');
    if(link){link.href=own?'/review?reviewId='+own.id:'/review?movieId='+document.querySelector('[data-movie-id]').dataset.movieId;
    link.textContent=own?'내 리뷰 수정하기':'리뷰 작성하기 ↗';
    }
  }
  async function loadMovieReviews(){
    const host=document.querySelector('#movieReviews');
    if(!host)return;
    try{
      const data=await ML.request('/api/reviews?movieId='+document.querySelector('[data-movie-id]').dataset.movieId);
      await ML.ready;
      movieReviews=data.reviews||[];
      document.querySelectorAll('[data-review-count]').forEach(e=>e.textContent=String(data.reviewCount||0));
      document.querySelector('[data-average-rating]').textContent=data.reviewCount?Number(data.averageRating).toFixed(1):'—';
      renderMovieReviews();
    }catch(error){failure(host,error,loadMovieReviews);
    }
  }
  async function loadMyReviews(){
    const host=document.querySelector('#myReviews');
    if(!host)return;
    try{
      const list=await ML.request('/api/reviews/my');
      await ML.ready;
      host.replaceChildren(...list.map(r=>reviewCard(r,true)));
      if(!list.length)host.append(ML.empty('아직 작성한 리뷰가 없습니다','기억에 남는 영화에 별점과 감상을 남겨 보세요.','/review','리뷰 작성하기'));
    }catch(error){if(error.status===401){ML.requireLogin();
    }failure(host,error,loadMyReviews);
    }
  }
  document.querySelector('#reviewSort')?.addEventListener('change',renderMovieReviews);
  const form=document.querySelector('#reviewForm');
  if(form){
    const content=form.querySelector('#reviewContent'),error=form.querySelector('#reviewError');
    const count=()=>document.querySelector('#characterCount').textContent=String(content.value.length);
    count();
    content.addEventListener('input',count);
    form.addEventListener('submit',async event=>{
      event.preventDefault();
      error.textContent='';
      const rating=new FormData(form).get('rating');
      if(!rating){error.textContent='별점을 선택해 주세요.';
      return;
      }
      if(!content.value.trim()){error.textContent='리뷰 내용을 입력해 주세요.';
      content.focus();
      return;
      }
      const button=form.querySelector('[type=submit]');
      button.disabled=true;
      try{
        const id=form.dataset.reviewId;
        await ML.request('/api/reviews'+(id?'/'+id:''),{method:id?'PUT':'POST',body:new URLSearchParams({movieId:form.dataset.movieId,rating,content:content.value.trim()})});
        await ML.dialog({title:id?'리뷰를 수정했습니다':'리뷰를 등록했습니다',message:'소중한 감상을 남겨 주셔서 감사합니다.'});
        location.href='/movies/'+form.dataset.movieId+'#reviews';
      }catch(e){error.textContent=e.message;
      if(e.status===401)ML.requireLogin();
      }
      finally{button.disabled=false;
      }
    });
  }
  loadMovieReviews();
  loadMyReviews();
})();
