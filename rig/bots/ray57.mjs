import mineflayer from 'mineflayer';
const b=mineflayer.createBot({host:'meshac-rig',port:25565,username:'mcRay57',version:'26.1.2',auth:'offline'});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
b.on('error',e=>{console.log('ERROR',e.message);process.exit(1)});
b.on('kicked',r=>{console.log('KICK',JSON.stringify(r));process.exit(0)});
b.once('spawn',async()=>{
 await sleep(12000);
 const e=Object.values(b.entities).find(e=>e.name==='zombie');if(!e)throw Error('no target');
 await b.lookAt(e.position.offset(0,1,0),true);await sleep(1000);console.log('AIMED',e.id,b.entity.position.toString());b.attack(e);await sleep(1500);
 await b.look(Math.PI,0,true);await sleep(1000);console.log('AWAY',e.id,b.entity.yaw);
 for(let i=0;i<4;i++){b.attack(e);await sleep(1000)}b.quit();
});
setTimeout(()=>process.exit(1),45000);
