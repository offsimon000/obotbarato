package view;
import dao.CategoriaDAO;
import model.Categoria;
import util.*;
import javax.swing.*;
import java.util.*;

public final class TelaCategorias extends TelaManutencao {
    private final CategoriaDAO dao=new CategoriaDAO();
    private java.util.List<Categoria> categorias=new ArrayList<>();
    public TelaCategorias(JFrame anterior){
        super(anterior,"Categorias",new String[]{"ID","Nome","Situação"});Acesso.telaAdmin();
        botao("Atualizar",this::carregar);botao("Editar / desativar",()->editar(categorias.get(selecionado())));botao("+ Nova categoria",()->editar(null));carregar();
    }
    private void carregar(){try{categorias=dao.listar(true);modelo.setRowCount(0);for(Categoria c:categorias)modelo.addRow(new Object[]{c.id(),c.nome(),c.ativo()?"Ativa":"Inativa"});}catch(Exception e){erro(e);}}
    private void editar(Categoria atual){
        JTextField nome=Tema.campo(24);JCheckBox ativo=new JCheckBox("Categoria ativa",true);
        if(atual!=null){nome.setText(atual.nome());ativo.setSelected(atual.ativo());}
        JPanel p=form(new String[]{"Nome","Situação"},new JComponent[]{nome,ativo});
        while(JOptionPane.showConfirmDialog(this,p,"Categoria",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try{dao.salvar(atual==null?0:atual.id(),nome.getText(),ativo.isSelected());carregar();return;}catch(Exception e){erro(e);}
        }
    }
}
