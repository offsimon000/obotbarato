package model;

import java.math.BigDecimal;
import util.Dinheiro;

public class ItemPedido {

    private int id;
    private int idPedido;
    private int idProduto;
    private String nomeProduto;
    private int quantidade;
    private BigDecimal precoUnitario = new BigDecimal("0.00");

    public ItemPedido() {}

    public ItemPedido(int idProduto, String nomeProduto, int quantidade, double precoUnitario) {
        this.idProduto = idProduto;
        this.nomeProduto = nomeProduto;
        setQuantidade(quantidade);
        setPrecoUnitario(precoUnitario);
    }

    public double getSubtotal() {
        return getSubtotalDecimal().doubleValue();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdPedido() { return idPedido; }
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }

    public int getIdProduto() { return idProduto; }
    public void setIdProduto(int idProduto) { this.idProduto = idProduto; }

    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        this.quantidade = quantidade;
    }

    public double getPrecoUnitario() { return precoUnitario.doubleValue(); }
    public void setPrecoUnitario(double precoUnitario) { this.precoUnitario = Dinheiro.de(precoUnitario); }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = Dinheiro.normalizar(precoUnitario); }
    public BigDecimal getPrecoUnitarioDecimal() { return precoUnitario; }
    public BigDecimal getSubtotalDecimal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
}