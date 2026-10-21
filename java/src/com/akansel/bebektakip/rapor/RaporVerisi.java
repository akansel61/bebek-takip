package com.akansel.bebektakip.rapor;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bir dönemin rapora girecek verileri ve hesaplanmış toplamları.
 *
 * Depodaki listeler yeniden eskiye sıralı; raporda ise kayıtlar okunduğu
 * gibi eskiden yeniye gider, bu yüzden burada ters çevrilerek kopyalanır.
 * PDF, Excel ve önizleme aynı nesneyi kullanır.
 */
public final class RaporVerisi {

    private static final Locale TR = new Locale("tr", "TR");

    /** Raporun bölümleri; kullanıcı istediklerini seçer. */
    public enum Bolum {
        MAMA("Mama"),
        VITAMIN("Vitamin"),
        ILAC("İlaç"),
        ATES("Ateş");

        private final String ad;

        Bolum(String ad) {
            this.ad = ad;
        }

        public String getAd() {
            return ad;
        }
    }

    /** Günlük özet tablosunun tek satırı. */
    public static final class GunOzeti {
        public final LocalDate tarih;
        public int mamaOgun;
        public double mamaMiktar;
        public int vitamin;
        public int ilac;
        public int atesOlcum;
        public double enYuksekAtes;

        GunOzeti(LocalDate tarih) {
            this.tarih = tarih;
        }
    }

    /** Adına göre kaç kez verildiği ve en son yazılan doz. */
    public static final class Sayim {
        public final String ad;
        public int adet;
        public String doz = "";

        Sayim(String ad) {
            this.ad = ad;
        }
    }

    private final Donem donem;
    private final Set<Bolum> bolumler;
    private final LocalDateTime hazirlanma;
    private final List<Kayit> mamalar = new ArrayList<>();
    private final List<IlacKayit> vitaminler = new ArrayList<>();
    private final List<IlacKayit> ilaclar = new ArrayList<>();
    private final List<AtesKayit> atesler = new ArrayList<>();
    private final List<GunOzeti> gunler = new ArrayList<>();

    private RaporVerisi(Donem donem, Set<Bolum> bolumler, LocalDateTime hazirlanma) {
        this.donem = donem;
        this.bolumler = bolumler.isEmpty()
                ? EnumSet.noneOf(Bolum.class) : EnumSet.copyOf(bolumler);
        this.hazirlanma = hazirlanma;
    }

    public static RaporVerisi hazirla(KayitDeposu depo, Donem donem, Set<Bolum> bolumler) {
        return hazirla(depo, donem, bolumler, LocalDateTime.now().withSecond(0).withNano(0));
    }

    /** Hazırlanma zamanı dışarıdan verilebilsin diye ayrı; testler sabit zaman kullanır. */
    public static RaporVerisi hazirla(KayitDeposu depo, Donem donem, Set<Bolum> bolumler,
                                      LocalDateTime hazirlanma) {
        RaporVerisi v = new RaporVerisi(donem, bolumler, hazirlanma);
        LocalDate son = v.sonGun();

        for (Kayit k : depo.getKayitlar()) {
            if (k.isMama() && v.kapsar(k.getTarih(), son)) {
                v.mamalar.add(k);
            }
        }
        for (IlacKayit i : depo.getIlaclar()) {
            if (v.kapsar(i.getTarih(), son)) {
                (i.isVitamin() ? v.vitaminler : v.ilaclar).add(i);
            }
        }
        for (AtesKayit a : depo.getAtesler()) {
            if (a.getDerece() > 0 && v.kapsar(a.getTarih(), son)) {
                v.atesler.add(a);
            }
        }
        v.mamalar.sort(Comparator.comparing(Kayit::zaman));
        v.vitaminler.sort(Comparator.comparing(IlacKayit::zaman));
        v.ilaclar.sort(Comparator.comparing(IlacKayit::zaman));
        v.atesler.sort(Comparator.comparing(AtesKayit::zaman));
        v.gunleriHesapla();
        return v;
    }

    /**
     * Raporun son günü. İçinde bulunulan ayın raporunda gelecek günler boş
     * satır olarak görünmesin diye dönem bugünde kesilir; ileri tarihli
     * (büyük olasılıkla yanlış girilmiş) kayıtlar da böylece toplamlara
     * girmez. Dönemin tamamı gelecekteyse kesilmez.
     */
    private LocalDate sonGun() {
        LocalDate son = donem.getBitis();
        LocalDate bugun = hazirlanma.toLocalDate();
        if (!donem.getBaslangic().isAfter(bugun) && son.isAfter(bugun)) {
            son = bugun;
        }
        return son;
    }

    private boolean kapsar(LocalDate tarih, LocalDate son) {
        return donem.icerir(tarih) && !tarih.isAfter(son);
    }

    private void gunleriHesapla() {
        LocalDate son = sonGun();
        Map<LocalDate, GunOzeti> harita = new LinkedHashMap<>();
        for (LocalDate g = donem.getBaslangic(); !g.isAfter(son); g = g.plusDays(1)) {
            GunOzeti o = new GunOzeti(g);
            harita.put(g, o);
            gunler.add(o);
        }
        for (Kayit k : mamalar) {
            GunOzeti o = harita.get(k.getTarih());
            if (o != null) {
                o.mamaOgun++;
                o.mamaMiktar += k.mamaMiktari();
            }
        }
        for (IlacKayit i : vitaminler) {
            GunOzeti o = harita.get(i.getTarih());
            if (o != null) {
                o.vitamin++;
            }
        }
        for (IlacKayit i : ilaclar) {
            GunOzeti o = harita.get(i.getTarih());
            if (o != null) {
                o.ilac++;
            }
        }
        for (AtesKayit a : atesler) {
            GunOzeti o = harita.get(a.getTarih());
            if (o != null) {
                o.atesOlcum++;
                o.enYuksekAtes = Math.max(o.enYuksekAtes, a.getDerece());
            }
        }
    }

    public Donem getDonem() {
        return donem;
    }

    public Set<Bolum> getBolumler() {
        return Collections.unmodifiableSet(bolumler);
    }

    public boolean bolumVar(Bolum b) {
        return bolumler.contains(b);
    }

    public LocalDateTime getHazirlanma() {
        return hazirlanma;
    }

    public List<Kayit> getMamalar() {
        return Collections.unmodifiableList(mamalar);
    }

    public List<IlacKayit> getVitaminler() {
        return Collections.unmodifiableList(vitaminler);
    }

    public List<IlacKayit> getIlaclar() {
        return Collections.unmodifiableList(ilaclar);
    }

    public List<AtesKayit> getAtesler() {
        return Collections.unmodifiableList(atesler);
    }

    public List<GunOzeti> getGunler() {
        return Collections.unmodifiableList(gunler);
    }

    // ---- mama ----

    public double toplamMama() {
        double t = 0;
        for (Kayit k : mamalar) {
            t += k.mamaMiktari();
        }
        return t;
    }

    /** Mama verilen gün sayısı; günlük ortalama bu günlere bölünür. */
    public int mamaGunSayisi() {
        int n = 0;
        for (GunOzeti g : gunler) {
            if (g.mamaOgun > 0) {
                n++;
            }
        }
        return n;
    }

    public double gunlukOrtalamaMama() {
        int gun = mamaGunSayisi();
        return gun == 0 ? 0 : toplamMama() / gun;
    }

    // ---- vitamin / ilaç ----

    public List<Sayim> vitaminSayimi() {
        return say(vitaminler);
    }

    public List<Sayim> ilacSayimi() {
        return say(ilaclar);
    }

    /** Aynı ad büyük/küçük harf farkı gözetmeden tek satırda toplanır. */
    private static List<Sayim> say(List<IlacKayit> liste) {
        Map<String, Sayim> harita = new LinkedHashMap<>();
        for (IlacKayit i : liste) {
            String ad = i.getAd().trim().isEmpty() ? "(adsız)" : i.getAd().trim();
            Sayim s = harita.computeIfAbsent(ad.toLowerCase(TR), anahtar -> new Sayim(ad));
            s.adet++;
            if (!i.getDoz().trim().isEmpty()) {
                s.doz = i.getDoz().trim();
            }
        }
        List<Sayim> sonuc = new ArrayList<>(harita.values());
        sonuc.sort((a, b) -> Integer.compare(b.adet, a.adet));
        return sonuc;
    }

    // ---- ateş ----

    /** En yüksek ölçüm; ölçüm yoksa null. Eşitlikte ilk ölçüm döner. */
    public AtesKayit enYuksekAtes() {
        AtesKayit en = null;
        for (AtesKayit a : atesler) {
            if (en == null || a.getDerece() > en.getDerece()) {
                en = a;
            }
        }
        return en;
    }

    /** 38 °C ve üstü ölçüm sayısı. */
    public int atesliOlcumSayisi() {
        int n = 0;
        for (AtesKayit a : atesler) {
            if (a.seviye().atesli()) {
                n++;
            }
        }
        return n;
    }

    public int atesliGunSayisi() {
        int n = 0;
        for (GunOzeti g : gunler) {
            if (AtesKayit.seviye(g.enYuksekAtes).atesli()) {
                n++;
            }
        }
        return n;
    }

    /** Belirtiler, en sık görülenden aza doğru. */
    public List<Sayim> belirtiSayimi() {
        Map<String, Sayim> harita = new LinkedHashMap<>();
        for (AtesKayit a : atesler) {
            for (String b : a.getBelirtiler()) {
                harita.computeIfAbsent(b, Sayim::new).adet++;
            }
        }
        List<Sayim> sonuc = new ArrayList<>(harita.values());
        sonuc.sort((a, b) -> Integer.compare(b.adet, a.adet));
        return sonuc;
    }

    /** Seçili bölümlerde hiç kayıt yoksa true. */
    public boolean bos() {
        return (!bolumVar(Bolum.MAMA) || mamalar.isEmpty())
                && (!bolumVar(Bolum.VITAMIN) || vitaminler.isEmpty())
                && (!bolumVar(Bolum.ILAC) || ilaclar.isEmpty())
                && (!bolumVar(Bolum.ATES) || atesler.isEmpty());
    }
}
