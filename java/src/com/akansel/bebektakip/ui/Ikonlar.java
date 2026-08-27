package com.akansel.bebektakip.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Emoji ya da resim dosyası kullanmadan, doğrudan Graphics2D ile çizilen ikonlar.
 * Böylece her Windows kurulumunda aynı görünür, ölçeklenirken bozulmazlar.
 *
 * Hepsi 24x24 birimlik bir ızgarada tasarlandı; çizim anında istenen
 * boyuta ölçekleniyor.
 */
public final class Ikonlar {

    private Ikonlar() {
    }

    private static final double IZGARA = 24.0;

    /** Çizim ortamını hazırlar; iş bitince eski durum geri yüklenir. */
    private interface Cizim {
        void ciz(Graphics2D g, float kalinlik);
    }

    private static void cerceve(Graphics2D g0, double x, double y, double boyut,
                                Color renk, Cizim cizim) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Tema.kaliteAyarla(g);
            g.translate(x, y);
            double olcek = boyut / IZGARA;
            g.transform(AffineTransform.getScaleInstance(olcek, olcek));
            g.setColor(renk);
            float kalinlik = (float) Math.max(1.2, IZGARA / 12.0);
            g.setStroke(new BasicStroke(kalinlik, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            cizim.ciz(g, kalinlik);
        } finally {
            g.dispose();
        }
    }

    /** Uygulama logosu: bebek yüzü. */
    public static void bebek(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // kulaklar
            g.fill(new Ellipse2D.Double(1.5, 9.5, 4.0, 5.0));
            g.fill(new Ellipse2D.Double(18.5, 9.5, 4.0, 5.0));
            // yüz
            g.fill(new Ellipse2D.Double(4.0, 4.5, 16.0, 16.0));
            // saç tutamı
            Path2D sac = new Path2D.Double();
            sac.moveTo(11.0, 4.8);
            sac.curveTo(11.2, 2.2, 13.6, 1.6, 14.6, 3.2);
            sac.curveTo(15.2, 4.2, 14.2, 5.0, 13.4, 4.4);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(sac);
        });
    }

    /** Yüzün üstüne zıt renkte çizilen ayrıntılar: göz ve ağız. */
    public static void bebekYuzu(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.fill(new Ellipse2D.Double(8.6, 10.4, 1.9, 2.4));
            g.fill(new Ellipse2D.Double(13.5, 10.4, 1.9, 2.4));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(9.0, 12.6, 6.0, 4.4, 200, 140, Arc2D.OPEN));
        });
    }

    /** Dashboard: sütun grafik. */
    public static void grafik(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.fill(new RoundRectangle2D.Double(3.5, 13.0, 4.0, 8.0, 1.6, 1.6));
            g.fill(new RoundRectangle2D.Double(10.0, 8.0, 4.0, 13.0, 1.6, 1.6));
            g.fill(new RoundRectangle2D.Double(16.5, 4.0, 4.0, 17.0, 1.6, 1.6));
        });
    }

    /** Kayıtlar: satırlı liste. */
    public static void liste(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            for (int i = 0; i < 3; i++) {
                double sy = 6.0 + i * 6.0;
                g.fill(new Ellipse2D.Double(3.0, sy - 1.3, 2.6, 2.6));
                g.fill(new RoundRectangle2D.Double(8.0, sy - 1.1, 13.0, 2.2, 1.1, 1.1));
            }
        });
    }

    /** İstatistikler: yükselen eğri. */
    public static void trend(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            p.moveTo(3.0, 16.5);
            p.lineTo(9.0, 10.5);
            p.lineTo(13.0, 14.5);
            p.lineTo(20.5, 6.5);
            g.draw(p);
            Path2D ok = new Path2D.Double();
            ok.moveTo(15.0, 6.5);
            ok.lineTo(20.8, 6.5);
            ok.lineTo(20.8, 12.0);
            g.draw(ok);
        });
    }

    /** Artı (yeni kayıt). */
    public static void arti(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(12, 5, 12, 19);
            g.drawLine(5, 12, 19, 12);
        });
    }

    /** Çöp kutusu (sil). */
    public static void cop(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(4, 6, 20, 6);
            Path2D govde = new Path2D.Double();
            govde.moveTo(6.2, 6.0);
            govde.lineTo(7.2, 20.0);
            govde.lineTo(16.8, 20.0);
            govde.lineTo(17.8, 6.0);
            g.draw(govde);
            Path2D kapak = new Path2D.Double();
            kapak.moveTo(9.5, 6.0);
            kapak.lineTo(9.5, 3.6);
            kapak.lineTo(14.5, 3.6);
            kapak.lineTo(14.5, 6.0);
            g.draw(kapak);
            g.drawLine(10, 9, 10, 17);
            g.drawLine(14, 9, 14, 17);
        });
    }

    /** Onay işareti. */
    public static void onay(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            p.moveTo(5.0, 12.5);
            p.lineTo(10.0, 17.2);
            p.lineTo(19.0, 7.0);
            g.draw(p);
        });
    }

    /** Boş değer çizgisi. */
    public static void tire(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(7, 12, 17, 12);
        });
    }

    /** Büyüteç (arama). */
    public static void ara(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Ellipse2D.Double(4.0, 4.0, 12.0, 12.0));
            g.drawLine(15, 15, 20, 20);
        });
    }

    /** Dışa aktar: kutuya inen ok. */
    public static void disaAktar(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(12, 3, 12, 14);
            Path2D ok = new Path2D.Double();
            ok.moveTo(7.5, 10.0);
            ok.lineTo(12.0, 14.6);
            ok.lineTo(16.5, 10.0);
            g.draw(ok);
            Path2D tepsi = new Path2D.Double();
            tepsi.moveTo(4.0, 16.0);
            tepsi.lineTo(4.0, 20.5);
            tepsi.lineTo(20.0, 20.5);
            tepsi.lineTo(20.0, 16.0);
            g.draw(tepsi);
        });
    }

    /** İçe aktar: kutudan çıkan ok. */
    public static void iceAktar(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(12, 14, 12, 3);
            Path2D ok = new Path2D.Double();
            ok.moveTo(7.5, 7.6);
            ok.lineTo(12.0, 3.0);
            ok.lineTo(16.5, 7.6);
            g.draw(ok);
            Path2D tepsi = new Path2D.Double();
            tepsi.moveTo(4.0, 16.0);
            tepsi.lineTo(4.0, 20.5);
            tepsi.lineTo(20.0, 20.5);
            tepsi.lineTo(20.0, 16.0);
            g.draw(tepsi);
        });
    }

    public static void takvim(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Double(3.5, 5.5, 17.0, 15.0, 3.0, 3.0));
            g.drawLine(3, 10, 21, 10);
            g.drawLine(8, 3, 8, 7);
            g.drawLine(16, 3, 16, 7);
        });
    }

    public static void okSag(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            p.moveTo(9.5, 5.5);
            p.lineTo(16.0, 12.0);
            p.lineTo(9.5, 18.5);
            g.draw(p);
        });
    }

    public static void okAsagi(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            p.moveTo(5.5, 9.5);
            p.lineTo(12.0, 16.0);
            p.lineTo(18.5, 9.5);
            g.draw(p);
        });
    }

    public static void okSol(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            p.moveTo(14.5, 5.5);
            p.lineTo(8.0, 12.0);
            p.lineTo(14.5, 18.5);
            g.draw(p);
        });
    }

    /** Boş durum için not defteri. */
    public static void bosDurum(Graphics2D g0, double x, double y, double boyut, Color renk) {
        cerceve(g0, x, y, boyut, renk, (g, k) -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Double(5.0, 3.0, 14.0, 18.0, 2.5, 2.5));
            g.drawLine(8, 8, 16, 8);
            g.drawLine(8, 12, 16, 12);
            g.drawLine(8, 16, 13, 16);
        });
    }
}
