package dao;
import util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.sql.*;
import java.util.*;

public final class CupomDAO {
    public record Cupom(int id,String codigo,BigDecimal percentual,LocalDate inicio,LocalDate fim,boolean ativo) {}
    public static String codigo(String valor){
        String s=valor==null?"":valor.trim().toUpperCase(Locale.ROOT);
        if(!s.matches("[A-Z0-9_-]{3,32}"))throw new IllegalArgumentException("Cupom: use de 3 a 32 letras, números, hífen ou sublinhado.");return s;
    }
    public List<Cupom> listar()throws SQLException{
        List<Cupom> lista=new ArrayList<>();try(Connection c=ConexaoBanco.getConexao()){Acesso.equipe(c,true);
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM cupons ORDER BY codigo")){while(r.next())lista.add(mapear(r));}
        }return lista;
    }
    public void salvar(int id,String codigo,BigDecimal percentual,LocalDate inicio,LocalDate fim,boolean ativo)throws SQLException{
        String cod=codigo(codigo);BigDecimal p;
        try{p=percentual.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException | NullPointerException e){throw new IllegalArgumentException("Percentual: use no máximo duas casas decimais.");}
        if(p.signum()<=0 || p.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Percentual deve ser maior que zero e até 100%.");
        if(inicio==null || fim==null || fim.isBefore(inicio))throw new IllegalArgumentException("Confira o início e fim da validade.");
        Transacao.executar(c->{Acesso.equipe(c,true);
            try(PreparedStatement s=c.prepareStatement(id==0?"INSERT INTO cupons(codigo,percentual,inicio,fim,ativo,criado_por,atualizado_por) VALUES(?,?,?,?,?,?,?)":"UPDATE cupons SET codigo=?,percentual=?,inicio=?,fim=?,ativo=?,atualizado_por=?,atualizado_em=CURRENT_TIMESTAMP WHERE id=?")){
                s.setString(1,cod);s.setBigDecimal(2,p);s.setDate(3,java.sql.Date.valueOf(inicio));s.setDate(4,java.sql.Date.valueOf(fim));s.setBoolean(5,ativo);s.setInt(6,Sessao.getIdUsuario());s.setInt(7,id==0?Sessao.getIdUsuario():id);
                if(s.executeUpdate()!=1)throw new IllegalArgumentException("Cupom não encontrado.");
            }return null;
        });
    }
    public Cupom consultar(String codigo)throws SQLException{
        try(Connection c=ConexaoBanco.getConexao()){Acesso.cliente(c,Sessao.getIdCliente());return validar(c,codigo,false);}
    }
    static Cupom validar(Connection c,String valor,boolean bloquear)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT * FROM cupons WHERE codigo=? AND ativo AND CURRENT_DATE BETWEEN inicio AND fim"+(bloquear?" FOR SHARE":""))){
            p.setString(1,codigo(valor));try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Cupom inválido, fora da validade ou desativado.");return mapear(r);}
        }
    }
    private static Cupom mapear(ResultSet r)throws SQLException{return new Cupom(r.getInt("id"),r.getString("codigo"),r.getBigDecimal("percentual"),r.getDate("inicio").toLocalDate(),r.getDate("fim").toLocalDate(),r.getBoolean("ativo"));}
}
