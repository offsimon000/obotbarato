package util;
import dao.*;import model.Usuario;import java.time.LocalDate;

/** Executado manualmente após backup; nunca faz parte da inicialização automática. */
public final class SimularUsoDiario {
    public static void main(String[] args)throws Exception{
        if(args.length>1)throw new IllegalArgumentException("Uso: util.SimularUsoDiario [aaaa-mm-dd]");
        Usuario admin=new UsuarioDAO().autenticar("admin","admin123");if(admin==null)throw new IllegalArgumentException("Conta admin de demonstração indisponível; credenciais existentes preservadas.");Sessao.iniciar(admin);
        try{var r=new SimulacaoUsoDAO().instalar(args.length==0?LocalDate.now():LocalDate.parse(args[0]));
            System.out.println("CONCLUÍDO: "+r.clientesNovos()+" clientes novos, "+r.pedidosNovos()+" pedidos novos.");
            System.out.println("SIMULAÇÃO: "+Datas.formatar(r.inicio())+" a "+Datas.formatar(r.fim())+"; "+r.pedidos()+" pedidos, "+r.confirmados()+" confirmados válidos, "+r.pendentes()+" pendentes, "+r.cancelados()+" cancelados; "+r.avaliacoes()+" avaliações; "+r.estoqueCritico()+" produtos com estoque crítico.");
            System.out.println("FATURAMENTO DA SIMULAÇÃO: "+PagamentoSimulado.moeda(r.faturamento()));
            System.out.println("Cliente fictício para login: "+SimulacaoUsoDAO.email(0)+" / "+SimulacaoUsoDAO.SENHA);
            System.out.println("Repetição preserva pedidos, senhas e saldo; nenhum registro anterior foi apagado.");
        }finally{Sessao.encerrar();}
    }
}
