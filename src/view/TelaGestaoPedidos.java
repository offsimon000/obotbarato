package view;

import dao.GestaoPedidoDAO;
import model.Cliente;
import util.*;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Gestão pela equipe e acompanhamento pelo titular usando as mesmas regras de banco. */
public class TelaGestaoPedidos extends TelaManutencao {
    private final GestaoPedidoDAO dao=new GestaoPedidoDAO();
    private final boolean equipe;
    private final int usuario,cliente;
    private final JTextField inicio=new JTextField(),fim=new JTextField(),busca=new JTextField();
    private final JComboBox<String> status=new JComboBox<>(),origem=new JComboBox<>(new String[]{"Todas","Loja","Balcão"});
    private final List<JButton> comandos=new ArrayList<>();
    private List<GestaoPedidoDAO.Resumo> pedidos=List.of();
    private long consulta;
    private boolean processando;
    private boolean carregando;
    private final JLabel orientacao=new JLabel("Selecione um pedido para consultar os detalhes.");
    private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TelaGestaoPedidos(JFrame anterior){this(anterior,null);}
    protected TelaGestaoPedidos(JFrame anterior,Cliente titular){
        super(anterior,titular==null?"Gestão de pedidos":"Meus pedidos",new String[]{"Pedido","Data","Cliente","Origem","Pedido / entrega","Pagamento","Total"});
        equipe=titular==null;
        if(equipe)Acesso.telaEquipe();else Acesso.telaCliente(titular.getId());
        usuario=Sessao.getIdUsuario();cliente=Sessao.getIdCliente();
        setSize(1000,600);setLocationRelativeTo(anterior);
        setMinimumSize(new Dimension(1000,570));
        status.addItem("Todos");for(String s:StatusPedido.TODOS)status.addItem(StatusPedido.nome(s));
        JPanel filtros=new JPanel(new GridLayout(2,equipe?5:4,10,5));filtros.setBackground(Tema.BG_ESCURO);filtros.setBorder(BorderFactory.createEmptyBorder(0,0,12,0));
        String[] rotulos=equipe?new String[]{"Início (dd/mm/aaaa)","Fim (dd/mm/aaaa)","Cliente: nome ou e-mail","Estado do pedido","Origem"}:new String[]{"Início (dd/mm/aaaa)","Fim (dd/mm/aaaa)","Estado do pedido","Origem"};
        for(String nome:rotulos){JLabel l=new JLabel(nome);l.setFont(Tema.FONTE_LABEL);filtros.add(l);}
        MascaraCampo.aplicarData(inicio);MascaraCampo.aplicarData(fim);
        inicio.setName("filtro-inicio");fim.setName("filtro-fim");busca.setName("filtro-cliente");status.setName("filtro-status");origem.setName("filtro-origem");
        for(JComponent c:new JComponent[]{inicio,fim,busca,status,origem}){c.setFont(Tema.FONTE_CAMPO);if(equipe || c!=busca)filtros.add(c);}
        if(!equipe){busca.setEnabled(false);busca.setToolTipText("A consulta mostra somente os seus pedidos.");}
        Container centro=(Container)((BorderLayout)getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);centro.add(filtros,BorderLayout.NORTH);
        comando("Filtrar / Atualizar",this::carregar);
        comando("Detalhes e histórico",()->TelaPagamentos.mostrarDetalhes(this,selecionar().id()));
        if(equipe){
            comando("Nova venda no balcão",()->{setVisible(false);new TelaPedidos(Sessao.getUsuario(),this).setVisible(true);});
            comando("Avançar entrega",this::avancar);
        }
        comando("Cancelar pedido",this::cancelar);
        JPanel rodape=new JPanel(new BorderLayout());rodape.setBackground(Tema.BG_CARD);
        orientacao.setFont(Tema.FONTE_LABEL);orientacao.setBorder(BorderFactory.createEmptyBorder(10,16,0,16));rodape.add(orientacao,BorderLayout.NORTH);rodape.add(botoes,BorderLayout.SOUTH);getContentPane().add(rodape,BorderLayout.SOUTH);
        tabela.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())atualizarComandos();});
        getRootPane().setDefaultButton(comandos.get(0));
        carregar();
    }
    private boolean mesmaSessao(){return usuario==Sessao.getIdUsuario() && cliente==Sessao.getIdCliente();}
    private void comando(String texto,Runnable acao){
        JButton b=Tema.botaoSecundario(texto);b.addActionListener(e->{try{if(processando)return;if(!mesmaSessao())throw new IllegalArgumentException("Sessão encerrada.");acao.run();}catch(Exception ex){erro(ex);}});comandos.add(b);botoes.add(b);
    }
    private GestaoPedidoDAO.Resumo selecionar(){int i=selecionado();if(i>=pedidos.size())throw new IllegalArgumentException("Atualize a lista.");return pedidos.get(i);}
    private LocalDate data(JTextField campo){return Datas.opcional(campo.getText());}
    private void carregar(){
        GestaoPedidoDAO.Filtro filtro=new GestaoPedidoDAO.Filtro(data(inicio),data(fim),equipe?busca.getText():null,status.getSelectedIndex()==0?null:StatusPedido.TODOS.get(status.getSelectedIndex()-1),origem.getSelectedIndex()==0?null:origem.getSelectedIndex()==1?"loja":"balcao");
        Integer selecionado=tabela.getSelectedRow()<0?null:selecionar().id();
        long versao=++consulta;carregando=true;atualizarComandos();orientacao.setText("Atualizando pedidos...");
        new SwingWorker<List<GestaoPedidoDAO.Resumo>,Void>(){
            protected List<GestaoPedidoDAO.Resumo> doInBackground()throws Exception{return dao.listar(filtro);}
            protected void done(){if(!mesmaSessao() || versao!=consulta || processando)return;try{pedidos=get();modelo.setRowCount(0);for(var p:pedidos)modelo.addRow(new Object[]{p.id(),DATA.format(p.data().toLocalDateTime()),p.cliente(),p.origem().equals("loja")?"Loja":"Balcão",StatusPedido.nome(p.status()),p.pagamento(),p.total()});if(selecionado!=null)for(int i=0;i<pedidos.size();i++)if(pedidos.get(i).id()==selecionado){tabela.setRowSelectionInterval(i,i);break;}}catch(Exception e){erro(e);}finally{carregando=false;atualizarComandos();}}
        }.execute();
    }
    private void atualizarComandos(){
        boolean livre=!processando && !carregando && mesmaSessao();
        int linha=tabela.getSelectedRow();var p=linha>=0 && linha<pedidos.size()?pedidos.get(linha):null;
        for(JButton b:comandos){
            boolean habilitado=livre;
            if(b.getText().equals("Filtrar / Atualizar"))habilitado=!processando && mesmaSessao();
            if(b.getText().equals("Detalhes e histórico"))habilitado &= p!=null;
            if(b.getText().equals("Cancelar pedido"))habilitado &= p!=null && StatusPedido.podeCancelar(p.status(),equipe);
            if(b.getText().equals("Avançar entrega"))habilitado &= p!=null && StatusPedido.proximo(p.status())!=null && p.pagamento().equals("confirmado");
            b.setEnabled(habilitado);
        }
        if(processando)orientacao.setText("Processando o pedido. Aguarde a confirmação.");
        else if(!carregando){
            if(pedidos.isEmpty())orientacao.setText(equipe?"Nenhum pedido encontrado com estes filtros.":"Nenhum pedido encontrado. Suas compras aparecerão aqui.");
            else if(p==null)orientacao.setText("Selecione um pedido para ver itens, entrega, pagamento e histórico.");
            else if(p.status().equals("cancelado"))orientacao.setText("Pedido cancelado: consulte o motivo no histórico. O cancelamento não pode ser repetido.");
            else if(!StatusPedido.podeCancelar(p.status(),equipe))orientacao.setText(equipe?"Pedido enviado ou entregue: cancelamento indisponível.":"Cancelamento disponível somente antes do preparo. Consulte o histórico para acompanhar.");
            else orientacao.setText(equipe?"Avance uma etapa por vez. Cancelamento permitido antes do envio.":"Você pode cancelar este pedido aberto, informando o motivo.");
        }
    }
    private void avancar(){
        var p=selecionar();String destino=StatusPedido.proximo(p.status());
        if(destino==null)throw new IllegalArgumentException("Este pedido não possui próxima etapa.");
        if(!p.pagamento().equals("confirmado"))throw new IllegalArgumentException("Confirme o pagamento simulado antes do preparo.");
        if(!confirmarAvanco(p,destino))return;
        executar(()->dao.avancar(p.id(),p.status(),destino));
    }
    private void cancelar(){
        var p=selecionar();
        if(!StatusPedido.podeCancelar(p.status(),equipe))throw new IllegalArgumentException(equipe?"Cancelamento permitido antes do envio.":"Cancelamento disponível somente enquanto o pedido estiver aberto.");
        String motivo=solicitarMotivo(p);
        if(motivo==null)return;
        String razao=Validacao.texto(motivo,"Motivo do cancelamento",3,255);
        executar(()->dao.cancelar(p.id(),razao));
    }
    protected boolean confirmarAvanco(GestaoPedidoDAO.Resumo pedido,String destino){return JOptionPane.showConfirmDialog(this,"Pedido #"+pedido.id()+": "+StatusPedido.nome(pedido.status())+" → "+StatusPedido.nome(destino)+"?\nA entrega é uma simulação acadêmica.","Avançar entrega",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    protected String solicitarMotivo(GestaoPedidoDAO.Resumo pedido){return JOptionPane.showInputDialog(this,"Motivo do cancelamento do pedido #"+pedido.id()+":\nO estoque será devolvido e o pagamento simulado será cancelado.","Cancelar pedido",JOptionPane.QUESTION_MESSAGE);}
    @FunctionalInterface private interface Operacao{void executar()throws Exception;}
    private void executar(Operacao operacao){
        processando=true;++consulta;atualizarComandos();
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{operacao.executar();return null;}
            protected void done(){processando=false;if(!mesmaSessao())return;try{get();}catch(Exception e){erro(e);}carregar();}
        }.execute();
    }
}
