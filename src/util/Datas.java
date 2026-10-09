package util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Datas brasileiras na interface; datas tipadas e ISO continuam no contrato interno. */
public final class Datas {
    private static final DateTimeFormatter DIA=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private Datas() {}
    public static LocalDate ler(String texto){
        try{
            if(texto==null || !texto.trim().matches("[0-9]{2}/[0-9]{2}/[0-9]{4}"))throw new IllegalArgumentException();
            LocalDate data=LocalDate.parse(texto.trim(),DIA);
            if(data.getYear()<1)throw new IllegalArgumentException();
            return data;
        }catch(java.time.DateTimeException | IllegalArgumentException e){throw new IllegalArgumentException("Informe uma data válida no formato dd/mm/aaaa.");}
    }
    public static LocalDate opcional(String texto){return texto==null || texto.isBlank()?null:ler(texto);}
    public static String formatar(LocalDate data){return data==null?"":DIA.format(data);}
    public static String formatar(Timestamp data){return data==null?"":HORA.format(data.toLocalDateTime());}
}
