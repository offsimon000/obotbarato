import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.nio.file.Path;

/** Lança a entrada oficial via JDI e fecha somente a VM criada pelo teste.
 * Verifica parâmetros equivalentes à configuração Java do VS Code;
 * não automatiza o teclado ou a interface do editor.
 */
public final class ExecucaoDepurador {
 public static void main(String[] args)throws Exception {
  if(args.length!=3)throw new IllegalArgumentException("Informe classpath, configuração e classe principal.");
  LaunchingConnector connector=Bootstrap.virtualMachineManager().defaultConnector();
  var parameters=connector.defaultArguments();
  parameters.get("main").setValue(args[2]);
  parameters.get("home").setValue(System.getProperty("java.home"));
  String options="-Dfile.encoding=UTF-8 \"-Dobotbarato.config="+Path.of(args[1]).toAbsolutePath()+"\" -cp \""+args[0]+"\"";
  parameters.get("options").setValue(options);
  parameters.get("suspend").setValue("true");
  VirtualMachine vm=null;
  try {
   vm=connector.launch(parameters);vm.resume();
   boolean opened=false;long limit=System.nanoTime()+30_000_000_000L;
   while(System.nanoTime()<limit){
    var types=vm.classesByName(args[2]);
    if(!types.isEmpty()){
     var type=types.get(0);
     if(type.isPrepared())for(ObjectReference window:type.instances(2)){
      Field visible=type.fieldByName("visible");
      if(visible!=null && ((BooleanValue)window.getValue(visible)).value())opened=true;
     }
    }
    if(opened)break;Thread.sleep(100);
   }
   if(!opened)throw new AssertionError("Entrada não abriu no processo depurado.");
   System.out.println("APROVADO: entrada oficial com classpath e configuração do VS Code, JVM conectada ao depurador JDI e janela visível. Nenhum cadastro ou venda alterado.");
  } finally {
   if(vm!=null){Process process=vm.process();try{vm.exit(0);}catch(VMDisconnectedException ignored){}if(!process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly();}
  }
 }
}
