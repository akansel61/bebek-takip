import com.akansel.bebektakip.depo.DisaAktarim;
import com.akansel.bebektakip.depo.Json;
import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.rapor.Donem;
import com.akansel.bebektakip.rapor.ExcelYazici;
import com.akansel.bebektakip.rapor.PdfYazici;
import com.akansel.bebektakip.rapor.RaporDizici;
import com.akansel.bebektakip.rapor.RaporVerisi;
import com.akansel.bebektakip.rapor.RaporYazisi;
import com.akansel.bebektakip.rapor.Sayfa;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        atesTesti();
        ilacTuruTesti();
        depoAtesTesti();
        donemTesti();
        raporVerisiTesti();
        raporSayfaTesti();
        pdfTesti();
        excelTesti();

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

    static void atesTesti() {
        System.out.println("[Ateş]");
        kontrol("girilmemiş", AtesKayit.Seviye.YOK, AtesKayit.seviye(0));
        kontrol("37,4 normal", AtesKayit.Seviye.NORMAL, AtesKayit.seviye(37.4));
        kontrol("37,5 hafif", AtesKayit.Seviye.HAFIF, AtesKayit.seviye(37.5));
        kontrol("37,9 hafif", AtesKayit.Seviye.HAFIF, AtesKayit.seviye(37.9));
        kontrol("38 ateş", AtesKayit.Seviye.ATES, AtesKayit.seviye(38.0));
        kontrol("38,9 ateş", AtesKayit.Seviye.ATES, AtesKayit.seviye(38.9));
        kontrol("39 yüksek", AtesKayit.Seviye.YUKSEK, AtesKayit.seviye(39.0));
        kontrol("ateşli sayılır", true, AtesKayit.Seviye.ATES.atesli());
        kontrol("hafif ateşli sayılmaz", false, AtesKayit.Seviye.HAFIF.atesli());

        kontrol("virgüllü derece", 38.4, AtesKayit.dereceCoz("38,4"));
        kontrol("birimli derece", 38.4, AtesKayit.dereceCoz("38.4 °C"));
        kontrol("tek ondalığa yuvarlanır", 38.5, AtesKayit.dereceCoz("38,45"));
        kontrol("aralık dışı", 0.0, AtesKayit.dereceCoz("100"));
        kontrol("sayı değil", 0.0, AtesKayit.dereceCoz("yüksek"));

        AtesKayit a = new AtesKayit();
        a.setTarih(LocalDate.of(2026, 10, 19));
        a.setSaat(LocalTime.of(3, 40));
        a.setDerece(38.64);
        a.setOlcumYeri("Kulak");
        a.setBelirtiler(List.of(" Öksürük", "Kusma", "Öksürük", ""));
        a.setNot("Ateş düşürücü verildi");
        kontrol("derece yuvarlandı", 38.6, a.getDerece());
        kontrol("belirtiler ayıklandı", List.of("Öksürük", "Kusma"), a.getBelirtiler());
        kontrol("belirti metni", "Öksürük, Kusma", a.belirtiMetni());
        kontrol("varsayılan ölçüm yeri", "Koltuk altı", new AtesKayit().getOlcumYeri());

        AtesKayit geri = AtesKayit.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(a.jsonaCevir())));
        kontrol("ateş id korundu", a.getId(), geri.getId());
        kontrol("ateş derecesi korundu", 38.6, geri.getDerece());
        kontrol("ateş saati korundu", LocalTime.of(3, 40), geri.getSaat());
        kontrol("ölçüm yeri korundu", "Kulak", geri.getOlcumYeri());
        kontrol("belirtiler korundu", List.of("Öksürük", "Kusma"), geri.getBelirtiler());
        kontrol("ateş notu korundu", "Ateş düşürücü verildi", geri.getNot());

        java.util.Map<?, ?> elle = (java.util.Map<?, ?>) Json.oku(
                "{\"derece\":\"38,2\",\"belirtiler\":\"İshal, Kusma\"}");
        AtesKayit elleGeri = AtesKayit.jsondan(elle);
        kontrol("metin derece çözüldü", 38.2, elleGeri.getDerece());
        kontrol("virgüllü belirtiler", List.of("İshal", "Kusma"), elleGeri.getBelirtiler());
    }

    static void ilacTuruTesti() {
        System.out.println("[Vitamin / ilaç türü]");
        kontrol("D vitamini vitamin", IlacKayit.TUR_VITAMIN, IlacKayit.turTahmin("D vitamini"));
        kontrol("demir vitamin sayılır", IlacKayit.TUR_VITAMIN,
                IlacKayit.turTahmin("Demir damlası"));
        kontrol("büyük harfli ad", IlacKayit.TUR_VITAMIN, IlacKayit.turTahmin("DEVİT-3"));
        kontrol("şurup ilaç", IlacKayit.TUR_ILAC, IlacKayit.turTahmin("Parasetamol şurup"));

        java.util.Map<?, ?> eski = (java.util.Map<?, ?>) Json.oku(
                "{\"ad\":\"Antibiyotik\",\"doz\":\"5 ml\"}");
        kontrol("eski kayıtta tür tahmini", false, IlacKayit.jsondan(eski).isVitamin());

        IlacKayit i = new IlacKayit();
        i.setAd("D vitamini");
        i.setTur(IlacKayit.TUR_ILAC);
        IlacKayit geri = IlacKayit.jsondan(
                (java.util.Map<?, ?>) Json.oku(Json.yaz(i.jsonaCevir())));
        kontrol("açık tür tahmini ezer", IlacKayit.TUR_ILAC, geri.getTur());
        kontrol("tür adı", "İlaç", geri.turAdi());
        i.setTur("bilinmeyen");
        kontrol("bilinmeyen tür vitamin", true, i.isVitamin());
    }

    static void depoAtesTesti() throws Exception {
        System.out.println("[Depo - ateş]");
        Path gecici = Files.createTempDirectory("bebek-ates");
        KayitDeposu depo = new KayitDeposu(gecici);
        depo.yukle();
        LocalDateTime simdi = LocalDateTime.of(2026, 10, 20, 12, 0);

        AtesKayit eski = new AtesKayit();
        eski.setTarih(LocalDate.of(2026, 10, 18));
        eski.setSaat(LocalTime.of(9, 0));
        eski.setDerece(39.2);
        depo.ekleAtes(eski);
        AtesKayit yeni = new AtesKayit();
        yeni.setTarih(LocalDate.of(2026, 10, 20));
        yeni.setSaat(LocalTime.of(8, 30));
        yeni.setDerece(37.8);
        yeni.setBelirtiler(List.of("Öksürük"));
        depo.ekleAtes(yeni);
        AtesKayit bos = new AtesKayit();
        bos.setTarih(LocalDate.of(2026, 10, 20));
        bos.setSaat(LocalTime.of(10, 0));
        depo.ekleAtes(bos);
        depo.sirala();

        kontrol("ateş dosyası oluştu", true, Files.exists(gecici.resolve("atesler.json")));
        KayitDeposu tekrar = new KayitDeposu(gecici);
        tekrar.yukle();
        kontrol("ateş geri okundu", 3, tekrar.getAtesler().size());
        kontrol("en yeni üstte", LocalTime.of(10, 0), tekrar.getAtesler().get(0).getSaat());
        kontrol("son 24 saat (boş ölçüm atlanır)", 37.8,
                tekrar.sonAtes(simdi, 24).getDerece());
        kontrol("son 1 saatte ölçüm yok", null, tekrar.sonAtes(simdi, 1));

        Path yedek = gecici.resolve("yedek.json");
        DisaAktarim.yedekYaz(yedek, tekrar);
        String metin = DisaAktarim.metinOku(yedek);
        kontrol("yedekte ateş", 3, KayitDeposu.metniCozAtes(metin).size());

        Files.write(gecici.resolve("atesler.json"), "{bozuk".getBytes(StandardCharsets.UTF_8));
        KayitDeposu bozuk = new KayitDeposu(gecici);
        bozuk.yukle();
        kontrol("bozuk ateş dosyası yakalandı", true,
                bozuk.getSonHata() != null && bozuk.getSonHata().contains("Ateş"));
    }

    static void donemTesti() {
        System.out.println("[Rapor dönemi]");
        Donem h = Donem.haftalik(LocalDate.of(2026, 10, 21));
        kontrol("hafta pazartesi başlar", LocalDate.of(2026, 10, 19), h.getBaslangic());
        kontrol("hafta pazar biter", LocalDate.of(2026, 10, 25), h.getBitis());
        kontrol("haftalık başlık", "19 – 25 Ekim 2026", h.aralikMetni());
        kontrol("ay aşan hafta", "28 Eylül – 4 Ekim 2026",
                Donem.haftalik(LocalDate.of(2026, 9, 30)).aralikMetni());
        kontrol("yıl aşan hafta", "28 Aralık 2026 – 3 Ocak 2027",
                Donem.haftalik(LocalDate.of(2027, 1, 1)).aralikMetni());
        kontrol("önceki hafta", LocalDate.of(2026, 10, 12), h.onceki().getBaslangic());

        Donem ay = Donem.aylik(LocalDate.of(2026, 2, 10));
        kontrol("şubat sonu", LocalDate.of(2026, 2, 28), ay.getBitis());
        kontrol("aylık başlık", "Şubat 2026", ay.aralikMetni());
        kontrol("sonraki ay", LocalDate.of(2026, 3, 31), ay.sonraki().getBitis());
        kontrol("aylık dosya eki", "2026-02", ay.dosyaEki());

        Donem gun = Donem.gunluk(LocalDate.of(2026, 10, 20));
        kontrol("günlük başlık", "20 Ekim 2026, Salı", gun.aralikMetni());
        kontrol("günlük rapor adı", "Günlük Rapor", gun.raporAdi());
        kontrol("ertesi gün", LocalDate.of(2026, 10, 21), gun.sonraki().getBaslangic());

        Donem ozel = Donem.ozel(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1));
        kontrol("ters aralık düzeldi", LocalDate.of(2026, 10, 1), ozel.getBaslangic());
        kontrol("özel gün sayısı", 10, ozel.gunSayisi());
        kontrol("özel önceki kayar", LocalDate.of(2026, 9, 21), ozel.onceki().getBaslangic());
        kontrol("özel başlık", "01.10.2026 – 10.10.2026", ozel.aralikMetni());
        kontrol("içerir", true, ozel.icerir(LocalDate.of(2026, 10, 10)));
        kontrol("içermez", false, ozel.icerir(LocalDate.of(2026, 10, 11)));
    }

    /** Raporun hesaplarını sınamak için bilinen küçük bir veri kümesi. */
    static KayitDeposu raporDeposu() throws Exception {
        KayitDeposu depo = new KayitDeposu(Files.createTempDirectory("bebek-rapor"));
        depo.yukle();
        Object[][] mamalar = {
                {19, 7, "120 ml", true}, {19, 11, "90", true}, {20, 8, "150 ml", true},
                {20, 9, "100", false}, {12, 9, "200", true},
        };
        for (Object[] m : mamalar) {
            Kayit k = new Kayit();
            k.setTarih(LocalDate.of(2026, 10, (Integer) m[0]));
            k.setSaat(LocalTime.of((Integer) m[1], 0));
            k.setMamaNotu((String) m[2]);
            k.setMama((Boolean) m[3]);
            depo.getKayitlar().add(k);
        }
        String[][] ilaclar = {
                {"19", "D vitamini", "vitamin"}, {"20", "d vitamini", "vitamin"},
                {"20", "Parasetamol", "ilac"}, {"21", "D vitamini", "vitamin"},
        };
        for (String[] i : ilaclar) {
            IlacKayit k = new IlacKayit();
            k.setTarih(LocalDate.of(2026, 10, Integer.parseInt(i[0])));
            k.setAd(i[1]);
            k.setDoz("3 damla");
            k.setTur(i[2]);
            depo.getIlaclar().add(k);
        }
        double[][] atesler = {{19, 38.2}, {19, 37.6}, {20, 39.1}, {20, 0}};
        int saat = 6;
        for (double[] a : atesler) {
            AtesKayit k = new AtesKayit();
            k.setTarih(LocalDate.of(2026, 10, (int) a[0]));
            k.setSaat(LocalTime.of(saat++, 0));
            k.setDerece(a[1]);
            if (a[1] >= 38) {
                k.setBelirtiler(List.of("Öksürük", "Kusma"));
            } else if (a[1] > 0) {
                k.setBelirtiler(List.of("Öksürük"));
            }
            depo.getAtesler().add(k);
        }
        depo.sirala();
        return depo;
    }

    static final LocalDateTime RAPOR_ZAMANI = LocalDateTime.of(2026, 10, 20, 18, 0);

    static void raporVerisiTesti() throws Exception {
        System.out.println("[Rapor verisi]");
        KayitDeposu depo = raporDeposu();
        RaporVerisi v = RaporVerisi.hazirla(depo, Donem.haftalik(LocalDate.of(2026, 10, 20)),
                EnumSet.allOf(RaporVerisi.Bolum.class), RAPOR_ZAMANI);
        kontrol("mama işaretsiz ve dönem dışı atlandı", 3, v.getMamalar().size());
        kontrol("eskiden yeniye", LocalTime.of(7, 0), v.getMamalar().get(0).getSaat());
        kontrol("toplam mama", 360.0, v.toplamMama());
        kontrol("mama verilen gün", 2, v.mamaGunSayisi());
        kontrol("günlük ortalama", 180.0, v.gunlukOrtalamaMama());
        kontrol("gelecek günler kesildi", 2, v.getGunler().size());
        kontrol("gelecek kayıt alınmadı (21 Ekim)", 2, v.getVitaminler().size());
        kontrol("aynı ad tek satırda", 1, v.vitaminSayimi().size());
        kontrol("vitamin sayısı", 2, v.vitaminSayimi().get(0).adet);
        kontrol("ilaç ayrıldı", "Parasetamol", v.getIlaclar().get(0).getAd());
        kontrol("derecesiz ölçüm atlandı", 3, v.getAtesler().size());
        kontrol("en yüksek ateş", 39.1, v.enYuksekAtes().getDerece());
        kontrol("ateşli ölçüm", 2, v.atesliOlcumSayisi());
        kontrol("ateşli gün", 2, v.atesliGunSayisi());
        kontrol("en sık belirti", "Öksürük", v.belirtiSayimi().get(0).ad);
        kontrol("belirti adedi", 3, v.belirtiSayimi().get(0).adet);
        RaporVerisi.GunOzeti ilkGun = v.getGunler().get(0);
        kontrol("ilk gün pazartesi", LocalDate.of(2026, 10, 19), ilkGun.tarih);
        kontrol("ilk gün mama", 210.0, ilkGun.mamaMiktar);
        kontrol("ilk gün en yüksek", 38.2, ilkGun.enYuksekAtes);

        RaporVerisi gecmis = RaporVerisi.hazirla(depo, Donem.haftalik(LocalDate.of(2026, 10, 12)),
                EnumSet.of(RaporVerisi.Bolum.MAMA), RAPOR_ZAMANI);
        kontrol("geçmiş hafta yedi gün", 7, gecmis.getGunler().size());
        kontrol("geçmiş hafta mama", 200.0, gecmis.toplamMama());
        kontrol("seçilmeyen bölüm boş sayılır", false, gecmis.bos());
        RaporVerisi bos = RaporVerisi.hazirla(depo, Donem.gunluk(LocalDate.of(2026, 1, 1)),
                EnumSet.allOf(RaporVerisi.Bolum.class), RAPOR_ZAMANI);
        kontrol("kayıtsız dönem boş", true, bos.bos());
    }

    static void raporSayfaTesti() throws Exception {
        System.out.println("[Rapor sayfaları]");
        RaporYazisi yazi = RaporYazisi.varsayilan();
        KayitDeposu depo = raporDeposu();
        RaporVerisi v = RaporVerisi.hazirla(depo, Donem.haftalik(LocalDate.of(2026, 10, 20)),
                EnumSet.allOf(RaporVerisi.Bolum.class), RAPOR_ZAMANI);
        List<Sayfa> sayfalar = RaporDizici.diz(v, yazi, "imza satırı");
        String ilk = sayfalar.get(0).duzMetin();
        kontrol("başlık", true, ilk.contains("Haftalık Rapor"));
        kontrol("aralık", true, ilk.contains("19 – 25 Ekim 2026"));
        kontrol("en yüksek ateş kutusu", true, ilk.contains("39,1 °C"));
        kontrol("sayfa numarası", true, ilk.contains("Sayfa 1 / " + sayfalar.size()));
        kontrol("imza", true, ilk.contains("imza satırı"));

        // uzun tablo sayfalara bölünmeli, her sayfada başlık satırı tekrarlanmalı
        KayitDeposu kalabalik = new KayitDeposu(Files.createTempDirectory("bebek-kalabalik"));
        kalabalik.yukle();
        for (int g = 1; g <= 31; g++) {
            for (int s = 0; s < 8; s++) {
                Kayit k = new Kayit();
                k.setTarih(LocalDate.of(2026, 8, g));
                k.setSaat(LocalTime.of(s * 3, 0));
                k.setMama(true);
                k.setMamaNotu("100");
                k.setNot(s == 3 ? "Uzun bir not: bu satır sütuna sığmayıp alt satıra kaymalı "
                        + "ki satır yüksekliği doğru hesaplansın ve tablo bozulmasın." : "");
                kalabalik.getKayitlar().add(k);
            }
        }
        kalabalik.sirala();
        RaporVerisi ayVerisi = RaporVerisi.hazirla(kalabalik, Donem.aylik(LocalDate.of(2026, 8, 1)),
                EnumSet.of(RaporVerisi.Bolum.MAMA), RAPOR_ZAMANI);
        List<Sayfa> uzun = RaporDizici.diz(ayVerisi, yazi, "");
        kontrol("çok sayfa", true, uzun.size() > 3);
        boolean basliklar = true;
        boolean tasmaYok = true;
        for (int i = 1; i < uzun.size(); i++) {
            if (!uzun.get(i).duzMetin().contains("Miktar (ml)")) {
                basliklar = false;
            }
        }
        for (Sayfa s : uzun) {
            for (Sayfa.Oge o : s.getOgeler()) {
                if (o instanceof Sayfa.Yazi) {
                    Sayfa.Yazi y = (Sayfa.Yazi) o;
                    double sag = y.x + yazi.genislik(y.metin, y.boyut, y.kalin);
                    if (y.y > Sayfa.YUKSEKLIK - 20 || sag > Sayfa.GENISLIK - 39) {
                        tasmaYok = false;
                    }
                }
            }
        }
        kontrol("devam sayfalarında tablo başlığı", true, basliklar);
        // tablo başlığından sonraki ilk hücre her sayfada bir tarih olmalı
        boolean tarihVar = true;
        for (int i = 1; i < uzun.size(); i++) {
            List<String> yazilar = List.of(uzun.get(i).duzMetin().split("\n"));
            int baslik = yazilar.indexOf("Miktar (ml)");
            if (baslik >= 0 && !yazilar.get(baslik + 2).matches("\\d\\d\\.08\\.2026")) {
                tarihVar = false;
            }
        }
        kontrol("devam sayfasında gün tarihi", true, tarihVar);
        kontrol("yazılar sayfa dışına taşmıyor", true, tasmaYok);
        kontrol("son sayfa numarası", true, uzun.get(uzun.size() - 1).duzMetin()
                .contains("Sayfa " + uzun.size() + " / " + uzun.size()));
    }

    static void pdfTesti() throws Exception {
        System.out.println("[PDF]");
        RaporYazisi yazi = RaporYazisi.varsayilan();
        kontrol("gömülebilir yazı tipi bulundu (" + yazi.getAciklama() + ")", true,
                yazi.pdfYazilabilir());
        if (!yazi.pdfYazilabilir()) {
            return;
        }
        RaporVerisi v = RaporVerisi.hazirla(raporDeposu(),
                Donem.haftalik(LocalDate.of(2026, 10, 20)),
                EnumSet.allOf(RaporVerisi.Bolum.class), RAPOR_ZAMANI);
        List<Sayfa> sayfalar = RaporDizici.diz(v, yazi, "Bebek Takip");
        byte[] pdf = PdfYazici.uret(sayfalar, yazi, "Bebek Takip – Haftalık Rapor");
        String metin = new String(pdf, StandardCharsets.ISO_8859_1);
        kontrol("PDF başlığı", true, metin.startsWith("%PDF-1.4"));
        kontrol("PDF sonu", true, metin.endsWith("%%EOF\n"));
        kontrol("sayfa sayısı", sayfalar.size(), sayac(metin, "/Type /Page "));
        kontrol("gömülü yazı tipi", 2, sayac(metin, "/FontFile2"));
        kontrol("ToUnicode", 2, sayac(metin, "/ToUnicode"));

        // xref tablosundaki her konum gerçekten "N 0 obj" satırına denk gelmeli
        int startxref = Integer.parseInt(metin.substring(metin.lastIndexOf("startxref") + 10,
                metin.lastIndexOf("%%EOF")).trim());
        kontrol("startxref xref'i gösteriyor", true, metin.startsWith("xref", startxref));
        Matcher m = Pattern.compile("(\\d{10}) 00000 n").matcher(metin.substring(startxref));
        int no = 1;
        boolean tutarli = true;
        while (m.find()) {
            int konum = Integer.parseInt(m.group(1));
            if (!metin.startsWith(no + " 0 obj", konum)) {
                tutarli = false;
            }
            no++;
        }
        kontrol("xref konumları tutarlı", true, tutarli && no > 10);

        // Gömülü alt kümeler Java'nın kendisince de okunabilmeli. Alt kümede
        // karakter tablosu yok; Türkçe harflerin glifleri ToUnicode
        // tablosundan bulunup çizim verisi taşıdıkları ayrıca denetlenir.
        List<Font> yazilar = new java.util.ArrayList<>();
        List<Map<Integer, Integer>> eslemeler = new java.util.ArrayList<>();
        Matcher akis = Pattern.compile("<< /Length (\\d+) /Filter /FlateDecode[^>]*>>\nstream\n")
                .matcher(metin);
        while (akis.find()) {
            byte[] sikisik = new byte[Integer.parseInt(akis.group(1))];
            System.arraycopy(pdf, akis.end(), sikisik, 0, sikisik.length);
            byte[] acik = ac(sikisik);
            if (akis.group().contains("/Length1")) {
                yazilar.add(Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(acik)));
            } else {
                String cmap = new String(acik, StandardCharsets.ISO_8859_1);
                if (cmap.contains("beginbfchar")) {
                    Map<Integer, Integer> esleme = new HashMap<>();
                    Matcher satir = Pattern.compile("<([0-9A-F]{4})> <([0-9A-F]{4})>")
                            .matcher(cmap);
                    while (satir.find()) {
                        esleme.put(Integer.parseInt(satir.group(2), 16),
                                Integer.parseInt(satir.group(1), 16));
                    }
                    eslemeler.add(esleme);
                }
            }
        }
        kontrol("alt küme yazı tipleri okunuyor", 2, yazilar.size());
        kontrol("iki ToUnicode tablosu", 2, eslemeler.size());
        int denetlenen = 0;
        boolean cizimVar = true;
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
        for (int i = 0; i < Math.min(yazilar.size(), eslemeler.size()); i++) {
            Font f = yazilar.get(i).deriveFont(12f);
            for (char c : "şŞıİğĞüöç".toCharArray()) {
                Integer glif = eslemeler.get(i).get((int) c);
                if (glif == null) {
                    continue;
                }
                double en = f.createGlyphVector(frc, new int[]{glif}).getOutline()
                        .getBounds2D().getWidth();
                cizimVar &= en > 0;
                denetlenen++;
            }
        }
        kontrol("Türkçe harflerin glifleri gömülü (" + denetlenen + ")", true,
                cizimVar && denetlenen >= 6);
    }

    static byte[] ac(byte[] sikisik) throws Exception {
        Inflater ac = new Inflater();
        ac.setInput(sikisik);
        ByteArrayOutputStream cikti = new ByteArrayOutputStream();
        byte[] tampon = new byte[8192];
        while (!ac.finished()) {
            int n = ac.inflate(tampon);
            if (n == 0 && (ac.needsInput() || ac.needsDictionary())) {
                break;
            }
            cikti.write(tampon, 0, n);
        }
        ac.end();
        return cikti.toByteArray();
    }

    static void excelTesti() throws Exception {
        System.out.println("[Excel]");
        RaporVerisi v = RaporVerisi.hazirla(raporDeposu(),
                Donem.haftalik(LocalDate.of(2026, 10, 20)),
                EnumSet.allOf(RaporVerisi.Bolum.class), RAPOR_ZAMANI);
        byte[] xlsx = ExcelYazici.uret(v);
        Map<String, String> girdiler = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx),
                StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                zip.transferTo(b);
                girdiler.put(e.getName(), b.toString(StandardCharsets.UTF_8));
            }
        }
        kontrol("içerik türleri", true, girdiler.containsKey("[Content_Types].xml"));
        kontrol("çalışma kitabı", true, girdiler.containsKey("xl/workbook.xml"));
        kontrol("beş sayfa", true, girdiler.containsKey("xl/worksheets/sheet5.xml")
                && !girdiler.containsKey("xl/worksheets/sheet6.xml"));
        boolean iyiBicimli = true;
        for (Map.Entry<String, String> e : girdiler.entrySet()) {
            try {
                byte[] xml = e.getValue().getBytes(StandardCharsets.UTF_8);
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(xml));
            } catch (Exception hata) {
                iyiBicimli = false;
                System.out.println("  bozuk XML: " + e.getKey() + " " + hata.getMessage());
            }
        }
        kontrol("bütün XML'ler iyi biçimli", true, iyiBicimli);
        String kitap = girdiler.get("xl/workbook.xml");
        kontrol("Türkçe sayfa adları", true, kitap.contains("name=\"Özet\"")
                && kitap.contains("name=\"İlaç\"") && kitap.contains("name=\"Ateş\""));
        long seri = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), LocalDate.of(2026, 10, 19));
        kontrol("tarih Excel sayısı olarak", true,
                girdiler.get("xl/worksheets/sheet2.xml").contains("<v>" + seri + "</v>"));
        kontrol("toplam formülü", true, girdiler.get("xl/worksheets/sheet1.xml")
                .contains("<f>SUM(D"));
        kontrol("ateş derecesi sayı", true,
                girdiler.get("xl/worksheets/sheet5.xml").contains("<v>39.1</v>"));

        RaporVerisi yalnizAtes = RaporVerisi.hazirla(raporDeposu(),
                Donem.haftalik(LocalDate.of(2026, 10, 20)),
                EnumSet.of(RaporVerisi.Bolum.ATES), RAPOR_ZAMANI);
        byte[] kucuk = ExcelYazici.uret(yalnizAtes);
        int sayfa = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(kucuk))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.getName().startsWith("xl/worksheets/")) {
                    sayfa++;
                }
            }
        }
        kontrol("yalnız seçilen bölüm sayfası", 2, sayfa);
    }

    static int sayac(String metin, String aranan) {
        int n = 0;
        for (int i = metin.indexOf(aranan); i >= 0; i = metin.indexOf(aranan, i + 1)) {
            n++;
        }
        return n;
    }

    static int oku4(byte[] b, int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8)
                | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24);
    }
}
