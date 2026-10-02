package view;
import dao.UsuarioDAO;
import model.Usuario;
import util.*;
import javax.swing.*;
import java.util.*;

public final class TelaUsuarios extends TelaManutencao {
    private final UsuarioDAO dao=new UsuarioDAO();
    private java.util.List<Usuario> usuarios=new ArrayList<>();
    public TelaUsuarios(JFrame anterior){
        super(anterior,"Usuários",new String[]{"ID","Nome","Login","Perfil","Situação"});
        Acesso.telaAdmin();
        botao("Atualizar",this::carregar);botao("Editar / desativar",()->editar(usuarios.get(selecionado())));botao("+ Novo usuário",()->editar(null));carregar();
    }
    private void carregar(){try{usuarios=dao.listar();modelo.setRowCount(0);for(Usuario u:usuarios)modelo.addRow(new Object[]{u.getId(),u.getNome(),u.getLogin(),u.getPerfil(),u.isAtivo()?"Ativo":"Inativo"});}catch(Exception e){erro(e);}}
    private void editar(Usuario atual){
        JTextField nome=Tema.campo(24),login=Tema.campo(24);JPasswordField senha=Tema.campSenha(24);
        JComboBox<String> perfil=new JComboBox<>(new String[]{"vendedor","admin"});JCheckBox ativo=new JCheckBox("Conta ativa",true);
        if(atual!=null){nome.setText(atual.getNome());login.setText(atual.getLogin());perfil.setSelectedItem(atual.getPerfil());ativo.setSelected(atual.isAtivo());}
        JPanel p=form(new String[]{"Nome","Login","Perfil","Senha (8 a 128; vazia mantém a atual)","Situação"},new JComponent[]{nome,login,perfil,senha,ativo});
        while(JOptionPane.showConfirmDialog(this,p,atual==null?"Novo usuário":"Editar usuário",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try{
                Usuario u=new Usuario();if(atual!=null)u.setId(atual.getId());u.setNome(nome.getText());u.setLogin(login.getText());u.setPerfil((String)perfil.getSelectedItem());u.setSenha(new String(senha.getPassword()));u.setAtivo(ativo.isSelected());
                if(atual==null)dao.salvar(u);else dao.atualizar(u);senha.setText("");carregar();
                if(u.getId()==Sessao.getIdUsuario() && (!u.isAtivo() || !u.isAdmin()))Sessao.sair();
                return;
            }catch(Exception e){erro(e);}
        }senha.setText("");
    }
}
