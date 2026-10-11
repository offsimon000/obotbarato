package util;
import java.awt.Window;
import java.awt.event.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;

/** Voltar e fechar compartilham o fluxo, sem reabrir janelas de uma sessão encerrada. */
public final class Navegacao {
    private Navegacao() {}
    public static JButton botao(Window atual,Window anterior,BooleanSupplier podeFechar){
        int usuario=Sessao.getIdUsuario(),cliente=Sessao.getIdCliente();
        Runnable voltar=()->{if(!podeFechar.getAsBoolean())return;atual.dispose();if(anterior!=null && usuario==Sessao.getIdUsuario() && cliente==Sessao.getIdCliente()){if(!anterior.isVisible())anterior.setVisible(true);anterior.toFront();}};
        JButton botao=Tema.botaoVoltar(voltar);botao.setName("nav-voltar");
        if(atual instanceof JFrame)((JFrame)atual).setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if(atual instanceof JDialog)((JDialog)atual).setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        atual.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){voltar.run();}});
        return botao;
    }
}
