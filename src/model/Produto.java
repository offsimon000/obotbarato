package model;

import java.math.BigDecimal;
import util.Dinheiro;

public class Produto {

    private int id;
    private String nome;
    private String descricao;
    private BigDecimal preco = new BigDecimal("0.00");
    private int idCategoria;
    private String nomeCategoria;
    private boolean ativo;
    private byte[] imagem;
    private int estoqueInicial;
    private int estoqueMinimo = 5;
    public byte[] getImagem(){ return imagem == null ? null : imagem.clone(); }
    public void setImagem(byte[] imagem){ this.imagem = imagem == null ? null : imagem.clone(); }
    public int getEstoqueInicial(){ return estoqueInicial; }
    public void setEstoqueInicial(int qtd){ if(qtd<0) throw new IllegalArgumentException("Estoque inicial inválido."); estoqueInicial=qtd; }
    public int getEstoqueMinimo(){ return estoqueMinimo; }
    public void setEstoqueMinimo(int qtd){ if(qtd<0) throw new IllegalArgumentException("Mínimo inválido."); estoqueMinimo=qtd; }

    public Produto() {}

    public Produto(int id, String nome, String descricao, double preco, int idCategoria, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        setPreco(preco);
        this.idCategoria = idCategoria;
        this.ativo = ativo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getPreco() { return preco.doubleValue(); }
    public void setPreco(double preco) { this.preco = Dinheiro.de(preco); }
    public BigDecimal getPrecoDecimal() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = Dinheiro.normalizar(preco); }

    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }

    public String getNomeCategoria() { return nomeCategoria; }
    public void setNomeCategoria(String nomeCategoria) { this.nomeCategoria = nomeCategoria; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public String toString() { return nome; }
}
