import mineflayer from "mineflayer";
import { Vec3 } from "vec3";
const GAP=parseInt(process.env.GAP||"0"), N=parseInt(process.env.N||"6");
const b=mineflayer.createBot({host:process.env.HOST,port:25565,username:process.env.U,version:process.env.MCV||"26.1",auth:"offline"});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
b.on("kicked",r=>{console.log("kicked",JSON.stringify(r));process.exit(0)});
b.on("error",e=>{console.log("error",e.message);process.exit(1)});
b.once("spawn",async()=>{
  await sleep(22000);
  const item=b.inventory.items().find(i=>i.name.includes("sign"));
  console.log("sign item",item?.name,item?.count);
  if(!item){process.exit(3)}
  await b.equip(item,"hand");
  const p=b.entity.position.floored();
  for(let i=0;i<N;i++){
    const ref=b.blockAt(p.offset(-3+i,-1,2)); // floor blocks in a row
    try{
      let opened=false; const h=(d)=>{opened=true;console.log("open_sign_entity",JSON.stringify(d.location),d.isFrontText)}; b._client.once("open_sign_entity",h);
      await b.placeBlock(ref,new Vec3(0,1,0));
      await sleep(200); console.log("editor opened:",opened);
      const pos=ref.position.offset(0,1,0);
      await sleep(GAP);
      b._client.write("update_sign",{location:pos,isFrontText:true,text1:"hello",text2:"hello",text3:"hello",text4:"hello"});
      b._client.on("packet",(d,m)=>{if(m.name==="block_change"||m.name==="tile_entity_data")console.log("srv",m.name,JSON.stringify(d).slice(0,160))}); console.log("placed+updated",i,pos.toString(),"gap",GAP);
    }catch(e){console.log("fail",i,e.message)}
    await sleep(600);
  }
  await sleep(1000); b.quit(); setTimeout(()=>process.exit(0),500);
});
setTimeout(()=>process.exit(2),70000);
