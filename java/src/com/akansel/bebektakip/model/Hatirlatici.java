package com.akansel.bebektakip.model;

import com.akansel.bebektakip.depo.Json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Aşı, ilaç ya da kontrol randevusu gibi tek bir hatırlatma. */
public class Hatirlatici {

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SAAT_BICIMI = DateTimeFormatter.ofPattern("HH:mm");

    private String id;
    private LocalDate tarih;
    private LocalTime saat;
    private String baslik;
    private String not;
    private boolean tamamlandi;

    public Hatirlatici() {
        this.id = UUID.randomUUID().toString();
        this.tarih = LocalDate.now();
        this.saat = LocalTime.now().withSecond(0).withNano(0);
        this.baslik = "";
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

    public String getBaslik() {
        return baslik == null ? "" : baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik == null ? "" : baslik;
    }

    public String getNot() {
        return not == null ? "" : not;
    }

    public void setNot(String not) {
        this.not = not == null ? "" : not;
    }

    public boolean isTamamlandi() {
        return tamamlandi;
    }

    public void setTamamlandi(boolean tamamlandi) {
        this.tamamlandi = tamamlandi;
    }

    /** Tarih + saat birleşimi; sıralamada kullanılır. */
    public LocalDateTime zaman() {
        return LocalDateTime.of(tarih, saat);
    }

    /** Zamanı geçmiş ama hâlâ tamamlanmamış mı? */
    public boolean gecikti() {
        return !tamamlandi && zaman().isBefore(LocalDateTime.now());
    }

    public Map<String, Object> jsonaCevir() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tarih", tarih.format(TARIH_BICIMI));
        m.put("saat", saat.format(SAAT_BICIMI));
        m.put("baslik", getBaslik());
        m.put("not", getNot());
        m.put("tamamlandi", tamamlandi);
        return m;
    }

    /** JSON nesnesinden hatırlatma üretir; çözülemeyen alanlar varsayılanda kalır. */
    public static Hatirlatici jsondan(Map<?, ?> m) {
        Hatirlatici h = new Hatirlatici();
        String id = Json.metinAl(m, "", "id");
        if (!id.isEmpty()) {
            h.id = id;
        }
        LocalDate t = Kayit.tarihCoz(Json.metinAl(m, "", "tarih"));
        if (t != null) {
            h.tarih = t;
        }
        LocalTime s = Kayit.saatCoz(Json.metinAl(m, "", "saat"));
        if (s != null) {
            h.saat = s;
        }
        h.baslik = Json.metinAl(m, "", "baslik");
        h.not = Json.metinAl(m, "", "not");
        h.tamamlandi = Json.mantiksalAl(m, false, "tamamlandi");
        return h;
    }

    @Override
    public String toString() {
        return tarih.format(TARIH_BICIMI) + " " + saat.format(SAAT_BICIMI) + " " + getBaslik();
    }
}
