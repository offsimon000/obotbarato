package util;
import java.math.BigDecimal;
import java.util.Map;

/** Regras únicas para o resumo da interface e a confirmação no banco. */
public final class PagamentoSimulado {
    public static final Map<String,String> NOMES=Map.of("credito","Cartão de crédito","debito","Cartão de débito","pix","PIX","boleto","Boleto","dinheiro","Dinheiro");
    private PagamentoSimulado() {}
    public static int percentual(String metodo){
        if(metodo==null || !NOMES.containsKey(metodo))throw new IllegalArgumentException("Método de pagamento inválido.");
        return "pix".equals(metodo)?10:"debito".equals(metodo)?5:0;
    }
    public static BigDecimal desconto(BigDecimal subtotal,String metodo){return Dinheiro.normalizar(subtotal.multiply(BigDecimal.valueOf(percentual(metodo))).divide(BigDecimal.valueOf(100)));}
    public static BigDecimal total(BigDecimal subtotal,String metodo){return subtotal.subtract(desconto(subtotal,metodo));}
    public static String status(String metodo){percentual(metodo);return "boleto".equals(metodo)?"pendente":"confirmado";}
    public static String moeda(BigDecimal valor){return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(valor);}
}
