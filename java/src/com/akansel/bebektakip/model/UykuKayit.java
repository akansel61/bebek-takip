package com.akansel.bebektakip.model;

import com.akansel.bebektakip.depo.Json;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tek bir uyku kaydı. Bitiş saati boş olabilir; bu, uykunun hâlâ
 * sürdüğü anlamına gelir ("Uykuyu Bitir" ile kapatılır).
 */
public class UykuKayit {

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SAAT_BICIMI = DateTimeFormatter.ofPattern("HH:mm");

    private String id;
    private LocalDate tarih;
    private LocalTime baslangic;
    private LocalTime bitis;
    private String not;

    public UykuKayit() {
        LocalDateTime simdi = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.tarih = simdi.toLocalDate();
        this.baslangic = simdi.toLocalTime().withSecond(0).withNano(0);
        this.bitis = null;
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

    public LocalTime getBaslangic() {
        return baslangic;
    }

    public void setBaslangic(LocalTime baslangic) {
        if (baslangic != null) {
            this.baslangic = baslangic.withSecond(0).withNano(0);
        }
    }

    public LocalTime getBitis() {
        return bitis;
    }

    /** null verilebilir; o zaman kayıt yeniden "uyku sürüyor" durumuna döner. */
    public void setBitis(LocalTime bitis) {
        this.bitis = bitis == null ? null : bitis.withSecond(0).withNano(0);
    }

    public String getNot() {
        return not == null ? "" : not;
    }

    public void setNot(String not) {
        this.not = not == null ? "" : not;
    }

    /** Bitiş saati girilmemişse uyku hâlâ sürüyor demektir. */
    public boolean devamEdiyor() {
        return bitis == null;
    }

    /**
     * Uyku süresi, dakika olarak. Gece yarısını aşan uykularda (23:30 - 06:15)
     * bitiş ertesi güne sayılır. Uyku sürüyorsa 0 döner.
     */
    public long sureDakika() {
        if (bitis == null) {
            return 0;
        }
        long dakika = Duration.between(baslangic, bitis).toMinutes();
        if (dakika < 0) {
            dakika += 24 * 60;
        }
        return dakika;
    }

    public Map<String, Object> jsonaCevir() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tarih", tarih.format(TARIH_BICIMI));
        m.put("baslangic", baslangic.format(SAAT_BICIMI));
        m.put("bitis", bitis == null ? "" : bitis.format(SAAT_BICIMI));
        m.put("not", getNot());
        return m;
    }

    /** JSON nesnesinden kayıt üretir; çözülemeyen alanlar varsayılanda kalır. */
    public static UykuKayit jsondan(Map<?, ?> m) {
        UykuKayit u = new UykuKayit();
        String id = Json.metinAl(m, "", "id");
        if (!id.isEmpty()) {
            u.id = id;
        }
        LocalDate t = Kayit.tarihCoz(Json.metinAl(m, "", "tarih"));
        if (t != null) {
            u.tarih = t;
        }
        LocalTime b = Kayit.saatCoz(Json.metinAl(m, "", "baslangic"));
        if (b != null) {
            u.baslangic = b;
        }
        u.bitis = Kayit.saatCoz(Json.metinAl(m, "", "bitis"));
        u.not = Json.metinAl(m, "", "not");
        return u;
    }

    @Override
    public String toString() {
        String son = bitis == null ? "…" : bitis.format(SAAT_BICIMI);
        return tarih.format(TARIH_BICIMI) + " " + baslangic.format(SAAT_BICIMI) + "-" + son;
    }
}
