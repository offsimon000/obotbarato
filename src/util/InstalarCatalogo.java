package util;

import model.Usuario;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Importação local do catálogo autorizado, sem exclusão de histórico ou reposição em reexecução. */
public final class InstalarCatalogo {
    private record Item(String codigo,String nome,String categoria,BigDecimal preco,int quantidade,int minimo,byte[] imagem,String descricao) {}
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Uso: util.InstalarCatalogo pasta-catalogo");
        Path pasta=Path.of(args[0]).toAbsolutePath().normalize();List<Item> itens=new ArrayList<>();Set<String> nomes=new HashSet<>();
        for(String linha:Files.readAllLines(pasta.resolve("produtos.tsv"),StandardCharsets.UTF_8)){
            if(linha.isBlank())continue;String[] f=linha.split("\t",-1);
            if(f.length!=8)throw new IllegalArgumentException("Linha do catálogo inválida.");
            String nome=Validacao.texto(f[1],"Produto",2,120),categoria=Validacao.texto(f[2],"Categoria",2,80);
            if(!nomes.add(nome.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Produto repetido: "+nome);
            if(!f[6].matches("[a-z0-9-]+\\.png"))throw new IllegalArgumentException("Nome de imagem inválido.");
            byte[] imagem=Imagens.ler(pasta.resolve("imagens").resolve(f[6]));
            BigDecimal preco=Dinheiro.normalizar(new BigDecimal(f[3]));int qtd=Integer.parseInt(f[4]),minimo=Integer.parseInt(f[5]);
            if(qtd<=0 || minimo<0 || preco.signum()<=0)throw new IllegalArgumentException("Preço ou estoque inválido.");
            itens.add(new Item(f[0],nome,categoria,preco,qtd,minimo,imagem,Validacao.texto(f[7],"Descrição",1,4000)));
        }
        if(itens.size()!=30)throw new IllegalArgumentException("Catálogo deve conter exatamente 30 produtos.");
        List<String> registros=Transacao.executar(c->{
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){
                r.next();if(!Set.of("obotbarato","obotbarato_test").contains(r.getString(1)))throw new IllegalArgumentException("Use a base local oficial ou a base isolada de teste.");
            }
            try(Statement s=c.createStatement()){s.execute("SELECT pg_advisory_xact_lock(726281)");s.execute("SELECT pg_advisory_xact_lock(726282)");}
            Usuario admin=new Usuario();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id,nome,login FROM usuarios WHERE ativo AND perfil='admin' ORDER BY id LIMIT 1")){
                if(!r.next())throw new IllegalArgumentException("É necessário administrador ativo.");admin.setId(r.getInt(1));admin.setNome(r.getString(2));admin.setLogin(r.getString(3));admin.setPerfil("admin");admin.setAtivo(true);
            }
            Sessao.iniciar(admin);Acesso.equipe(c,true);
            List<Integer> ids=new ArrayList<>();List<String> resultado=new ArrayList<>();int novos=0;
            for(Item item:itens){
                int categoria;
                try(PreparedStatement p=c.prepareStatement("SELECT id,ativo FROM categorias WHERE lower(trim(nome))=lower(trim(?))")){
                    p.setString(1,item.categoria());try(ResultSet r=p.executeQuery()){categoria=r.next()?r.getInt(1):0;if(categoria>0 && !r.getBoolean(2))throw new IllegalArgumentException("Categoria desativada: "+item.categoria());}
                }
                if(categoria==0){try(PreparedStatement p=c.prepareStatement("INSERT INTO categorias(nome) VALUES(?) RETURNING id")){p.setString(1,item.categoria());try(ResultSet r=p.executeQuery()){r.next();categoria=r.getInt(1);}}}
                int id=0;
                try(PreparedStatement p=c.prepareStatement("SELECT id FROM produtos WHERE lower(trim(nome))=lower(trim(?)) AND id_categoria=? FOR UPDATE")){
                    p.setString(1,item.nome());p.setInt(2,categoria);try(ResultSet r=p.executeQuery()){if(r.next())id=r.getInt(1);if(r.next())throw new IllegalArgumentException("Mais de um cadastro com o nome "+item.nome());}
                }
                if(id==0){
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO produtos(nome,descricao,preco,id_categoria,ativo,criado_por,imagem) VALUES(?,?,?,?,TRUE,?,?) RETURNING id")){
                        p.setString(1,item.nome());p.setString(2,item.descricao());p.setBigDecimal(3,item.preco());p.setInt(4,categoria);p.setInt(5,admin.getId());p.setBytes(6,item.imagem());try(ResultSet r=p.executeQuery()){r.next();id=r.getInt(1);}
                    }
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO estoque(id_produto,quantidade,estoque_minimo) VALUES(?,?,?)")){p.setInt(1,id);p.setInt(2,item.quantidade());p.setInt(3,item.minimo());p.executeUpdate();}
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO movimentos_estoque(id_produto,tipo,quantidade,saldo,motivo,id_usuario) VALUES(?,'inicial',?,?,'Instalação do catálogo de apresentação',?)")){p.setInt(1,id);p.setInt(2,item.quantidade());p.setInt(3,item.quantidade());p.setInt(4,admin.getId());p.executeUpdate();}
                    novos++;
                }
                ids.add(id);resultado.add(item.codigo()+" | "+id+" | "+item.nome()+" | "+PagamentoSimulado.moeda(item.preco()));
            }
            // Soft delete mantém pedidos, favoritos, saldos e imagens anteriores recuperáveis.
            int desativados;
            try(PreparedStatement p=c.prepareStatement("UPDATE produtos SET ativo=FALSE WHERE ativo AND NOT(id=ANY(?))")){p.setArray(1,c.createArrayOf("integer",ids.toArray()));desativados=p.executeUpdate();}
            resultado.add("CONCLUÍDO: "+novos+" produtos novos; "+desativados+" anteriores desativados; saldos de produtos já importados preservados.");return resultado;
        });
        Sessao.encerrar();for(String linha:registros)System.out.println(linha);
    }
}
