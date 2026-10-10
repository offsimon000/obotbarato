package view.loja;
import dao.AvaliacaoDAO;
import util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Avaliação/editável por produto, vinculada a um pedido entregue do titular. */
public final class TelaAvaliacao extends JFrame {
    private final int pedido,cliente=Sessao.getIdCliente();
    private final JComboBox<AvaliacaoDAO.ProdutoEntregue> produto=new JComboBox<>();
    private final JSpinner nota=new JSpinner(new SpinnerNumberModel(5,1,5,1));
    private final JTextArea comentario=new JTextArea(6,36);
    private final JLabel aviso=new JLabel("Carregando produtos entregues...");
    private final JButton salvar=TemaLoja.botaoPrimario("Salvar avaliação");private boolean gravando;private long consulta;
    public TelaAvaliacao(JFrame anterior,int pedido){
        Acesso.telaCliente(cliente);this.pedido=pedido;setTitle("OBotBarato - Avaliar pedido #"+pedido);setSize(580,500);setLocationRelativeTo(anterior);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel p=new JPanel(new BorderLayout(12,12));p.setBackground(TemaLoja.BG_PRINCIPAL);p.setBorder(BorderFactory.createEmptyBorder(18,20,18,20));
        JPanel campos=new JPanel(new GridLayout(0,1,4,8));campos.setOpaque(false);JLabel titulo=new JLabel("Avaliar pedido #"+pedido);titulo.setFont(TemaLoja.FONTE_TITULO);campos.add(titulo);campos.add(new JLabel("Produto comprado e entregue"));campos.add(produto);campos.add(new JLabel("Nota de 1 a 5"));campos.add(nota);p.add(campos,BorderLayout.NORTH);
        comentario.setName("avaliacao-comentario");nota.setName("avaliacao-nota");produto.setName("avaliacao-produto");comentario.setLineWrap(true);comentario.setWrapStyleWord(true);comentario.setFont(TemaLoja.FONTE_CAMPO);p.add(TemaLoja.scroll(comentario));
        JPanel rodape=new JPanel(new GridLayout(0,1,4,10));rodape.setOpaque(false);rodape.add(new JLabel("Comentário opcional, até 1000 caracteres. Fica visível na loja."));aviso.setFont(TemaLoja.FONTE_LABEL);rodape.add(aviso);salvar.addActionListener(e->salvar());rodape.add(salvar);p.add(rodape,BorderLayout.SOUTH);setContentPane(p);
        produto.addActionListener(e->carregarMinha());habilitar(false);
        new SwingWorker<List<AvaliacaoDAO.ProdutoEntregue>,Void>(){
            protected List<AvaliacaoDAO.ProdutoEntregue> doInBackground()throws Exception{return new AvaliacaoDAO().produtosEntregues(pedido);}
            protected void done(){if(Sessao.getIdCliente()!=cliente)return;try{for(var item:get())produto.addItem(item);produto.setEnabled(produto.getItemCount()>0);if(produto.getItemCount()==0)aviso.setText("Somente pedidos próprios entregues e pagos podem ser avaliados.");}catch(Exception e){aviso.setText(Erros.mensagem(e));}}
        }.execute();
    }
    private void habilitar(boolean habilitado){produto.setEnabled(habilitado);nota.setEnabled(habilitado);comentario.setEnabled(habilitado);salvar.setEnabled(habilitado);}
    private void carregarMinha(){var item=(AvaliacaoDAO.ProdutoEntregue)produto.getSelectedItem();if(item==null)return;long versao=++consulta;salvar.setEnabled(false);nota.setEnabled(false);comentario.setEnabled(false);
        new SwingWorker<AvaliacaoDAO.Avaliacao,Void>(){
            protected AvaliacaoDAO.Avaliacao doInBackground()throws Exception{return new AvaliacaoDAO().minha(item.id());}
            protected void done(){if(Sessao.getIdCliente()!=cliente || consulta!=versao)return;try{var a=get();nota.setValue(a==null?5:a.nota());comentario.setText(a==null?"":a.comentario());aviso.setText(a==null?"Você ainda não avaliou este produto.":"Avaliação existente: salvar atualiza sua nota e comentário.");habilitar(true);}catch(Exception e){aviso.setText(Erros.mensagem(e));}}
        }.execute();
    }
    private void salvar(){if(gravando || Sessao.getIdCliente()!=cliente)return;var item=(AvaliacaoDAO.ProdutoEntregue)produto.getSelectedItem();if(item==null)return;
        String texto=comentario.getText();if(texto.trim().length()>1000){aviso.setText("Comentário: limite de 1000 caracteres.");return;}try{nota.commitEdit();}catch(java.text.ParseException e){aviso.setText("Informe uma nota de 1 a 5.");return;}int valor=(Integer)nota.getValue();gravando=true;habilitar(false);aviso.setText("Salvando avaliação...");
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{new AvaliacaoDAO().salvar(pedido,item.id(),valor,texto);return null;}
            protected void done(){gravando=false;if(Sessao.getIdCliente()!=cliente)return;try{get();aviso.setText("Avaliação salva. Atualize a loja para ver a média.");}catch(Exception e){aviso.setText(Erros.mensagem(e));}finally{habilitar(true);}}
        }.execute();
    }
}
