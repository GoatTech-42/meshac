package dev.meshac;
import java.nio.file.Files;
import java.util.UUID;
import com.google.gson.*;
/** Real Cases/Gson; synthetic historical case shapes and corrupt rows, temp files only. */
public class CasesFieldsTest {
 static int checks;static void ck(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  var file=net.fabricmc.loader.api.FabricLoader.dir.resolve("meshac-cases.json");var id=UUID.randomUUID();
  var original=Cases.add("fixture",id,"ban","scripted","staff",0,java.util.List.of("fixture"));
  var valid=JsonParser.parseString(Files.readString(file)).getAsJsonArray().get(0).getAsJsonObject();
  // Fields missing in the oldest writer remain compatible, as do unknown owner fields.
  var legacy=valid.deepCopy();legacy.remove("lifted");legacy.addProperty("ownerMetadata",17);
  Files.writeString(file,"["+legacy+"]");Cases.load();ck(Cases.activeBan(id)!=null,"legacy case without lifted must retain ban");
  var known=Cases.activeBan(id);
  for(String field:new String[]{"id","player","uuid","action","reason","by","evidence","evidence-entry"}){
   var invalid=valid.deepCopy();
   if(field.equals("evidence-entry")){var ev=new JsonArray();ev.add(JsonNull.INSTANCE);invalid.add("evidence",ev);}else invalid.add(field,JsonNull.INSTANCE);
   String input="["+invalid+"]";Files.writeString(file,input);Cases.load();
   try {ck(Cases.activeBan(id)==known && Cases.find(original.id)==known && Cases.of("fixture").size()==1,"invalid "+field+" must preserve known usable store");}
   catch(NullPointerException error){throw new AssertionError("invalid "+field+" must not poison case consumers",error);}
   ck(Files.readString(file).equals(input),"invalid field load must not rewrite input");
  }
  Files.writeString(file,"[]");Cases.load();ck(Cases.activeBan(id)==null,"valid [] still replaces known state");
  System.out.println("PASS cases-fields checks="+checks);
 }
}
