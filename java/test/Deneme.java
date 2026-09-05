import com.akansel.bebektakip.depo.DisaAktarim;
import com.akansel.bebektakip.depo.Json;
import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Veri katmanı için hızlı doğrulama. */
public class Deneme {

    static int basarili = 0;
    static int basarisiz = 0;

    static void kontrol(String ad, Object beklenen, Object gercek) {
        boolean ok = beklenen == null ? gercek == null : beklenen.equals(gercek);
        if (ok) {
            basarili++;
        } else {
            basarisiz++;
            System.out.println("  BAŞARISIZ " + ad + ": beklenen=<" + beklenen
                    + "> gerçek=<" + gercek + ">");
        }
    }

    public static void main(String[] args) throws Exception {
        jsonTesti();
        sayiTesti();
        tarayiciBicimiTesti();
        depoTuruTesti();
        csvTesti();
        icoTesti();
        uykuTesti();
        buyumeTesti();
        hatirlaticiTesti();
        ilacTesti();
        depoCokluTesti();

        System.out.println();
        System.out.println("Başarılı: " + basarili + "  Başarısız: " + basarisiz);
        if (basarisiz > 0) {
            System.exit(1);
        }
    }

    static void jsonTesti() {
        System.out.println("[JSON]");
        String metin = "{\"ad\":\"Çiş\\nKaka \\\"tırnak\\\"\",\"sayi\":120.5,"
                + "\"tam\":7,\"dogru\":true,\"bos\":null,\"dizi\":[1,2,{\"i\":\"ç\"}]}";
        java.util.Map<?, ?> m = (java.util.Map<?, ?>) Json.oku(metin);
        kontrol("metin kaçışı", "Çiş\nKaka \"tırnak\"", m.get("ad"));
        kontrol("ondalık", 120.5, m.get("sayi"));
        kontrol("doğruluk", Boolean.TRUE, m.get("dogru"));
        kontrol("boş", null, m.get("bos"));
        kontrol("dizi boyu", 3, ((List<?>) m.get("dizi")).size());

        String yazilan = Json.yaz(m);
        java.util.Map<?, ?> tekrar = (java.util.Map<?, ?>) Json.oku(yazilan);
        kontrol("gidiş-dönüş metin", m.get("ad"), tekrar.get("ad"));
        kontrol("tam sayı yazımı", true, yazilan.contains("\"tam\": 7"));

        kontrol("boş nesne", "{}", Json.yaz(new java.util.LinkedHashMap<String, Object>()));
        kontrol("boş dizi", "[]", Json.yaz(new java.util.ArrayList<Object>()));
    }

    static void sayiTesti() {
        System.out.println("[Mama miktarı]");
        Kayit k = new Kayit();
        k.setMama(true);
        k.setMamaNotu("120");
        kontrol("düz sayı", 120.0, k.mamaMiktari());
        k.setMamaNotu("120 ml");
        kontrol("birimli", 120.0, k.mamaMiktari());
        k.setMamaNotu("90ml");
        kontrol("bitişik birim", 90.0, k.mamaMiktari());
        k.setMamaNotu("7,5");
        kontrol("virgüllü ondalık", 7.5, k.mamaMiktari());
        k.setMamaNotu("7.5");
        kontrol("noktalı ondalık", 7.5, k.mamaMiktari());
        k.setMamaNotu("");
        kontrol("boş", 0.0, k.mamaMiktari());
        k.setMamaNotu("yok");
        kontrol("sayısız metin", 0.0, k.mamaMiktari());
        k.setMamaNotu("100");
        k.setMama(false);
        kontrol("mama işaretsiz", 0.0, k.mamaMiktari());

        kontrol("tarih ISO", LocalDate.of(2025, 8, 26), Kayit.tarihCoz("2025-08-26"));
        kontrol("tarih TR", LocalDate.of(2025, 8, 26), Kayit.tarihCoz("26.08.2025"));
        kontrol("tarih bozuk", null, Kayit.tarihCoz("abc"));
        kontrol("saat", LocalTime.of(14, 30), Kayit.saatCoz("14:30"));
        kontrol("saat saniyeli", LocalTime.of(14, 30), Kayit.saatCoz("14:30:59"));
        kontrol("saat geçersiz", null, Kayit.saatCoz("25:00"));
        kontrol("saat bozuk", null, Kayit.saatCoz("xx"));
    }

    static void tarayiciBicimiTesti() {
        System.out.println("[Tarayıcı localStorage biçimi]");
        String metin = "[{\"date\":\"2025-08-26\",\"time\":\"14:30\",\"cis\":true,"
                + "\"kaka\":false,\"sagMeme\":true,\"solMeme\":false,\"mama\":true,"
                + "\"mamaNotu\":\"120\",\"not\":\"iyi içti\"}]";
        List<Kayit> liste = KayitDeposu.metniCoz(metin);
        kontrol("kayıt sayısı", 1, liste.size());
        Kayit k = liste.get(0);
        kontrol("tarih", LocalDate.of(2025, 8, 26), k.getTarih());
        kontrol("saat", LocalTime.of(14, 30), k.getSaat());
        kontrol("çiş", true, k.isCis());
        kontrol("kaka", false, k.isKaka());
        kontrol("sag meme", true, k.isSagMeme());
        kontrol("mama notu", "120", k.getMamaNotu());
        kontrol("not", "iyi içti", k.getNot());
        kontrol("emzirme", true, k.isEmzirme());
        kontrol("mama miktarı", 120.0, k.mamaMiktari());

        List<Kayit> bosluk = KayitDeposu.metniCoz("   ");
        kontrol("boş metin", 0, bosluk.size());
    }

    static void depoTuruTesti() throws Exception {
        System.out.println("[Depo kaydet/yükle]");
        Path gecici = Files.createTempDirectory("bebek-deneme");
        KayitDeposu depo = new KayitDeposu(gecici);
        depo.yukle();
        kontrol("boş başlangıç", 0, depo.sayi());

        Kayit eski = new Kayit();
        eski.setTarih(LocalDate.of(2025, 8, 20));
        eski.setSaat(LocalTime.of(9, 0));
        eski.setNot("Türkçe karakter: şğüöçİI");
        eski.setCis(true);
        depo.ekle(eski);

        Kayit yeni = new Kayit();
        yeni.setTarih(LocalDate.of(2025, 8, 25));
        yeni.setSaat(LocalTime.of(23, 45));
        yeni.setMama(true);
        yeni.setMamaNotu("150 ml");
        depo.ekle(yeni);

        kontrol("dosya oluştu", true, Files.exists(depo.getDosya()));

        KayitDeposu tekrar = new KayitDeposu(gecici);
        tekrar.yukle();
        kontrol("hata yok", null, tekrar.getSonHata());
        kontrol("kayıt sayısı", 2, tekrar.sayi());
        kontrol("sıralama yeniden eskiye", LocalDate.of(2025, 8, 25),
                tekrar.getKayitlar().get(0).getTarih());
        kontrol("Türkçe karakter korundu", "Türkçe karakter: şğüöçİI",
                tekrar.getKayitlar().get(1).getNot());
        kontrol("id korundu", eski.getId(), tekrar.getKayitlar().get(1).getId());
        kontrol("mama toplamı", 150.0, KayitDeposu.mamaToplami(tekrar.getKayitlar()));
        kontrol("günün kayıtları", 1,
                tekrar.gununKayitlari(LocalDate.of(2025, 8, 20)).size());

        // yedek dosyası ikinci yazmadan sonra oluşmalı
        kontrol("yedek dosyası", true,
                Files.exists(gecici.resolve("kayitlar.json.bak")));
        // geçici dosya bırakılmamalı
        kontrol("geçici dosya temiz", false,
                Files.exists(gecici.resolve("kayitlar.json.tmp")));

        // bozuk dosya uygulamayı çökertmemeli
        Files.write(gecici.resolve("kayitlar.json"),
                "{bozuk".getBytes(StandardCharsets.UTF_8));
        KayitDeposu bozuk = new KayitDeposu(gecici);
        bozuk.yukle();
        kontrol("bozuk dosya yakalandı", true, bozuk.getSonHata() != null);
        kontrol("bozuk dosyada boş liste", 0, bozuk.sayi());
    }

    static void csvTesti() throws Exception {
        System.out.println("[CSV]");
        Path gecici = Files.createTempDirectory("bebek-csv");
        Path hedef = gecici.resolve("cikti.csv");

        Kayit k = new Kayit();
        k.setTarih(LocalDate.of(2025, 8, 26));
        k.setSaat(LocalTime.of(7, 5));
        k.setCis(true);
        k.setMama(true);
        k.setMamaNotu("120 ml");
        k.setNot("Noktalı; virgül ve \"tırnak\"");
        DisaAktarim.csvYaz(hedef, List.of(k));

        byte[] bayt = Files.readAllBytes(hedef);
        kontrol("BOM var", true,
                bayt.length > 3 && (bayt[0] & 0xFF) == 0xEF
                        && (bayt[1] & 0xFF) == 0xBB && (bayt[2] & 0xFF) == 0xBF);
        String metin = new String(bayt, StandardCharsets.UTF_8);
        String[] satirlar = metin.split("\r\n");
        kontrol("satır sayısı", 2, satirlar.length);
        kontrol("başlık", true, satirlar[0].contains("Sağ Meme"));
        kontrol("tarih biçimi", true, satirlar[1].startsWith("26.08.2025;07:05;"));
        kontrol("evet/hayir", true, satirlar[1].contains(";Evet;"));
        kontrol("miktar sütunu", true, satirlar[1].contains(";120;"));
        kontrol("tırnak kaçışı", true,
                satirlar[1].contains("\"Noktalı; virgül ve \"\"tırnak\"\"\""));

        // JSON gidiş-dönüş
        Path jsonDosya = gecici.resolve("yedek.json");
        DisaAktarim.jsonYaz(jsonDosya, List.of(k));
        List<Kayit> geri = DisaAktarim.jsonOku(jsonDosya);
        kontrol("json geri okuma", 1, geri.size());
        kontrol("json not korundu", k.getNot(), geri.get(0).getNot());
        kontrol("json saat korundu", LocalTime.of(7, 5), geri.get(0).getSaat());
    }

    static void icoTesti() throws Exception {
        System.out.println("[Simge]");
        byte[] ico = com.akansel.bebektakip.arac.IkonUret.icoUret();
        kontrol("ico başlığı", 0, (int) ico[0] | (int) ico[1]);
        kontrol("ico türü", 1, (ico[2] & 0xFF) | ((ico[3] & 0xFF) << 8));
        int adet = (ico[4] & 0xFF) | ((ico[5] & 0xFF) << 8);
        kontrol("resim sayısı", 8, adet);
        kontrol("dosya boyutu makul", true, ico.length > 10_000 && ico.length < 400_000);

        // her girdinin ofset + uzunluğu dosya sınırları içinde kalmalı
        boolean tutarli = true;
        for (int i = 0; i < adet; i++) {
            int taban = 6 + i * 16;
            int uzunluk = oku4(ico, taban + 8);
            int ofset = oku4(ico, taban + 12);
            if (ofset + uzunluk > ico.length || uzunluk <= 0) {
                tutarli = false;
            }
        }
        kontrol("girdi ofsetleri tutarlı", true, tutarli);
    }

    static void uykuTesti() {
        System.out.println("[Uyku]");
        UykuKayit u = new UykuKayit();
        u.setTarih(LocalDate.of(2026, 8, 20));
        u.setBaslangic(LocalTime.of(13, 0));
        u.setBitis(LocalTime.of(14, 30));
        u.setNot("Öğle uykusu");
        kontrol("uyku süresi", 90L, u.sureDakika());

        u.setBaslangic(LocalTime.of(23, 30));
        u.setBitis(LocalTime.of(6, 15));
        kontrol("gece yarısını aşan süre", 405L, u.sureDakika());

        UykuKayit geri = UykuKayit.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(u.jsonaCevir())));
        kontrol("uyku başlangıcı korundu", LocalTime.of(23, 30), geri.getBaslangic());
        kontrol("uyku bitişi korundu", LocalTime.of(6, 15), geri.getBitis());
        kontrol("uyku notu korundu", "Öğle uykusu", geri.getNot());

        UykuKayit acik = new UykuKayit();
        kontrol("açık uykuda süre 0", 0L, acik.sureDakika());
        kontrol("açık uyku sürüyor", true, acik.devamEdiyor());
        UykuKayit acikGeri = UykuKayit.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(acik.jsonaCevir())));
        kontrol("açık uyku korunarak okundu", true, acikGeri.devamEdiyor());
    }

    static void buyumeTesti() {
        System.out.println("[Büyüme]");
        BuyumeKayit b = new BuyumeKayit();
        b.setTarih(LocalDate.of(2026, 8, 25));
        b.setKilo(4.2);
        b.setBoy(56);
        b.setBasCevresi(38.5);
        b.setNot("2. ay kontrolü");
        BuyumeKayit geri = BuyumeKayit.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(b.jsonaCevir())));
        kontrol("kilo korundu", 4.2, geri.getKilo());
        kontrol("boy korundu", 56.0, geri.getBoy());
        kontrol("baş çevresi korundu", 38.5, geri.getBasCevresi());
        kontrol("ölçüm notu korundu", "2. ay kontrolü", geri.getNot());
        kontrol("ölçüm tarihi korundu", LocalDate.of(2026, 8, 25), geri.getTarih());

        // elle yazılmış yedeklerdeki virgüllü değerler de okunmalı
        java.util.Map<?, ?> elle = (java.util.Map<?, ?>) Json.oku(
                "{\"tarih\":\"2026-08-01\",\"kilo\":\"4,2\"}");
        kontrol("virgüllü kilo çözüldü", 4.2, BuyumeKayit.jsondan(elle).getKilo());
    }

    static void hatirlaticiTesti() {
        System.out.println("[Hatırlatıcı]");
        Hatirlatici h = new Hatirlatici();
        h.setTarih(LocalDate.of(2026, 9, 15));
        h.setSaat(LocalTime.of(10, 30));
        h.setBaslik("KKK aşısı");
        h.setTamamlandi(true);
        Hatirlatici geri = Hatirlatici.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(h.jsonaCevir())));
        kontrol("hatırlatıcı başlığı korundu", "KKK aşısı", geri.getBaslik());
        kontrol("tamamlandı korundu", true, geri.isTamamlandi());
        kontrol("hatırlatıcı saati korundu", LocalTime.of(10, 30), geri.getSaat());

        java.util.Map<?, ?> elle = (java.util.Map<?, ?>) Json.oku(
                "{\"baslik\":\"D vitamini\",\"tamamlandi\":\"1\"}");
        kontrol("metin 1 doğru sayıldı", true, Hatirlatici.jsondan(elle).isTamamlandi());
        kontrol("varsayılan tamamlanmadı", false, new Hatirlatici().isTamamlandi());
    }

    static void ilacTesti() {
        System.out.println("[Vitamin ve ilaç]");
        IlacKayit i = new IlacKayit();
        i.setTarih(LocalDate.of(2026, 8, 28));
        i.setSaat(LocalTime.of(8, 15));
        i.setAd("D vitamini");
        i.setDoz("3 damla");
        i.setNot("Kahvaltıdan önce");
        IlacKayit geri = IlacKayit.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(i.jsonaCevir())));
        kontrol("ilaç adı korundu", "D vitamini", geri.getAd());
        kontrol("doz korundu", "3 damla", geri.getDoz());
        kontrol("ilaç saati korundu", LocalTime.of(8, 15), geri.getSaat());
        kontrol("ilaç notu korundu", "Kahvaltıdan önce", geri.getNot());
        kontrol("ilaç tarihi korundu", LocalDate.of(2026, 8, 28), geri.getTarih());
    }

    static void depoCokluTesti() throws Exception {
        System.out.println("[Depo - uyku/büyüme/hatırlatıcı]");
        Path gecici = Files.createTempDirectory("bebek-coklu");
        KayitDeposu depo = new KayitDeposu(gecici);
        depo.yukle();

        Kayit beslenme = new Kayit();
        beslenme.setNot("beslenme kaydı");
        depo.ekle(beslenme);

        UykuKayit u = new UykuKayit();
        u.setTarih(LocalDate.of(2026, 8, 29));
        u.setBaslangic(LocalTime.of(13, 0));
        u.setBitis(LocalTime.of(14, 30));
        depo.ekleUyku(u);

        BuyumeKayit b = new BuyumeKayit();
        b.setTarih(LocalDate.of(2026, 8, 25));
        b.setKilo(4.2);
        b.setBoy(56);
        depo.ekleBuyume(b);

        Hatirlatici h = new Hatirlatici();
        h.setBaslik("Aşı");
        depo.ekleHatirlatici(h);
        Hatirlatici h2 = new Hatirlatici();
        h2.setBaslik("Kontrol");
        h2.setTamamlandi(true);
        depo.ekleHatirlatici(h2);

        IlacKayit ilac = new IlacKayit();
        ilac.setAd("D vitamini");
        ilac.setDoz("3 damla");
        depo.ekleIlac(ilac);

        kontrol("uyku dosyası oluştu", true,
                Files.exists(gecici.resolve("uykular.json")));
        kontrol("büyüme dosyası oluştu", true,
                Files.exists(gecici.resolve("buyumeler.json")));
        kontrol("hatırlatıcı dosyası oluştu", true,
                Files.exists(gecici.resolve("hatirlaticilar.json")));
        kontrol("ilaç dosyası oluştu", true,
                Files.exists(gecici.resolve("ilaclar.json")));

        KayitDeposu tekrar = new KayitDeposu(gecici);
        tekrar.yukle();
        kontrol("uyku geri okundu", 1, tekrar.getUykular().size());
        kontrol("ölçüm geri okundu", 1, tekrar.getBuyumeler().size());
        kontrol("hatırlatıcı geri okundu", 2, tekrar.getHatirlaticilar().size());
        kontrol("ilaç geri okundu", "D vitamini", tekrar.getIlaclar().get(0).getAd());
        kontrol("bekleyen hatırlatıcı", 1, tekrar.bekleyenHatirlatici());
        kontrol("günün uyku süresi", 90L,
                tekrar.gununUykuSuresi(LocalDate.of(2026, 8, 29)));
        kontrol("son ölçüm kilosu", 4.2, tekrar.sonBuyume().getKilo());

        // bozuk uyku dosyası öbür verileri götürmemeli
        Files.write(gecici.resolve("uykular.json"),
                "{bozuk".getBytes(StandardCharsets.UTF_8));
        KayitDeposu bozuk = new KayitDeposu(gecici);
        bozuk.yukle();
        kontrol("bozuk uyku dosyası yakalandı", true, bozuk.getSonHata() != null);
        kontrol("bozuk uykuda beslenme sağlam", 1, bozuk.sayi());
        kontrol("bozuk uykuda boş uyku listesi", 0, bozuk.getUykular().size());
    }

    static int oku4(byte[] b, int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8)
                | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24);
    }
}
