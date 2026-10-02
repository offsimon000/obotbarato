package util;
import model.Cliente;
import java.util.Locale;

public final class Validacao {
    private Validacao() {}
    public static String texto(String valor, String campo, int min, int max) {
        String s = valor == null ? "" : valor.trim();
        if (s.length() < min || s.length() > max) throw new IllegalArgumentException(campo+": informe de "+min+" a "+max+" caracteres.");
        return s;
    }
    public static String email(String valor) {
        String s = texto(valor,"E-mail",5,150).toLowerCase(Locale.ROOT);
        if (!s.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+")) throw new IllegalArgumentException("Informe um e-mail válido.");
        return s;
    }
    public static void senha(String senha) {
        if (senha == null || senha.isBlank() || senha.length()<8 || senha.length()>128) throw new IllegalArgumentException("Senha: informe de 8 a 128 caracteres.");
    }
    public static String cpf(String valor) {
        if (valor == null || valor.isBlank()) return null;
        if (!valor.matches("[0-9.\\-\\s]+")) throw new IllegalArgumentException("CPF inválido.");
        String n = MascaraCampo.somenteNumeros(valor);
        if (n.length()!=11 || n.chars().allMatch(c -> c==n.charAt(0))) throw new IllegalArgumentException("CPF inválido.");
        for (int d=9;d<11;d++) {
            int soma=0;
            for(int i=0;i<d;i++) soma+=(n.charAt(i)-'0')*(d+1-i);
            int r=(soma*10)%11; if(r==10) r=0;
            if(r!=n.charAt(d)-'0') throw new IllegalArgumentException("CPF inválido.");
        }
        return n.substring(0,3)+"."+n.substring(3,6)+"."+n.substring(6,9)+"-"+n.substring(9);
    }
    public static String telefone(String valor) {
        if(valor==null || valor.isBlank()) return null;
        if(!valor.matches("[0-9()\\-\\s]+")) throw new IllegalArgumentException("Telefone inválido.");
        String n=MascaraCampo.somenteNumeros(valor);
        if((n.length()!=10 && n.length()!=11) || n.charAt(0)=='0' || n.charAt(2)=='0') throw new IllegalArgumentException("Telefone: informe DDD e 8 ou 9 dígitos.");
        return "("+n.substring(0,2)+") "+n.substring(2,n.length()-4)+"-"+n.substring(n.length()-4);
    }
    public static void cliente(Cliente c) {
        c.setNome(texto(c.getNome(),"Nome",3,120)); c.setEmail(email(c.getEmail()));
        c.setCpf(cpf(c.getCpf())); c.setTelefone(telefone(c.getTelefone()));
        String e=texto(c.getEndereco(),"Endereço",0,255); c.setEndereco(e.isEmpty()?null:e);
    }
}
