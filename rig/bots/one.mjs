import mineflayer from "mineflayer";
const b=mineflayer.createBot({host:process.env.HOST,port:25570,username:"probe1",version:process.env.V||"26.1",auth:"offline"});
b.on("error",e=>console.log("err",e.message)); b.on("kicked",r=>console.log("kicked",JSON.stringify(r).slice(0,300))); b.on("end",r=>console.log("end",r));
b.once("spawn",()=>{console.log("SPAWN");setTimeout(()=>{b.quit()},3000)});
setTimeout(()=>process.exit(0),20000);
