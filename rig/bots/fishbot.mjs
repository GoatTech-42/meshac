import mineflayer from "mineflayer";
const DELAY=parseInt(process.env.DELAY||"0"), N=parseInt(process.env.N||"6");
const b=mineflayer.createBot({host:process.env.HOST,port:25565,username:process.env.U,version:process.env.MCV||"26.1",auth:"offline"});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(0)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
let castAt=0, bites=0, casts=0, reeled=0, state="idle";
b.on("soundEffectHeard",async(name,pos)=>{ if(/fishing_bobber\.splash/.test(String(name))&&state==="cast"&&Date.now()-castAt>2500){ state="bite"; bites++; console.log("bite",bites,Date.now()); if(DELAY>0) await sleep(DELAY); b.activateItem(); reeled++; console.log("reel",reeled,Date.now()); state="idle"; setTimeout(cast,250);} });
async function cast(){ if(bites>=N){ console.log("done bites",bites); await sleep(500); b.quit(); setTimeout(()=>process.exit(0),500); return;} state="cast"; castAt=Date.now(); casts++; b.activateItem(); console.log("cast",casts); }
b.once("spawn",async()=>{ await sleep(32000); await b.equip(b.inventory.items().find(i=>i.name==="fishing_rod"),"hand"); await b.look(0,-0.1,true); await sleep(800); console.log("yaw",b.entity.yaw,"pitch",b.entity.pitch); cast(); });
const seen=new Set(); b.on("soundEffectHeard",(n)=>{ if(!seen.has(n)){seen.add(n);console.log("snd",n)} });
setTimeout(()=>{console.log("timeout bites",bites,"casts",casts);process.exit(2)},400000);
