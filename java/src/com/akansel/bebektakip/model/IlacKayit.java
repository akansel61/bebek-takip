package com.akansel.bebektakip.model;

import com.akansel.bebektakip.depo.Json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bebeğe verilen tek bir vitamin ya da ilaç: ne verildi, ne zaman, hangi
 * dozda. Doz serbest metindir ("3 damla", "2,5 ml" gibi).
 */
public class IlacKayit {

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SAAT_BICIMI = DateTimeFormatter.ofPattern("HH:mm");

    private String id;
    private LocalDate tarih;
    private LocalTime saat;
    private String ad;
    private String doz;
    private String not;

    public IlacKayit() {
        LocalDateTime simdi = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.tarih = simdi.toLocalDate();
        this.saat = simdi.toLocalTime().withSecond(0).withNano(0);
        this.ad = "";
        this.doz = "";
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

    public String getAd() {
        return ad == null ? "" : ad;
    }

    public void setAd(String ad) {
        this.ad = ad == null ? "" : ad;
    }

    public String getDoz() {
        return doz == null ? "" : doz;
    }

    public void setDoz(String doz) {
        this.doz = doz == null ? "" : doz;
    }

    public String getNot() {
        return not == null ? "" : not;
    }

    public void setNot(String not) {
        this.not = not == null ? "" : not;
    }

    /** Tarih + saat birleşimi; sıralamada kullanılır. */
    public LocalDateTime zaman() {
        return LocalDateTime.of(tarih, saat);
    }

    public Map<String, Object> jsonaCevir() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tarih", tarih.format(TARIH_BICIMI));
        m.put("saat", saat.format(SAAT_BICIMI));
        m.put("ad", getAd());
        m.put("doz", getDoz());
        m.put("not", getNot());
        return m;
    }

    /** JSON nesnesinden kayıt üretir; çözülemeyen alanlar varsayılanda kalır. */
    public static IlacKayit jsondan(Map<?, ?> m) {
        IlacKayit i = new IlacKayit();
        String id = Json.metinAl(m, "", "id");
        if (!id.isEmpty()) {
            i.id = id;
        }
        LocalDate t = Kayit.tarihCoz(Json.metinAl(m, "", "tarih"));
        if (t != null) {
            i.tarih = t;
        }
        LocalTime s = Kayit.saatCoz(Json.metinAl(m, "", "saat"));
        if (s != null) {
            i.saat = s;
        }
        i.ad = Json.metinAl(m, "", "ad");
        i.doz = Json.metinAl(m, "", "doz");
        i.not = Json.metinAl(m, "", "not");
        return i;
    }

    @Override
    public String toString() {
        return tarih.format(TARIH_BICIMI) + " " + saat.format(SAAT_BICIMI) + " " + getAd();
    }
}
