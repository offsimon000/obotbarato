package util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    private Dinheiro() {}

    public static BigDecimal de(double valor) {
        if (!Double.isFinite(valor)) throw new IllegalArgumentException("Preço inválido.");
        return normalizar(BigDecimal.valueOf(valor));
    }

    public static BigDecimal normalizar(BigDecimal valor) {
        if (valor == null || valor.signum() < 0)
            throw new IllegalArgumentException("O valor deve ser zero ou positivo.");
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
