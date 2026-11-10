/* Deterministic scene engine: window.seek(t) draws the exact frame for time t (seconds). No CSS transitions, so every
   frame can be captured in any order. Stage is 1920x1080. Scenes come from window.PROJECT (see template/scenes.example.js). */
(function(){
const P=window.PROJECT, stage=document.getElementById('stage');
const clamp=(x,a=0,b=1)=>Math.min(b,Math.max(a,x));
const easeOut=p=>1-Math.pow(1-p,3);
const back=p=>{const c=1.7;return 1+(c+1)*Math.pow(p-1,3)+c*Math.pow(p-1,2)};
const S=(d)=>`<svg viewBox="0 0 24 24" fill="currentColor">${d}</svg>`;
const ICON={
 bulb:S('<path d="M12 2a7 7 0 0 0-4 12.7V17a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1v-2.3A7 7 0 0 0 12 2zm-2 18h4v1a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1z"/>'),
 doc:S('<path d="M6 2h8l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm7 1.5V8h4.5zM8 12h8v1.6H8zm0 3.5h8V17H8z"/>'),
 q:S('<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-1 15v-2h2v2zm3.1-6.2c-.6.6-1.1 1-1.1 2.2h-2c0-1.8.8-2.5 1.5-3.1.5-.4.8-.7.8-1.2 0-.7-.6-1.2-1.3-1.2s-1.3.5-1.4 1.3l-2-.2C8.8 7.4 10.1 6 12 6c1.8 0 3.2 1.1 3.2 2.7 0 1.1-.6 1.7-1.1 2.1z"/>'),
 check:S('<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-1.5 14.5L6 12l1.4-1.4 3.1 3.1 6.1-6.1L18 9z"/>'),
 cursor:S('<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-3 5l9 4-3.6 1.4L13 16z"/>'),
 smile:S('<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM8.5 9a1.3 1.3 0 1 1 0 2.6 1.3 1.3 0 0 1 0-2.6zm7 0a1.3 1.3 0 1 1 0 2.6 1.3 1.3 0 0 1 0-2.6zM12 18c-2.3 0-4-1.4-4.6-3h9.2c-.6 1.6-2.3 3-4.6 3z"/>'),
 x:S('<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm4.2 12.8l-1.4 1.4-2.8-2.8-2.8 2.8-1.4-1.4 2.8-2.8-2.8-2.8 1.4-1.4 2.8 2.8 2.8-2.8 1.4 1.4-2.8 2.8z"/>'),
 bell:S('<path d="M12 22a2.2 2.2 0 0 0 2.2-2.2H9.8A2.2 2.2 0 0 0 12 22zm6-6V11a6 6 0 0 0-4.5-5.8V4.5a1.5 1.5 0 0 0-3 0v.7A6 6 0 0 0 6 11v5l-2 2v1h16v-1z"/>'),
 follow:S('<path d="M10 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.3 0-8 1.7-8 5v1h12v-1c0-1.3.5-2.4 1.3-3.2C14.2 14.7 12 14 10 14zm9-5V6h-2v3h-3v2h3v3h2v-3h3V9z"/>'),
 share:S('<path d="M12 2l5 5-1.4 1.4L13 5.8V15h-2V5.8L8.4 8.4 7 7zM5 11h3v2H5v7h14v-7h-3v-2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"/>'),
 mic:S('<path d="M12 14a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.9V21h2v-3.1A7 7 0 0 0 19 11z"/>'),
 play:S('<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-2 14V8l6 4z"/>'),
 rocket:S('<path d="M12 2c3 2 5 5.5 5 9.5L15 14h-6l-2-2.5C7 7.5 9 4 12 2zm0 5a1.6 1.6 0 1 0 0 3.2A1.6 1.6 0 0 0 12 7zM8 15h8l1 4-3-1-2 3-2-3-3 1z"/>')
 };
const el=(cls,html,css)=>{const n=document.createElement('div');n.className=cls;if(html!=null)n.innerHTML=html;if(css)Object.assign(n.style,css);return n};
const pos=(n,o)=>{n.style.left=(o.x||0)+'px';n.style.top=(o.y||0)+'px';if(o.w)n.style.width=o.w+'px';if(o.h)n.style.height=o.h+'px';
  n._base=o.anchor==='c'?'translateX(-50%)':''; if(o.anchor==='c')n.style.textAlign='center';};
const ico=k=>ICON[k]||'';
const build={
 ghost:(o,sc)=>{const n=el('ghost',o.text);sc.node.appendChild(n);sc.ghost=n;return[]},
 kicker:(o)=>{const n=el('el kicker'+(o.small?' small':''),o.text);pos(n,o);return[{n,at:o.at,dur:.3,anim:o.anim||'rise'}]},
 sub:(o)=>{const n=el('el sub',o.text);pos(n,o);return[{n,at:o.at,dur:.3,anim:o.anim||'rise'}]},
 pill:(o)=>{const n=el('el pill '+(o.tone||'dark'),(o.icon?ico(o.icon):'')+o.text);if(o.icon)n.querySelector('svg').style.cssText='width:30px;height:30px';pos(n,o);return[{n,at:o.at,dur:.3,anim:o.anim||'pop'}]},
 headline:(o)=>{const n=el('el headline '+(o.size||'l')+' '+(o.color||'crimson')+(o.outline?' outline':''));pos(n,o);
   const items=[];String(o.text).split(' ').forEach((w,i)=>{const s=el('w',w);n.appendChild(s);items.push({n:s,at:o.at+i*(o.stagger??.14),dur:.28,anim:o.anim||'pop',base:''})});
   if(o.strike){n.innerHTML='';const s=el('strike',o.text);const b=document.createElement('b');s.appendChild(b);n.appendChild(s);
     items.length=0;items.push({n:s,at:o.at,dur:.3,anim:'pop',base:''},{n:b,at:o.strike,dur:.35,anim:'wipe',base:''});}
   n._skipItem=true;return[{n,at:0,dur:.01,anim:'none'},...items]},
 chips:(o)=>{const n=el('el chips');pos(n,o);const items=[];(o.items||[]).forEach((t,i)=>{const p=el('pill '+(o.active===i?'red':'light'),t);n.appendChild(p);items.push({n:p,at:o.at+i*.12,dur:.28,anim:'pop',base:''})});return[{n,at:0,dur:.01,anim:'none'},...items]},
 card:(o,sc)=>{const w=o.w||560,h=o.h||780;const n=el('card'+(o.red?' redcard':''),(o.num?`<div class="num">${o.num}</div>`:'')+(o.timer?'<div class="tm"></div>':ico(o.icon))+(o.label?`<div class="label">${o.label}</div>`:''));
   n.style.color=o.red?'#fff':'var(--crimson)';pos(n,{x:o.x,y:o.y,w,h,anchor:o.anchor});const it=[{n,at:o.at,dur:.4,anim:o.anim||'pop'}];
   if(o.brackets){const b=el('brackets','<i></i><i></i><i></i><i></i>');pos(b,{x:(o.x||0)-34,y:(o.y||0)-34,w:w+68,h:h+68});b.style.position='absolute';b._base='';it.push({n:b,at:o.at+.1,dur:.3,anim:'fade',base:''});sc._extra.push(b)}
   if(o.timer){const t=n.querySelector('.tm');const to=o.timer.to||3,from=o.timer.from||0,secs=o.timer.secs||(to-from);
     t.style.cssText='position:relative;width:70%;aspect-ratio:1;display:flex;align-items:center;justify-content:center;font-weight:900;font-size:96px;color:var(--ink)';
     t.innerHTML='<svg viewBox="0 0 100 100" style="position:absolute;inset:0;width:100%;height:100%"><circle cx="50" cy="50" r="44" fill="none" stroke="#D9C79A" stroke-width="8"/><circle class="arc" cx="50" cy="50" r="44" fill="none" stroke="#9A0B12" stroke-width="8" stroke-linecap="round" transform="rotate(-90 50 50)" stroke-dasharray="276.5" stroke-dashoffset="276.5"/></svg><span class="v">0:00</span>';
     n._upd=(lt)=>{const k=clamp((lt-o.at)/secs);t.querySelector('.arc').setAttribute('stroke-dashoffset',276.5*(1-k));t.querySelector('.v').textContent='0:0'+Math.min(to,Math.floor(from+k*(to-from+.999)))}}
   return it},
 row:(o)=>{const items=[];const cw=o.cw||250,ch=o.ch||330,gap=o.gap||90;const total=o.items.length*cw+(o.items.length-1)*gap;let x=o.x??(1920-total)/2;
   o.items.forEach((c,i)=>{const n=el('card',`<div class="num">${String(i+1).padStart(2,'0')}</div><div class="round" style="width:96px;height:96px;background:var(--crimson);color:#fff;box-shadow:none">${ico(c.icon)}</div><div class="label" style="font-size:30px">${c.label}</div>`);
     pos(n,{x,y:o.y,w:cw,h:ch});n.style.color='#fff';n.style.borderRadius='38px';items.push({n,at:o.at+i*(o.stagger||.7),dur:.4,anim:'pop',base:''});
     if(i<o.items.length-1){const p=el('el','+',{left:(x+cw+gap/2)+'px',top:(o.y+ch/2-40)+'px',fontSize:'72px',fontWeight:900,color:'var(--crimson)',transform:'translateX(-50%)'});p._base='translateX(-50%)';items.push({n:p,at:o.at+i*(o.stagger||.7)+.35,dur:.25,anim:'pop'})}
     x+=cw+gap});return items},
 phone:(o)=>{const n=el('phone','<div class="screen'+(o.img?'':' ph')+'"></div>');pos(n,{x:o.x,y:o.y,w:o.w||420,h:o.h||850,anchor:o.anchor});
   if(o.img)n.firstChild.style.backgroundImage=`url("${o.img}")`;return[{n,at:o.at,dur:.45,anim:o.anim||'rise'}]},
 buttons:(o)=>{const wrap=el('el');pos(wrap,o);wrap.style.display='flex';wrap.style.gap='28px';const items=[{n:wrap,at:0,dur:.01,anim:'none'}];
   o.items.forEach((b,i)=>{const n=el('btn'+(b.dark?' dark':''),ico(b.icon)+b.label);wrap.appendChild(n);items.push({n,at:o.at+i*.25,dur:.3,anim:'pop',base:''})});return items},
 comment:(o)=>{const n=el('el comment','<div class="av"></div><div class="txt"></div><div class="post">POST</div>');pos(n,o);const txt=n.querySelector('.txt');
   n._upd=(lt)=>{const c=Math.floor(clamp((lt-o.at-.3)/(o.typeSecs||.8))*o.text.length);txt.textContent=o.text.slice(0,c)+(c<o.text.length||Math.floor(lt*2)%2?'|':'')};
   return[{n,at:o.at,dur:.3,anim:'rise'}]},
 round:(o)=>{const w=o.w||150;const n=el('el round',ico(o.icon));n.style.width=n.style.height=w+'px';n.style.color=o.color||'var(--crimson)';pos(n,{x:o.x,y:o.y,anchor:o.anchor});return[{n,at:o.at,dur:.35,anim:o.anim||'pop'}]}
};
// ---- build scenes
P.scenes.forEach(s=>{s.node=el('scene '+s.bg);s.items=[];s._extra=[];stage.appendChild(s.node);
  (s.els||[]).forEach(o=>{const its=build[o.t](o,s);its.forEach(it=>{if(it.n.parentNode!==undefined&&!it.n.parentNode)s.node.appendChild(it.n);it.base=it.base??it.n._base??'';s.items.push(it);if(it.n._upd)s.items.push({upd:it.n._upd})});});
  // attach only top-level nodes (children already inside parents)
  s.items.forEach(it=>{if(it.n&&!it.n.parentNode)s.node.appendChild(it.n)});s._extra.forEach(b=>s.node.appendChild(b));});
// dust
const dust=[];for(let i=0;i<16;i++){const d=el('dust');const r=3+((i*7)%5);d.style.width=d.style.height=r+'px';d._x=(i*337)%1920;d._y=(i*211)%1000;d._s=4+(i%5)*2;stage.appendChild(d);dust.push(d)}
// progress
const prog=el('progress');(P.progress||[]).forEach(g=>{const sg=el('seg',`<div class="lab">${g.label}</div><div class="bar"><div class="fill"></div></div>`);prog.appendChild(sg);g.fill=sg.querySelector('.fill')});stage.appendChild(prog);
function applyAnim(it,p){const n=it.n,b=it.base||'';let o=1,tf='';
  switch(it.anim){case'pop':o=clamp(p*4);tf=`scale(${.55+.45*back(p)})`;break;case'rise':o=easeOut(p);tf=`translateY(${(1-easeOut(p))*46}px)`;break;
   case'slide':o=easeOut(p);tf=`translateX(${(1-easeOut(p))*-90}px)`;break;case'fade':o=easeOut(p);break;case'none':o=1;break;
   case'wipe':n.style.width=(easeOut(p)*104)+'%';o=p>0?1:0;break;}
  n.style.opacity=o;n.style.transform=(b+' '+tf).trim();}
window.seek=function(t){
  let cur=null;P.scenes.forEach(s=>{const on=t>=s.start&&t<s.end;s.node.style.display=on?'block':'none';if(!on)return;cur=s;const lt=t-s.start;
    if(s.ghost)s.ghost.style.transform=`translate(-50%,-50%) scale(${1+.06*lt/(s.end-s.start)})`;
    s.items.forEach(it=>{if(it.upd)it.upd(lt);else applyAnim(it,clamp((lt-it.at)/it.dur))})});
  dust.forEach(d=>{d.style.left=d._x+'px';d.style.top=((d._y-t*d._s+1080*10)%1000)+'px';d.style.display=cur&&cur.bg==='cream'?'block':'none'});
  const cls=cur&&cur.bg==='red'?'on-red':'on-cream';prog.className='progress '+cls;
  (P.progress||[]).forEach(g=>g.fill.style.width=(clamp((t-g.from)/(g.to-g.from))*100)+'%');
};
window.PROJECT_DURATION=P.duration;seek(0);
})();
