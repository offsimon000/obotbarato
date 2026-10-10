package view.loja;
import dao.ClienteLojaDAO;
import model.Cliente;
import util.*;
import javax.swing.*;
import java.awt.*;

/** Atualiza o titular autenticado; confirma a senha atual e preserva o preenchimento em erros. */
public final class TelaPerfil extends JFrame {
    private final TelaLoja loja;
    private final int id;
    private final JTextField nome=TemaLoja.campo(28),cpf=TemaLoja.campo(18),email=TemaLoja.campo(28),telefone=TemaLoja.campo(18),endereco=TemaLoja.campo(32);
    private final JPasswordField atual=TemaLoja.campoSenha(24),nova=TemaLoja.campoSenha(24),confirmacao=TemaLoja.campoSenha(24);
    private final JLabel aviso=new JLabel("Carregando perfil...");
    private final JButton salvar=TemaLoja.botaoPrimario("Salvar perfil");
    private boolean salvando;
    public TelaPerfil(TelaLoja loja,Cliente cliente){
        Acesso.telaCliente(cliente.getId());this.loja=loja;id=cliente.getId();
        setTitle("OBotBarato - Meu perfil");setSize(570,700);setLocationRelativeTo(loja);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosing(java.awt.event.WindowEvent e){if(!salvando)dispose();}});
        JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(TemaLoja.BG_PRINCIPAL);p.setBorder(BorderFactory.createEmptyBorder(16,20,16,20));
        JLabel titulo=new JLabel("Meu perfil");titulo.setFont(TemaLoja.FONTE_TITULO);p.add(titulo,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout());form.setBackground(TemaLoja.BG_PRINCIPAL);GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(4,6,5,6);
        String[] labels={"Nome completo","CPF (opcional)","E-mail de acesso","Telefone (opcional)","Endereço (opcional)","Senha atual para confirmar alterações","Nova senha (opcional)","Confirme a nova senha"};
        JComponent[] campos={nome,cpf,email,telefone,endereco,atual,nova,confirmacao};String[] chaves={"nome","cpf","email","telefone","endereco","atual","nova","confirmacao"};
        for(int i=0;i<campos.length;i++){campos[i].setName("perfil-"+chaves[i]);g.gridy=i*2;form.add(TemaLoja.rotulo(labels[i]),g);g.gridy++;form.add(campos[i],g);}
        MascaraCampo.aplicarCpf(cpf);MascaraCampo.aplicarTelefone(telefone);p.add(TemaLoja.scroll(form));
        JPanel rodape=new JPanel(new BorderLayout(0,10));rodape.setOpaque(false);aviso.setFont(TemaLoja.FONTE_LABEL);rodape.add(aviso,BorderLayout.NORTH);salvar.addActionListener(e->salvar());rodape.add(salvar,BorderLayout.SOUTH);p.add(rodape,BorderLayout.SOUTH);setContentPane(p);
        habilitar(false);
        new SwingWorker<Cliente,Void>(){
            protected Cliente doInBackground()throws Exception{return new ClienteLojaDAO().buscarPerfil(id);}
            protected void done(){if(Sessao.getIdCliente()!=id)return;try{Cliente c=get();nome.setText(c.getNome());cpf.setText(c.getCpf());email.setText(c.getEmail());telefone.setText(c.getTelefone());endereco.setText(c.getEndereco());aviso.setText("Deixe a nova senha vazia para manter a atual.");habilitar(true);}catch(Exception e){aviso.setText(Erros.mensagem(e));}}
        }.execute();
    }
    private void habilitar(boolean ativo){for(JComponent c:new JComponent[]{nome,cpf,email,telefone,endereco,atual,nova,confirmacao,salvar})c.setEnabled(ativo);}
    private void salvar(){
        if(salvando || Sessao.getIdCliente()!=id)return;
        String senhaAtual=new String(atual.getPassword()),novaSenha=new String(nova.getPassword());
        if(!novaSenha.equals(new String(confirmacao.getPassword()))){aviso.setText("A confirmação da nova senha não confere.");return;}
        Cliente dados=new Cliente(id,nome.getText(),cpf.getText(),email.getText(),telefone.getText(),endereco.getText());
        try{Validacao.cliente(dados);if(!novaSenha.isEmpty())Validacao.senha(novaSenha);if(senhaAtual.isEmpty())throw new IllegalArgumentException("Informe a senha atual.");}catch(Exception e){aviso.setText(Erros.mensagem(e));return;}
        salvando=true;habilitar(false);aviso.setText("Salvando alterações...");
        new SwingWorker<Cliente,Void>(){
            protected Cliente doInBackground()throws Exception{return new ClienteLojaDAO().atualizarPerfil(dados,senhaAtual,novaSenha);}
            protected void done(){salvando=false;if(Sessao.getIdCliente()!=id)return;try{loja.atualizarPerfil(get());atual.setText("");nova.setText("");confirmacao.setText("");aviso.setText("Perfil salvo. No próximo acesso, use o e-mail e a senha atuais.");}catch(Exception e){aviso.setText(Erros.mensagem(e));}finally{habilitar(true);}}
        }.execute();
    }
}
