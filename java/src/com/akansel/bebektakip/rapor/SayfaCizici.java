package com.akansel.bebektakip.rapor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;

/**
 * Sayfaları Graphics2D üzerine çizer: hem önizleme ekranı hem de yazıcı
 * bunu kullanır. Koordinatlar punto; çağıran taraf ölçeği ayarlar.
 */
public final class SayfaCizici {

    private SayfaCizici() {
    }

    /** Sayfayı (0,0) noktasından itibaren, 1 birim = 1 punto olarak çizer. */
    public static void ciz(Graphics2D g0, Sayfa sayfa, RaporYazisi yazi) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // Satır kırılımları kesirli ölçüyle hesaplandı; çizim de öyle olmalı.
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(0, 0, Sayfa.GENISLIK, Sayfa.YUKSEKLIK));

            for (Sayfa.Oge o : sayfa.getOgeler()) {
                if (o instanceof Sayfa.Kutu) {
                    Sayfa.Kutu k = (Sayfa.Kutu) o;
                    g.setColor(k.renk);
                    if (k.yaricap > 0) {
                        g.fill(new RoundRectangle2D.Double(k.x, k.y, k.genislik, k.yukseklik,
                                k.yaricap * 2, k.yaricap * 2));
                    } else {
                        g.fill(new Rectangle2D.Double(k.x, k.y, k.genislik, k.yukseklik));
                    }
                } else if (o instanceof Sayfa.Cizgi) {
                    Sayfa.Cizgi c = (Sayfa.Cizgi) o;
                    g.setColor(c.renk);
                    g.setStroke(new BasicStroke((float) c.kalinlik));
                    g.draw(new Line2D.Double(c.x1, c.y1, c.x2, c.y2));
                } else if (o instanceof Sayfa.Yazi) {
                    Sayfa.Yazi y = (Sayfa.Yazi) o;
                    g.setColor(y.renk);
                    g.setFont(yazi.font(y.kalin, y.boyut));
                    g.drawString(y.metin, (float) y.x, (float) y.y);
                }
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Windows'un yazdırma penceresini açar, onaylanırsa sayfaları yazıcıya
     * gönderir. Vazgeçilirse false döner.
     */
    public static boolean yazdir(List<Sayfa> sayfalar, RaporYazisi yazi, String isAdi)
            throws PrinterException {
        PrinterJob is = PrinterJob.getPrinterJob();
        is.setJobName(isAdi);
        PageFormat bicim = is.defaultPage();
        Paper a4 = new Paper();
        a4.setSize(Sayfa.GENISLIK, Sayfa.YUKSEKLIK);
        a4.setImageableArea(0, 0, Sayfa.GENISLIK, Sayfa.YUKSEKLIK);
        bicim.setPaper(a4);
        bicim.setOrientation(PageFormat.PORTRAIT);
        PageFormat gecerli = is.validatePage(bicim);
        is.setPrintable(new Yazdirilabilir(sayfalar, yazi), gecerli);
        if (!is.printDialog()) {
            return false;
        }
        is.print();
        return true;
    }

    /**
     * Yazıcının basabildiği alana sığacak şekilde ölçekler. Raporun kendi
     * kenar boşluğu olduğu için çoğu yazıcıda ölçek 1'e çok yakın kalır.
     */
    private static final class Yazdirilabilir implements Printable {
        private final List<Sayfa> sayfalar;
        private final RaporYazisi yazi;

        Yazdirilabilir(List<Sayfa> sayfalar, RaporYazisi yazi) {
            this.sayfalar = sayfalar;
            this.yazi = yazi;
        }

        @Override
        public int print(Graphics g, PageFormat bicim, int sira) {
            if (sira < 0 || sira >= sayfalar.size()) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) g;
            double olcek = Math.min(1.0, Math.min(bicim.getImageableWidth() / Sayfa.GENISLIK,
                    bicim.getImageableHeight() / Sayfa.YUKSEKLIK));
            double bosX = (bicim.getImageableWidth() - Sayfa.GENISLIK * olcek) / 2;
            double bosY = (bicim.getImageableHeight() - Sayfa.YUKSEKLIK * olcek) / 2;
            g2.translate(bicim.getImageableX() + bosX, bicim.getImageableY() + bosY);
            g2.scale(olcek, olcek);
            ciz(g2, sayfalar.get(sira), yazi);
            return PAGE_EXISTS;
        }
    }
}
