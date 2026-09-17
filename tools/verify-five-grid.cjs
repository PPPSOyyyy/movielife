const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
class Node {
 constructor(cls='',id){this.className=cls;this.id=id;this.children=[];this.events={};this.classList={contains:x=>this.className.split(' ').includes(x)};}
 matches(selector){return selector.split(',').some(s=>this.className.split(' ').includes(s.trim().slice(1)));}
 append(...nodes){for(const n of nodes){if(n.parent)n.parent.children.splice(n.parent.children.indexOf(n),1);n.parent=this;this.children.push(n);}}
 replaceWith(...nodes){const parent=this.parent,index=parent.children.indexOf(this);for(const n of nodes){if(n.parent)n.parent.children.splice(n.parent.children.indexOf(n),1);n.parent=parent;}parent.children.splice(index,1,...nodes);this.parent=null;}
 querySelector(selector){if(selector.startsWith(':scope'))return this.children.find(n=>n.className==='movie-tail');for(const n of this.children){if(n.matches(selector))return n;const nested=n.querySelector(selector);if(nested)return nested;}}
 setAttribute(){}addEventListener(k,v){this.events[k]=v;}scrollBy(){}
}
let assertions=0;
for(const count of [0,1,5,7,18,20,82]){
 const grid=new Node('movie-grid');for(let i=0;i<count;i++)grid.append(new Node('movie-card',i));let observer;
 class Observer {constructor(cb){this.cb=cb;observer=this;}disconnect(){}observe(){}}
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../src/main/resources/static/js/balanced-grid.js'),'utf8'),{document:{querySelectorAll:()=>[grid],createElement:()=>new Node()},MutationObserver:Observer,matchMedia:()=>({matches:false})});
 function ids(){return grid.children.flatMap(n=>n.className==='movie-tail'?n.querySelector('.movie-tail-track').children:[n]).map(n=>n.id);}
 assert.deepEqual(ids(),Array.from({length:count},(_,i)=>i));assertions++;
 if(count>5&&count%5){assert.equal(grid.children.filter(n=>n.className==='movie-card').length%5,0);assert.equal(grid.querySelector('.movie-tail-track').children.length,5+count%5);assertions+=2;}
 grid.append(new Node('movie-card',count));observer.cb([{target:grid}]);assert.deepEqual(ids(),Array.from({length:count+1},(_,i)=>i));assertions++;
}
console.log('Five-column grid: '+assertions+' assertions passed; order and every card preserved.');
