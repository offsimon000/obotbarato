package model;
import java.util.*;
import java.math.BigDecimal;

/** Carrinho da sessão: cópias impedem alterações por outra janela durante checkout. */
public final class Carrinho {
    private final Map<Integer,ItemPedido> itens=new LinkedHashMap<>();
    private UUID checkout;
    private UUID tentativa;
    public synchronized void adicionar(Produto p,int quantidade,int disponivel){
        editavel();if(quantidade<=0)throw new IllegalArgumentException("Quantidade inválida.");
        int anterior=itens.containsKey(p.getId())?itens.get(p.getId()).getQuantidade():0;
        definir(p,Math.addExact(anterior,quantidade),disponivel);
    }
    public synchronized void definir(Produto p,int quantidade,int disponivel){
        editavel();if(quantidade<=0 || quantidade>disponivel)throw new IllegalArgumentException("Quantidade maior que o estoque disponível ou inválida.");
        ItemPedido item=new ItemPedido();item.setIdProduto(p.getId());item.setNomeProduto(p.getNome());item.setQuantidade(quantidade);item.setPrecoUnitario(p.getPrecoDecimal());
        itens.put(p.getId(),item);tentativa=null;
    }
    public synchronized void remover(int id){editavel();itens.remove(id);tentativa=null;}
    public synchronized void limpar(){editavel();itens.clear();tentativa=null;}
    public synchronized List<ItemPedido> itens(){return itens.values().stream().map(Carrinho::copiar).toList();}
    public synchronized BigDecimal subtotal(){return itens.values().stream().map(ItemPedido::getSubtotalDecimal).reduce(new BigDecimal("0.00"),BigDecimal::add);}
    public synchronized UUID iniciarCheckout(){
        editavel();if(itens.isEmpty())throw new IllegalArgumentException("Carrinho vazio.");
        if(tentativa==null)tentativa=UUID.randomUUID();checkout=tentativa;return checkout;
    }
    public synchronized void liberar(UUID chave){if(Objects.equals(checkout,chave))checkout=null;}
    public synchronized void concluir(UUID chave){if(!Objects.equals(checkout,chave))throw new IllegalArgumentException("Checkout inválido.");itens.clear();checkout=null;tentativa=null;}
    public synchronized boolean bloqueado(){return checkout!=null;}
    public synchronized void encerrar(){itens.clear();checkout=null;tentativa=null;}
    private void editavel(){if(checkout!=null)throw new IllegalArgumentException("Conclua ou feche o checkout antes de alterar o carrinho.");}
    private static ItemPedido copiar(ItemPedido i){ItemPedido c=new ItemPedido();c.setIdProduto(i.getIdProduto());c.setNomeProduto(i.getNomeProduto());c.setQuantidade(i.getQuantidade());c.setPrecoUnitario(i.getPrecoUnitarioDecimal());return c;}
}
