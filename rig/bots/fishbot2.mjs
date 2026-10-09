import mineflayer from "mineflayer";
import fs from "fs";
const DELAY=parseInt(process.env.DELAY||"0"), N=parseInt(process.env.N||"8"), FLAG="/w/bite.flag";
const b=mineflayer.createBot({host:process.env.HOST,port:25565,username:process.env.U,version:process.env.MCV||"26.1",auth:"offline"});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(0)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.once("spawn",async()=>{
  await sleep(32000);
  await b.equip(b.inventory.items().find(i=>i.name==="fishing_rod"),"hand"); await b.look(0,-0.1,true); await sleep(800);
  for(let n=1;n<=N;n++){
    try{fs.unlinkSync(FLAG)}catch{}
    b.activateItem(); console.log("cast",n);
    const t0=Date.now(); while(!fs.existsSync(FLAG)){ await sleep(5); if(Date.now()-t0>90000){console.log("no bite in 90s");process.exit(2)} }
    const seen=Date.now(); if(DELAY>0) await sleep(DELAY); b.activateItem(); console.log("reel",n,"after flag ms",Date.now()-seen);
    await sleep(1200);
  }
  console.log("done"); b.quit(); setTimeout(()=>process.exit(0),500);
});
setTimeout(()=>{console.log("overall timeout");process.exit(3)},900000);
