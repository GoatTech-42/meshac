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
  String invalid=args.length>0 && args[0].equals("row")?"[null]":"null";
  Files.writeString(file,invalid);Cases.load();
  try { ck(Cases.activeBan(id)!=null,"invalid reload must preserve known active bans"); }
  catch(NullPointerException error){throw new AssertionError("invalid row reload must not poison active ban lookup",error);}
  ck(Files.readString(file).equals(invalid),"failed load must not rewrite input");
  Cases.add("other",UUID.randomUUID(),"kick","scripted","staff",0,java.util.List.of());
  ck(Files.readString(file).contains(original.uuid),"next write must preserve prior known ban");
  Files.writeString(file,"not json");Cases.load();
  ck(Cases.activeBan(id)!=null,"syntax-error reload must preserve known active bans");
  Files.writeString(file,"[]");Cases.load();
  ck(Cases.activeBan(id)==null,"valid empty store must replace old state");
  System.out.println("PASS cases-load checks="+checks);
 }
}
