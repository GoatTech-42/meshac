package dev.meshac;
import java.nio.file.Files;
import java.util.UUID;
/** Real Cases and Gson, isolated temporary config directory only. */
public class CasesLoadTest {
 static int checks;
 static void ck(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  var file=net.fabricmc.loader.api.FabricLoader.dir.resolve("meshac-cases.json");var id=UUID.randomUUID();
  var original=Cases.add("fixture",id,"ban","scripted","staff",0,java.util.List.of("fixture"));
  ck(Cases.activeBan(id)!=null,"fixture ban must exist before reload");
  Files.writeString(file,"null");Cases.load();
  ck(Cases.activeBan(id)!=null,"null reload must preserve known active bans");
  ck(Files.readString(file).equals("null"),"failed load must not rewrite input");
  Cases.add("other",UUID.randomUUID(),"kick","scripted","staff",0,java.util.List.of());
  ck(Files.readString(file).contains(original.uuid),"next write must preserve prior known ban");
  Files.writeString(file,"not json");Cases.load();
  ck(Cases.activeBan(id)!=null,"syntax-error reload must preserve known active bans");
  Files.writeString(file,"[]");Cases.load();
  ck(Cases.activeBan(id)==null,"valid empty store must replace old state");
  System.out.println("PASS cases-load checks="+checks);
 }
}
