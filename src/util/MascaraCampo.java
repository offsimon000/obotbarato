package util;
import javax.swing.*;
import javax.swing.text.*;

/** Preserva posições de inserção/seleção/exclusão; aceita telefone fixo e celular. */
public final class MascaraCampo {
    private MascaraCampo() {}
    public static String somenteNumeros(String s){ return s==null?"":s.replaceAll("[^0-9]",""); }
    public static void aplicarCpf(JTextField c){ aplicar(c,11,"cpf"); }
    public static void aplicarTelefone(JTextField c){ aplicar(c,11,"telefone"); }
    public static void aplicarCep(JTextField c){ aplicar(c,8,"cep"); }
    public static void aplicarData(JTextField c){ aplicar(c,8,"data");c.setToolTipText("Digite dia, mês e ano; as barras são inseridas automaticamente."); }
    private static void aplicar(JTextField c,int max,String tipo) {
        ((AbstractDocument)c.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override public void insertString(FilterBypass b,int o,String s,AttributeSet a)throws BadLocationException{ replace(b,o,0,s,a); }
            @Override public void remove(FilterBypass b,int o,int l)throws BadLocationException{ replace(b,o,l,"",null); }
            @Override public void replace(FilterBypass b,int o,int l,String s,AttributeSet a)throws BadLocationException {
                String atual=b.getDocument().getText(0,b.getDocument().getLength());
                int inicio=somenteNumeros(atual.substring(0,o)).length();
                int fim=somenteNumeros(atual.substring(0,o+l)).length();
                String dig=somenteNumeros(atual), novo=somenteNumeros(s);
                // Apagar só um separador remove o dígito anterior, evitando uma máscara impossível de apagar.
                if(l>0 && inicio==fim && novo.isEmpty() && inicio>0) inicio--;
                String resultado=dig.substring(0,inicio)+novo+dig.substring(fim);
                if(resultado.length()>max) return;
                String formato=tipo.equals("data")?"##/##/####":tipo.equals("cpf")?"###.###.###-##":tipo.equals("cep")?"#####-###":resultado.length()>10?"(##) #####-####":"(##) ####-####";
                StringBuilder out=new StringBuilder(); int i=0;
                for(char ch:formato.toCharArray()){
                    if(i>=resultado.length()) break;
                    out.append(ch=='#'?resultado.charAt(i++):ch);
                }
                b.replace(0,b.getDocument().getLength(),out.toString(),a);
                int alvo=inicio+novo.length(), pos=0, cont=0;
                while(pos<out.length() && cont<alvo){ if(Character.isDigit(out.charAt(pos))) cont++; pos++; }
                c.setCaretPosition(pos);
            }
        });
    }
}
