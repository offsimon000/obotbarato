package view.loja;

import dao.CompraDAO;
import model.*;
import util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Checkout de uma tentativa reservada; valores exibidos usam a mesma regra da persistência. */
public class TelaCheckout extends JFrame {
    private final TelaCarrinho telaCarrinho;
    private final TelaLoja loja;
    private final Cliente cliente;
    private final List<ItemPedido> itens;
    private final UUID chave;
    private final BigDecimal subtotal;
    private final JPanel etapa=new JPanel(new BorderLayout());
    private final JTextField destinatario=TemaLoja.campo(24),telefone=TemaLoja.campo(18),endereco=TemaLoja.campo(32);
    private String metodo="credito";
    private boolean processando;
    private CompraDAO.Resultado resultado;
    private JButton voltar;
    /** Construtor de visualização usado na verificação das telas; confirmação exige reserva. */
    public TelaCheckout(TelaCarrinho anterior,Cliente cliente,List<ItemPedido> itens,TelaLoja loja){this(anterior,cliente,itens,loja,null);}
    public TelaCheckout(TelaCarrinho anterior,Cliente cliente,List<ItemPedido> itens,TelaLoja loja,UUID chave){
        Acesso.telaCliente(cliente.getId());this.telaCarrinho=anterior;this.loja=loja;this.cliente=cliente;this.itens=List.copyOf(itens);this.chave=chave;
        subtotal=itens.stream().map(ItemPedido::getSubtotalDecimal).reduce(new BigDecimal("0.00"),BigDecimal::add);
        setTitle("OBotBarato - Checkout / simulação");setSize(700,650);setLocationRelativeTo(loja);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){if(!processando)dispose();}});
        JPanel p=new JPanel(new BorderLayout());p.setBackground(TemaLoja.BG_PRINCIPAL);
        JPanel topo=new JPanel(new BorderLayout());topo.setBackground(TemaLoja.BG_CARD);topo.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        voltar=TemaLoja.botaoFantasia("< Voltar ao carrinho");voltar.addActionListener(e->{if(!processando)dispose();});topo.add(voltar,BorderLayout.WEST);
        JLabel titulo=new JLabel("Compra e pagamento simulados");titulo.setFont(TemaLoja.FONTE_BOTAO);topo.add(titulo,BorderLayout.EAST);p.add(topo,BorderLayout.NORTH);
        etapa.setBackground(TemaLoja.BG_PRINCIPAL);p.add(etapa);setContentPane(p);
        destinatario.setText(cliente.getNome());MascaraCampo.aplicarTelefone(telefone);telefone.setText(cliente.getTelefone());endereco.setText(cliente.getEndereco());
        trocar(resumo());
    }
    private JPanel base(){JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(TemaLoja.BG_PRINCIPAL);p.setBorder(BorderFactory.createEmptyBorder(20,24,20,24));return p;}
    private JPanel resumo(){
        JPanel p=base();JLabel titulo=new JLabel("1. Revise os itens e a entrega");titulo.setFont(new Font("Segoe UI",Font.BOLD,18));p.add(titulo,BorderLayout.NORTH);
        JPanel lista=new JPanel(new GridLayout(0,1,4,8));lista.setBackground(TemaLoja.BG_CARD);
        for(ItemPedido item:itens){JLabel l=new JLabel(item.getNomeProduto()+" × "+item.getQuantidade()+" — "+PagamentoSimulado.moeda(item.getSubtotalDecimal()));l.setFont(TemaLoja.FONTE_LABEL);lista.add(l);}
        p.add(TemaLoja.scroll(lista));
        JPanel rodape=new JPanel(new GridLayout(0,1,3,7));rodape.setOpaque(false);
        JLabel total=new JLabel("Subtotal: "+PagamentoSimulado.moeda(subtotal)+" | Entrega simulada grátis");total.setFont(TemaLoja.FONTE_TOTAL);rodape.add(total);
        rodape.add(new JLabel("DESTINATÁRIO"));rodape.add(destinatario);rodape.add(new JLabel("TELEFONE"));rodape.add(telefone);rodape.add(new JLabel("ENDEREÇO DE ENTREGA"));rodape.add(endereco);
        JButton avancar=TemaLoja.botaoPrimario("Ir para pagamento >");avancar.setEnabled(!itens.isEmpty() && chave!=null);
        avancar.addActionListener(e->{try{Validacao.texto(destinatario.getText(),"Destinatário",3,120);if(Validacao.telefone(telefone.getText())==null)throw new IllegalArgumentException("Informe telefone.");Validacao.texto(endereco.getText(),"Endereço",5,255);trocar(pagamento());}catch(Exception ex){aviso(ex);}});
        rodape.add(avancar);p.add(rodape,BorderLayout.SOUTH);return p;
    }
    private JPanel pagamento(){
        JPanel p=base();JLabel titulo=new JLabel("2. Escolha a simulação de pagamento");titulo.setFont(new Font("Segoe UI",Font.BOLD,18));p.add(titulo,BorderLayout.NORTH);
        JPanel formas=new JPanel(new GridLayout(0,1,0,12));formas.setOpaque(false);ButtonGroup grupo=new ButtonGroup();
        JLabel valor=new JLabel();valor.setFont(TemaLoja.FONTE_TOTAL);atualizarValor(valor);
        String[] codigos={"credito","debito","pix","boleto"};String[] descricoes={"Aprovação simulada automática","5% de desconto; aprovação simulada","10% de desconto; aprovação simulada","Pendente até o administrador confirmar a simulação"};
        for(int i=0;i<codigos.length;i++){
            String codigo=codigos[i];JRadioButton rb=new JRadioButton(PagamentoSimulado.NOMES.get(codigo)+" — "+descricoes[i],codigo.equals(metodo));
            rb.setFont(TemaLoja.FONTE_LABEL);rb.setBackground(TemaLoja.BG_CARD);rb.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));grupo.add(rb);formas.add(rb);
            rb.addActionListener(e->{metodo=codigo;atualizarValor(valor);});
        }p.add(formas);
        JPanel rodape=new JPanel(new GridLayout(0,1,4,12));rodape.setOpaque(false);rodape.add(valor);
        rodape.add(new JLabel("Nenhuma cobrança real. Preço e estoque serão conferidos ao confirmar."));
        JPanel botoes=new JPanel(new FlowLayout(FlowLayout.RIGHT));botoes.setOpaque(false);
        JButton anterior=TemaLoja.botaoFantasia("< Resumo"),confirmar=TemaLoja.botaoPrimario("Confirmar compra simulada");anterior.addActionListener(e->{if(!processando)trocar(resumo());});confirmar.addActionListener(e->confirmar(confirmar,anterior,valor));
        botoes.add(anterior);botoes.add(confirmar);rodape.add(botoes);p.add(rodape,BorderLayout.SOUTH);return p;
    }
    private void atualizarValor(JLabel label){label.setText("Desconto: "+PagamentoSimulado.moeda(PagamentoSimulado.desconto(subtotal,metodo))+" | Total: "+PagamentoSimulado.moeda(PagamentoSimulado.total(subtotal,metodo)));}
    private void confirmar(JButton confirmar,JButton anterior,JLabel status){
        if(processando || resultado!=null || chave==null)return;
        String nome=destinatario.getText(),tel=telefone.getText(),end=endereco.getText(),pagamento=metodo;
        processando=true;confirmar.setEnabled(false);anterior.setEnabled(false);voltar.setEnabled(false);status.setText("Confirmando a compra...");
        // Bloqueia também os radios enquanto a confirmação executa fora do EDT.
        habilitarRadios(etapa,false);
        new SwingWorker<CompraDAO.Resultado,Void>(){
            protected CompraDAO.Resultado doInBackground()throws Exception{return new CompraDAO().confirmar(cliente.getId(),itens,pagamento,nome,tel,end,chave);}
            protected void done(){
                processando=false;
                try{
                    resultado=get();
                    if(Sessao.getIdCliente()!=cliente.getId())return;
                    loja.getModeloCarrinho().concluir(chave);loja.atualizarBadgeCarrinho();loja.atualizarCatalogo();
                    if(isDisplayable())trocar(confirmacao());
                }catch(Exception ex){if(Sessao.getIdCliente()==cliente.getId()){aviso(ex);atualizarValor(status);}}
                finally{confirmar.setEnabled(resultado==null);anterior.setEnabled(resultado==null);voltar.setEnabled(true);habilitarRadios(etapa,true);}
            }
        }.execute();
    }
    private JPanel confirmacao(){
        JPanel p=base();JPanel card=new JPanel(new GridLayout(0,1,4,14));card.setBackground(TemaLoja.BG_CARD);card.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JLabel titulo=new JLabel("Pedido #"+resultado.id()+" registrado!",SwingConstants.CENTER);titulo.setFont(new Font("Segoe UI",Font.BOLD,22));card.add(titulo);
        card.add(new JLabel("Pagamento SIMULADO: "+PagamentoSimulado.NOMES.get(resultado.metodo())+" / "+resultado.pagamento(),SwingConstants.CENTER));
        card.add(new JLabel("Total: "+PagamentoSimulado.moeda(resultado.total()),SwingConstants.CENTER));
        card.add(new JLabel("Acompanhe preparo e entrega em Meus Pedidos.",SwingConstants.CENTER));
        if(resultado.pagamento().equals("pendente"))card.add(new JLabel("Aguardando confirmação do administrador; nenhuma cobrança real.",SwingConstants.CENTER));
        JButton detalhes=TemaLoja.botaoFantasia("Ver detalhes");detalhes.addActionListener(e->view.TelaPagamentos.mostrarDetalhes(this,resultado.id()));card.add(detalhes);
        JButton continuar=TemaLoja.botaoPrimario("Continuar comprando");continuar.addActionListener(e->{dispose();telaCarrinho.dispose();});card.add(continuar);
        JButton pedidos=TemaLoja.botaoFantasia("Ver meus pedidos");pedidos.addActionListener(e->{dispose();telaCarrinho.dispose();loja.abrirPedidos();});card.add(pedidos);p.add(card);return p;
    }
    private void trocar(JPanel painel){etapa.removeAll();etapa.add(painel);etapa.revalidate();etapa.repaint();}
    private static void habilitarRadios(Container c,boolean habilitado){for(Component filho:c.getComponents()){if(filho instanceof JRadioButton)filho.setEnabled(habilitado);if(filho instanceof Container)habilitarRadios((Container)filho,habilitado);}}
    private void aviso(Throwable ex){JOptionPane.showMessageDialog(this,Erros.mensagem(ex),"Compra",JOptionPane.WARNING_MESSAGE);}
    @Override public void dispose(){if(loja!=null){loja.getModeloCarrinho().liberar(chave);if(Sessao.getIdCliente()==cliente.getId())loja.atualizarBadgeCarrinho();}super.dispose();}
}
