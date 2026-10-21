package com.akansel.bebektakip.rapor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Raporda kullanılan sayı ve tarih yazımları; PDF ve Excel aynı biçimi kullanır. */
final class Bicim {

    static final Locale TR = new Locale("tr", "TR");

    private static final DateTimeFormatter TARIH = DateTimeFormatter.ofPattern("dd.MM.yyyy", TR);
    private static final DateTimeFormatter SAAT = DateTimeFormatter.ofPattern("HH:mm", TR);
    private static final DateTimeFormatter GUN_ADI = DateTimeFormatter.ofPattern("EEEE", TR);
    private static final DateTimeFormatter KISA = DateTimeFormatter.ofPattern("d MMM HH:mm", TR);

    private Bicim() {
    }

    static String tarih(LocalDate d) {
        return d.format(TARIH);
    }

    static String saat(LocalTime t) {
        return t.format(SAAT);
    }

    static String gunAdi(LocalDate d) {
        return d.format(GUN_ADI);
    }

    static String tarihSaat(LocalDateTime t) {
        return t.format(TARIH) + " " + t.format(SAAT);
    }

    static String kisaZaman(LocalDateTime t) {
        return t.format(KISA);
    }

    /** Ondalık kısmı yoksa tam sayı: 120 / 7,5. */
    static String sayi(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.0001) {
            return String.valueOf((long) Math.rint(d));
        }
        return String.format(TR, "%.1f", d);
    }

    static String derece(double d) {
        return String.format(TR, "%.1f °C", d);
    }
}
