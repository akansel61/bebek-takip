package com.akansel.bebektakip.rapor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Bir .ttf dosyasının PDF'e gömmek için gereken kısmı: karakterden glif
 * numarasına geçiş tablosu, glif genişlikleri ve yalnızca kullanılan
 * glifleri içeren küçültülmüş kopya.
 *
 * Yalnızca glyf tabanlı, tek yazı tipli dosyalar okunur. .ttc
 * koleksiyonları ve CFF tabanlı .otf dosyaları reddedilir; çağıran taraf
 * listedeki bir sonraki yazı tipini dener.
 */
final class TrueTypeYazi {

    /**
     * Gömülü kopyaya alınan tablolar. PDF yalnızca çizim ve ölçü tablolarını
     * ister; name ve OS/2 de konuyor, çünkü bazı yazıcı sürücüleri yazı
     * tipini bunlar olmadan tanımıyor.
     */
    private static final String[] GOMULU_TABLOLAR = {
            "OS/2", "cvt ", "fpgm", "glyf", "head", "hhea",
            "hmtx", "loca", "maxp", "name", "prep"
    };

    private final byte[] veri;
    private final Map<String, int[]> tablolar = new HashMap<>();

    final String ad;
    final int birimEm;
    final int glifSayisi;
    final int yukselen;
    final int alcalan;
    final int buyukHarf;
    final int xMin;
    final int yMin;
    final int xMax;
    final int yMax;
    final double egiklik;

    private final int olcuSayisi;
    private final int[] loca;
    private final char[] karakterGlif = new char[0x10000];

    private TrueTypeYazi(byte[] veri, String yedekAd) throws IOException {
        this.veri = veri;
        long surum = u32(0);
        if (surum != 0x00010000L && surum != 0x74727565L) {
            throw new IOException("Desteklenmeyen yazı tipi biçimi");
        }
        int tabloSayisi = u16(4);
        for (int i = 0; i < tabloSayisi; i++) {
            int kayit = 12 + i * 16;
            String etiket = new String(veri, kayit, 4, StandardCharsets.ISO_8859_1);
            int konum = (int) u32(kayit + 8);
            int uzunluk = (int) u32(kayit + 12);
            if (konum < 0 || uzunluk < 0 || (long) konum + uzunluk > veri.length) {
                throw new IOException("Bozuk tablo: " + etiket);
            }
            tablolar.put(etiket, new int[]{konum, uzunluk});
        }
        String[] gerekenler = {"head", "hhea", "maxp", "hmtx", "loca", "glyf", "cmap"};
        for (String gerekli : gerekenler) {
            if (!tablolar.containsKey(gerekli)) {
                throw new IOException("Eksik tablo: " + gerekli);
            }
        }

        int head = tablo("head");
        birimEm = u16(head + 18);
        xMin = s16(head + 36);
        yMin = s16(head + 38);
        xMax = s16(head + 40);
        yMax = s16(head + 42);
        int locaBicimi = s16(head + 50);

        int hhea = tablo("hhea");
        yukselen = s16(hhea + 4);
        alcalan = s16(hhea + 6);
        olcuSayisi = u16(hhea + 34);

        glifSayisi = u16(tablo("maxp") + 4);

        Integer os2 = tablolar.containsKey("OS/2") ? tablo("OS/2") : null;
        if (os2 != null && u16(os2) >= 2 && tablolar.get("OS/2")[1] >= 90) {
            buyukHarf = s16(os2 + 88);
        } else {
            buyukHarf = (int) Math.round(yukselen * 0.7);
        }
        egiklik = tablolar.containsKey("post") ? s32(tablo("post") + 4) / 65536.0 : 0;

        loca = new int[glifSayisi + 1];
        int locaKonum = tablo("loca");
        for (int i = 0; i <= glifSayisi; i++) {
            loca[i] = locaBicimi == 0 ? u16(locaKonum + i * 2) * 2 : (int) u32(locaKonum + i * 4);
        }

        cmapOku();
        String psAd = psAdiOku();
        ad = temizAd(psAd != null ? psAd : yedekAd);
    }

    static TrueTypeYazi oku(Path dosya) throws IOException {
        String yedek = dosya.getFileName().toString().replaceFirst("\\.[^.]*$", "");
        try {
            return new TrueTypeYazi(Files.readAllBytes(dosya), yedek);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Yazı tipi dosyası okunamadı: " + dosya.getFileName(), e);
        }
    }

    private int tablo(String etiket) {
        return tablolar.get(etiket)[0];
    }

    private int u16(int i) {
        return ((veri[i] & 0xFF) << 8) | (veri[i + 1] & 0xFF);
    }

    private int s16(int i) {
        return (short) u16(i);
    }

    private long u32(int i) {
        return ((long) u16(i) << 16) | u16(i + 2);
    }

    private int s32(int i) {
        return (int) u32(i);
    }

    /** Unicode cmap'i okur; 32 bitlik (biçim 12) tablo varsa o tercih edilir. */
    private void cmapOku() throws IOException {
        int cmap = tablo("cmap");
        int adet = u16(cmap + 2);
        int enIyi = -1;
        int enIyiPuan = 0;
        for (int i = 0; i < adet; i++) {
            int kayit = cmap + 4 + i * 8;
            int platform = u16(kayit);
            int kodlama = u16(kayit + 2);
            int konum = cmap + (int) u32(kayit + 4);
            int bicim = u16(konum);
            int puan = 0;
            if (platform == 3 && kodlama == 10 && bicim == 12) {
                puan = 4;
            } else if (platform == 0 && bicim == 12) {
                puan = 3;
            } else if (platform == 3 && kodlama == 1 && bicim == 4) {
                puan = 2;
            } else if (platform == 0 && bicim == 4) {
                puan = 1;
            }
            if (puan > enIyiPuan) {
                enIyiPuan = puan;
                enIyi = konum;
            }
        }
        if (enIyi < 0) {
            throw new IOException("Unicode karakter tablosu bulunamadı");
        }
        if (u16(enIyi) == 12) {
            bicim12(enIyi);
        } else {
            bicim4(enIyi);
        }
    }

    private void bicim4(int t) {
        int segIki = u16(t + 6);
        int bitisler = t + 14;
        int baslangiclar = bitisler + segIki + 2;
        int farklar = baslangiclar + segIki;
        int araliklar = farklar + segIki;
        for (int i = 0; i < segIki / 2; i++) {
            int bitis = u16(bitisler + i * 2);
            int bas = u16(baslangiclar + i * 2);
            int fark = u16(farklar + i * 2);
            int aralik = u16(araliklar + i * 2);
            for (int c = bas; c <= bitis && c < 0xFFFF; c++) {
                int g;
                if (aralik == 0) {
                    g = (c + fark) & 0xFFFF;
                } else {
                    int adres = araliklar + i * 2 + aralik + (c - bas) * 2;
                    if (adres + 1 >= veri.length) {
                        continue;
                    }
                    g = u16(adres);
                    if (g != 0) {
                        g = (g + fark) & 0xFFFF;
                    }
                }
                karakterGlif[c] = (char) g;
            }
        }
    }

    private void bicim12(int t) {
        long grup = u32(t + 12);
        for (long i = 0; i < grup; i++) {
            int kayit = t + 16 + (int) i * 12;
            long bas = u32(kayit);
            long bitis = Math.min(u32(kayit + 4), 0xFFFF);
            long glif = u32(kayit + 8);
            for (long c = bas; c <= bitis; c++) {
                karakterGlif[(int) c] = (char) (glif + c - bas);
            }
        }
    }

    /** "name" tablosundaki PostScript adı (kimlik 6); yoksa null. */
    private String psAdiOku() {
        int[] t = tablolar.get("name");
        if (t == null) {
            return null;
        }
        int bas = t[0];
        int adet = u16(bas + 2);
        int metinler = bas + u16(bas + 4);
        for (int i = 0; i < adet; i++) {
            int kayit = bas + 6 + i * 12;
            int platform = u16(kayit);
            int kimlik = u16(kayit + 6);
            int uzunluk = u16(kayit + 8);
            int konum = metinler + u16(kayit + 10);
            if (kimlik != 6 || konum + uzunluk > veri.length) {
                continue;
            }
            if (platform == 3 || platform == 0) {
                return new String(veri, konum, uzunluk, StandardCharsets.UTF_16BE);
            }
            if (platform == 1) {
                return new String(veri, konum, uzunluk, StandardCharsets.ISO_8859_1);
            }
        }
        return null;
    }

    private static String temizAd(String ad) {
        StringBuilder sb = new StringBuilder();
        for (char c : ad.toCharArray()) {
            boolean harf = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
            if (harf || (c >= '0' && c <= '9') || c == '-') {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "Yazi" : sb.toString();
    }

    /** Karakterin glif numarası; yazı tipinde yoksa 0 (.notdef). */
    int glif(int kod) {
        return kod >= 0 && kod < karakterGlif.length ? karakterGlif[kod] : 0;
    }

    /** Glifin ilerleme genişliği, yazı tipi biriminde. */
    int ilerleme(int glif) {
        int i = Math.min(glif, olcuSayisi - 1);
        return u16(tablo("hmtx") + i * 4);
    }

    private int solBosluk(int glif) {
        int hmtx = tablo("hmtx");
        if (glif < olcuSayisi) {
            return s16(hmtx + glif * 4 + 2);
        }
        return s16(hmtx + olcuSayisi * 4 + (glif - olcuSayisi) * 2);
    }

    /** Birimi 1000'e ölçeklenmiş genişlik; PDF'in /W dizisi bunu ister. */
    int binde(int glif) {
        return (int) Math.round(ilerleme(glif) * 1000.0 / birimEm);
    }

    /** Yazı tipi birimindeki bir ölçüyü 1000'lik ölçeğe çevirir. */
    double olcekle(double deger) {
        return deger * 1000.0 / birimEm;
    }

    /** Birleşik (composite) glifin kullandığı alt glifler. */
    private List<Integer> bilesenler(int glif) {
        List<Integer> sonuc = new ArrayList<>();
        if (glif < 0 || glif >= glifSayisi || loca[glif + 1] - loca[glif] < 10) {
            return sonuc;
        }
        int bas = tablo("glyf") + loca[glif];
        if (s16(bas) >= 0) {
            return sonuc;
        }
        int i = bas + 10;
        while (true) {
            int bayrak = u16(i);
            sonuc.add(u16(i + 2));
            i += 4;
            i += (bayrak & 0x0001) != 0 ? 4 : 2;
            if ((bayrak & 0x0008) != 0) {
                i += 2;
            } else if ((bayrak & 0x0040) != 0) {
                i += 4;
            } else if ((bayrak & 0x0080) != 0) {
                i += 8;
            }
            if ((bayrak & 0x0020) == 0) {
                break;
            }
        }
        return sonuc;
    }

    /**
     * Yalnızca verilen gliflerin (ve birleşik gliflerin parçalarının) çizim
     * verisini taşıyan yazı tipi dosyası üretir. Glif numaraları değişmez;
     * kullanılmayan gliflerin verisi boş bırakılır ve en büyük kullanılan
     * numaradan sonrası kesilir.
     */
    byte[] altKume(Collection<Integer> glifler) throws IOException {
        TreeSet<Integer> kume = new TreeSet<>();
        kume.add(0);
        for (Integer g : glifler) {
            if (g != null && g >= 0 && g < glifSayisi) {
                kume.add(g);
            }
        }
        Deque<Integer> bekleyen = new ArrayDeque<>(kume);
        while (!bekleyen.isEmpty()) {
            for (int parca : bilesenler(bekleyen.pop())) {
                if (parca < glifSayisi && kume.add(parca)) {
                    bekleyen.push(parca);
                }
            }
        }
        int yeniSayi = kume.last() + 1;

        ByteArrayOutputStream glyf = new ByteArrayOutputStream();
        ByteArrayOutputStream yeniLoca = new ByteArrayOutputStream();
        int glyfKonum = tablo("glyf");
        for (int g = 0; g < yeniSayi; g++) {
            yaz32(yeniLoca, glyf.size());
            if (kume.contains(g)) {
                int uzunluk = loca[g + 1] - loca[g];
                if (uzunluk > 0) {
                    glyf.write(veri, glyfKonum + loca[g], uzunluk);
                    while (glyf.size() % 4 != 0) {
                        glyf.write(0);
                    }
                }
            }
        }
        yaz32(yeniLoca, glyf.size());

        ByteArrayOutputStream hmtx = new ByteArrayOutputStream();
        for (int g = 0; g < yeniSayi; g++) {
            yaz16(hmtx, ilerleme(g));
            yaz16(hmtx, solBosluk(g));
        }

        byte[] head = kopya("head");
        head[8] = 0;
        head[9] = 0;
        head[10] = 0;
        head[11] = 0;
        head[50] = 0;
        head[51] = 1;
        byte[] hhea = kopya("hhea");
        hhea[34] = (byte) (yeniSayi >> 8);
        hhea[35] = (byte) yeniSayi;
        byte[] maxp = kopya("maxp");
        maxp[4] = (byte) (yeniSayi >> 8);
        maxp[5] = (byte) yeniSayi;

        Map<String, byte[]> yeni = new TreeMap<>();
        yeni.put("glyf", glyf.toByteArray());
        yeni.put("loca", yeniLoca.toByteArray());
        yeni.put("hmtx", hmtx.toByteArray());
        yeni.put("head", head);
        if (tablolar.containsKey("post") && tablolar.get("post")[1] >= 32) {
            // 3.0 sürümü: glif adları taşınmaz, yalnızca 32 baytlık başlık kalır
            byte[] post = new byte[32];
            System.arraycopy(veri, tablo("post"), post, 0, 32);
            post[0] = 0;
            post[1] = 3;
            post[2] = 0;
            post[3] = 0;
            yeni.put("post", post);
        }
        yeni.put("hhea", hhea);
        yeni.put("maxp", maxp);
        for (String etiket : GOMULU_TABLOLAR) {
            if (!yeni.containsKey(etiket) && tablolar.containsKey(etiket)) {
                yeni.put(etiket, kopya(etiket));
            }
        }
        return dosyaKur(yeni);
    }

    private byte[] kopya(String etiket) {
        int[] t = tablolar.get(etiket);
        byte[] b = new byte[t[1]];
        System.arraycopy(veri, t[0], b, 0, t[1]);
        return b;
    }

    /** Tablo dizinini, sağlama toplamlarını ve head düzeltmesini yazar. */
    private static byte[] dosyaKur(Map<String, byte[]> yeni) throws IOException {
        int adet = yeni.size();
        int kuvvet = 1;
        int ust = 0;
        while (kuvvet * 2 <= adet) {
            kuvvet *= 2;
            ust++;
        }
        ByteArrayOutputStream cikti = new ByteArrayOutputStream();
        yaz32(cikti, 0x00010000);
        yaz16(cikti, adet);
        yaz16(cikti, kuvvet * 16);
        yaz16(cikti, ust);
        yaz16(cikti, adet * 16 - kuvvet * 16);

        int konum = 12 + adet * 16;
        int headKonum = 0;
        for (Map.Entry<String, byte[]> e : yeni.entrySet()) {
            byte[] b = e.getValue();
            cikti.write(e.getKey().getBytes(StandardCharsets.ISO_8859_1));
            yaz32(cikti, (int) toplam(b));
            yaz32(cikti, konum);
            yaz32(cikti, b.length);
            if (e.getKey().equals("head")) {
                headKonum = konum;
            }
            konum += (b.length + 3) & ~3;
        }
        for (byte[] b : yeni.values()) {
            cikti.write(b);
            for (int i = b.length; i % 4 != 0; i++) {
                cikti.write(0);
            }
        }
        byte[] dosya = cikti.toByteArray();
        long duzeltme = (0xB1B0AFBAL - toplam(dosya)) & 0xFFFFFFFFL;
        dosya[headKonum + 8] = (byte) (duzeltme >> 24);
        dosya[headKonum + 9] = (byte) (duzeltme >> 16);
        dosya[headKonum + 10] = (byte) (duzeltme >> 8);
        dosya[headKonum + 11] = (byte) duzeltme;
        return dosya;
    }

    private static long toplam(byte[] b) {
        long t = 0;
        for (int i = 0; i < b.length; i += 4) {
            long kelime = 0;
            for (int k = 0; k < 4; k++) {
                kelime = (kelime << 8) | (i + k < b.length ? b[i + k] & 0xFF : 0);
            }
            t = (t + kelime) & 0xFFFFFFFFL;
        }
        return t;
    }

    private static void yaz16(ByteArrayOutputStream o, int d) {
        o.write((d >> 8) & 0xFF);
        o.write(d & 0xFF);
    }

    private static void yaz32(ByteArrayOutputStream o, int d) {
        yaz16(o, (d >>> 16) & 0xFFFF);
        yaz16(o, d & 0xFFFF);
    }
}
