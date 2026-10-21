package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.rapor.RaporYazisi;
import com.akansel.bebektakip.rapor.Sayfa;
import com.akansel.bebektakip.rapor.SayfaCizici;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Rapor sayfalarının kâğıt görünümünde, alt alta önizlemesi. Sayfalar
 * genişliğe sığacak kadar küçültülür; çizim, PDF'e ve yazıcıya giden
 * sayfa listesinin aynısından yapılır.
 */
public class SayfaOnizleme extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    private static final int BOSLUK = 24;
    private static final double EN_BUYUK_OLCEK = 1.3;

    private final List<Sayfa> sayfalar = new ArrayList<>();
    private RaporYazisi yazi;
    private String bosMesaj = "";

    public SayfaOnizleme() {
        setOpaque(true);
        setBackground(Tema.ARKA);
    }

    public void setSayfalar(List<Sayfa> yeni, RaporYazisi yazi) {
        sayfalar.clear();
        if (yeni != null) {
            sayfalar.addAll(yeni);
        }
        this.yazi = yazi;
        revalidate();
        repaint();
    }

    public void setBosMesaj(String bosMesaj) {
        this.bosMesaj = bosMesaj == null ? "" : bosMesaj;
        repaint();
    }

    private double olcek(int genislik) {
        double o = (genislik - BOSLUK * 2.0) / Sayfa.GENISLIK;
        return Math.max(0.3, Math.min(EN_BUYUK_OLCEK, o));
    }

    private int genislik() {
        if (getWidth() > 0) {
            return getWidth();
        }
        return getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 700;
    }

    @Override
    public Dimension getPreferredSize() {
        double o = olcek(genislik());
        int sayfaBoyu = (int) Math.ceil(Sayfa.YUKSEKLIK * o);
        int adet = Math.max(1, sayfalar.size());
        return new Dimension(400, BOSLUK + adet * (sayfaBoyu + BOSLUK));
    }

    @Override
    public void setBounds(int x, int y, int g, int y2) {
        boolean genislikDegisti = g != getWidth();
        super.setBounds(x, y, g, y2);
        if (genislikDegisti) {
            SwingUtilities.invokeLater(this::revalidate);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            if (sayfalar.isEmpty() || yazi == null) {
                g2.setFont(Tema.font(13));
                g2.setColor(Tema.METIN_SOLUK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(bosMesaj, (getWidth() - fm.stringWidth(bosMesaj)) / 2, 80);
                return;
            }
            double o = olcek(getWidth());
            double sw = Sayfa.GENISLIK * o;
            double sh = Sayfa.YUKSEKLIK * o;
            double x = (getWidth() - sw) / 2;
            Rectangle gorunen = g2.getClipBounds();
            for (int i = 0; i < sayfalar.size(); i++) {
                double y = BOSLUK + i * (sh + BOSLUK);
                if (gorunen != null && (y > gorunen.getMaxY() || y + sh < gorunen.getY())) {
                    continue;
                }
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fill(new RoundRectangle2D.Double(x - 1, y + 1, sw + 2, sh + 3, 6, 6));
                g2.setColor(Tema.CIZGI);
                g2.draw(new Rectangle2D.Double(x - 0.5, y - 0.5, sw + 1, sh + 1));

                Graphics2D sayfaG = (Graphics2D) g2.create();
                try {
                    sayfaG.translate(x, y);
                    sayfaG.scale(o, o);
                    sayfaG.clip(new Rectangle2D.Double(0, 0, Sayfa.GENISLIK, Sayfa.YUKSEKLIK));
                    SayfaCizici.ciz(sayfaG, sayfalar.get(i), yazi);
                } finally {
                    sayfaG.dispose();
                }
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle gorunur, int yon, int taraf) {
        return 32;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle gorunur, int yon, int taraf) {
        return Math.max(64, gorunur.height - 48);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
