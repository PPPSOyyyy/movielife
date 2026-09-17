/* Run: node tools/verify-client.cjs (no npm install required). */
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict'),cp=require('node:child_process');
const root=path.resolve(__dirname,'../src/main/resources/static/js');let checks=0;
for(const file of fs.readdirSync(root).filter(x=>x.endsWith('.js'))){cp.execFileSync(process.execPath,['--check',path.join(root,file)]);checks++;}
function theme(saved,systemDark,blocked=false){const events={},listeners={},select={value:'',addEventListener:(n,f)=>listeners[n]=f};const doc={documentElement:{dataset:{},style:{}},querySelectorAll:()=>[select],querySelector:()=>null,addEventListener:(n,f)=>events[n]=f};const media={matches:systemDark,addEventListener:(n,f)=>events.media=f};const storage={getItem:()=>{if(blocked)throw Error();return saved;},setItem:(k,v)=>{if(blocked)throw Error();saved=v;}};vm.runInNewContext(fs.readFileSync(path.join(root,'theme.js'),'utf8'),{document:doc,localStorage:storage,matchMedia:()=>media,window:{addEventListener:(n,f)=>events[n]=f}});events.DOMContentLoaded();return {doc,events,select,media,listeners,saved:()=>saved};}
let t=theme(null,true);assert.equal(t.doc.documentElement.dataset.theme,'light');checks++;
t=theme('dark',false);assert.equal(t.doc.documentElement.dataset.theme,'dark');checks++;
t=theme('system',true);assert.equal(t.doc.documentElement.dataset.theme,'dark');t.media.matches=false;t.events.media();assert.equal(t.doc.documentElement.dataset.theme,'light');checks+=2;
t.select.value='dark';t.listeners.change();assert.equal(t.saved(),'dark');t.media.matches=true;t.events.media();assert.equal(t.doc.documentElement.dataset.theme,'dark');checks+=2;
t.events.storage({key:'movielife-theme',newValue:'light'});assert.equal(t.doc.documentElement.dataset.theme,'light');checks++;
t=theme(null,true,true);t.select.value='dark';t.listeners.change();assert.equal(t.doc.documentElement.dataset.theme,'dark');checks++;
const source=fs.readFileSync(path.join(root,'ml-core.js'),'utf8');const store=new Map();const context={ML:{},URL,URLSearchParams,location:{origin:'https://example.test',pathname:'/mypage',search:'',hash:''},sessionStorage:{setItem:(k,v)=>store.set(k,v),getItem:k=>store.get(k),removeItem:k=>store.delete(k)},scrollY:543,Date,requestAnimationFrame:f=>f(),scrollTo:v=>context.restored=v.top};
vm.runInNewContext(source.slice(source.indexOf('  ML.safeReturn ='),source.indexOf('  ML.ready =')),context);
for(const bad of ['//evil.test','/\\evil.test','https://evil.test','javascript:alert(1)','/\n/evil.test']){assert.equal(context.ML.safeReturn(bad),'/');checks++;}
assert.equal(context.ML.safeReturn('/movies/42?x=1#reviews'),'/movies/42?x=1#reviews');checks++;
assert.equal(decodeURIComponent(context.ML.reviewReturn()),'/mypage#reviewsPanel');context.ML.restoreReviewScroll();assert.equal(context.restored,543);assert.equal(store.size,0);checks+=3;
console.log(`Client verification: ${checks} checks passed (syntax, theme, safe return, review scroll).`);
