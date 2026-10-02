package view;
import dao.*;
import model.*;
import util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.*;

public class FormularioProduto extends JDialog {
    private final ProdutoDAO dao=new ProdutoDAO();
    private final Integer idProduto;
    private final JTextField nome=Tema.campo(24),preco=Tema.campo(12);
    private final JTextArea descricao=new JTextArea(3,24);
    private final JComboBox<Categoria> categoria=new JComboBox<>();
    private final JCheckBox ativo=new JCheckBox("Produto ativo",true);
    private final JSpinner quantidade=new JSpinner(new SpinnerNumberModel(0,0,1000000,1));
    private final JSpinner minimo=new JSpinner(new SpinnerNumberModel(5,0,1000000,1));
    private final JPanel preview=new JPanel(new BorderLayout());
    private byte[] imagem;
    public FormularioProduto(JFrame parent,Integer id){
        super(parent,id==null?"Novo Produto":"Editar Produto",true);Acesso.telaAdmin();idProduto=id;
        setSize(500,690);setLocationRelativeTo(parent);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel p=new JPanel(new GridBagLayout());p.setBackground(Tema.BG_ESCURO);p.setBorder(BorderFactory.createEmptyBorder(16,24,16,24));
        GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(5,0,5,0);
        int row=0;
        JLabel titulo=new JLabel(id==null?"Novo Produto":"Editar Produto");titulo.setFont(Tema.FONTE_TITULO);g.gridy=row++;p.add(titulo,g);
        descricao.setFont(Tema.FONTE_CAMPO);descricao.setLineWrap(true);descricao.setWrapStyleWord(true);
        String[] labels={"NOME","PREÇO (R$)","CATEGORIA","DESCRIÇÃO"};JComponent[] campos={nome,preco,categoria,Tema.scrollDark(descricao)};
        for(int i=0;i<labels.length;i++){JLabel l=new JLabel(labels[i]);l.setFont(Tema.FONTE_LABEL);g.gridy=row++;p.add(l,g);g.gridy=row++;p.add(campos[i],g);}
        if(id==null){
            JLabel l=new JLabel("ESTOQUE INICIAL / MÍNIMO");g.gridy=row++;p.add(l,g);
            JPanel sp=new JPanel(new GridLayout(1,2,10,0));sp.add(quantidade);sp.add(minimo);g.gridy=row++;p.add(sp,g);
        }
        g.gridy=row++;p.add(ativo,g);
        preview.setPreferredSize(new Dimension(180,95));g.gridy=row++;p.add(preview,g);atualizarImagem();
        JPanel botoesImagem=new JPanel(new FlowLayout(FlowLayout.LEFT));botoesImagem.setBackground(Tema.BG_ESCURO);
        JButton escolher=Tema.botaoSecundario("Selecionar imagem"),remover=Tema.botaoSecundario("Remover imagem");
        escolher.addActionListener(e->{JFileChooser fc=new JFileChooser();fc.setFileFilter(new FileNameExtensionFilter("PNG / JPEG","png","jpg","jpeg"));if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)try{imagem=Imagens.ler(fc.getSelectedFile().toPath());atualizarImagem();}catch(Exception ex){aviso(ex);}});
        remover.addActionListener(e->{imagem=null;atualizarImagem();});botoesImagem.add(escolher);botoesImagem.add(remover);g.gridy=row++;p.add(botoesImagem,g);
        JButton salvar=Tema.botaoPrimario("Salvar Produto");salvar.addActionListener(e->salvar(salvar));g.gridy=row;p.add(salvar,g);
        setContentPane(Tema.scrollDark(p));
        try{
            java.util.List<Categoria> cats=new CategoriaDAO().listar(false);for(Categoria c:cats)categoria.addItem(c);
            if(id!=null){Produto produto=dao.buscarPorId(id);nome.setText(produto.getNome());preco.setText(produto.getPrecoDecimal().toPlainString());descricao.setText(produto.getDescricao());ativo.setSelected(produto.isAtivo());imagem=produto.getImagem();atualizarImagem();
                boolean achou=false;for(Categoria c:cats)if(c.id()==produto.getIdCategoria()){categoria.setSelectedItem(c);achou=true;break;}
                if(!achou){Categoria c=new Categoria(produto.getIdCategoria(),produto.getNomeCategoria()+" (inativa)",false);categoria.addItem(c);categoria.setSelectedItem(c);}
            }
        }catch(Exception ex){aviso(ex);salvar.setEnabled(false);}
    }
    private void atualizarImagem(){preview.removeAll();preview.add(Imagens.painel(imagem,"Sem imagem",95));preview.revalidate();preview.repaint();}
    private void salvar(JButton botao){
        Produto p=new Produto();
        try{
            if(idProduto!=null)p.setId(idProduto);p.setNome(nome.getText());p.setDescricao(descricao.getText());p.setPreco(new java.math.BigDecimal(preco.getText().trim().replace(",",".")));
            Categoria c=(Categoria)categoria.getSelectedItem();if(c==null)throw new IllegalArgumentException("Cadastre e selecione uma categoria.");p.setIdCategoria(c.id());p.setAtivo(ativo.isSelected());p.setImagem(imagem);
            p.setEstoqueInicial((Integer)quantidade.getValue());p.setEstoqueMinimo((Integer)minimo.getValue());
        }catch(Exception ex){aviso(ex);return;}
        botao.setEnabled(false);
        new SwingWorker<Boolean,Void>(){
            protected Boolean doInBackground()throws Exception{return idProduto==null?dao.salvar(p):dao.atualizar(p);}
            protected void done(){botao.setEnabled(true);try{if(!get())throw new IllegalArgumentException("Produto não encontrado.");JOptionPane.showMessageDialog(FormularioProduto.this,"Produto salvo com sucesso!");dispose();}catch(Exception ex){aviso(ex);}}
        }.execute();
    }
    private void aviso(Throwable ex){JOptionPane.showMessageDialog(this,Erros.mensagem(ex),"Produto",JOptionPane.WARNING_MESSAGE);}
}
