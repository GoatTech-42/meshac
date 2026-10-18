// usage: MODE=legit|speed|fly|highjump node cheat.mjs   (HOST, MCVER env)
import mineflayer from "mineflayer";
const mode=process.env.MODE||"legit", host=process.env.HOST||"meshac-rig", version=process.env.MCVER||"26.1";
const b=mineflayer.createBot({host,port:25565,username:"b_"+mode+Math.random().toString(36).slice(2,5),version,auth:"offline",physicsEnabled:false});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const send=(x,y,z,g)=>b._client.write("position",{x,y,z,flags:{onGround:g,hasHorizontalCollision:false}});
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(1)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.on("messagestr",m=>{if(m.includes("meshac"))console.log("chat:",m)});
b.once("spawn",async()=>{
  await b.waitForChunksToLoad(); await sleep(1500);
  if(mode==="legit") b.physicsEnabled=true; // a vanilla client sends nothing until terrain is loaded
  const p0=b.entity.position.clone();
  console.log(mode,"start",p0.toString());
  if(mode==="legit"){
    b.setControlState("forward",true);b.setControlState("sprint",true);
    for(let i=0;i<30;i++){b.setControlState("jump",i%3===0);b.look(i*0.4,0,true);await sleep(500);}
  } else {
    let {x,y,z}=p0; let next=Date.now(); globalThis.T0=next;
    for(let t=0;t<200;t++){
      if(mode==="speed"){x+=0.9;send(x,y,z,true);}
      if(mode==="fall"){y-=0.3;send(x,y,z,false);}
      if(mode==="nofall"){y-=0.3;x+=0.0;send(x,y,z,true);}
      if(mode==="timer"){x+=(t%2?0.02:-0.02);send(x,y,z,true);}
      if(mode==="fly"){send(x,y+(t<5?0.3*t:0.0),z,false);y+=(t<5?0.3*t:0);}
      if(mode==="highjump"){ if(t%30===0){y+=1.2;send(x,y,z,true);} else send(x,y,z,t%30>3&&false);}
      next+=(mode==="timer"?20:50); await sleep(Math.max(0,next-Date.now())); // drift-free
    }
  }
  console.log("elapsed",Date.now()-(globalThis.T0||0));
  console.log(mode,"end",b.entity.position.toString());
  b.quit(); setTimeout(()=>process.exit(0),300);
});
