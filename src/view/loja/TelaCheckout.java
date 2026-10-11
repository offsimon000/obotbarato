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
    private final JTextField textoCupom=TemaLoja.campo(14);
    private dao.CupomDAO.Cupom cupom;
    private boolean validandoCupom;
    private JButton aplicarCupom,removerCupom;
    private javax.swing.event.DocumentListener edicaoCupom;
    /** Construtor de visualização usado na verificação das telas; confirmação exige reserva. */
    public TelaCheckout(TelaCarrinho anterior,Cliente cliente,List<ItemPedido> itens,TelaLoja loja){this(anterior,cliente,itens,loja,null);}
    public TelaCheckout(TelaCarrinho anterior,Cliente cliente,List<ItemPedido> itens,TelaLoja loja,UUID chave){
        Acesso.telaCliente(cliente.getId());this.telaCarrinho=anterior;this.loja=loja;this.cliente=cliente;this.itens=List.copyOf(itens);this.chave=chave;
        subtotal=itens.stream().map(ItemPedido::getSubtotalDecimal).reduce(new BigDecimal("0.00"),BigDecimal::add);
        setTitle("OBotBarato - Checkout / simulação");setSize(700,650);setLocationRelativeTo(loja);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel p=new JPanel(new BorderLayout());p.setBackground(TemaLoja.BG_PRINCIPAL);
        JPanel topo=new JPanel(new BorderLayout());topo.setBackground(TemaLoja.BG_CARD);topo.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        voltar=Tema.botaoVoltar(this,anterior,()->!processando && !validandoCupom);voltar.setText("< Voltar ao carrinho");topo.add(voltar,BorderLayout.WEST);
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
        JPanel rodape=new JPanel();rodape.setLayout(new BoxLayout(rodape,BoxLayout.Y_AXIS));rodape.setOpaque(false);rodape.add(valor);rodape.add(Box.createVerticalStrut(10));
        JPanel linhaCupom=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));linhaCupom.setOpaque(false);linhaCupom.add(new JLabel("Cupom:"));textoCupom.setName("checkout-cupom");linhaCupom.add(textoCupom);
        aplicarCupom=TemaLoja.botaoFantasia("Aplicar cupom");removerCupom=TemaLoja.botaoFantasia("Remover");linhaCupom.add(aplicarCupom);linhaCupom.add(removerCupom);rodape.add(linhaCupom);
        JLabel avisoCupom=new JLabel(cupom==null?"Um cupom por compra; desconto de pagamento sobre o restante.":"Cupom "+cupom.codigo()+" aplicado: "+cupom.percentual()+"%.");avisoCupom.setFont(TemaLoja.FONTE_LABEL);rodape.add(Box.createVerticalStrut(6));rodape.add(avisoCupom);rodape.add(Box.createVerticalStrut(12));
        rodape.add(new JLabel("Nenhuma cobrança real. Preço e estoque serão conferidos ao confirmar."));
        JPanel botoes=new JPanel(new FlowLayout(FlowLayout.RIGHT));botoes.setOpaque(false);
        JButton anterior=Tema.botaoVoltar(()->{if(!processando && !validandoCupom)trocar(resumo());}),confirmar=TemaLoja.botaoPrimario("Confirmar compra simulada");anterior.setText("< Resumo");confirmar.addActionListener(e->confirmar(confirmar,anterior,valor));
        textoCupom.setEditable(cupom==null);aplicarCupom.setEnabled(cupom==null);
        if(edicaoCupom!=null)textoCupom.getDocument().removeDocumentListener(edicaoCupom);
        edicaoCupom=new javax.swing.event.DocumentListener(){private void atualizar(){confirmar.setEnabled(!processando && !validandoCupom && (textoCupom.getText().isBlank() || cupom!=null));}public void insertUpdate(javax.swing.event.DocumentEvent e){atualizar();}public void removeUpdate(javax.swing.event.DocumentEvent e){atualizar();}public void changedUpdate(javax.swing.event.DocumentEvent e){atualizar();}};textoCupom.getDocument().addDocumentListener(edicaoCupom);
        removerCupom.addActionListener(e->{if(processando || validandoCupom)return;cupom=null;textoCupom.setText("");textoCupom.setEditable(true);aplicarCupom.setEnabled(true);confirmar.setEnabled(true);avisoCupom.setText("Cupom removido.");atualizarValor(valor);});
        aplicarCupom.addActionListener(e->{
            if(processando || validandoCupom)return;String codigo=textoCupom.getText();
            try{dao.CupomDAO.codigo(codigo);}catch(Exception ex){avisoCupom.setText(Erros.mensagem(ex));return;}
            validandoCupom=true;aplicarCupom.setEnabled(false);removerCupom.setEnabled(false);textoCupom.setEditable(false);confirmar.setEnabled(false);anterior.setEnabled(false);voltar.setEnabled(false);avisoCupom.setText("Validando cupom...");
            new SwingWorker<dao.CupomDAO.Cupom,Void>(){
                protected dao.CupomDAO.Cupom doInBackground()throws Exception{return new dao.CupomDAO().consultar(codigo);}
                protected void done(){validandoCupom=false;if(Sessao.getIdCliente()!=cliente.getId() || !isDisplayable())return;try{cupom=get();textoCupom.setText(cupom.codigo());avisoCupom.setText("Cupom "+cupom.codigo()+": "+cupom.percentual()+"%. Revalidado ao confirmar.");atualizarValor(valor);}catch(Exception e){cupom=null;avisoCupom.setText(Erros.mensagem(e));}finally{textoCupom.setEditable(cupom==null);aplicarCupom.setEnabled(cupom==null);removerCupom.setEnabled(true);confirmar.setEnabled(cupom!=null || textoCupom.getText().isBlank());anterior.setEnabled(true);voltar.setEnabled(true);}}
            }.execute();
        });
        confirmar.setEnabled(textoCupom.getText().isBlank() || cupom!=null);
        botoes.add(anterior);botoes.add(confirmar);rodape.add(botoes);p.add(rodape,BorderLayout.SOUTH);return p;
    }
    private void atualizarValor(JLabel label){var v=DescontosCompra.calcular(subtotal,metodo,cupom==null?null:cupom.percentual());label.setText("<html>Desconto do cupom: "+PagamentoSimulado.moeda(v.cupom())+"<br>Desconto de pagamento: "+PagamentoSimulado.moeda(v.pagamento())+"<br>Total: "+PagamentoSimulado.moeda(v.total())+"</html>");}
    private void confirmar(JButton confirmar,JButton anterior,JLabel status){
        if(processando || validandoCupom || resultado!=null || chave==null)return;
        if(!textoCupom.getText().isBlank() && cupom==null){aviso(new IllegalArgumentException("Aplique ou remova o cupom antes de confirmar."));return;}
        String nome=destinatario.getText(),tel=telefone.getText(),end=endereco.getText(),pagamento=metodo;
        var cupomConfirmado=cupom;aplicarCupom.setEnabled(false);removerCupom.setEnabled(false);textoCupom.setEditable(false);
        processando=true;confirmar.setEnabled(false);anterior.setEnabled(false);voltar.setEnabled(false);status.setText("Confirmando a compra...");
        // Bloqueia também os radios enquanto a confirmação executa fora do EDT.
        habilitarRadios(etapa,false);
        new SwingWorker<CompraDAO.Resultado,Void>(){
            protected CompraDAO.Resultado doInBackground()throws Exception{return new CompraDAO().confirmar(cliente.getId(),itens,pagamento,nome,tel,end,chave,cupomConfirmado==null?null:cupomConfirmado.codigo(),cupomConfirmado==null?null:cupomConfirmado.percentual());}
            protected void done(){
                processando=false;
                try{
                    resultado=get();
                    if(Sessao.getIdCliente()!=cliente.getId())return;
                    loja.getModeloCarrinho().concluir(chave);loja.atualizarBadgeCarrinho();loja.atualizarCatalogo();
                    if(isDisplayable())trocar(confirmacao());
                }catch(Exception ex){if(Sessao.getIdCliente()==cliente.getId()){aviso(ex);atualizarValor(status);}}
                finally{confirmar.setEnabled(resultado==null);anterior.setEnabled(resultado==null);voltar.setEnabled(true);habilitarRadios(etapa,true);aplicarCupom.setEnabled(resultado==null && cupom==null);removerCupom.setEnabled(resultado==null);textoCupom.setEditable(resultado==null && cupom==null);}
            }
        }.execute();
    }
    private JPanel confirmacao(){
        JPanel p=base();JPanel card=new JPanel(new GridLayout(0,1,4,14));card.setBackground(TemaLoja.BG_CARD);card.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JLabel titulo=new JLabel("Pedido #"+resultado.id()+" registrado!",SwingConstants.CENTER);titulo.setFont(new Font("Segoe UI",Font.BOLD,22));card.add(titulo);
        card.add(new JLabel("Pagamento SIMULADO: "+PagamentoSimulado.NOMES.get(resultado.metodo())+" / "+resultado.pagamento(),SwingConstants.CENTER));
        card.add(new JLabel("Total: "+PagamentoSimulado.moeda(resultado.total()),SwingConstants.CENTER));
        if(resultado.cupom()!=null)card.add(new JLabel("Cupom "+resultado.cupom()+": "+PagamentoSimulado.moeda(resultado.descontoCupom()),SwingConstants.CENTER));
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
