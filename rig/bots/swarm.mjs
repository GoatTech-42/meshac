import mineflayer from "mineflayer";
const N=parseInt(process.env.N||"10");
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
for(let i=0;i<N;i++){
  const b=mineflayer.createBot({host:process.env.HOST,port:25565,username:"TpaSwarm"+i+(process.env.SUF||""),version:process.env.MCV||"26.1",auth:"offline"});
  b.on("error",e=>console.log("err",i,e.message)); b.on("kicked",r=>console.log("kicked",i));
  b.once("spawn",()=>console.log("up",i));
  await sleep(1500);
}
setTimeout(()=>process.exit(0),parseInt(process.env.LIFE||"240")*1000);
