package util;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/** CSV brasileiro: UTF-8 com BOM, ponto e vírgula e valores decimais com vírgula. */
public final class ExportarCsv {
    private ExportarCsv() {}
    public static void salvar(Path destino,List<String> cabecalho,List<Object[]> linhas)throws IOException {
        Path absoluto=destino.toAbsolutePath();Path temporario=Files.createTempFile(absoluto.getParent(),"obotbarato-csv-",".tmp");
        try {
            try(BufferedWriter w=Files.newBufferedWriter(temporario,StandardCharsets.UTF_8)) {
                w.write('\ufeff');linha(w,cabecalho.toArray());for(Object[] valores:linhas)linha(w,valores);
            }
            try{Files.move(temporario,absoluto,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporario,absoluto,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporario);}
    }
    private static void linha(Writer w,Object[] valores)throws IOException {
        for(int i=0;i<valores.length;i++){if(i>0)w.write(';');w.write('"');w.write(formatar(valores[i]).replace("\"","\"\""));w.write('"');}w.write("\r\n");
    }
    private static String formatar(Object valor){
        if(valor==null)return "";
        if(valor instanceof BigDecimal)return ((BigDecimal)valor).toPlainString().replace('.',',');
        if(valor instanceof Timestamp)return Datas.formatar((Timestamp)valor);
        if(valor instanceof LocalDate)return Datas.formatar((LocalDate)valor);
        String texto=valor.toString();String limpo=texto.stripLeading();
        if(valor instanceof String && ((!limpo.isEmpty() && "=+-@".indexOf(limpo.charAt(0))>=0) || (!texto.isEmpty() && "\t\r\n".indexOf(texto.charAt(0))>=0)))return "'"+texto;
        return texto;
    }
}
