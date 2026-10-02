package dao;
import util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.sql.*;

/** Atendimento local: admin entrega código de uso único após identificar o cliente. */
public final class CodigoAcessoDAO {
    public String emitir(int id) throws SQLException {
        byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);
        String codigo=HexFormat.of().formatHex(bytes);
        Transacao.executar(c -> {
            Acesso.equipe(c,true);
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM clientes WHERE id=? AND ativo FOR UPDATE")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Cliente inativo ou inexistente.");}
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO acessos_cliente(id_cliente,codigo_hash,expira_em,emitido_por) VALUES(?,?,CURRENT_TIMESTAMP+INTERVAL '30 minutes',?) ON CONFLICT(id_cliente) DO UPDATE SET codigo_hash=EXCLUDED.codigo_hash,expira_em=EXCLUDED.expira_em,tentativas=0,emitido_por=EXCLUDED.emitido_por")){
                p.setInt(1,id);p.setString(2,hash(codigo));p.setInt(3,Sessao.getIdUsuario());p.executeUpdate();
            }return null;
        });return codigo;
    }
    public boolean redefinir(String email,String codigo,String senha) throws SQLException {
        String e=Validacao.email(email);Validacao.senha(senha);
        if(codigo==null || !codigo.trim().matches("(?i)[0-9a-f]{32}"))return false;
        String h=hash(codigo.trim().toLowerCase(java.util.Locale.ROOT));
        String nova=SenhaCripto.hashear(senha);
        return Transacao.executar(c -> {
            int id;String esperado;boolean valido;
            try(PreparedStatement p=c.prepareStatement("SELECT c.id,a.codigo_hash,(a.expira_em>CURRENT_TIMESTAMP AND a.tentativas<5) AS valido FROM clientes c JOIN acessos_cliente a ON a.id_cliente=c.id WHERE lower(trim(c.email))=? AND c.ativo FOR UPDATE OF c,a")){
                p.setString(1,e);try(ResultSet r=p.executeQuery()){
                    if(!r.next())return false;id=r.getInt(1);esperado=r.getString(2);valido=r.getBoolean(3);
                }
            }
            if(!valido)return false;
            if(!MessageDigest.isEqual(h.getBytes(StandardCharsets.US_ASCII),esperado.getBytes(StandardCharsets.US_ASCII))){
                try(PreparedStatement p=c.prepareStatement("UPDATE acessos_cliente SET tentativas=tentativas+1 WHERE id_cliente=?")){p.setInt(1,id);p.executeUpdate();}
                return false; // Transação confirma o contador mesmo com código incorreto.
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE clientes SET senha=? WHERE id=?")){p.setString(1,nova);p.setInt(2,id);if(p.executeUpdate()!=1)throw new SQLException("Senha não alterada.");}
            try(PreparedStatement p=c.prepareStatement("DELETE FROM acessos_cliente WHERE id_cliente=?")){p.setInt(1,id);p.executeUpdate();}
            return true;
        });
    }
    private static String hash(String s){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
