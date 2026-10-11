package util;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/** Busca por partes do nome, sem acentos, caixa ou ordem fixa das palavras. */
public final class BuscaTexto {
    private BuscaTexto() {}
    public record Filtro(String sql,List<String> termos) {
        public void aplicar(PreparedStatement p)throws SQLException{for(int i=0;i<termos.size();i++)p.setString(i+1,termos.get(i));}
    }
    public static String normalizar(String texto){
        return Normalizer.normalize(texto==null?"":texto,Normalizer.Form.NFD)
            .replaceAll("[\\u0300-\\u036f]+", "").toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{Punct}&&[^%_]]|[\\u2010-\\u2015]", " ")
            .replaceAll("[\\s\\p{Z}]+", " ").trim();
    }
    public static Filtro porPalavras(String coluna,String busca){
        if(coluna==null || !coluna.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?"))throw new IllegalArgumentException("Coluna de busca inválida.");
        String texto=normalizar(busca);
        List<String> termos=texto.isEmpty()?List.of():Arrays.stream(texto.split(" ")).distinct().toList();
        // Remove marcas antes de lower: também funciona na locale C com caracteres portugueses.
        String normalizado="lower(regexp_replace(normalize(COALESCE("+coluna+",''),NFD),'[\u0300-\u036f]','','g'))";
        String sql=termos.isEmpty()?"TRUE":termos.stream().map(t->"strpos("+normalizado+",?)>0").collect(Collectors.joining(" AND "));
        return new Filtro(sql,termos);
    }
}
