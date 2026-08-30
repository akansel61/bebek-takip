package com.akansel.bebektakip.depo;

import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Kayıtların CSV ve JSON olarak dışa / içe aktarılması. */
public final class DisaAktarim {

    private DisaAktarim() {
    }

    private static final char AYIRAC = ';';

    /** Excel'in UTF-8'i tanıması için dosya başına konan BOM işareti. */
    private static final char BOM = (char) 0xFEFF;

    private static final String[] BASLIKLAR = {
            "Tarih", "Saat", "Çiş", "Kaka", "Sağ Meme", "Sol Meme",
            "Mama", "Mama Notu", "Mama Miktarı", "Not"
    };

    /**
     * Beslenme kayıtlarını CSV olarak yazar.
     *
     * Türkçe Excel'de çift tıklayınca doğru açılsın diye ayraç noktalı virgül,
     * kodlama BOM'lu UTF-8.
     */
    public static void csvYaz(Path hedef, List<Kayit> kayitlar) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(BOM);
        for (int i = 0; i < BASLIKLAR.length; i++) {
            if (i > 0) {
                sb.append(AYIRAC);
            }
            sb.append(kacir(BASLIKLAR[i]));
        }
        sb.append("\r\n");

        for (Kayit k : kayitlar) {
            satirYaz(sb, k);
        }
        Files.write(hedef, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void satirYaz(StringBuilder sb, Kayit k) {
        String[] hucreler = {
                bicimliTarih(k),
                k.saatMetni(),
                evetHayir(k.isCis()),
                evetHayir(k.isKaka()),
                evetHayir(k.isSagMeme()),
                evetHayir(k.isSolMeme()),
                evetHayir(k.isMama()),
                k.getMamaNotu(),
                k.mamaMiktari() > 0 ? sayi(k.mamaMiktari()) : "",
                k.getNot()
        };
        for (int i = 0; i < hucreler.length; i++) {
            if (i > 0) {
                sb.append(AYIRAC);
            }
            sb.append(kacir(hucreler[i]));
        }
        sb.append("\r\n");
    }

    private static String bicimliTarih(Kayit k) {
        return String.format("%02d.%02d.%04d",
                k.getTarih().getDayOfMonth(),
                k.getTarih().getMonthValue(),
                k.getTarih().getYear());
    }

    private static String sayi(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.0001) {
            return String.valueOf((long) Math.rint(d));
        }
        // Türkçe Excel ondalık ayracı olarak virgül bekler
        return String.valueOf(d).replace('.', ',');
    }

    private static String evetHayir(boolean b) {
        return b ? "Evet" : "Hayır";
    }

    private static String kacir(String s) {
        String metin = s == null ? "" : s;
        boolean gerekli = metin.indexOf(AYIRAC) >= 0
                || metin.indexOf('"') >= 0
                || metin.indexOf('\n') >= 0
                || metin.indexOf('\r') >= 0;
        if (!gerekli) {
            return metin;
        }
        return '"' + metin.replace("\"", "\"\"") + '"';
    }

    /** Yalnızca beslenme kayıtlarını JSON dosyasına yazar (eski biçim). */
    public static void jsonYaz(Path hedef, List<Kayit> kayitlar) throws IOException {
        Map<String, Object> kok = new LinkedHashMap<>();
        kok.put("surum", KayitDeposu.SURUM);
        kok.put("uygulama", "Bebek Takip");
        kok.put("kaydedilme", LocalDateTime.now().withNano(0).toString());
        List<Object> liste = new ArrayList<>(kayitlar.size());
        for (Kayit k : kayitlar) {
            liste.add(k.jsonaCevir());
        }
        kok.put("kayitlar", liste);
        Files.write(hedef, Json.yaz(kok).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Depodaki bütün verileri (beslenme, uyku, büyüme, hatırlatıcı) tek bir
     * yedek dosyasına yazar. İçe aktarma bu dosyayı olduğu gibi geri yükler.
     */
    public static void yedekYaz(Path hedef, KayitDeposu depo) throws IOException {
        Map<String, Object> kok = new LinkedHashMap<>();
        kok.put("surum", KayitDeposu.SURUM);
        kok.put("uygulama", "Bebek Takip");
        kok.put("kaydedilme", LocalDateTime.now().withNano(0).toString());

        List<Object> beslenme = new ArrayList<>(depo.getKayitlar().size());
        for (Kayit k : depo.getKayitlar()) {
            beslenme.add(k.jsonaCevir());
        }
        kok.put("kayitlar", beslenme);

        List<Object> uykular = new ArrayList<>(depo.getUykular().size());
        for (UykuKayit u : depo.getUykular()) {
            uykular.add(u.jsonaCevir());
        }
        kok.put("uykular", uykular);

        List<Object> buyumeler = new ArrayList<>(depo.getBuyumeler().size());
        for (BuyumeKayit b : depo.getBuyumeler()) {
            buyumeler.add(b.jsonaCevir());
        }
        kok.put("buyumeler", buyumeler);

        List<Object> hatirlaticilar = new ArrayList<>(depo.getHatirlaticilar().size());
        for (Hatirlatici h : depo.getHatirlaticilar()) {
            hatirlaticilar.add(h.jsonaCevir());
        }
        kok.put("hatirlaticilar", hatirlaticilar);

        Files.write(hedef, Json.yaz(kok).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON dosyasından beslenme kaydı okur. Hem bu uygulamanın yedeklerini
     * hem de tarayıcı sürümünün localStorage çıktısını kabul eder.
     */
    public static List<Kayit> jsonOku(Path kaynak) throws IOException {
        return KayitDeposu.metniCoz(metinOku(kaynak));
    }

    /** Dosyayı UTF-8 metin olarak okur; içe aktarma bunun üstünden çözümler. */
    public static String metinOku(Path kaynak) throws IOException {
        return new String(Files.readAllBytes(kaynak), StandardCharsets.UTF_8);
    }
}
