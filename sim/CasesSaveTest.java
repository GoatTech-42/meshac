package dev.meshac;
import java.nio.file.Files;
import java.util.UUID;
/** Real Cases/Gson under a child-process file size cap, temporary directory only. */
public class CasesSaveTest {
 public static void main(String[] args)throws Exception{
  var file=net.fabricmc.loader.api.FabricLoader.dir.resolve("meshac-cases.json");
  var id=UUID.randomUUID();Cases.add("fixture",id,"ban","scripted","staff",0,java.util.List.of("fixture"));
  var second=Cases.add("second",UUID.randomUUID(),"kick","scripted","staff",0,java.util.List.of());
  Cases.load();
  if(Cases.activeBan(id)==null || Cases.find(second.id)==null)throw new AssertionError("normal consecutive atomic saves must reload all cases");
  byte[] prior=Files.readAllBytes(file);
  Cases.add("other",UUID.randomUUID(),"kick","X".repeat(12000),"staff",0,java.util.List.of());
  if(!java.util.Arrays.equals(prior,Files.readAllBytes(file)))throw new AssertionError("failed oversized save must preserve prior case-store bytes");
  try(var paths=Files.list(file.getParent())){if(paths.count()!=1)throw new AssertionError("failed save must clean temporary files");}
  System.out.println("PASS cases-save failed-write preservation");
 }
}
