/* EXAMPLE PROJECT: rebuild of reference video 2 ("Script ka formula"), ~49 s. Replace text/timings for your own topic.
   Every scene: bg cream|red, start/end (s, from your voiceover), els = elements with `at` (s after scene start).
   Coordinates are px on the 1920x1080 stage. Text column x=150; object column x=1190..1750.  */
window.PROJECT={duration:49.1,
progress:[{label:'INTRO',from:0,to:14.4},{label:'STEPS',from:14.4,to:37.2},{label:'CTA',from:37.2,to:49.1}],
scenes:[
{bg:'cream',start:0,end:2.4,els:[
 {t:'kicker',text:'Kya?',small:true,x:150,y:150,at:0},
 {t:'headline',text:'Script',size:'xl',x:150,y:200,at:.1},
 {t:'card',icon:'doc',red:true,x:1190,y:150,at:.2,brackets:true,w:560,h:700},
 {t:'round',icon:'q',x:1660,y:760,w:150,at:.6}]},
{bg:'red',start:2.4,end:7.2,els:[
 {t:'ghost',text:'FORMULA'},
 {t:'headline',text:'Dekho,',size:'l',x:150,y:120,at:.1},
 {t:'kicker',text:'Main batata hoon',x:150,y:290,at:.5,small:false},
 {t:'card',icon:'bulb',x:1330,y:120,w:300,h:300,at:.3,brackets:true},
 {t:'kicker',text:'Is formula ko',x:150,y:560,at:1.9},
 {t:'headline',text:'Formula',size:'xl',x:150,y:620,at:2.1}]},
{bg:'cream',start:7.2,end:14.4,els:[
 {t:'kicker',text:'Script ka formula',small:true,anchor:'c',x:960,y:90,at:0},
 {t:'row',y:330,at:.4,stagger:1.2,cw:250,ch:340,gap:100,items:[{icon:'cursor',label:'Hook'},{icon:'q',label:'Problem'},{icon:'bulb',label:'Solution'},{icon:'check',label:'Result'},{icon:'cursor',label:'CTA'}]}]},
{bg:'red',start:14.4,end:17.2,els:[
 {t:'ghost',text:'BAS'},
 {t:'headline',text:'Bas!',size:'xl',x:150,y:300,at:.05},
 {t:'kicker',text:'Itna sab sunkar',x:150,y:560,at:.5,small:false},
 {t:'headline',text:'Ghabrana mat',size:'m',x:150,y:620,at:.7,stagger:.18},
 {t:'round',icon:'smile',x:1560,y:150,w:230,at:.4}]},
{bg:'cream',start:17.2,end:24.5,els:[
 {t:'pill',text:'Step 01',x:150,y:130,at:0,tone:'dark'},
 {t:'kicker',text:'Sabse pehle',x:150,y:260,at:.3},
 {t:'headline',text:'Hook',size:'xl',x:150,y:330,at:.5},
 {t:'sub',text:'2-3 seconds',x:150,y:590,at:1.2},
 {t:'chips',items:['Audience ruk jaaye'],active:0,x:150,y:720,at:2.6},
 {t:'card',timer:{from:0,to:3,secs:4.5},x:1190,y:150,w:560,h:760,at:.2,brackets:true}]},
{bg:'cream',start:24.5,end:27.0,els:[
 {t:'pill',text:'Step 02',x:150,y:130,at:0},
 {t:'kicker',text:'Phir',x:150,y:260,at:.2},
 {t:'headline',text:'Problem',size:'l',x:150,y:330,at:.4},
 {t:'card',icon:'q',red:false,x:1190,y:150,w:560,h:760,at:.1,brackets:true}]},
{bg:'cream',start:27.0,end:34.5,els:[
 {t:'pill',text:'Step 03',x:150,y:130,at:0},
 {t:'kicker',text:'Uske baad',x:150,y:260,at:.2},
 {t:'headline',text:'Solution',size:'l',x:150,y:330,at:2.4},
 {t:'sub',text:'Problem fix',x:150,y:520,at:3.0},
 {t:'chips',items:['Problem','Fix'],active:1,x:150,y:650,at:3.6},
 {t:'card',icon:'bulb',x:1190,y:150,w:560,h:760,at:.1,brackets:true}]},
{bg:'cream',start:34.5,end:37.2,els:[
 {t:'pill',text:'Step 04',x:150,y:130,at:0},
 {t:'kicker',text:'Aur bas',x:150,y:260,at:.2},
 {t:'headline',text:'Result',size:'l',x:150,y:330,at:.4},
 {t:'card',icon:'check',x:1190,y:150,w:560,h:760,at:.1,brackets:true}]},
{bg:'red',start:37.2,end:44.0,els:[
 {t:'ghost',text:'CTA'},
 {t:'kicker',text:'Aakhiri aur sabse zyada',small:true,anchor:'c',x:960,y:180,at:0},
 {t:'headline',text:'Important',size:'m',anchor:'c',x:960,y:250,at:1.2},
 {t:'headline',text:'CTA',size:'xl',anchor:'c',x:960,y:380,at:1.6},
 {t:'buttons',x:610,y:760,at:3.6,items:[{icon:'follow',label:'Follow'},{icon:'share',label:'Share'}]}]},
{bg:'cream',start:44.0,end:49.1,els:[
 {t:'pill',text:'Bas ho gaya,',anchor:'c',x:960,y:190,at:0},
 {t:'kicker',text:'Ab tum bhi',anchor:'c',x:960,y:300,at:1.4},
 {t:'headline',text:'Follow',size:'xl',anchor:'c',x:960,y:360,at:1.6},
 {t:'sub',text:'Kar lo!',anchor:'c',x:960,y:620,at:2.1},
 {t:'buttons',x:700,y:790,at:2.6,items:[{icon:'check',label:'Following',dark:true}]},
 {t:'round',icon:'bell',x:1240,y:775,w:110,at:3.0}]}
]};
