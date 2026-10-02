package util;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256; aceita MD5 e SHA256+salt antigos só para migração. */
public final class SenhaCripto {
    private static final int ITERACOES=600000;
    private SenhaCripto() {}
    public static String hashear(String senha) { Validacao.senha(senha); return migrar(senha); }
    public static String migrar(String senha) {
        if(senha==null || senha.isEmpty() || senha.length()>128) throw new IllegalArgumentException("Senha inválida.");
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        return "pbkdf2$"+ITERACOES+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derivar(senha,salt,ITERACOES));
    }
    private static byte[] derivar(String senha,byte[] salt,int iteracoes) {
        PBEKeySpec spec=new PBEKeySpec(senha.toCharArray(),salt,iteracoes,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException e){ throw new IllegalStateException("PBKDF2 indisponível.",e); }
        finally { spec.clearPassword(); }
    }
    public static boolean verificar(String senha,String hash) {
        if(senha==null || hash==null || senha.length()>128) return false;
        try {
            if(hash.startsWith("pbkdf2$")) {
                String[] p=hash.split("\\$"); if(p.length!=4) return false;
                int n=Integer.parseInt(p[1]); if(n<10000 || n>1000000) return false;
                byte[] salt=Base64.getDecoder().decode(p[2]), esperado=Base64.getDecoder().decode(p[3]);
                return salt.length==16 && esperado.length==32 && MessageDigest.isEqual(esperado,derivar(senha,salt,n));
            }
            if(hash.contains(":")) {
                String[] p=hash.split(":",2); MessageDigest md=MessageDigest.getInstance("SHA-256");
                md.update(Base64.getDecoder().decode(p[0]));
                return MessageDigest.isEqual(Base64.getDecoder().decode(p[1]),md.digest(senha.getBytes(StandardCharsets.UTF_8)));
            }
            return hash.matches("(?i)[0-9a-f]{32}") && MessageDigest.isEqual(HexFormat.of().parseHex(hash),MessageDigest.getInstance("MD5").digest(senha.getBytes(StandardCharsets.UTF_8)));
        } catch(GeneralSecurityException | IllegalArgumentException e){ return false; }
    }
    public static boolean ehFormatoLegado(String hash){ return hash!=null && !hash.startsWith("pbkdf2$"+ITERACOES+"$"); }
}
