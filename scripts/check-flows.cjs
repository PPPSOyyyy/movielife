const fs=require('fs'),vm=require('vm'),assert=require('assert/strict');
const root='src/main/resources/static/js/';
class E {
 constructor(){this.events={};this.children=[];this.dataset={};this.style={};this.value='';this.classList={toggle(){}};this.textContent='';}
 addEventListener(k,f){this.events[k]=f} append(...a){this.children.push(...a)} replaceChildren(...a){this.children=a} setAttribute(k,v){this[k]=v} removeAttribute(k){delete this[k]} focus(){} remove(){} reset(){} reportValidity(){} showModal(){this.open=true} close(v){this.open=false;this.returnValue=v;this.events.close?.()} querySelector(s){return this.lookup?.(s)||null}
}
const tick=()=>new Promise(r=>setTimeout(r,0));
function setup(names){
 const ids=Object.fromEntries(names.map(n=>[n,new E()])),body=new E(),calls=[],buttons=[],nav=new E();let handler;
 const find=s=>s.startsWith('#')?ids[s.slice(1)]||null:null;
 const doc={body,activeElement:null,createElement:()=>new E(),getElementById:n=>ids[n],querySelector:find,querySelectorAll:s=>s==='[data-check]'?buttons:s==='.ml-auth, .header-right'?[nav]:[],addEventListener:(k,f)=>handler=f};
 for(const e of Object.values(ids))e.lookup=s=>s==='[type=submit]'?e.submit??=new E():find(s);
 const ctx={console,document:doc,URL,URLSearchParams,TextEncoder,AbortController,TypeError,CustomEvent:class{},FormData:class{get(){return '5'}},setTimeout,clearTimeout,location:{origin:'http://localhost:8080',pathname:'/',search:'',hash:'',href:''},addEventListener(){},fetch:async(url,opt)=>{calls.push([url,opt]);return new Response(JSON.stringify(url.includes('/check-')?false:url==='/api/members/me'?{userId:'tester',nickname:'테스터'}:url==='/api/favorites/my'?[]:{ok:true}));}};
 ctx.window=ctx;vm.createContext(ctx);const run=n=>vm.runInContext(fs.readFileSync(root+n,'utf8'),ctx);run('auth-header.js');
 return {ids,body,calls,buttons,nav,ctx,run,clickFavorite: b=>handler({target:{closest:s=>s==='[data-favorite]'?b:null},preventDefault(){}})};
}
function submit(h,n){return h.ids[n].events.submit({preventDefault(){},target:h.ids[n]})}
async function confirm(h){await tick();const d=h.body.children.findLast(x=>x.open);assert(d,'central dialog opens');d.children.at(-1).children.at(-1).events.click();}
(async()=>{
 let h=setup(['loginForm','loginUserId','loginPassword','loginError']);h.run('account.js');h.ids.loginUserId.value='tester';h.ids.loginPassword.value='Password9!';let p=submit(h,'loginForm');await confirm(h);await p;assert.equal(h.ctx.location.href,'/');assert(h.calls.some(x=>x[0]==='/api/members/login'));assert.equal(h.nav.children[0].textContent,'테스터님');
 h=setup(['signupForm','signupUserId','signupNickname','signupPassword','signupPasswordConfirm','signupError','userIdCheck','nicknameCheck']);for(const key of ['userId','nickname']){let b=new E();b.dataset.check=key;h.buttons.push(b)}h.run('account.js');h.ids.signupUserId.value='tester';h.ids.signupNickname.value='테스터';for(const b of h.buttons)await b.events.click();h.ids.signupPassword.value=h.ids.signupPasswordConfirm.value='Password9!';p=submit(h,'signupForm');await tick();assert.equal(h.ctx.location.href,'');await confirm(h);await p;assert(h.ctx.location.href.startsWith('/login?'));
 h=setup(['withdrawOpen','withdrawDialog','withdrawForm','withdrawError','withdrawPassword']);h.run('account.js');h.ids.withdrawOpen.events.click();assert(h.ids.withdrawDialog.open);h.ids.withdrawPassword.value='Password9!';p=submit(h,'withdrawForm');await confirm(h);await p;assert(h.calls.some(x=>x[0]==='/api/members/withdraw'&&x[1].method==='DELETE'));
 h=setup(['reviewForm','reviewContent','reviewError','characterCount']);h.ids.reviewForm.dataset.movieId='42';h.ids.reviewContent.value='좋은 영화입니다';h.run('reviews.js');p=submit(h,'reviewForm');await confirm(h);await p;assert(h.calls.some(x=>x[0]==='/api/reviews'&&x[1].method==='POST'));assert.equal(h.ctx.location.href,'/movies/42#reviews');
 h=setup([]);await h.ctx.ML.ready;h.ctx.ML.toast=()=>{};let b=new E();b.dataset.favorite='42';await h.clickFavorite(b);assert(h.ctx.ML.favorites.has(42));await h.clickFavorite(b);assert(!h.ctx.ML.favorites.has(42));let original=h.ctx.ML;h.run('auth-header.js');assert.equal(h.ctx.ML,original);
 h=setup([]);const section=new E(),result=new E(),provider=new E();provider.dataset.provider='8';section.querySelector=s=>s==='[data-ott-results]'?result:null;section.querySelectorAll=()=>[provider];h.ctx.document.querySelectorAll=s=>s==='[data-ott-section]'?[section]:[];let fail=true;h.ctx.fetch=async()=>new Response(JSON.stringify(fail?{message:'TMDB 인증 실패'}:[]),{status:fail?502:200});h.run('catalog.js');await tick();assert.equal(result.children[0].children[1].textContent,'TMDB 인증 실패');fail=false;result.children[0].children.at(-1).events.click();await tick();assert.equal(result.children[0].children[0].textContent,'제공 중인 영화가 없습니다');
 console.log('PASS: actual common + account/reviews scripts: login/header, signup confirmation, withdrawal, review POST, favorite add/remove, duplicate initialization, OTT failure/retry');
})().catch(e=>{console.error(e);process.exitCode=1});
