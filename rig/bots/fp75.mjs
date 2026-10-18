import mineflayer from 'mineflayer';
const name=process.env.NAME, brand=process.env.BRAND||'fabric', channel=process.env.CHANNEL;
const b=mineflayer.createBot({host:process.env.H||'meshac-rig',port:25565,username:name,version:process.env.MCV||'26.1.2',auth:'offline'});
b.on('kicked',r=>console.log('KICK',JSON.stringify(r)));
b.on('error',e=>{console.log('ERROR',e.message);process.exitCode=1;});
b._client.on('state',(st)=>{if(st==='configuration')setTimeout(()=>{const raw=Buffer.from(brand);b._client.write('custom_payload',{channel:'minecraft:brand',data:Buffer.concat([Buffer.from([raw.length]),raw])});console.log('CFGBRAND',brand)},0)});
b.once('spawn',()=>{console.log('SPAWN',name);if(channel)b._client.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(channel)});setTimeout(()=>{console.log('ALIVE',name);b.quit();},5000);});
b.on('end',()=>setTimeout(()=>process.exit(process.exitCode||0),100));setTimeout(()=>{b.quit();process.exit(1);},20000);
