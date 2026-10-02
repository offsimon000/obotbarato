package model;
public record Categoria(int id, String nome, boolean ativo) {
    @Override public String toString(){ return nome; }
}
