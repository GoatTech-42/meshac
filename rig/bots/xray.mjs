import mineflayer from 'mineflayer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const b=mineflayer.createBot({host:process.env.HOST,port:25565,username:process.env.NAME,version:'26.1.2',auth:'offline'});
b.on('error',e=>{console.log('ERROR',e.message);process.exit(1)});
b.on('kicked',r=>{console.log('KICK',JSON.stringify(r));process.exit(1)});
b.once('spawn',async()=>{
 await sleep(Number(process.env.WAIT||14000));
 const o=b.entity.position.floored(); const out=[];
 for(let x=-32;x<=32;x++)for(let z=-32;z<=32;z++)for(let y=-62;y<=-30;y++){
  const bl=b.blockAt(o.offset(0,0,0).set(x,y,z)); if(bl&&/_ore$|ancient_debris/.test(bl.name)) out.push([x,y,z]);}
 console.log("ORES "+JSON.stringify(out));
 if(process.env.PX){ await sleep(12000); const t=b.entity.position.floored().set(+process.env.PX,+process.env.PY,+process.env.PZ); const bl=b.blockAt(t); const nb=b.blockAt(t.offset(1,0,0)); console.log("REVEAL "+(bl&&bl.name)+" neighbour "+(nb&&nb.name)); }
 b.quit(); setTimeout(()=>process.exit(0),500);
});
setTimeout(()=>{console.log('TIMEOUT');process.exit(2)},100000);
