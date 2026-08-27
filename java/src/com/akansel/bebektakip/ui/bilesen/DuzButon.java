package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.IkonCizici;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * İşletim sisteminin buton görünümünü kullanmayan, kendini çizen buton.
 * Üç biçimi var: dolu (koyu), çerçeveli ve yalın (bağlantı görünümlü).
 */
public class DuzButon extends JButton {

    private static final long serialVersionUID = 1L;

    /** Buton biçimi. */
    public enum Bicim {
        DOLU, CERCEVELI, YALIN
    }

    private Bicim bicim = Bicim.DOLU;
    private IkonCizici ikon;
    private boolean ikonSagda;
    private int ikonBoyutu = 15;
    private int yaricap = 8;
    private int yatayBosluk = 16;
    private int dikeyBosluk = 9;
    private int ikonAraligi = 7;
    private Color ozelRenk;

    private boolean uzerinde;
    private boolean basili;

    public DuzButon(String metin) {
        this(metin, null, Bicim.DOLU);
    }

    public DuzButon(String metin, IkonCizici ikon, Bicim bicim) {
        super(metin);
        this.ikon = ikon;
        this.bicim = bicim;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Tema.font(13));
        getModel().addChangeListener(e -> {
            boolean u = getModel().isRollover();
            boolean b = getModel().isPressed();
            if (u != uzerinde || b != basili) {
                uzerinde = u;
                basili = b;
                repaint();
            }
        });
    }

    public DuzButon bicim(Bicim bicim) {
        this.bicim = bicim;
        return this;
    }

    public DuzButon ikon(IkonCizici ikon) {
        this.ikon = ikon;
        return this;
    }

    public DuzButon ikonSagda(boolean sagda) {
        this.ikonSagda = sagda;
        return this;
    }

    public DuzButon ikonBoyutu(int boyut) {
        this.ikonBoyutu = boyut;
        return this;
    }

    public DuzButon bosluk(int yatay, int dikey) {
        this.yatayBosluk = yatay;
        this.dikeyBosluk = dikey;
        return this;
    }

    public DuzButon yaricap(int yaricap) {
        this.yaricap = yaricap;
        return this;
    }

    public DuzButon renk(Color renk) {
        this.ozelRenk = renk;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        String metin = getText() == null ? "" : getText();
        int genislik = fm.stringWidth(metin);
        if (ikon != null) {
            genislik += ikonBoyutu + (metin.isEmpty() ? 0 : ikonAraligi);
        }
        int yukseklik = Math.max(fm.getHeight(), ikon != null ? ikonBoyutu : 0);
        return new Dimension(genislik + yatayBosluk * 2, yukseklik + dikeyBosluk * 2);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private Color anaRenk() {
        if (ozelRenk != null) {
            return ozelRenk;
        }
        return bicim == Bicim.DOLU ? Tema.VURGU : Tema.METIN_SOLUK;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int w = getWidth();
            int h = getHeight();
            Color ana = anaRenk();
            Color yazi;

            RoundRectangle2D sekil =
                    new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, yaricap, yaricap);

            if (!isEnabled()) {
                if (bicim == Bicim.DOLU) {
                    g2.setColor(Tema.karistir(ana, Color.WHITE, 0.6));
                    g2.fill(sekil);
                    yazi = Color.WHITE;
                } else {
                    yazi = Tema.PASIF_KUTU;
                }
            } else if (bicim == Bicim.DOLU) {
                Color zemin = ana;
                if (basili) {
                    zemin = Tema.karistir(ana, Color.BLACK, 0.25);
                } else if (uzerinde) {
                    zemin = Tema.karistir(ana, Color.WHITE, 0.15);
                }
                g2.setColor(zemin);
                g2.fill(sekil);
                yazi = Color.WHITE;
            } else if (bicim == Bicim.CERCEVELI) {
                if (basili) {
                    g2.setColor(Tema.SECILI);
                    g2.fill(sekil);
                } else if (uzerinde) {
                    g2.setColor(Tema.ARKA);
                    g2.fill(sekil);
                }
                g2.setColor(Tema.CIZGI);
                g2.draw(sekil);
                yazi = Tema.METIN;
            } else {
                if (uzerinde || basili) {
                    g2.setColor(basili ? Tema.SECILI : Tema.ARKA);
                    g2.fill(sekil);
                }
                yazi = uzerinde ? Tema.METIN : ana;
            }

            String metin = getText() == null ? "" : getText();
            FontMetrics fm = g2.getFontMetrics(getFont());
            int metinGenislik = fm.stringWidth(metin);
            int toplam = metinGenislik;
            if (ikon != null) {
                toplam += ikonBoyutu + (metin.isEmpty() ? 0 : ikonAraligi);
            }
            int x = (w - toplam) / 2;
            int metinY = (h - fm.getHeight()) / 2 + fm.getAscent();
            double ikonY = (h - ikonBoyutu) / 2.0;

            if (ikon != null && !ikonSagda) {
                ikon.ciz(g2, x, ikonY, ikonBoyutu, yazi);
                x += ikonBoyutu + (metin.isEmpty() ? 0 : ikonAraligi);
            }
            if (!metin.isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(yazi);
                g2.drawString(metin, x, metinY);
                x += metinGenislik;
            }
            if (ikon != null && ikonSagda) {
                ikon.ciz(g2, x + (metin.isEmpty() ? 0 : ikonAraligi), ikonY, ikonBoyutu, yazi);
            }
        } finally {
            g2.dispose();
        }
    }
}
