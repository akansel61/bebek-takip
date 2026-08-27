package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Tema;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** "Etiket ......... Değer" biçiminde basit ölçüm satırları. */
public class OlcumListesi extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SATIR_YUKSEKLIGI = 36;

    /** Listedeki tek satır. */
    public static class Olcum {
        final String etiket;
        final String deger;
        final boolean vurgulu;

        public Olcum(String etiket, String deger) {
            this(etiket, deger, false);
        }

        public Olcum(String etiket, String deger, boolean vurgulu) {
            this.etiket = etiket;
            this.deger = deger;
            this.vurgulu = vurgulu;
        }
    }

    private final List<Olcum> olcumler = new ArrayList<>();

    public OlcumListesi() {
        setOpaque(false);
    }

    public void setOlcumler(List<Olcum> yeni) {
        olcumler.clear();
        if (yeni != null) {
            olcumler.addAll(yeni);
        }
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(240, Math.max(1, olcumler.size()) * SATIR_YUKSEKLIGI);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int w = getWidth();
            for (int i = 0; i < olcumler.size(); i++) {
                Olcum o = olcumler.get(i);
                int ust = i * SATIR_YUKSEKLIGI;
                int orta = ust + SATIR_YUKSEKLIGI / 2;

                g2.setFont(Tema.font(13));
                g2.setColor(Tema.METIN_SOLUK);
                FontMetrics fmE = g2.getFontMetrics();
                g2.drawString(o.etiket, 0, orta + fmE.getAscent() / 2 - 1);

                g2.setFont(o.vurgulu ? Tema.fontKalin(15) : Tema.fontKalin(13));
                g2.setColor(Tema.METIN);
                FontMetrics fmD = g2.getFontMetrics();
                g2.drawString(o.deger, w - fmD.stringWidth(o.deger),
                        orta + fmD.getAscent() / 2 - 1);

                if (i < olcumler.size() - 1) {
                    g2.setColor(Tema.CIZGI_ACIK);
                    g2.drawLine(0, ust + SATIR_YUKSEKLIGI - 1, w, ust + SATIR_YUKSEKLIGI - 1);
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
