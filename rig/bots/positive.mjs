// Protocol-level scripted stream driver, not a native hacked client or physics test.
// Requires an externally prepared, verified rig. Sends a standing settle sequence before
// each stream, but does not verify world geometry, server grace, packet receipt, effective
// config or enforcement. Completion means only the client reached the end
// of its local stream and disconnected normally. Server log assertions are in the runner.
import mineflayer from "mineflayer";
import { randomBytes } from "node:crypto";
const mode=process.env.MODE||"walk", host=process.env.HOST||"meshac-rig", version=process.env.MCVER||"26.1";
const modes=new Set(["hover","ascend","airjump","speed","timer","step","walk","arc","sprintarc","slowcadence"]);
if(!modes.has(mode)){console.error("unknown mode",mode);process.exit(2);}
const name="bp_"+randomBytes(6).toString("hex"); // 15 characters, within MC's limit
console.log("PLAYER",name);
const b=mineflayer.createBot({host,port:25565,username:name,version,auth:"offline",physicsEnabled:false});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
// Locked Mineflayer 4.39.0 sends movement even with physicsEnabled:false.
// Gate its automatic movement only during our stream; protocol housekeeping passes.
const rawWrite=b._client.write.bind(b._client);
const movement=new Set(["position","position_look","look","flying"]);
let active=false, sent=0, suppressed=0;
b._client.write=(packet,data)=>{
  if(active && movement.has(packet)){suppressed++;return;}
  return rawWrite(packet,data);
};
b._client.on("position",()=>{
  if(active){console.error("error server position update during stream; diagnostic invalid");process.exit(1);}
});
const send=(x,y,z,g)=>{sent++;return rawWrite("position",{x,y,z,flags:{onGround:g,hasHorizontalCollision:false}});};
let complete=false;
const watchdog=setTimeout(()=>{console.error("error completion timeout");process.exit(1);},110000);
b.on("end",()=>{
  clearTimeout(watchdog);
  if(!complete){console.error("error disconnected before completion");process.exit(1);}
  console.log("COMPLETE",name,mode);process.exit(0);
});
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(1)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.once("spawn",()=>{ (async()=>{
  await b.waitForChunksToLoad(); await sleep(1500);
  const p0=b.entity.position.clone();
  let {x,y,z}=p0;
  active=true;
  console.log("CONNECTED",name,mode);
  console.log("MODE",mode,"start",p0.toString());
  let next=Date.now();
  const tick=async(stepMs)=>{next+=stepMs;await sleep(Math.max(0,next-Date.now()));}; // target cadence only; actual runtime timing is not validated here
  for(let i=0;i<30;i++){send(x,y,z,true);await tick(50);} // settle attempt only; actual server grace/world state is not verified here
  if(mode==="hover"){y+=0.42;send(x,y,z,false);await tick(50);for(let i=0;i<15;i++){send(x,y,z,false);await tick(50);}}
  else if(mode==="ascend"){y+=0.42;send(x,y,z,false);await tick(50);for(let i=0;i<15;i++){y+=0.25;send(x,y,z,false);await tick(50);}}
  else if(mode==="airjump"){y+=0.42;send(x,y,z,false);await tick(50);send(x,y,z,false);await tick(50);send(x,y,z,false);await tick(50);y+=0.5;send(x,y,z,false);await tick(50);send(x,y,z,false);await tick(50);y+=0.5;send(x,y,z,false);await tick(50);}
  else if(mode==="speed"){y+=0.42;send(x,y,z,false);await tick(50);for(let i=0;i<8;i++){x+=0.8;send(x,y,z,false);await tick(50);}}
  else if(mode==="timer"){for(let i=0;i<60;i++){send(x,y,z,true);await tick(25);}}
  else if(mode==="step"){for(let i=0;i<3;i++){y+=0.31;send(x,y,z,true);await tick(50);}}
  else if(mode==="walk"){for(let i=0;i<40;i++){x+=0.2;send(x,y,z,true);await tick(50);}}
  else if(mode==="arc"||mode==="sprintarc"){const h=mode==="sprintarc"?0.28:0;let dy=0.42;y+=dy;x+=h;send(x,y,z,false);await tick(50);
    while(true){dy=(dy-0.08)*0.98;const ny=y+dy;if(ny<=p0.y){x+=h;send(x,p0.y,z,true);await tick(50);break;}y=ny;x+=h;send(x,y,z,false);await tick(50);}}
  else if(mode==="slowcadence"){for(let i=0;i<30;i++){send(x,y,z,true);await tick(80);}}
  else {console.log("unknown mode",mode);process.exit(1);}
  await sleep(1000); // observation delay only, not server acceptance acknowledgement
  console.log("MODE",mode,"end",b.entity.position.toString());
  console.log("OUTBOUND",name,mode,"scripted",sent,"automatic_suppressed",suppressed);
  complete=true;b.quit();
})().catch(e=>{console.error("error",e.message);process.exit(1);}); });
