package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Tema;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** Özet ekranındaki tek ölçüm kartı: başlık, büyük değer ve birim. */
public class IstatistikKarti extends Kart {

    private static final long serialVersionUID = 1L;

    private final String baslik;
    private final String birim;
    private final Color vurgu;
    private String deger = "0";

    public IstatistikKarti(String baslik, String birim, Color vurgu) {
        this.baslik = baslik;
        this.birim = birim == null ? "" : birim;
        this.vurgu = vurgu;
        setPreferredSize(new Dimension(170, birim.isEmpty() ? 86 : 100));
    }

    public void setDeger(String deger) {
        if (!this.deger.equals(deger)) {
            this.deger = deger;
            repaint();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(140, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int x = 16;
            int y = 16;

            if (vurgu != null) {
                g2.setColor(vurgu);
                g2.fill(new Ellipse2D.Double(x, y + 1, 7, 7));
                x += 12;
            }

            g2.setFont(Tema.font(12));
            g2.setColor(Tema.METIN_SOLUK);
            FontMetrics fmBaslik = g2.getFontMetrics();
            g2.drawString(Tema.kisalt(g2, baslik, getWidth() - x - 12),
                    x, y + fmBaslik.getAscent() - 1);

            g2.setFont(Tema.fontSayi(30));
            g2.setColor(Tema.METIN);
            FontMetrics fmDeger = g2.getFontMetrics();
            int degerY = y + fmBaslik.getHeight() + 8 + fmDeger.getAscent();
            g2.drawString(Tema.kisalt(g2, deger, getWidth() - 32), 16, degerY);

            if (!birim.isEmpty()) {
                g2.setFont(Tema.font(11));
                g2.setColor(Tema.METIN_SOLUK);
                g2.drawString(birim, 16, degerY + g2.getFontMetrics().getAscent() + 4);
            }
        } finally {
            g2.dispose();
        }
    }
}
