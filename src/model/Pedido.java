package model;

import java.math.BigDecimal;
import util.Dinheiro;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Pedido {

    private int id;
    private int idCliente;
    private String nomeCliente;
    private Date dataPedido;
    private String status; // aberto → em_preparo → enviado → entregue, ou cancelado
    private BigDecimal total = new BigDecimal("0.00");
    private List<ItemPedido> itens;
    private String metodoPagamento;
    private String statusPagamento;
    public String getMetodoPagamento(){return metodoPagamento;}
    public void setMetodoPagamento(String metodo){metodoPagamento=metodo;}
    public String getStatusPagamento(){return statusPagamento;}
    public void setStatusPagamento(String status){statusPagamento=status;}

    public Pedido() {
        this.itens = new ArrayList<>();
        this.status = "aberto";
        this.dataPedido = new Date();
    }

    public void adicionarItem(ItemPedido item) {
        itens.add(item);
        recalcularTotal();
    }

    public void removerItem(ItemPedido item) {
        itens.remove(item);
        recalcularTotal();
    }

    public void recalcularTotal() {
        total = itens.stream().map(ItemPedido::getSubtotalDecimal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public Date getDataPedido() { return dataPedido; }
    public void setDataPedido(Date dataPedido) { this.dataPedido = dataPedido; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotal() { return total.doubleValue(); }
    public void setTotal(double total) { this.total = Dinheiro.de(total); }
    public BigDecimal getTotalDecimal() { return total; }
    public void setTotal(BigDecimal total) { this.total = Dinheiro.normalizar(total); }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
        recalcularTotal();
    }
}
