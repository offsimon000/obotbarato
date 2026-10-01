package util;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.Toolkit;

/**
 * Formata campos automaticamente enquanto o usuário digita.
 *
 * Máscaras disponíveis:
 *   CPF:      000.000.000-00   (11 dígitos)
 *   TELEFONE: (00) 00000-0000  (11 dígitos — celular)
 *             (00) 0000-0000   (10 dígitos — fixo)
 *   CEP:      00000-000        (8 dígitos)
 *
 * COMO USAR:
 *   JTextField txtCpf = new JTextField();
 *   MascaraCampo.aplicarCpf(txtCpf);
 *
 *   JTextField txtTel = new JTextField();
 *   MascaraCampo.aplicarTelefone(txtTel);
 *
 * O campo só aceita números — letras e símbolos são bloqueados.
 * Os pontos, traço e parênteses são inseridos automaticamente.
 *
 * Para recuperar só os dígitos (sem máscara), use:
 *   String cpfLimpo = MascaraCampo.somenteNumeros(txtCpf.getText());
 */
public class MascaraCampo {

    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS DE APLICAÇÃO
    // ═══════════════════════════════════════════════════════════════

    /** Aplica máscara CPF: 000.000.000-00 */
    public static void aplicarCpf(JTextField campo) {
        campo.setDocument(new FiltroMascara("###.###.###-##"));
        campo.setToolTipText("CPF: 000.000.000-00");
    }

    /** Aplica máscara de telefone: (00) 00000-0000 */
    public static void aplicarTelefone(JTextField campo) {
        campo.setDocument(new FiltroMascara("(##) #####-####"));
        campo.setToolTipText("Telefone: (00) 00000-0000");
    }

    /** Aplica máscara CEP: 00000-000 */
    public static void aplicarCep(JTextField campo) {
        campo.setDocument(new FiltroMascara("#####-###"));
        campo.setToolTipText("CEP: 00000-000");
    }

    /** Remove tudo que não for dígito — útil para salvar no banco */
    public static String somenteNumeros(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^0-9]", "");
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTRO INTERNO
    // ═══════════════════════════════════════════════════════════════

    /**
     * DocumentFilter que aplica a máscara enquanto o usuário digita.
     *   '#' na máscara = posição de dígito
     *   qualquer outro caractere na máscara = inserido automaticamente
     */
    private static class FiltroMascara extends PlainDocument {

        private final String mascara;

        FiltroMascara(String mascara) {
            this.mascara = mascara;
        }

        @Override
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {

            if (str == null) return;

            // Só aceita dígitos
            String apenasDigitos = str.replaceAll("[^0-9]", "");
            if (apenasDigitos.isEmpty()) {
                Toolkit.getDefaultToolkit().beep(); // feedback sonoro se bloqueado
                return;
            }

            // Monta o texto atual + novo dígito, aplica a máscara
            String atualSemMascara = somenteNumeros(getText(0, getLength()));
            String novosDigitos    = atualSemMascara + apenasDigitos;

            // Conta quantos '#' existem na máscara (= limite de dígitos)
            int maxDigitos = mascara.replace("[^#]", "")
                                    .chars()
                                    .filter(c -> c == '#')
                                    .sum() / 1; // truque para contar '#'
            maxDigitos = (int) mascara.chars().filter(c -> c == '#').count();

            if (novosDigitos.length() > maxDigitos) return; // campo cheio

            // Aplica a máscara sobre os dígitos
            String formatado = aplicarMascara(novosDigitos);

            // Substitui todo o conteúdo pelo texto formatado
            super.remove(0, getLength());
            super.insertString(0, formatado, a);
        }

        @Override
        public void remove(int offs, int len) throws BadLocationException {
            // Pega os dígitos atuais e remove o último
            String atual = somenteNumeros(getText(0, getLength()));
            if (atual.isEmpty()) return;

            String semUltimo = atual.substring(0, Math.max(0, atual.length() - 1));
            String formatado = semUltimo.isEmpty() ? "" : aplicarMascara(semUltimo);

            super.remove(0, getLength());
            if (!formatado.isEmpty()) super.insertString(0, formatado, null);
        }

        /** Aplica os caracteres da máscara sobre os dígitos puros */
        private String aplicarMascara(String digitos) {
            StringBuilder sb      = new StringBuilder();
            int           idxDig  = 0;

            for (int i = 0; i < mascara.length() && idxDig < digitos.length(); i++) {
                char m = mascara.charAt(i);
                if (m == '#') {
                    sb.append(digitos.charAt(idxDig++));
                } else {
                    sb.append(m); // insere separador automaticamente
                }
            }
            return sb.toString();
        }
    }
}