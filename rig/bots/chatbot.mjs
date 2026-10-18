import mineflayer from "mineflayer";
const GAP=parseInt(process.env.GAP||"250"), N=parseInt(process.env.N||"12"), MODE=process.env.MODE||"same";
const b=mineflayer.createBot({host:process.env.HOST,port:25565,username:process.env.U,version:process.env.MCV||"26.1",auth:"offline"});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(0)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.once("spawn",async()=>{
  await sleep(22000);
  for(let i=0;i<N;i++){ const who=MODE==="same"?"Obs_probe":("P"+i); b.chat("/tpa "+who); console.log("sent",i,who); await sleep(GAP<0?300+Math.random()*2700:GAP); }
  await sleep(1000); b.quit(); setTimeout(()=>process.exit(0),500);
});
setTimeout(()=>process.exit(2),60000);
