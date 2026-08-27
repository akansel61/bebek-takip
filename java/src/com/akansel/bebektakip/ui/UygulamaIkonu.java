package com.akansel.bebektakip.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Uygulama simgesini çalışma anında çizer; ayrı resim dosyası gerekmez. */
public final class UygulamaIkonu {

    private UygulamaIkonu() {
    }

    /** Verilen kenar uzunluğunda simge üretir. */
    public static BufferedImage olustur(int boyut) {
        BufferedImage img = new BufferedImage(boyut, boyut, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            Tema.kaliteAyarla(g);
            double yaricap = boyut * 0.22;
            g.setColor(Tema.VURGU);
            g.fill(new RoundRectangle2D.Double(0, 0, boyut, boyut, yaricap, yaricap));

            double ic = boyut * 0.70;
            double kenar = (boyut - ic) / 2.0;
            Ikonlar.bebek(g, kenar, kenar, ic, Color.WHITE);
            Ikonlar.bebekYuzu(g, kenar, kenar, ic, Tema.VURGU);
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Pencere ve görev çubuğu için farklı boyutlarda simge listesi. */
    public static List<BufferedImage> tumBoyutlar() {
        List<BufferedImage> liste = new ArrayList<>();
        for (int boyut : new int[]{16, 20, 24, 32, 48, 64, 128, 256}) {
            liste.add(olustur(boyut));
        }
        return liste;
    }
}
