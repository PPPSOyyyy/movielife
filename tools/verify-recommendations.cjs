const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
function node(group,value){const selected=new Set();return {dataset:{group,value},attrs:{},events:{},children:[],disabled:false,classList:{contains:x=>selected.has(x),remove:x=>selected.delete(x),toggle:x=>{if(selected.has(x)){selected.delete(x);return false;}selected.add(x);return true;}},setAttribute(k,v){this.attrs[k]=v;},addEventListener(k,v){this.events[k]=v;},append(...c){this.children.push(...c);},replaceChildren(...c){this.children=c;}};}
(async()=>{
const choices=[node('genre','액션'),node('genre','코미디'),node('mood','laugh'),node('rating','8'),node('rating','9')];
const button=node(),host=node(),result=node();let request,fail=false;
const doc={querySelectorAll:()=>choices,getElementById:id=>({recommendButton:button,recommendationList:host,recommendationResult:result})[id]};
const ML={request:async url=>{request=url;if(fail)throw Error('실제 실패');return [{id:1,voteAverage:8.4,siteRating:null}];},card:()=>{let n=node();n.querySelector=()=>n;return n;},el:(tag,cls,text)=>({text}),empty:()=>node(),syncFavorites:async()=>{}};
// Score container needs append, just like its browser DOM counterpart.
ML.el=(tag,cls,text)=>Object.assign(node(),{text});
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../src/main/resources/static/js/recommendations.js'),'utf8'),{document:doc,ML,URLSearchParams});
choices[0].events.click();choices[1].events.click();choices[2].events.click();
assert.equal(choices[0].attrs['aria-pressed'],'true');assert.equal(choices[1].attrs['aria-pressed'],'true');
choices[3].events.click();choices[4].events.click();assert.equal(choices[3].attrs['aria-pressed'],'false');assert.equal(choices[4].attrs['aria-pressed'],'true');
await button.events.click();const p=new URL(request,'https://test.invalid').searchParams;assert.equal(p.get('genre'),'액션,코미디');assert.equal(p.get('mood'),'laugh');assert.equal(p.get('rating'),'9');assert.equal(button.disabled,false);assert.equal(host.attrs['aria-busy'],'false');
assert.equal(host.children[0].children[0].children[1].text,'회원 ★ 아직 평가 없음');
choices[0].events.click();assert.equal(choices[0].attrs['aria-pressed'],'false');
fail=true;await button.events.click();assert.equal(host.textContent,'실제 실패');assert.equal(button.disabled,false);
console.log('Recommendation client: 13 assertions passed');
})().catch(e=>{console.error(e);process.exitCode=1;});
