import mineflayer from "mineflayer";
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const host=process.env.HOST, role=process.env.ROLE, name=process.env.NAME, other=process.env.OTHER;
const b=mineflayer.createBot({host,port:25565,username:name,version:"26.1.2",auth:"offline"});
b.on("error",e=>{console.log("ERROR",e.message);process.exit(1)});
b.on("kicked",r=>{console.log("KICK",JSON.stringify(r));process.exit(1)});
const t0=Date.now();
b.once("spawn",async()=>{
  console.log("SPAWN",role,Date.now()-t0);
  if(role==="viewer"){
    setInterval(()=>{const e=Object.values(b.entities).find(x=>x.username===other);console.log("S",Date.now(),e?1:0,e?e.position.x.toFixed(2):"-",e?e.position.z.toFixed(2):"-")},100);
  } else {
    // victim: walks +x when told by a marker file-free signal: sprint forward along +x continuously after WALK
    b.on("message",()=>{});
    process.stdin.on("data",d=>{const c=d.toString().trim(); if(c==="WALK"){b.look(-Math.PI/2,0,true);b.setControlState("forward",true);b.setControlState("sprint",true);console.log("WALKSTART",Date.now())} if(c==="STOP"){b.clearControlStates()}});
    setInterval(()=>console.log("V",Date.now(),b.entity.position.x.toFixed(2),b.entity.position.z.toFixed(2)),100);
  }
});
setTimeout(()=>process.exit(0),Number(process.env.TTL||240000));
