package view.loja;

import model.Cliente;
import view.TelaGestaoPedidos;

/** Acompanhamento filtrado e autorizado pelo titular no banco. */
public final class TelaMeusPedidos extends TelaGestaoPedidos {
    public TelaMeusPedidos(TelaLoja loja,Cliente cliente){super(loja,cliente);}
}
