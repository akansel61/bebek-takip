package com.akansel.bebektakip.ui.bilesen;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * CSS'teki "repeat(auto-fit, minmax(160px, 1fr))" davranışının karşılığı:
 * kartları mevcut genişliğe göre eşit böler, sığmayanları alt satıra taşır.
 */
public class KartIzgara extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int enAzGenislik;
    private final int bosluk;

    public KartIzgara(int enAzGenislik, int bosluk) {
        super(null);
        this.enAzGenislik = enAzGenislik;
        this.bosluk = bosluk;
        setOpaque(false);
    }

    private int sutunSayisi(int genislik) {
        int kullanilabilir = genislik - getInsets().left - getInsets().right;
        int n = (kullanilabilir + bosluk) / (enAzGenislik + bosluk);
        return Math.max(1, Math.min(n, getComponentCount()));
    }

    private int satirYuksekligi() {
        int en = 0;
        for (Component c : getComponents()) {
            en = Math.max(en, c.getPreferredSize().height);
        }
        return en;
    }

    @Override
    public void doLayout() {
        int adet = getComponentCount();
        if (adet == 0) {
            return;
        }
        Insets i = getInsets();
        int sutun = sutunSayisi(getWidth());
        int kullanilabilir = getWidth() - i.left - i.right;
        int toplamBosluk = bosluk * (sutun - 1);
        int hucreGenislik = Math.max(1, (kullanilabilir - toplamBosluk) / sutun);
        int artik = Math.max(0, kullanilabilir - toplamBosluk - hucreGenislik * sutun);
        int yukseklik = satirYuksekligi();

        int x = i.left;
        int y = i.top;
        for (int k = 0; k < adet; k++) {
            int s = k % sutun;
            if (k > 0 && s == 0) {
                x = i.left;
                y += yukseklik + bosluk;
            }
            int g = hucreGenislik + (s < artik ? 1 : 0);
            getComponent(k).setBounds(x, y, g, yukseklik);
            x += g + bosluk;
        }
    }

    @Override
    public void setBounds(int x, int y, int g, int y2) {
        boolean genislikDegisti = g != getWidth();
        super.setBounds(x, y, g, y2);
        if (genislikDegisti && getPreferredSize().height != y2) {
            javax.swing.SwingUtilities.invokeLater(this::revalidate);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int adet = getComponentCount();
        Insets i = getInsets();
        if (adet == 0) {
            return new Dimension(i.left + i.right, i.top + i.bottom);
        }
        int genislik = getWidth();
        if (genislik <= 0 && getParent() != null) {
            genislik = getParent().getWidth();
        }
        if (genislik <= 0) {
            genislik = enAzGenislik * adet + bosluk * (adet - 1);
        }
        int sutun = sutunSayisi(genislik);
        int satir = (adet + sutun - 1) / sutun;
        int yukseklik = satirYuksekligi() * satir + bosluk * (satir - 1);
        return new Dimension(i.left + i.right + enAzGenislik,
                i.top + i.bottom + yukseklik);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension d = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, d.height);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = getPreferredSize();
        return new Dimension(enAzGenislik, d.height);
    }
}
