package com.akansel.bebektakip.depo;

import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bütün kayıt türlerinin bellekteki listeleri ve disk üzerindeki JSON dosyaları.
 *
 * Her tür kendi dosyasında durur, %USERPROFILE%\.bebek-takip altında:
 *   beslenme/bez -> kayitlar.json      uyku          -> uykular.json
 *   büyüme       -> buyumeler.json     hatırlatıcı   -> hatirlaticilar.json
 *   vitamin/ilaç -> ilaclar.json
 *
 * Yazma atomik: önce .tmp dosyasına yazılır, sonra yerine taşınır. Her
 * başarılı yazmadan önce bir önceki sürüm .bak olarak saklanır.
 */
public class KayitDeposu {

    /** Dosya biçimi sürümü. */
    public static final int SURUM = 1;

    private final Path dizin;
    private final Path dosya;
    private final Path yedek;
    private final Path uykuDosya;
    private final Path uykuYedek;
    private final Path buyumeDosya;
    private final Path buyumeYedek;
    private final Path hatirlaticiDosya;
    private final Path hatirlaticiYedek;
    private final Path ilacDosya;
    private final Path ilacYedek;

    private final List<Kayit> kayitlar = new ArrayList<>();
    private final List<UykuKayit> uykular = new ArrayList<>();
    private final List<BuyumeKayit> buyumeler = new ArrayList<>();
    private final List<Hatirlatici> hatirlaticilar = new ArrayList<>();
    private final List<IlacKayit> ilaclar = new ArrayList<>();
    private final List<Runnable> dinleyiciler = new CopyOnWriteArrayList<>();

    private LocalDateTime sonKayitZamani;
    private String sonHata;

    public KayitDeposu() {
        this(varsayilanDizin());
    }

    public KayitDeposu(Path dizin) {
        this.dizin = dizin;
        this.dosya = dizin.resolve("kayitlar.json");
        this.yedek = dizin.resolve("kayitlar.json.bak");
        this.uykuDosya = dizin.resolve("uykular.json");
        this.uykuYedek = dizin.resolve("uykular.json.bak");
        this.buyumeDosya = dizin.resolve("buyumeler.json");
        this.buyumeYedek = dizin.resolve("buyumeler.json.bak");
        this.hatirlaticiDosya = dizin.resolve("hatirlaticilar.json");
        this.hatirlaticiYedek = dizin.resolve("hatirlaticilar.json.bak");
        this.ilacDosya = dizin.resolve("ilaclar.json");
        this.ilacYedek = dizin.resolve("ilaclar.json.bak");
    }

    public static Path varsayilanDizin() {
        return Paths.get(System.getProperty("user.home"), ".bebek-takip");
    }

    public Path getDosya() {
        return dosya;
    }

    public LocalDateTime getSonKayitZamani() {
        return sonKayitZamani;
    }

    /** Son okuma/yazma sırasında oluşan hata mesajı; yoksa null. */
    public String getSonHata() {
        return sonHata;
    }

    public void dinleyiciEkle(Runnable r) {
        dinleyiciler.add(r);
    }

    private void degistiBildir() {
        for (Runnable r : dinleyiciler) {
            r.run();
        }
    }

    /** Kopyası değil, listenin kendisi; arayüz tek iş parçacığında çalışıyor. */
    public List<Kayit> getKayitlar() {
        return kayitlar;
    }

    public List<UykuKayit> getUykular() {
        return uykular;
    }

    public List<BuyumeKayit> getBuyumeler() {
        return buyumeler;
    }

    public List<Hatirlatici> getHatirlaticilar() {
        return hatirlaticilar;
    }

    public List<IlacKayit> getIlaclar() {
        return ilaclar;
    }

    public int sayi() {
        return kayitlar.size();
    }

    public void ekle(Kayit k) {
        kayitlar.add(0, k);
        kaydet();
        degistiBildir();
    }

    public void sil(Kayit k) {
        if (kayitlar.remove(k)) {
            kaydet();
            degistiBildir();
        }
    }

    public void ekleUyku(UykuKayit u) {
        uykular.add(0, u);
        kaydetUyku();
        degistiBildir();
    }

    public void silUyku(UykuKayit u) {
        if (uykular.remove(u)) {
            kaydetUyku();
            degistiBildir();
        }
    }

    public void ekleBuyume(BuyumeKayit b) {
        buyumeler.add(0, b);
        kaydetBuyume();
        degistiBildir();
    }

    public void silBuyume(BuyumeKayit b) {
        if (buyumeler.remove(b)) {
            kaydetBuyume();
            degistiBildir();
        }
    }

    public void ekleHatirlatici(Hatirlatici h) {
        hatirlaticilar.add(0, h);
        sirala();
        kaydetHatirlatici();
        degistiBildir();
    }

    public void silHatirlatici(Hatirlatici h) {
        if (hatirlaticilar.remove(h)) {
            kaydetHatirlatici();
            degistiBildir();
        }
    }

    public void ekleIlac(IlacKayit i) {
        ilaclar.add(0, i);
        kaydetIlac();
        degistiBildir();
    }

    public void silIlac(IlacKayit i) {
        if (ilaclar.remove(i)) {
            kaydetIlac();
            degistiBildir();
        }
    }

    public void hepsiniSil() {
        kayitlar.clear();
        kaydet();
        degistiBildir();
    }

    /** Toplu değişiklikten sonra çağrılır: diske yaz, dinleyicileri uyar. */
    public void guncellendi() {
        kaydet();
        degistiBildir();
    }

    /** Her listeyi kendi doğal sırasına sokar (kayıtlar yeniden eskiye). */
    public void sirala() {
        kayitlar.sort(Comparator.comparing(Kayit::zaman).reversed());
        uykular.sort(Comparator.comparing(
                (UykuKayit u) -> u.getTarih().atTime(u.getBaslangic())).reversed());
        buyumeler.sort(Comparator.comparing(BuyumeKayit::getTarih).reversed());
        ilaclar.sort(Comparator.comparing(IlacKayit::zaman).reversed());
        // hatırlatıcılar ters değil: en yakın tarih en üstte dursun
        hatirlaticilar.sort(Comparator.comparing(Hatirlatici::zaman));
    }

    /** Bütün veri dosyalarını okur; olmayan dosya boş liste sayılır. */
    public void yukle() {
        sonHata = null;
        kayitlar.clear();
        uykular.clear();
        buyumeler.clear();
        hatirlaticilar.clear();
        ilaclar.clear();

        String beslenme = dosyaOku(dosya, "Kayıt dosyası okunamadı");
        if (beslenme != null) {
            try {
                kayitlar.addAll(metniCoz(beslenme));
            } catch (RuntimeException e) {
                hataEkle("Kayıt dosyası okunamadı: " + e.getMessage());
            }
        }
        String uyku = dosyaOku(uykuDosya, "Uyku dosyası okunamadı");
        if (uyku != null) {
            try {
                uykular.addAll(metniCozUyku(uyku));
            } catch (RuntimeException e) {
                hataEkle("Uyku dosyası okunamadı: " + e.getMessage());
            }
        }
        String buyume = dosyaOku(buyumeDosya, "Büyüme dosyası okunamadı");
        if (buyume != null) {
            try {
                buyumeler.addAll(metniCozBuyume(buyume));
            } catch (RuntimeException e) {
                hataEkle("Büyüme dosyası okunamadı: " + e.getMessage());
            }
        }
        String hatirlatma = dosyaOku(hatirlaticiDosya, "Hatırlatıcı dosyası okunamadı");
        if (hatirlatma != null) {
            try {
                hatirlaticilar.addAll(metniCozHatirlatici(hatirlatma));
            } catch (RuntimeException e) {
                hataEkle("Hatırlatıcı dosyası okunamadı: " + e.getMessage());
            }
        }
        String ilac = dosyaOku(ilacDosya, "İlaç dosyası okunamadı");
        if (ilac != null) {
            try {
                ilaclar.addAll(metniCozIlac(ilac));
            } catch (RuntimeException e) {
                hataEkle("İlaç dosyası okunamadı: " + e.getMessage());
            }
        }
        sirala();
    }

    /** Dosya yoksa null döner; okuma hatasında sonHata doldurulur. */
    private String dosyaOku(Path p, String hataBasligi) {
        if (!Files.exists(p)) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            hataEkle(hataBasligi + ": " + e.getMessage());
            return null;
        }
    }

    private void hataEkle(String mesaj) {
        sonHata = sonHata == null ? mesaj : sonHata + "\n" + mesaj;
    }

    /**
     * JSON metnini beslenme kaydı listesine çevirir. Hem bu uygulamanın
     * sarmalanmış biçimini hem de tarayıcı sürümünün düz dizisini kabul eder.
     */
    public static List<Kayit> metniCoz(String metin) {
        List<Kayit> sonuc = new ArrayList<>();
        String temiz = temizle(metin);
        if (temiz.isEmpty()) {
            return sonuc;
        }
        Object kok = Json.oku(temiz);
        List<?> liste = null;
        if (kok instanceof List) {
            liste = (List<?>) kok;
        } else if (kok instanceof Map) {
            Object k = ((Map<?, ?>) kok).get("kayitlar");
            if (k == null) {
                k = ((Map<?, ?>) kok).get("records");
            }
            if (k instanceof List) {
                liste = (List<?>) k;
            }
        }
        if (liste == null) {
            throw new Json.JsonHatasi("Beklenen biçimde kayıt listesi bulunamadı");
        }
        for (Object o : liste) {
            if (o instanceof Map) {
                sonuc.add(Kayit.jsondan((Map<?, ?>) o));
            }
        }
        return sonuc;
    }

    /** "uykular" dizisini çözer; düz dizi kabul edilmez, o biçim beslenmeye aittir. */
    public static List<UykuKayit> metniCozUyku(String metin) {
        List<UykuKayit> sonuc = new ArrayList<>();
        for (Object o : adliListe(metin, "uykular")) {
            if (o instanceof Map) {
                sonuc.add(UykuKayit.jsondan((Map<?, ?>) o));
            }
        }
        return sonuc;
    }

    public static List<BuyumeKayit> metniCozBuyume(String metin) {
        List<BuyumeKayit> sonuc = new ArrayList<>();
        for (Object o : adliListe(metin, "buyumeler")) {
            if (o instanceof Map) {
                sonuc.add(BuyumeKayit.jsondan((Map<?, ?>) o));
            }
        }
        return sonuc;
    }

    public static List<Hatirlatici> metniCozHatirlatici(String metin) {
        List<Hatirlatici> sonuc = new ArrayList<>();
        for (Object o : adliListe(metin, "hatirlaticilar")) {
            if (o instanceof Map) {
                sonuc.add(Hatirlatici.jsondan((Map<?, ?>) o));
            }
        }
        return sonuc;
    }

    public static List<IlacKayit> metniCozIlac(String metin) {
        List<IlacKayit> sonuc = new ArrayList<>();
        for (Object o : adliListe(metin, "ilaclar")) {
            if (o instanceof Map) {
                sonuc.add(IlacKayit.jsondan((Map<?, ?>) o));
            }
        }
        return sonuc;
    }

    private static List<?> adliListe(String metin, String anahtar) {
        String temiz = temizle(metin);
        if (temiz.isEmpty()) {
            return new ArrayList<>();
        }
        Object kok = Json.oku(temiz);
        if (kok instanceof Map) {
            Object k = ((Map<?, ?>) kok).get(anahtar);
            if (k instanceof List) {
                return (List<?>) k;
            }
        }
        return new ArrayList<>();
    }

    /** Baştaki boşluğu ve UTF-8 BOM işaretini atar. */
    private static String temizle(String metin) {
        String temiz = metin == null ? "" : metin.trim();
        if (!temiz.isEmpty() && temiz.charAt(0) == 0xFEFF) {
            temiz = temiz.substring(1).trim();
        }
        return temiz;
    }

    /** Beslenme kayıtlarını diske yazar; başarısız olursa sonHata doldurulur. */
    public boolean kaydet() {
        List<Map<String, Object>> veriler = new ArrayList<>(kayitlar.size());
        for (Kayit k : kayitlar) {
            veriler.add(k.jsonaCevir());
        }
        return yaz(dosya, yedek, "kayitlar", veriler);
    }

    public boolean kaydetUyku() {
        List<Map<String, Object>> veriler = new ArrayList<>(uykular.size());
        for (UykuKayit u : uykular) {
            veriler.add(u.jsonaCevir());
        }
        return yaz(uykuDosya, uykuYedek, "uykular", veriler);
    }

    public boolean kaydetBuyume() {
        List<Map<String, Object>> veriler = new ArrayList<>(buyumeler.size());
        for (BuyumeKayit b : buyumeler) {
            veriler.add(b.jsonaCevir());
        }
        return yaz(buyumeDosya, buyumeYedek, "buyumeler", veriler);
    }

    public boolean kaydetHatirlatici() {
        List<Map<String, Object>> veriler = new ArrayList<>(hatirlaticilar.size());
        for (Hatirlatici h : hatirlaticilar) {
            veriler.add(h.jsonaCevir());
        }
        return yaz(hatirlaticiDosya, hatirlaticiYedek, "hatirlaticilar", veriler);
    }

    public boolean kaydetIlac() {
        List<Map<String, Object>> veriler = new ArrayList<>(ilaclar.size());
        for (IlacKayit i : ilaclar) {
            veriler.add(i.jsonaCevir());
        }
        return yaz(ilacDosya, ilacYedek, "ilaclar", veriler);
    }

    private boolean yaz(Path hedef, Path yedegi, String anahtar,
                        List<Map<String, Object>> veriler) {
        sonHata = null;
        try {
            Files.createDirectories(dizin);
            Map<String, Object> kok = new LinkedHashMap<>();
            kok.put("surum", SURUM);
            kok.put("uygulama", "Bebek Takip");
            kok.put("kaydedilme", LocalDateTime.now().withNano(0).toString());
            kok.put(anahtar, veriler);

            Path gecici = dizin.resolve(hedef.getFileName() + ".tmp");
            Files.write(gecici, Json.yaz(kok).getBytes(StandardCharsets.UTF_8));

            if (Files.exists(hedef)) {
                try {
                    Files.copy(hedef, yedegi, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException yoksay) {
                    // yedek alınamadıysa asıl yazmayı engelleme
                }
            }
            try {
                Files.move(gecici, hedef,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(gecici, hedef, StandardCopyOption.REPLACE_EXISTING);
            }
            sonKayitZamani = LocalDateTime.now();
            return true;
        } catch (IOException | RuntimeException e) {
            sonHata = "Kayıt dosyası yazılamadı: " + e.getMessage();
            return false;
        }
    }

    public List<Kayit> gununKayitlari(LocalDate gun) {
        List<Kayit> sonuc = new ArrayList<>();
        for (Kayit k : kayitlar) {
            if (k.getTarih().equals(gun)) {
                sonuc.add(k);
            }
        }
        return sonuc;
    }

    public List<UykuKayit> gununUykulari(LocalDate gun) {
        List<UykuKayit> sonuc = new ArrayList<>();
        for (UykuKayit u : uykular) {
            if (u.getTarih().equals(gun)) {
                sonuc.add(u);
            }
        }
        return sonuc;
    }

    /** Verilen günün toplam uyku süresi, dakika olarak. */
    public long gununUykuSuresi(LocalDate gun) {
        long toplam = 0;
        for (UykuKayit u : gununUykulari(gun)) {
            toplam += u.sureDakika();
        }
        return toplam;
    }

    /** Tamamlanmamış hatırlatıcı sayısı. */
    public int bekleyenHatirlatici() {
        int n = 0;
        for (Hatirlatici h : hatirlaticilar) {
            if (!h.isTamamlandi()) {
                n++;
            }
        }
        return n;
    }

    /** En yeni büyüme ölçümü; hiç ölçüm yoksa null. */
    public BuyumeKayit sonBuyume() {
        return buyumeler.isEmpty() ? null : buyumeler.get(0);
    }

    public static int say(List<Kayit> liste, java.util.function.Predicate<Kayit> kosul) {
        int n = 0;
        for (Kayit k : liste) {
            if (kosul.test(k)) {
                n++;
            }
        }
        return n;
    }

    public static double mamaToplami(List<Kayit> liste) {
        double t = 0;
        for (Kayit k : liste) {
            t += k.mamaMiktari();
        }
        return t;
    }
}
