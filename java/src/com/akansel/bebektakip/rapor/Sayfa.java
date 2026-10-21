package com.akansel.bebektakip.rapor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bir A4 sayfanın çizim listesi. Ölçüler punto (1/72 inç), başlangıç
 * noktası sol üst köşe. Aynı liste hem PDF'e hem ekrana/yazıcıya çizilir;
 * böylece önizlemede görülen ile kâğıda çıkan birebir aynı olur.
 */
public final class Sayfa {

    public static final double GENISLIK = 595.28;
    public static final double YUKSEKLIK = 841.89;

    /** Çizim komutlarının ortak türü. */
    public interface Oge {
    }

    /** Tek satır yazı; y taban çizgisidir. */
    public static final class Yazi implements Oge {
        public final double x;
        public final double y;
        public final String metin;
        public final double boyut;
        public final boolean kalin;
        public final Color renk;

        Yazi(double x, double y, String metin, double boyut, boolean kalin, Color renk) {
            this.x = x;
            this.y = y;
            this.metin = metin;
            this.boyut = boyut;
            this.kalin = kalin;
            this.renk = renk;
        }
    }

    /** Dolu dikdörtgen; yarıçap sıfırdan büyükse köşeler yuvarlanır. */
    public static final class Kutu implements Oge {
        public final double x;
        public final double y;
        public final double genislik;
        public final double yukseklik;
        public final double yaricap;
        public final Color renk;

        Kutu(double x, double y, double genislik, double yukseklik, double yaricap, Color renk) {
            this.x = x;
            this.y = y;
            this.genislik = genislik;
            this.yukseklik = yukseklik;
            this.yaricap = yaricap;
            this.renk = renk;
        }
    }

    public static final class Cizgi implements Oge {
        public final double x1;
        public final double y1;
        public final double x2;
        public final double y2;
        public final double kalinlik;
        public final Color renk;

        Cizgi(double x1, double y1, double x2, double y2, double kalinlik, Color renk) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.kalinlik = kalinlik;
            this.renk = renk;
        }
    }

    private final List<Oge> ogeler = new ArrayList<>();

    void yazi(double x, double y, String metin, double boyut, boolean kalin, Color renk) {
        if (metin != null && !metin.isEmpty()) {
            ogeler.add(new Yazi(x, y, metin, boyut, kalin, renk));
        }
    }

    void kutu(double x, double y, double genislik, double yukseklik, double yaricap, Color renk) {
        ogeler.add(new Kutu(x, y, genislik, yukseklik, yaricap, renk));
    }

    void cizgi(double x1, double y1, double x2, double y2, double kalinlik, Color renk) {
        ogeler.add(new Cizgi(x1, y1, x2, y2, kalinlik, renk));
    }

    public List<Oge> getOgeler() {
        return Collections.unmodifiableList(ogeler);
    }

    /** Sayfadaki bütün yazılar alt alta; testlerde içerik aramak için. */
    public String duzMetin() {
        StringBuilder sb = new StringBuilder();
        for (Oge o : ogeler) {
            if (o instanceof Yazi) {
                sb.append(((Yazi) o).metin).append('\n');
            }
        }
        return sb.toString();
    }
}
