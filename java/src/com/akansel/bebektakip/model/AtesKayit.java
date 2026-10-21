package com.akansel.bebektakip.model;

import com.akansel.bebektakip.depo.Json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tek bir ateş ölçümü: kaç derece, nereden ölçüldü ve o sırada hangi
 * belirtiler vardı. Hastalık günlerinde doktora gösterilecek dökümün
 * kaynağı bu kayıtlardır.
 */
public class AtesKayit {

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SAAT_BICIMI = DateTimeFormatter.ofPattern("HH:mm");

    /** Seçim listesinde hazır gelen belirtiler; başka bir şey not alanına yazılır. */
    public static final List<String> BELIRTILER = List.of(
            "Öksürük", "Burun akıntısı", "Burun tıkanıklığı", "Hırıltı / nefes darlığı",
            "Kusma", "İshal", "İştahsızlık", "Huzursuzluk", "Halsizlik / uyku hali",
            "Kulak çekiştirme", "Döküntü", "Titreme", "Havale");

    public static final List<String> OLCUM_YERLERI = List.of(
            "Koltuk altı", "Kulak", "Alın", "Makat", "Ağız");

    /** Çocuk doktorlarının yaygın kullandığı sınırlar (°C). */
    public static final double HAFIF_SINIR = 37.5;
    public static final double ATES_SINIRI = 38.0;
    public static final double YUKSEK_SINIR = 39.0;

    /** Bu aralığın dışındaki değerler yazım hatası sayılır. */
    public static final double EN_DUSUK = 34.0;
    public static final double EN_YUKSEK = 43.0;

    /** Ölçümün sınıfı. */
    public enum Seviye {
        YOK("—"),
        NORMAL("Normal"),
        HAFIF("Hafif yüksek"),
        ATES("Ateş"),
        YUKSEK("Yüksek ateş");

        private final String ad;

        Seviye(String ad) {
            this.ad = ad;
        }

        public String getAd() {
            return ad;
        }

        /** Ateş ya da yüksek ateş mi. */
        public boolean atesli() {
            return this == ATES || this == YUKSEK;
        }
    }

    private String id;
    private LocalDate tarih;
    private LocalTime saat;
    private double derece;
    private String olcumYeri;
    private final List<String> belirtiler = new ArrayList<>();
    private String not;

    public AtesKayit() {
        LocalDateTime simdi = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.tarih = simdi.toLocalDate();
        this.saat = simdi.toLocalTime().withSecond(0).withNano(0);
        this.olcumYeri = OLCUM_YERLERI.get(0);
        this.not = "";
    }

    public String getId() {
        return id;
    }

    public LocalDate getTarih() {
        return tarih;
    }

    public void setTarih(LocalDate tarih) {
        if (tarih != null) {
            this.tarih = tarih;
        }
    }

    public LocalTime getSaat() {
        return saat;
    }

    public void setSaat(LocalTime saat) {
        if (saat != null) {
            this.saat = saat.withSecond(0).withNano(0);
        }
    }

    /** Ölçülen değer; henüz girilmediyse 0. */
    public double getDerece() {
        return derece;
    }

    /** Değer tek ondalığa yuvarlanır; 0 "girilmedi" demektir. */
    public void setDerece(double derece) {
        this.derece = derece <= 0 ? 0 : Math.round(derece * 10) / 10.0;
    }

    public String getOlcumYeri() {
        return olcumYeri == null ? "" : olcumYeri;
    }

    public void setOlcumYeri(String olcumYeri) {
        this.olcumYeri = olcumYeri == null ? "" : olcumYeri.trim();
    }

    /** Kopyası döner; değiştirmek için setBelirtiler kullanılır. */
    public List<String> getBelirtiler() {
        return new ArrayList<>(belirtiler);
    }

    public void setBelirtiler(List<String> yeni) {
        belirtiler.clear();
        if (yeni == null) {
            return;
        }
        for (String b : yeni) {
            String t = b == null ? "" : b.trim();
            if (!t.isEmpty() && !belirtiler.contains(t)) {
                belirtiler.add(t);
            }
        }
    }

    /** Belirtiler virgülle ayrılmış tek satır olarak. */
    public String belirtiMetni() {
        return String.join(", ", belirtiler);
    }

    public String getNot() {
        return not == null ? "" : not;
    }

    public void setNot(String not) {
        this.not = not == null ? "" : not;
    }

    public Seviye seviye() {
        return seviye(derece);
    }

    public static Seviye seviye(double derece) {
        if (derece <= 0) {
            return Seviye.YOK;
        }
        if (derece >= YUKSEK_SINIR) {
            return Seviye.YUKSEK;
        }
        if (derece >= ATES_SINIRI) {
            return Seviye.ATES;
        }
        if (derece >= HAFIF_SINIR) {
            return Seviye.HAFIF;
        }
        return Seviye.NORMAL;
    }

    /**
     * Yazılan dereceyi çözer: "38,4", "38.4", "38,4 °C" hepsi olur. Makul
     * aralıkta değilse ya da sayı yoksa 0 döner.
     */
    public static double dereceCoz(String metin) {
        double d = Kayit.sayiyiCoz(metin);
        if (d < EN_DUSUK || d > EN_YUKSEK) {
            return 0;
        }
        return Math.round(d * 10) / 10.0;
    }

    /** Tarih + saat birleşimi; sıralamada ve grafikte kullanılır. */
    public LocalDateTime zaman() {
        return LocalDateTime.of(tarih, saat);
    }

    public Map<String, Object> jsonaCevir() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tarih", tarih.format(TARIH_BICIMI));
        m.put("saat", saat.format(SAAT_BICIMI));
        m.put("derece", derece);
        m.put("olcumYeri", getOlcumYeri());
        m.put("belirtiler", new ArrayList<Object>(belirtiler));
        m.put("not", getNot());
        return m;
    }

    /** JSON nesnesinden kayıt üretir; çözülemeyen alanlar varsayılanda kalır. */
    public static AtesKayit jsondan(Map<?, ?> m) {
        AtesKayit a = new AtesKayit();
        String id = Json.metinAl(m, "", "id");
        if (!id.isEmpty()) {
            a.id = id;
        }
        LocalDate t = Kayit.tarihCoz(Json.metinAl(m, "", "tarih"));
        if (t != null) {
            a.tarih = t;
        }
        LocalTime s = Kayit.saatCoz(Json.metinAl(m, "", "saat"));
        if (s != null) {
            a.saat = s;
        }
        a.setDerece(Json.sayiAl(m, 0, "derece"));
        a.olcumYeri = Json.metinAl(m, a.olcumYeri, "olcumYeri");

        // Elle yazılmış dosyalarda belirtiler virgüllü tek metin olabilir.
        Object b = m.get("belirtiler");
        List<String> liste = new ArrayList<>();
        if (b instanceof List) {
            for (Object o : (List<?>) b) {
                if (o != null) {
                    liste.add(String.valueOf(o));
                }
            }
        } else if (b instanceof String) {
            for (String parca : ((String) b).split(",")) {
                liste.add(parca);
            }
        }
        a.setBelirtiler(liste);
        a.not = Json.metinAl(m, "", "not");
        return a;
    }

    @Override
    public String toString() {
        return tarih.format(TARIH_BICIMI) + " " + saat.format(SAAT_BICIMI) + " " + derece;
    }
}
