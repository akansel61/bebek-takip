package com.akansel.bebektakip.rapor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Raporun kapsadığı tarih aralığı. Başlangıç ve bitiş günleri dahildir.
 * Hafta pazartesi başlar, pazar biter.
 */
public final class Donem {

    private static final Locale TR = new Locale("tr", "TR");
    private static final DateTimeFormatter GUN_AY_YIL =
            DateTimeFormatter.ofPattern("d MMMM yyyy", TR);
    private static final DateTimeFormatter GUN_AY = DateTimeFormatter.ofPattern("d MMMM", TR);
    private static final DateTimeFormatter AY_YIL = DateTimeFormatter.ofPattern("MMMM yyyy", TR);
    private static final DateTimeFormatter GUN_ADI = DateTimeFormatter.ofPattern("EEEE", TR);
    private static final DateTimeFormatter NOKTALI = DateTimeFormatter.ofPattern("dd.MM.yyyy", TR);

    /** Dönem türü. */
    public enum Tur {
        GUNLUK("Günlük"),
        HAFTALIK("Haftalık"),
        AYLIK("Aylık"),
        OZEL("Özel aralık");

        private final String ad;

        Tur(String ad) {
            this.ad = ad;
        }

        public String getAd() {
            return ad;
        }
    }

    private final Tur tur;
    private final LocalDate baslangic;
    private final LocalDate bitis;

    private Donem(Tur tur, LocalDate baslangic, LocalDate bitis) {
        this.tur = tur;
        this.baslangic = baslangic;
        this.bitis = bitis;
    }

    public static Donem gunluk(LocalDate gun) {
        return new Donem(Tur.GUNLUK, gun, gun);
    }

    /** Verilen günü içeren pazartesi-pazar haftası. */
    public static Donem haftalik(LocalDate gun) {
        LocalDate pazartesi = gun.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Donem(Tur.HAFTALIK, pazartesi, pazartesi.plusDays(6));
    }

    public static Donem aylik(LocalDate gun) {
        return new Donem(Tur.AYLIK, gun.withDayOfMonth(1),
                gun.with(TemporalAdjusters.lastDayOfMonth()));
    }

    /** Tarihler ters verilirse yer değiştirilir. */
    public static Donem ozel(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? new Donem(Tur.OZEL, b, a) : new Donem(Tur.OZEL, a, b);
    }

    /** Aynı türde, verilen günü içeren dönem. Özel aralıkta gün başlangıç olur. */
    public static Donem olustur(Tur tur, LocalDate gun) {
        switch (tur) {
            case HAFTALIK:
                return haftalik(gun);
            case AYLIK:
                return aylik(gun);
            case OZEL:
                return ozel(gun, gun.plusDays(6));
            default:
                return gunluk(gun);
        }
    }

    public Tur getTur() {
        return tur;
    }

    public LocalDate getBaslangic() {
        return baslangic;
    }

    public LocalDate getBitis() {
        return bitis;
    }

    public boolean icerir(LocalDate gun) {
        return !gun.isBefore(baslangic) && !gun.isAfter(bitis);
    }

    public int gunSayisi() {
        return (int) ChronoUnit.DAYS.between(baslangic, bitis) + 1;
    }

    /** Bir önceki dönem; özel aralık kendi uzunluğu kadar kayar. */
    public Donem onceki() {
        switch (tur) {
            case GUNLUK:
                return gunluk(baslangic.minusDays(1));
            case HAFTALIK:
                return haftalik(baslangic.minusWeeks(1));
            case AYLIK:
                return aylik(baslangic.minusMonths(1));
            default:
                return ozel(baslangic.minusDays(gunSayisi()), baslangic.minusDays(1));
        }
    }

    public Donem sonraki() {
        switch (tur) {
            case GUNLUK:
                return gunluk(baslangic.plusDays(1));
            case HAFTALIK:
                return haftalik(baslangic.plusWeeks(1));
            case AYLIK:
                return aylik(baslangic.plusMonths(1));
            default:
                return ozel(bitis.plusDays(1), bitis.plusDays(gunSayisi()));
        }
    }

    /** Raporun ana başlığı: "Günlük Rapor", "Haftalık Rapor"... */
    public String raporAdi() {
        return tur == Tur.OZEL ? "Dönem Raporu" : tur.getAd() + " Rapor";
    }

    /** Okunur aralık: "20 Ekim 2026, Salı", "14 – 20 Ekim 2026", "Ekim 2026". */
    public String aralikMetni() {
        if (tur == Tur.GUNLUK) {
            return baslangic.format(GUN_AY_YIL) + ", " + baslangic.format(GUN_ADI);
        }
        if (tur == Tur.AYLIK) {
            return baslangic.format(AY_YIL);
        }
        if (tur == Tur.OZEL) {
            return baslangic.format(NOKTALI) + " – " + bitis.format(NOKTALI);
        }
        if (baslangic.getYear() != bitis.getYear()) {
            return baslangic.format(GUN_AY_YIL) + " – " + bitis.format(GUN_AY_YIL);
        }
        if (baslangic.getMonth() != bitis.getMonth()) {
            return baslangic.format(GUN_AY) + " – " + bitis.format(GUN_AY_YIL);
        }
        return baslangic.getDayOfMonth() + " – " + bitis.format(GUN_AY_YIL);
    }

    /** Dosya adı için kısa ek: 2026-10-20, 2026-10, 2026-10-14_2026-10-20. */
    public String dosyaEki() {
        if (tur == Tur.GUNLUK) {
            return baslangic.toString();
        }
        if (tur == Tur.AYLIK) {
            return baslangic.toString().substring(0, 7);
        }
        return baslangic + "_" + bitis;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Donem)) {
            return false;
        }
        Donem d = (Donem) o;
        return tur == d.tur && baslangic.equals(d.baslangic) && bitis.equals(d.bitis);
    }

    @Override
    public int hashCode() {
        return (tur.hashCode() * 31 + baslangic.hashCode()) * 31 + bitis.hashCode();
    }

    @Override
    public String toString() {
        return tur + " " + baslangic + " / " + bitis;
    }
}
