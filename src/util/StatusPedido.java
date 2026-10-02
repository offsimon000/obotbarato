package util;

import java.util.List;

/** Fluxo logístico acadêmico, independente do pagamento simulado. */
public final class StatusPedido {
    public static final List<String> TODOS=List.of("aberto","em_preparo","enviado","entregue","cancelado");
    private StatusPedido() {}
    public static String nome(String status){
        return switch(status){case "aberto"->"Aberto";case "em_preparo"->"Em preparo";case "enviado"->"Enviado";case "entregue"->"Entregue";case "cancelado"->"Cancelado";default->status;};
    }
    public static String proximo(String status){
        return switch(status){case "aberto"->"em_preparo";case "em_preparo"->"enviado";case "enviado"->"entregue";default->null;};
    }
    public static boolean podeCancelar(String status,boolean equipe){return "aberto".equals(status) || (equipe && "em_preparo".equals(status));}
}
