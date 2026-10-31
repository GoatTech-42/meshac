package dev.meshac;
import java.nio.file.Files;
/** Real Config/Gson in isolated temporary directory, no real configuration. */
public class ConfigPresetTest {
 static int checks;static void ck(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  var file=net.fabricmc.loader.api.FabricLoader.dir.resolve("meshac.json");
  Files.writeString(file,"{\"preset\":\"lenient\",\"monitorOnly\":true,\"unknownOwnerSetting\":17}");Config.load();
  ck(Config.get().monitor() && Config.get().removeAtD()==8,"valid monitor config must load");
  Files.writeString(file,"{\"preset\":null}");Config.load();
  ck(Config.get().monitor(),"invalid null-preset reload must preserve known monitor policy");
  try{ck(Config.get().removeAtD()==8,"known preset must remain usable");}catch(NullPointerException error){throw new AssertionError("invalid preset must not poison gameplay config",error);}
  ck(Files.readString(file).equals("{\"preset\":null}"),"invalid config must not be rewritten");
  Files.writeString(file,"{\"preset\":\"owner-custom\",\"unknownOwnerSetting\":17}");Config.load();
  ck(Config.get().removeAtD()==6,"unknown non-null preset keeps existing default fallback");
  ck(Files.readString(file).contains("unknownOwnerSetting"),"valid owner fields remain intact");
  System.out.println("PASS config-preset checks="+checks);
 }
}
