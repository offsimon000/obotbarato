package view;
import dao.CupomDAO;
import util.*;
import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Administração de percentuais e validade, com datas brasileiras mascaradas. */
public final class TelaCupons extends TelaManutencao {
    private final CupomDAO dao=new CupomDAO();
    private List<CupomDAO.Cupom> cupons=List.of();
    private final int usuario=Sessao.getIdUsuario();
    public TelaCupons(JFrame anterior){
        super(anterior,"Cupons",new String[]{"Código","Desconto (%)","Início","Fim","Situação"});Acesso.telaAdmin();
        botao("Atualizar",this::carregar);botao("Novo cupom",()->editar(null));botao("Editar / ativar",()->editar(cupons.get(selecionado())));carregar();
    }
    private void carregar(){
        new SwingWorker<List<CupomDAO.Cupom>,Void>(){
            protected List<CupomDAO.Cupom> doInBackground()throws Exception{return dao.listar();}
            protected void done(){if(Sessao.getIdUsuario()!=usuario)return;try{cupons=get();modelo.setRowCount(0);for(var c:cupons)modelo.addRow(new Object[]{c.codigo(),c.percentual().toPlainString().replace('.',','),Datas.formatar(c.inicio()),Datas.formatar(c.fim()),c.ativo()?"Ativo":"Inativo"});}catch(Exception e){erro(e);}}
        }.execute();
    }
    private void editar(CupomDAO.Cupom cupom){
        JTextField codigo=Tema.campo(24),percentual=Tema.campo(12),inicio=Tema.campo(12),fim=Tema.campo(12);MascaraCampo.aplicarData(inicio);MascaraCampo.aplicarData(fim);
        JCheckBox ativo=new JCheckBox("Cupom ativo",cupom==null || cupom.ativo());ativo.setBackground(Tema.BG_ESCURO);
        codigo.setText(cupom==null?"":cupom.codigo());percentual.setText(cupom==null?"10":cupom.percentual().toPlainString().replace('.',','));inicio.setText(Datas.formatar(cupom==null?LocalDate.now():cupom.inicio()));fim.setText(Datas.formatar(cupom==null?LocalDate.now().plusDays(30):cupom.fim()));
        JPanel campos=form(new String[]{"Código (3 a 32 caracteres)","Desconto percentual (0,01 a 100)","Início da validade","Último dia de validade","Ativação"},new JComponent[]{codigo,percentual,inicio,fim,ativo});
        while(true){
            if(JOptionPane.showConfirmDialog(this,campos,cupom==null?"Novo cupom":"Editar cupom",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            try{dao.salvar(cupom==null?0:cupom.id(),codigo.getText(),new BigDecimal(percentual.getText().trim().replace(',','.')),Datas.ler(inicio.getText()),Datas.ler(fim.getText()),ativo.isSelected());carregar();return;}catch(Exception e){erro(e instanceof NumberFormatException?new IllegalArgumentException("Informe um percentual válido."):e);}
        }
    }
}
