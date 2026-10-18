import mineflayer from "mineflayer";
const b=mineflayer.createBot({host:process.env.HOST||"meshac-rig",port:25565,username:process.env.U,version:process.env.MCV||"26.1",auth:"offline"});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(0)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.once("spawn",async()=>{
  await sleep(22000);
  const p=b.entity.position; console.log("pos",p.toString());
  const front=b.blockAt(p.offset(0,1,1).floored()); const behind=b.blockAt(p.offset(0,1,2).floored());
  console.log("front",front?.name,front?.position.toString(),"behind",behind?.name,behind?.position.toString());
  const tgt=process.env.T==="front"?front:behind;
  for(let i=0;i<4;i++){ try{ await b.dig(tgt,true); console.log("dug",i);}catch(e){console.log("digfail",e.message)} await sleep(800);
    await new Promise(r=>{ r() }); }
  b.quit(); setTimeout(()=>process.exit(0),500);
});
setTimeout(()=>process.exit(2),60000);
