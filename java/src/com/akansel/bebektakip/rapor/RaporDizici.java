package com.akansel.bebektakip.rapor;

import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rapor verisini A4 sayfalara yerleştirir.
 *
 * Sayfa düzeni: ilk sayfada başlık ve özet kutuları, ardından günlük özet
 * tablosu ve seçilen her bölümün ayrıntı tablosu. Tablo sayfaya sığmazsa
 * yeni sayfada başlık satırı tekrarlanarak devam eder. Sayfa numaraları
 * en sonda, toplam sayfa sayısı belli olunca yazılır.
 */
public final class RaporDizici {

    private static final double KENAR = 40;
    private static final double SAG = Sayfa.GENISLIK - KENAR;
    private static final double ICERIK = Sayfa.GENISLIK - 2 * KENAR;
    /** Bu çizginin altı sayfa altbilgisine ayrılmıştır. */
    private static final double ALT_SINIR = Sayfa.YUKSEKLIK - 54;

    static final Color METIN = new Color(0x1D1D1F);
    static final Color SOLUK = new Color(0x6E6E73);
    static final Color CIZGI = new Color(0xE5E5E7);
    static final Color CIZGI_ACIK = new Color(0xEDEDF0);
    static final Color ZEMIN = new Color(0xF5F5F7);
    static final Color BASLIK_ZEMIN = new Color(0xF0F0F3);
    static final Color KOYU_SATIR = new Color(0xF8F8FA);

    static final Color MAMA = new Color(0xFF9500);
    static final Color VITAMIN = new Color(0x34C759);
    static final Color ILAC = new Color(0x0071E3);
    static final Color ATES = new Color(0xFF3B30);

    private static final double GOVDE = 8.6;
    private static final double SATIR_ARALIGI = 11.2;
    private static final double HUCRE_DIKEY = 5.2;
    private static final double HUCRE_YATAY = 6;
    private static final int EN_FAZLA_SATIR = 20;

    private final RaporVerisi veri;
    private final RaporYazisi yazi;
    private final String imza;
    private final List<Sayfa> sayfalar = new ArrayList<>();
    private Sayfa sayfa;
    private double y;

    /** Tablonun tek sütunu; oranlar toplanıp içerik genişliğine paylaştırılır. */
    private static final class Sutun {
        final String baslik;
        final double oran;
        final boolean sag;

        Sutun(String baslik, double oran, boolean sag) {
            this.baslik = baslik;
            this.oran = oran;
            this.sag = sag;
        }
    }

    private static final class Hucre {
        final String metin;
        Color renk = METIN;
        Color rozet;
        boolean kalin;
        /** Boş hücre sayfanın ilk satırına düşerse bunun yerine yazılır. */
        String sayfaBasinda;

        Hucre(String metin) {
            this.metin = metin == null ? "" : metin;
        }
    }

    /** Tablo satırı ve zemininin bir ton koyu olup olmadığı (gün gruplaması). */
    private static final class Satir {
        final Hucre[] hucreler;
        final boolean koyu;
        boolean ustCizgi;

        Satir(Hucre[] hucreler, boolean koyu) {
            this.hucreler = hucreler;
            this.koyu = koyu;
        }
    }

    private RaporDizici(RaporVerisi veri, RaporYazisi yazi, String imza) {
        this.veri = veri;
        this.yazi = yazi;
        this.imza = imza == null ? "" : imza;
    }

    /**
     * @param imza her sayfanın altına yazılan uygulama satırı
     */
    public static List<Sayfa> diz(RaporVerisi veri, RaporYazisi yazi, String imza) {
        return new RaporDizici(veri, yazi, imza).calis();
    }

    private List<Sayfa> calis() {
        ilkSayfa();
        ozetKutulari();
        if (veri.getGunler().size() > 1) {
            gunlukOzet();
        }
        if (veri.bolumVar(RaporVerisi.Bolum.MAMA)) {
            mamaBolumu();
        }
        if (veri.bolumVar(RaporVerisi.Bolum.VITAMIN)) {
            ilacBolumu(true);
        }
        if (veri.bolumVar(RaporVerisi.Bolum.ILAC)) {
            ilacBolumu(false);
        }
        if (veri.bolumVar(RaporVerisi.Bolum.ATES)) {
            atesBolumu();
            y += 4;
            paragraf("Ateş sınırları: 37,5 °C altı normal, 37,5–37,9 °C hafif yüksek, "
                    + "38 °C ve üstü ateş, 39 °C ve üstü yüksek ateş. Bu rapor bilgilendirme "
                    + "amaçlıdır; tıbbi değerlendirme ve tedavi için doktorunuza danışın.",
                    7.6, SOLUK);
        }
        altBilgileriYaz();
        return sayfalar;
    }

    // ------------------------------------------------------------ sayfa

    private void ilkSayfa() {
        sayfa = new Sayfa();
        sayfalar.add(sayfa);
        Donem d = veri.getDonem();
        double ust = KENAR;
        sayfa.yazi(KENAR, ust + 9, "Bebek Takip", 9, true, SOLUK);
        sayfa.yazi(KENAR, ust + 37, d.raporAdi(), 22, true, METIN);
        sayfa.yazi(KENAR, ust + 56, d.aralikMetni(), 11.5, false, SOLUK);

        sagaYasli(SAG, ust + 9, "Hazırlanma", 7.6, false, SOLUK);
        sagaYasli(SAG, ust + 22, Bicim.tarihSaat(veri.getHazirlanma()), 9.5, true, METIN);
        sagaYasli(SAG, ust + 56, "İçerik: " + icerikListesi(), 8.5, false, SOLUK);

        y = ust + 72;
        sayfa.cizgi(KENAR, y, SAG, y, 0.8, CIZGI);
        y += 18;
    }

    private String icerikListesi() {
        StringBuilder sb = new StringBuilder();
        for (RaporVerisi.Bolum b : veri.getBolumler()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(b.getAd());
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }

    private void devamSayfasi() {
        sayfa = new Sayfa();
        sayfalar.add(sayfa);
        Donem d = veri.getDonem();
        sayfa.yazi(KENAR, KENAR + 8, d.raporAdi() + " · " + d.aralikMetni(), 8.5, false, SOLUK);
        y = KENAR + 16;
        sayfa.cizgi(KENAR, y, SAG, y, 0.6, CIZGI);
        y += 14;
    }

    /** Verilen yükseklik sayfaya sığmıyorsa yeni sayfaya geçer; geçtiyse true. */
    private boolean yerAc(double yukseklik) {
        if (y + yukseklik > ALT_SINIR) {
            devamSayfasi();
            return true;
        }
        return false;
    }

    private void altBilgileriYaz() {
        int toplam = sayfalar.size();
        for (int i = 0; i < toplam; i++) {
            Sayfa s = sayfalar.get(i);
            double cy = Sayfa.YUKSEKLIK - 36;
            s.cizgi(KENAR, cy, SAG, cy, 0.6, CIZGI);
            s.yazi(KENAR, cy + 14, imza, 7.5, false, SOLUK);
            String no = "Sayfa " + (i + 1) + " / " + toplam;
            s.yazi(SAG - yazi.genislik(no, 7.5, false), cy + 14, no, 7.5, false, SOLUK);
        }
    }

    // ------------------------------------------------------------ özet

    private void ozetKutulari() {
        List<String[]> kutular = new ArrayList<>();
        List<Color> renkler = new ArrayList<>();
        if (veri.bolumVar(RaporVerisi.Bolum.MAMA)) {
            int ogun = veri.getMamalar().size();
            String alt = ogun + " öğün";
            if (veri.mamaGunSayisi() > 1) {
                alt += " · günlük ort. "
                        + Bicim.sayi(Math.round(veri.gunlukOrtalamaMama())) + " ml";
            }
            kutular.add(new String[]{"Mama", Bicim.sayi(veri.toplamMama()) + " ml", alt});
            renkler.add(MAMA);
        }
        if (veri.bolumVar(RaporVerisi.Bolum.VITAMIN)) {
            kutular.add(new String[]{"Vitamin", veri.getVitaminler().size() + " kez",
                    sayimOzeti(veri.vitaminSayimi(), "Kayıt yok")});
            renkler.add(VITAMIN);
        }
        if (veri.bolumVar(RaporVerisi.Bolum.ILAC)) {
            kutular.add(new String[]{"İlaç", veri.getIlaclar().size() + " kez",
                    sayimOzeti(veri.ilacSayimi(), "Kayıt yok")});
            renkler.add(ILAC);
        }
        if (veri.bolumVar(RaporVerisi.Bolum.ATES)) {
            AtesKayit en = veri.enYuksekAtes();
            String deger = en == null ? "—" : Bicim.derece(en.getDerece());
            String alt = en == null ? "Ölçüm yok"
                    : veri.getAtesler().size() + " ölçüm · " + veri.atesliOlcumSayisi() + " ateşli";
            kutular.add(new String[]{"En yüksek ateş", deger, alt});
            renkler.add(ATES);
        }
        int n = kutular.size();
        if (n == 0) {
            paragraf("Rapora eklenecek bölüm seçilmedi.", 9.5, SOLUK);
            return;
        }
        double bosluk = 10;
        double g = (ICERIK - bosluk * (n - 1)) / n;
        double h = 66;
        for (int i = 0; i < n; i++) {
            double x = KENAR + i * (g + bosluk);
            String[] k = kutular.get(i);
            sayfa.kutu(x, y, g, h, 8, ZEMIN);
            sayfa.kutu(x + 12, y + 13, 6, 6, 3, renkler.get(i));
            sayfa.yazi(x + 23, y + 19, kisalt(k[0], g - 35, 8.2, false), 8.2, false, SOLUK);
            sayfa.yazi(x + 12, y + 42, kisalt(k[1], g - 24, 17, true), 17, true, METIN);
            sayfa.yazi(x + 12, y + 56, kisalt(k[2], g - 24, 7.6, false), 7.6, false, SOLUK);
        }
        y += h + 22;
    }

    private static String sayimOzeti(List<RaporVerisi.Sayim> sayimlar, String bos) {
        if (sayimlar.isEmpty()) {
            return bos;
        }
        StringBuilder sb = new StringBuilder();
        for (RaporVerisi.Sayim s : sayimlar) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(s.ad).append(' ').append(s.adet);
        }
        return sb.toString();
    }

    private void gunlukOzet() {
        boolean mama = veri.bolumVar(RaporVerisi.Bolum.MAMA);
        boolean vitamin = veri.bolumVar(RaporVerisi.Bolum.VITAMIN);
        boolean ilac = veri.bolumVar(RaporVerisi.Bolum.ILAC);
        boolean ates = veri.bolumVar(RaporVerisi.Bolum.ATES);
        if (!mama && !vitamin && !ilac && !ates) {
            return;
        }

        List<Sutun> sutunlar = new ArrayList<>();
        sutunlar.add(new Sutun("Tarih", 0.15, false));
        sutunlar.add(new Sutun("Gün", 0.14, false));
        if (mama) {
            sutunlar.add(new Sutun("Mama öğün", 0.11, true));
            sutunlar.add(new Sutun("Mama (ml)", 0.12, true));
        }
        if (vitamin) {
            sutunlar.add(new Sutun("Vitamin", 0.10, true));
        }
        if (ilac) {
            sutunlar.add(new Sutun("İlaç", 0.10, true));
        }
        if (ates) {
            sutunlar.add(new Sutun("Ateş ölçümü", 0.12, true));
            sutunlar.add(new Sutun("En yüksek", 0.13, true));
        }

        List<Satir> satirlar = new ArrayList<>();
        int ogun = 0;
        double ml = 0;
        int vit = 0;
        int ilc = 0;
        int olcum = 0;
        double enYuksek = 0;
        boolean koyu = false;
        for (RaporVerisi.GunOzeti g : veri.getGunler()) {
            List<Hucre> h = new ArrayList<>();
            h.add(new Hucre(Bicim.tarih(g.tarih)));
            h.add(new Hucre(Bicim.gunAdi(g.tarih)));
            if (mama) {
                h.add(new Hucre(sayiYadaTire(g.mamaOgun)));
                h.add(new Hucre(g.mamaMiktar > 0 ? Bicim.sayi(g.mamaMiktar) : "—"));
            }
            if (vitamin) {
                h.add(new Hucre(sayiYadaTire(g.vitamin)));
            }
            if (ilac) {
                h.add(new Hucre(sayiYadaTire(g.ilac)));
            }
            if (ates) {
                h.add(new Hucre(sayiYadaTire(g.atesOlcum)));
                h.add(dereceHucresi(g.enYuksekAtes));
            }
            for (Hucre hc : h) {
                if ("—".equals(hc.metin)) {
                    hc.renk = SOLUK;
                }
            }
            satirlar.add(new Satir(h.toArray(new Hucre[0]), koyu));
            koyu = !koyu;
            ogun += g.mamaOgun;
            ml += g.mamaMiktar;
            vit += g.vitamin;
            ilc += g.ilac;
            olcum += g.atesOlcum;
            enYuksek = Math.max(enYuksek, g.enYuksekAtes);
        }

        List<Hucre> toplam = new ArrayList<>();
        toplam.add(new Hucre("Toplam"));
        toplam.add(new Hucre(veri.getGunler().size() + " gün"));
        if (mama) {
            toplam.add(new Hucre(String.valueOf(ogun)));
            toplam.add(new Hucre(Bicim.sayi(ml)));
        }
        if (vitamin) {
            toplam.add(new Hucre(String.valueOf(vit)));
        }
        if (ilac) {
            toplam.add(new Hucre(String.valueOf(ilc)));
        }
        if (ates) {
            toplam.add(new Hucre(String.valueOf(olcum)));
            toplam.add(dereceHucresi(enYuksek));
        }
        for (Hucre hc : toplam) {
            hc.kalin = true;
        }
        Satir toplamSatiri = new Satir(toplam.toArray(new Hucre[0]), false);
        toplamSatiri.ustCizgi = true;
        satirlar.add(toplamSatiri);

        bolumBasligi("Günlük Özet", METIN, veri.getGunler().size() + " gün");
        tablo(sutunlar.toArray(new Sutun[0]), satirlar);
    }

    private static String sayiYadaTire(int n) {
        return n == 0 ? "—" : String.valueOf(n);
    }

    private static Hucre dereceHucresi(double derece) {
        if (derece <= 0) {
            Hucre h = new Hucre("—");
            h.renk = SOLUK;
            return h;
        }
        Hucre h = new Hucre(Bicim.derece(derece));
        h.renk = seviyeRengi(AtesKayit.seviye(derece));
        h.kalin = AtesKayit.seviye(derece).atesli();
        return h;
    }

    /** Ateş seviyesinin koyu (yazı) rengi. */
    static Color seviyeRengi(AtesKayit.Seviye s) {
        switch (s) {
            case YUKSEK:
                return new Color(0xB00012);
            case ATES:
                return new Color(0xD70015);
            case HAFIF:
                return new Color(0xB25000);
            case NORMAL:
                return new Color(0x1E7B34);
            default:
                return SOLUK;
        }
    }

    /** Ateş seviyesinin açık (zemin) rengi. */
    static Color seviyeZemini(AtesKayit.Seviye s) {
        switch (s) {
            case YUKSEK:
                return new Color(0xFFD9D6);
            case ATES:
                return new Color(0xFFE5E3);
            case HAFIF:
                return new Color(0xFFF0D9);
            case NORMAL:
                return new Color(0xE3F6E8);
            default:
                return ZEMIN;
        }
    }

    // ------------------------------------------------------------ bölümler

    private void mamaBolumu() {
        List<Kayit> liste = veri.getMamalar();
        String ozet = liste.size() + " öğün · toplam " + Bicim.sayi(veri.toplamMama()) + " ml";
        bolumBasligi("Mama", MAMA, ozet);
        if (liste.isEmpty()) {
            bosBolum();
            return;
        }
        Sutun[] sutunlar = {
                new Sutun("Tarih", 0.15, false),
                new Sutun("Saat", 0.09, false),
                new Sutun("Miktar (ml)", 0.13, true),
                new Sutun("Not", 0.63, false),
        };
        List<Satir> satirlar = new ArrayList<>();
        LocalDate onceki = null;
        boolean koyu = true;
        for (Kayit k : liste) {
            boolean yeniGun = !k.getTarih().equals(onceki);
            if (yeniGun) {
                koyu = !koyu;
            }
            double miktar = k.mamaMiktari();
            Hucre m = new Hucre(miktar > 0 ? Bicim.sayi(miktar) : "—");
            if (miktar <= 0) {
                m.renk = SOLUK;
            }
            String aciklama = mamaAciklamasi(k.getMamaNotu());
            String not = aciklama.isEmpty() || k.getNot().trim().isEmpty()
                    ? aciklama + k.getNot().trim() : aciklama + " · " + k.getNot().trim();
            satirlar.add(new Satir(new Hucre[]{
                    tarihHucresi(k.getTarih(), yeniGun),
                    new Hucre(Bicim.saat(k.getSaat())),
                    m,
                    new Hucre(not),
            }, koyu));
            onceki = k.getTarih();
        }
        tablo(sutunlar, satirlar);
    }

    private void ilacBolumu(boolean vitaminMi) {
        List<IlacKayit> liste = vitaminMi ? veri.getVitaminler() : veri.getIlaclar();
        bolumBasligi(vitaminMi ? "Vitamin" : "İlaç", vitaminMi ? VITAMIN : ILAC,
                liste.size() + " kayıt");
        if (liste.isEmpty()) {
            bosBolum();
            return;
        }
        Sutun[] sutunlar = {
                new Sutun("Tarih", 0.15, false),
                new Sutun("Saat", 0.09, false),
                new Sutun(vitaminMi ? "Vitamin" : "İlaç", 0.26, false),
                new Sutun("Doz", 0.16, false),
                new Sutun("Not", 0.34, false),
        };
        List<Satir> satirlar = new ArrayList<>();
        LocalDate onceki = null;
        boolean koyu = true;
        for (IlacKayit i : liste) {
            boolean yeniGun = !i.getTarih().equals(onceki);
            if (yeniGun) {
                koyu = !koyu;
            }
            satirlar.add(new Satir(new Hucre[]{
                    tarihHucresi(i.getTarih(), yeniGun),
                    new Hucre(Bicim.saat(i.getSaat())),
                    new Hucre(i.getAd().isEmpty() ? "(adsız)" : i.getAd()),
                    new Hucre(i.getDoz()),
                    new Hucre(i.getNot()),
            }, koyu));
            onceki = i.getTarih();
        }
        tablo(sutunlar, satirlar);

        StringBuilder toplam = new StringBuilder("Toplam: ");
        List<RaporVerisi.Sayim> sayimlar = vitaminMi ? veri.vitaminSayimi() : veri.ilacSayimi();
        for (int k = 0; k < sayimlar.size(); k++) {
            RaporVerisi.Sayim s = sayimlar.get(k);
            if (k > 0) {
                toplam.append(", ");
            }
            toplam.append(s.ad).append(' ').append(s.adet).append(" kez");
            if (!s.doz.isEmpty()) {
                toplam.append(" (").append(s.doz).append(')');
            }
        }
        paragraf(toplam.toString(), 8.4, SOLUK);
        y += 10;
    }

    private void atesBolumu() {
        List<AtesKayit> liste = veri.getAtesler();
        AtesKayit en = veri.enYuksekAtes();
        String ozet = liste.size() + " ölçüm";
        if (en != null) {
            ozet += " · " + veri.atesliOlcumSayisi() + " ateşli · en yüksek "
                    + Bicim.derece(en.getDerece()) + " (" + Bicim.kisaZaman(en.zaman()) + ")";
        }
        bolumBasligi("Ateş", ATES, ozet);
        if (liste.isEmpty()) {
            bosBolum();
            return;
        }
        Sutun[] sutunlar = {
                new Sutun("Tarih", 0.13, false),
                new Sutun("Saat", 0.08, false),
                new Sutun("Derece", 0.11, true),
                new Sutun("Durum", 0.14, false),
                new Sutun("Ölçüm yeri", 0.12, false),
                new Sutun("Belirtiler", 0.24, false),
                new Sutun("Not", 0.18, false),
        };
        List<Satir> satirlar = new ArrayList<>();
        LocalDate onceki = null;
        boolean koyu = true;
        for (AtesKayit a : liste) {
            boolean yeniGun = !a.getTarih().equals(onceki);
            if (yeniGun) {
                koyu = !koyu;
            }
            AtesKayit.Seviye s = a.seviye();
            Hucre durum = new Hucre(s.getAd());
            durum.renk = seviyeRengi(s);
            durum.rozet = seviyeZemini(s);
            satirlar.add(new Satir(new Hucre[]{
                    tarihHucresi(a.getTarih(), yeniGun),
                    new Hucre(Bicim.saat(a.getSaat())),
                    dereceHucresi(a.getDerece()),
                    durum,
                    new Hucre(a.getOlcumYeri()),
                    new Hucre(a.belirtiMetni()),
                    new Hucre(a.getNot()),
            }, koyu));
            onceki = a.getTarih();
        }
        tablo(sutunlar, satirlar);

        List<RaporVerisi.Sayim> belirtiler = veri.belirtiSayimi();
        if (!belirtiler.isEmpty()) {
            StringBuilder sb = new StringBuilder("Görülen belirtiler: ");
            for (int i = 0; i < belirtiler.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(belirtiler.get(i).ad).append(" (").append(belirtiler.get(i).adet)
                        .append(')');
            }
            paragraf(sb.toString(), 8.4, SOLUK);
            y += 10;
        }
    }

    /**
     * Mama notu çoğu zaman yalnızca miktardır ("120 ml"); o zaman miktar
     * sütununu tekrar etmesin diye boş sayılır. Fazlası varsa ("120 ml
     * biberonla") olduğu gibi kalır.
     */
    static String mamaAciklamasi(String mamaNotu) {
        String t = mamaNotu == null ? "" : mamaNotu.trim();
        String kalan = t.replaceFirst(
                "^\\d+([.,]\\d+)?\\s*(ml|mL|ML|gr|GR|g|cc)?\\.?$", "");
        return kalan.isEmpty() ? "" : t;
    }

    /**
     * Aynı günün ikinci satırından itibaren tarih tekrar yazılmaz; gün yeni
     * sayfaya taşarsa o sayfanın ilk satırında soluk olarak yine görünür.
     */
    private static Hucre tarihHucresi(LocalDate tarih, boolean goster) {
        Hucre h = new Hucre(goster ? Bicim.tarih(tarih) : "");
        h.sayfaBasinda = Bicim.tarih(tarih);
        return h;
    }

    private void bosBolum() {
        paragraf("Bu dönemde kayıt yok.", 9, SOLUK);
        y += 12;
    }

    private void bolumBasligi(String baslik, Color renk, String ozet) {
        // başlık, tablo başlığı ve en az bir satır aynı sayfada kalsın
        yerAc(20 + 19 + 22);
        sayfa.kutu(KENAR, y + 3.5, 8, 8, 4, renk);
        sayfa.yazi(KENAR + 14, y + 11.5, baslik, 12.5, true, METIN);
        double baslikSonu = KENAR + 14 + yazi.genislik(baslik, 12.5, true) + 16;
        String kisa = kisalt(ozet, SAG - baslikSonu, 8.5, false);
        sagaYasli(SAG, y + 11.5, kisa, 8.5, false, SOLUK);
        y += 21;
    }

    // ------------------------------------------------------------ tablo

    private void tablo(Sutun[] sutunlar, List<Satir> satirlar) {
        double toplamOran = 0;
        for (Sutun s : sutunlar) {
            toplamOran += s.oran;
        }
        double[] genislik = new double[sutunlar.length];
        for (int i = 0; i < sutunlar.length; i++) {
            genislik[i] = ICERIK * sutunlar[i].oran / toplamOran;
        }

        tabloBasligi(sutunlar, genislik);
        boolean sayfaBasi = true;
        for (Satir satir : satirlar) {
            List<List<String>> metinler = new ArrayList<>();
            int enCok = 1;
            for (int i = 0; i < sutunlar.length; i++) {
                Hucre h = satir.hucreler[i];
                double alan = genislik[i] - HUCRE_YATAY * 2 - (h.rozet != null ? 10 : 0);
                List<String> parcalar = sar(h.metin, alan, GOVDE, h.kalin);
                metinler.add(parcalar);
                enCok = Math.max(enCok, parcalar.size());
            }
            double h = enCok * SATIR_ARALIGI + HUCRE_DIKEY * 2;
            if (yerAc(h)) {
                tabloBasligi(sutunlar, genislik);
                sayfaBasi = true;
            }
            Hucre ilk = satir.hucreler[0];
            if (sayfaBasi && ilk.metin.isEmpty() && ilk.sayfaBasinda != null) {
                ilk = new Hucre(ilk.sayfaBasinda);
                ilk.renk = SOLUK;
                metinler.set(0, sar(ilk.metin, genislik[0] - HUCRE_YATAY * 2, GOVDE, false));
            }
            sayfaBasi = false;
            if (satir.koyu) {
                sayfa.kutu(KENAR, y, ICERIK, h, 0, KOYU_SATIR);
            }
            if (satir.ustCizgi) {
                sayfa.cizgi(KENAR, y, SAG, y, 0.8, CIZGI);
            }
            double x = KENAR;
            for (int i = 0; i < sutunlar.length; i++) {
                hucreCiz(i == 0 ? ilk : satir.hucreler[i], metinler.get(i), x, genislik[i],
                        sutunlar[i].sag);
                x += genislik[i];
            }
            y += h;
            sayfa.cizgi(KENAR, y, SAG, y, 0.5, CIZGI_ACIK);
        }
        y += 14;
    }

    private void tabloBasligi(Sutun[] sutunlar, double[] genislik) {
        double h = 19;
        sayfa.kutu(KENAR, y, ICERIK, h, 0, BASLIK_ZEMIN);
        double x = KENAR;
        for (int i = 0; i < sutunlar.length; i++) {
            String b = kisalt(sutunlar[i].baslik, genislik[i] - HUCRE_YATAY * 2, 7.6, true);
            double bx = sutunlar[i].sag
                    ? x + genislik[i] - HUCRE_YATAY - yazi.genislik(b, 7.6, true)
                    : x + HUCRE_YATAY;
            sayfa.yazi(bx, y + 12.6, b, 7.6, true, SOLUK);
            x += genislik[i];
        }
        y += h;
    }

    private void hucreCiz(Hucre h, List<String> satirlar, double x, double genislik, boolean sag) {
        double taban = y + HUCRE_DIKEY + 8.4;
        for (int i = 0; i < satirlar.size(); i++) {
            String s = satirlar.get(i);
            if (s.isEmpty()) {
                continue;
            }
            double g = yazi.genislik(s, GOVDE, h.kalin);
            double sx = sag ? x + genislik - HUCRE_YATAY - g : x + HUCRE_YATAY;
            if (h.rozet != null && i == 0) {
                double rx = sag ? sx - 10 : sx - 1;
                sayfa.kutu(rx, taban - 9.6, g + 10, 13.4, 6.7, h.rozet);
                sx = sag ? sx - 5 : sx + 4;
            }
            sayfa.yazi(sx, taban + i * SATIR_ARALIGI, s, GOVDE, h.kalin, h.renk);
        }
    }

    // ------------------------------------------------------------ yazı

    private void paragraf(String metin, double boyut, Color renk) {
        double aralik = boyut * 1.35;
        for (String satir : sar(metin, ICERIK, boyut, false)) {
            yerAc(aralik);
            sayfa.yazi(KENAR, y + boyut, satir, boyut, false, renk);
            y += aralik;
        }
    }

    private void sagaYasli(double sag, double taban, String metin, double boyut, boolean kalin,
                           Color renk) {
        sayfa.yazi(sag - yazi.genislik(metin, boyut, kalin), taban, metin, boyut, kalin, renk);
    }

    /** Metni sığmıyorsa sonuna üç nokta koyarak kısaltır. */
    private String kisalt(String metin, double genislik, double boyut, boolean kalin) {
        if (metin == null || yazi.genislik(metin, boyut, kalin) <= genislik) {
            return metin;
        }
        String son = "…";
        int n = metin.length();
        while (n > 0 && yazi.genislik(metin.substring(0, n) + son, boyut, kalin) > genislik) {
            n--;
        }
        return metin.substring(0, n).trim() + son;
    }

    /**
     * Metni sütun genişliğine göre satırlara böler. Sözcük sınırından
     * kırılır; tek başına sığmayan uzun sözcük harf harf bölünür.
     */
    private List<String> sar(String metin, double genislik, double boyut, boolean kalin) {
        List<String> sonuc = new ArrayList<>();
        if (metin == null || metin.trim().isEmpty()) {
            sonuc.add("");
            return sonuc;
        }
        for (String paragraf : metin.split("\\r?\\n")) {
            StringBuilder satir = new StringBuilder();
            for (String kelime : paragraf.trim().split("\\s+")) {
                if (kelime.isEmpty()) {
                    continue;
                }
                String aday = satir.length() == 0 ? kelime : satir + " " + kelime;
                if (yazi.genislik(aday, boyut, kalin) <= genislik) {
                    satir.setLength(0);
                    satir.append(aday);
                    continue;
                }
                if (satir.length() > 0) {
                    sonuc.add(satir.toString());
                    satir.setLength(0);
                }
                while (kelime.length() > 1 && yazi.genislik(kelime, boyut, kalin) > genislik) {
                    int n = kelime.length() - 1;
                    while (n > 1
                            && yazi.genislik(kelime.substring(0, n), boyut, kalin) > genislik) {
                        n--;
                    }
                    sonuc.add(kelime.substring(0, n));
                    kelime = kelime.substring(n);
                }
                satir.append(kelime);
            }
            sonuc.add(satir.toString());
        }
        if (sonuc.size() > EN_FAZLA_SATIR) {
            List<String> kisa = new ArrayList<>(sonuc.subList(0, EN_FAZLA_SATIR));
            kisa.set(EN_FAZLA_SATIR - 1, kisalt(kisa.get(EN_FAZLA_SATIR - 1) + " …", genislik,
                    boyut, kalin));
            return kisa;
        }
        return sonuc;
    }
}
