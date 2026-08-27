package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.IkonCizici;
import com.akansel.bebektakip.ui.Ikonlar;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Kayıt yokken gösterilen bilgilendirme alanı. */
public class BosDurum extends JPanel {

    private static final long serialVersionUID = 1L;

    private final IkonCizici ikon;
    private String mesaj;

    public BosDurum(String mesaj) {
        this(mesaj, Ikonlar::bosDurum);
    }

    public BosDurum(String mesaj, IkonCizici ikon) {
        this.mesaj = mesaj;
        this.ikon = ikon;
        setOpaque(false);
        setPreferredSize(new Dimension(200, 180));
    }

    public void setMesaj(String mesaj) {
        this.mesaj = mesaj;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int orta = getHeight() / 2;
            ikon.ciz(g2, (getWidth() - 44) / 2.0, orta - 44, 44, Tema.PASIF_KUTU);
            g2.setFont(Tema.font(13));
            g2.setColor(Tema.METIN_SOLUK);
            FontMetrics fm = g2.getFontMetrics();
            String[] satirlar = mesaj.split("\n");
            int y = orta + 16;
            for (String s : satirlar) {
                g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, y + fm.getAscent());
                y += fm.getHeight() + 2;
            }
        } finally {
            g2.dispose();
        }
    }
}
