package view;
import dao.CompraDAO;
import util.*;
import javax.swing.*;
import java.awt.*;

/** Consulta pagamentos fictícios e confirma boletos pendentes pela conta de admin. */
public final class TelaPagamentos extends TelaManutencao {
    private final CompraDAO dao=new CompraDAO();
    private boolean processando;
    protected boolean podeVoltar(){return !processando;}
    public TelaPagamentos(JFrame anterior){
        super(anterior,"Pagamentos simulados",new String[]{"Pedido","Cliente","Total","Método","Pagamento","Origem"});Acesso.telaEquipe();
        botao("Atualizar",this::carregar);botao("Detalhes",()->{try{mostrarDetalhes(this,(int)Integer.parseInt(modelo.getValueAt(selecionado(),0).toString()));}catch(Exception e){erro(e);}});
        if(Sessao.isAdminAtivo())botao("Confirmar simulação",this::confirmar);carregar();
    }
    private void carregar(){
        if(processando)return;
        int solicitante=Sessao.getIdUsuario();
        new SwingWorker<java.util.List<String[]>,Void>(){
            protected java.util.List<String[]> doInBackground()throws Exception{return dao.listarPagamentos();}
            protected void done(){if(Sessao.getIdUsuario()!=solicitante || !isDisplayable())return;try{modelo.setRowCount(0);for(String[] r:get())modelo.addRow(r);}catch(Exception e){erro(e);}}
        }.execute();
    }
    private void confirmar(){
        if(processando)return;
        int id=Integer.parseInt(modelo.getValueAt(selecionado(),0).toString());
        if(JOptionPane.showConfirmDialog(this,"Confirmar o pagamento SIMULADO do pedido #"+id+"? Nenhuma cobrança real será feita.","Simulação",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        int solicitante=Sessao.getIdUsuario();processando=true;for(Component c:botoes.getComponents())c.setEnabled(false);
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{dao.confirmarPagamento(id);return null;}
            protected void done(){processando=false;if(Sessao.getIdUsuario()!=solicitante || !isDisplayable())return;for(Component c:botoes.getComponents())c.setEnabled(true);try{get();carregar();}catch(Exception e){erro(e);}}
        }.execute();
    }
    public static void mostrarDetalhes(Component parent,int id){
        int usuario=Sessao.getIdUsuario(),cliente=Sessao.getIdCliente();
        new SwingWorker<String,Void>(){
            protected String doInBackground()throws Exception{return new CompraDAO().detalhes(id);}
            protected void done(){if(Sessao.getIdUsuario()!=usuario || Sessao.getIdCliente()!=cliente)return;try{JTextArea area=new JTextArea(get(),22,58);area.setEditable(false);area.setFont(Tema.FONTE_LABEL);JOptionPane.showMessageDialog(parent,Tema.scrollDark(area),"Detalhes do pedido",JOptionPane.INFORMATION_MESSAGE);}catch(Exception e){JOptionPane.showMessageDialog(parent,Erros.mensagem(e));}}
        }.execute();
    }
}
