import mineflayer from "mineflayer";
const N=parseInt(process.env.N||"20"), LIFE=parseInt(process.env.LIFE||"360")*1000, SUF=process.env.SUF||"a";
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const stats={joined:0,kicked:0,errors:0,spawns:0,attacks:0,dug:0,placed:0,chat:0,respawns:0};
const errs={};
function mk(i,gen){
  const b=mineflayer.createBot({host:process.env.HOST,port:parseInt(process.env.PORT||"25565"),username:`strs${SUF}${i}`,version:"26.1",auth:"offline"});
  b.on("error",e=>{stats.errors++;errs[e.message]=(errs[e.message]||0)+1});
  b.on("kicked",r=>{stats.kicked++;errs["kick:"+JSON.stringify(r).slice(0,80)]=(errs["kick:"+JSON.stringify(r).slice(0,80)]||0)+1});
  b.on("death",()=>{stats.respawns++});
  b.once("spawn",()=>{stats.spawns++;run(b,i,gen)});
  return b;
}
async function run(b,i,gen){
  const end=Date.now()+ (gen>0?60000:LIFE);
  const rnd=(a)=>a[Math.floor(Math.random()*a.length)];
  while(Date.now()<end && b.entity){
    try{
      const r=Math.random();
      b.look(Math.random()*6.28,(Math.random()-0.5)*0.6,true);
      b.setControlState("forward",true); b.setControlState("sprint",Math.random()<0.5); b.setControlState("jump",Math.random()<0.3);
      if(r<0.25){ // combat
        const t=Object.values(b.entities).filter(e=>e.type==="player"&&e!==b.entity&&e.position.distanceTo(b.entity.position)<5)[0];
        if(t){await b.lookAt(t.position.offset(0,1.6,0)); b.attack(t); stats.attacks++;}
      } else if(r<0.45){ // dig
        const bl=b.blockAt(b.entity.position.offset(1,-1,0)); if(bl&&bl.name!=="air"&&b.canDigBlock(bl)){await b.dig(bl).catch(()=>{});stats.dug++;}
      } else if(r<0.55){ b.chat("hi "+i); stats.chat++; }
    }catch(e){}
    await sleep(400+Math.random()*600);
  }
  b.setControlState("forward",false); 
  if(gen>0||Date.now()>=end){ b.quit(); }
}
const bots=[];
for(let i=0;i<N;i++){bots.push(mk(i,0)); await sleep(1000);}
console.log("ALL_STARTED",Date.now());
// churn: reconnect 6 bots a few times
for(let c=0;c<4;c++){
  await sleep(35000);
  for(let k=0;k<6;k++){const i=(c*6+k)%N; try{bots[i].quit()}catch{} await sleep(300); bots[i]=mk(100+c*10+k,1);}
}
await sleep(Math.max(0,LIFE-N*1000-4*35000-5000));
console.log("STATS",JSON.stringify(stats),JSON.stringify(errs));
for(const b of bots){try{b.quit()}catch{}}
setTimeout(()=>process.exit(0),3000);
