const fs=require('node:fs');const vm=require('node:vm');const assert=require('node:assert/strict');
const queue=[];let captured=[];
const document={querySelectorAll:()=>[],querySelector:()=>null,addEventListener(){},createElement:tag=>({tagName:tag}),body:{append(){}}};
const context={document,location:{origin:'https://movies.example',pathname:'/',search:'',hash:''},URL,URLSearchParams,AbortController,setTimeout,clearTimeout,CustomEvent:class{},console,TypeError,window:{addEventListener(){}},fetch:async(url,options)=>{captured.push([url,options]);if(queue.length)return queue.shift();return new Response('로그인이 필요합니다.',{status:401});}};
context.window.window=context.window;vm.createContext(context);
vm.runInContext(fs.readFileSync('src/main/resources/static/js/auth-header.js','utf8'),context);
(async()=>{
 const ML=context.window.ML;await ML.ready;
 let checks=0;
 function eq(a,b){assert.equal(a,b);checks++;}
 eq(ML.safeReturn('/movies?query=%EA%B0%80#reviews'),'/movies?query=%EA%B0%80#reviews');
 eq(ML.safeReturn('/review?movieId=42'),'/review?movieId=42');
 for(const unsafe of ['https://evil.example','//evil.example','/\\evil.example','javascript:alert(1)','/login?returnUrl=/movies','/signup','/\u0000evil',null])eq(ML.safeReturn(unsafe),'/');
 eq(ML.safeReturn('/movie/../login'),'/');
 eq(ML.safeReturn('/favorite-movies'),'/favorite-movies');
 queue.push(new Response(JSON.stringify({ok:true}),{status:200}));eq((await ML.request('/api/sample')).ok,true);
 eq(captured.at(-1)[1].credentials,'same-origin');eq(captured.at(-1)[1].cache,'no-store');
 queue.push(new Response('입력 내용을 확인해 주세요.',{status:400}));
 await assert.rejects(ML.request('/api/sample'),e=>e.status===400 && e.message==='입력 내용을 확인해 주세요.');checks++;
 queue.push(new Response(JSON.stringify({message:'이미 등록되었습니다.'}),{status:409}));
 await assert.rejects(ML.request('/api/sample'),e=>e.status===409 && e.message==='이미 등록되었습니다.');checks++;
 queue.push(new Response('<html>server trace</html>',{status:500}));
 await assert.rejects(ML.request('/api/sample'),e=>!e.message.includes('server trace'));checks++;
 const hostile='<img src=x onerror=alert(1)>';const node=ML.el('p','review-content',hostile);eq(node.textContent,hostile);eq(node.innerHTML,undefined);
 eq(ML.json('PUT',{nickname:'영화산책'}).headers['Content-Type'],'application/json');
 console.log(`PASS: ${checks} client behavior checks`);
})().catch(e=>{console.error(e);process.exitCode=1;});
