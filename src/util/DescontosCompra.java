package util;
import java.math.BigDecimal;

/** Cupom primeiro; pagamento sobre o restante, com cada desconto arredondado a centavos. */
public final class DescontosCompra {
    public record Valores(BigDecimal cupom,BigDecimal pagamento,BigDecimal desconto,BigDecimal total) {}
    private DescontosCompra() {}
    public static Valores calcular(BigDecimal subtotal,String metodo,BigDecimal percentual){
        subtotal=Dinheiro.normalizar(subtotal);BigDecimal p=percentual==null?BigDecimal.ZERO:percentual;
        if(p.signum()<0 || p.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Percentual inválido.");
        BigDecimal cupom=Dinheiro.normalizar(subtotal.multiply(p).divide(new BigDecimal("100")));
        BigDecimal pagamento=PagamentoSimulado.desconto(subtotal.subtract(cupom),metodo);
        return new Valores(cupom,pagamento,cupom.add(pagamento),subtotal.subtract(cupom).subtract(pagamento));
    }
}
