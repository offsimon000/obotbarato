package util;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;

/** Imagem guardada no PostgreSQL: acompanha backup e não depende do caminho original. */
public final class Imagens {
    private Imagens(){}
    public static byte[] ler(Path arquivo)throws IOException{
        if(Files.size(arquivo)>5*1024*1024)throw new IllegalArgumentException("Imagem: limite de 5 MB.");
        byte[] dados=Files.readAllBytes(arquivo);validar(dados);return dados;
    }
    public static void validar(byte[] dados)throws IOException{
        if(dados==null)return;
        if(dados.length>5*1024*1024)throw new IllegalArgumentException("Imagem: limite de 5 MB.");
        try(ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(dados))){
            Iterator<ImageReader> leitores=ImageIO.getImageReaders(in);
            if(!leitores.hasNext())throw new IllegalArgumentException("Selecione uma imagem PNG ou JPEG.");
            ImageReader r=leitores.next();
            try{
                r.setInput(in);String formato=r.getFormatName();
                if(!formato.equalsIgnoreCase("PNG") && !formato.equalsIgnoreCase("JPEG"))throw new IllegalArgumentException("Use PNG ou JPEG.");
                if(r.getWidth(0)>4096 || r.getHeight(0)>4096)throw new IllegalArgumentException("Imagem: dimensão máxima de 4096 pixels.");
                r.read(0);
            }finally{r.dispose();}
        }
    }
    public static JPanel painel(byte[] dados,String legenda,int altura){
        BufferedImage img=null;
        try{if(dados!=null)img=ImageIO.read(new ByteArrayInputStream(dados));}catch(IOException ignored){}
        final BufferedImage imagem=img;
        JPanel p=new JPanel(){@Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(imagem!=null){
                double fator=Math.min((double)getWidth()/imagem.getWidth(),(double)getHeight()/imagem.getHeight());
                int w=(int)(imagem.getWidth()*fator),h=(int)(imagem.getHeight()*fator);
                Graphics2D g2=(Graphics2D)g.create();
                try{g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g2.drawImage(imagem,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);}finally{g2.dispose();}
            }else{
                g.setColor(Tema.ROXO_PRIMARIO);g.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g.setColor(Color.WHITE);g.setFont(Tema.FONTE_LABEL);
                String s=legenda==null?"Sem imagem":legenda;FontMetrics m=g.getFontMetrics();
                g.drawString(s,Math.max(4,(getWidth()-m.stringWidth(s))/2),getHeight()/2+4);
            }
        }};p.setPreferredSize(new Dimension(180,altura));p.setBackground(Tema.BG_CARD);return p;
    }
}
