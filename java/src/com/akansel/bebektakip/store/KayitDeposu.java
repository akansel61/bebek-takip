package com.akansel.bebektakip.store;

import com.akansel.bebektakip.model.Kayit;

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
 * Kayıtların bellekteki listesi ve disk üzerindeki JSON dosyası.
 *
 * Dosya: %USERPROFILE%\.bebek-takip\kayitlar.json
 * Yazma atomik: önce .tmp dosyasına yazılır, sonra yerine taşınır. Her
 * başarılı yazmadan önce bir önceki sürüm .bak olarak saklanır.
 */
public class KayitDeposu {

    /** Dosya biçimi sürümü. */
    public static final int SURUM = 1;

    private final Path dizin;
    private final Path dosya;
    private final Path yedek;

    private final List<Kayit> kayitlar = new ArrayList<>();
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

    /** Tarih + saate göre yeniden eskiye sıralar. */
    public void sirala() {
        kayitlar.sort(Comparator.comparing(Kayit::zaman).reversed());
    }

    /** Dosyayı okur; dosya yoksa boş listeyle başlar. */
    public void yukle() {
        sonHata = null;
        kayitlar.clear();
        if (!Files.exists(dosya)) {
            return;
        }
        try {
            String metin = new String(Files.readAllBytes(dosya), StandardCharsets.UTF_8);
            kayitlar.addAll(metniCoz(metin));
            sirala();
        } catch (IOException | RuntimeException e) {
            sonHata = "Kayit dosyasi okunamadi: " + e.getMessage();
        }
    }

    /**
     * JSON metnini kayıt listesine çevirir. Hem bu uygulamanın sarmalanmış
     * biçimini hem de tarayıcı sürümünün düz dizisini kabul eder.
     */
    public static List<Kayit> metniCoz(String metin) {
        List<Kayit> sonuc = new ArrayList<>();
        String temiz = metin == null ? "" : metin.trim();
        if (temiz.isEmpty()) {
            return sonuc;
        }
        // UTF-8 BOM
        if (temiz.charAt(0) == '\uFEFF') {
            temiz = temiz.substring(1).trim();
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
            throw new Json.JsonHatasi("Beklenen bicimde kayit listesi bulunamadi");
        }
        for (Object o : liste) {
            if (o instanceof Map) {
                sonuc.add(Kayit.jsondan((Map<?, ?>) o));
            }
        }
        return sonuc;
    }

    /** Listeyi diske yazar; başarısız olursa sonHata doldurulur. */
    public boolean kaydet() {
        sonHata = null;
        try {
            Files.createDirectories(dizin);
            Map<String, Object> kok = new LinkedHashMap<>();
            kok.put("surum", SURUM);
            kok.put("uygulama", "Bebek Takip");
            kok.put("kaydedilme", LocalDateTime.now().withNano(0).toString());
            List<Object> liste = new ArrayList<>(kayitlar.size());
            for (Kayit k : kayitlar) {
                liste.add(k.jsonaCevir());
            }
            kok.put("kayitlar", liste);

            Path gecici = dizin.resolve("kayitlar.json.tmp");
            Files.write(gecici, Json.yaz(kok).getBytes(StandardCharsets.UTF_8));

            if (Files.exists(dosya)) {
                try {
                    Files.copy(dosya, yedek, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException yoksay) {
                    // yedek alınamadıysa asıl yazmayı engelleme
                }
            }
            try {
                Files.move(gecici, dosya,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(gecici, dosya, StandardCopyOption.REPLACE_EXISTING);
            }
            sonKayitZamani = LocalDateTime.now();
            return true;
        } catch (IOException | RuntimeException e) {
            sonHata = "Kayit dosyasi yazilamadi: " + e.getMessage();
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
