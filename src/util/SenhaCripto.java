package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitário de criptografia de senhas.
 *
 * PROBLEMA RESOLVIDO: o projeto usava MD5 puro — algoritmo quebrado desde 2004,
 * sem salt, vulnerável a rainbow tables e brute force.
 *
 * SOLUÇÃO: SHA-256 com salt aleatório por usuário.
 * Formato armazenado no banco: "SALT:HASH" (ambos em Base64)
 *
 * Para projetos em produção real, use BCrypt (biblioteca externa).
 * SHA-256 + salt já é infinitamente mais seguro que MD5 puro.
 */
public class SenhaCripto {

    private static final String ALGORITMO  = "SHA-256";
    private static final int    SALT_BYTES  = 16;

    /**
     * Gera hash seguro de uma senha nova.
     * Retorna string no formato "SALT:HASH" para guardar no banco.
     */
    public static String hashear(String senhaPlana) {
        if (senhaPlana == null || senhaPlana.isEmpty())
            throw new IllegalArgumentException("Senha nao pode ser vazia.");
        try {
            byte[] salt = gerarSalt();
            byte[] hash = calcularHash(senhaPlana, salt);
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            return saltB64 + ":" + hashB64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo " + ALGORITMO + " nao disponivel.", e);
        }
    }

    /**
     * Verifica se uma senha digitada corresponde ao hash armazenado.
     * Aceita tanto o formato novo "SALT:HASH" quanto o legado MD5
     * (para não quebrar usuários existentes na migração).
     */
    public static boolean verificar(String senhaPlana, String hashArmazenado) {
        if (senhaPlana == null || hashArmazenado == null) return false;
        try {
            // Formato novo: contém ':'
            if (hashArmazenado.contains(":")) {
                String[] partes = hashArmazenado.split(":", 2);
                if (partes.length != 2) return false;
                byte[] salt = Base64.getDecoder().decode(partes[0]);
                byte[] hashEsperado = Base64.getDecoder().decode(partes[1]);
                byte[] hashDigitado = calcularHash(senhaPlana, salt);
                return comparacaoSegura(hashEsperado, hashDigitado);
            }
            // Formato legado: MD5 hexadecimal (compatibilidade retroativa)
            return hashArmazenado.equalsIgnoreCase(md5Legado(senhaPlana));
        } catch (Exception e) {
            return false; // hash corrompido — negar acesso
        }
    }

    /**
     * Verifica se um hash ainda está no formato legado (MD5).
     * Útil para migrar usuários na próxima vez que fizerem login.
     */
    public static boolean ehFormatoLegado(String hash) {
        return hash != null && !hash.contains(":") && hash.length() == 32;
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVADOS
    // ═══════════════════════════════════════════════════════════════

    private static byte[] gerarSalt() {
        SecureRandom rng = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        rng.nextBytes(salt);
        return salt;
    }

    private static byte[] calcularHash(String senha, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITMO);
        md.update(salt);
        return md.digest(senha.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Comparação em tempo constante — evita timing attacks */
    private static boolean comparacaoSegura(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= (a[i] ^ b[i]);
        return diff == 0;
    }

    /** Compatibilidade com hashes MD5 antigos (hex lowercase) */
    private static String md5Legado(String senha) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(senha.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}