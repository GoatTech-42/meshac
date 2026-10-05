import mineflayer from "mineflayer";
const host=process.env.HOST||"meshac-rig", version=process.env.MCVER||"26.3";
const b=mineflayer.createBot({host,port:25565,username:"bot_smoke",version,auth:"offline"});
b.once("spawn",()=>{console.log("spawned",b.entity.position.toString());b.setControlState("forward",true);setTimeout(()=>{console.log("moved to",b.entity.position.toString());b.quit();},3000)});
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(1)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.on("end",()=>setTimeout(()=>process.exit(0),200));
