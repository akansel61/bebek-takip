package com.akansel.bebektakip.model;

import com.akansel.bebektakip.depo.Json;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tek bir büyüme ölçümü: kilo (kg), boy (cm) ve baş çevresi (cm).
 * Girilmemiş ölçü 0 olarak durur ve arayüzde boş gösterilir.
 */
public class BuyumeKayit {

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ISO_LOCAL_DATE;

    private String id;
    private LocalDate tarih;
    private double kilo;
    private double boy;
    private double basCevresi;
    private String not;

    public BuyumeKayit() {
        this.id = UUID.randomUUID().toString();
        this.tarih = LocalDate.now();
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

    public double getKilo() {
        return kilo;
    }

    public void setKilo(double kilo) {
        this.kilo = Math.max(0, kilo);
    }

    public double getBoy() {
        return boy;
    }

    public void setBoy(double boy) {
        this.boy = Math.max(0, boy);
    }

    public double getBasCevresi() {
        return basCevresi;
    }

    public void setBasCevresi(double basCevresi) {
        this.basCevresi = Math.max(0, basCevresi);
    }

    public String getNot() {
        return not == null ? "" : not;
    }

    public void setNot(String not) {
        this.not = not == null ? "" : not;
    }

    public Map<String, Object> jsonaCevir() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tarih", tarih.format(TARIH_BICIMI));
        m.put("kilo", kilo);
        m.put("boy", boy);
        m.put("basCevresi", basCevresi);
        m.put("not", getNot());
        return m;
    }

    /** JSON nesnesinden ölçüm üretir; "4,2" gibi virgüllü yazımlar da okunur. */
    public static BuyumeKayit jsondan(Map<?, ?> m) {
        BuyumeKayit b = new BuyumeKayit();
        String id = Json.metinAl(m, "", "id");
        if (!id.isEmpty()) {
            b.id = id;
        }
        LocalDate t = Kayit.tarihCoz(Json.metinAl(m, "", "tarih"));
        if (t != null) {
            b.tarih = t;
        }
        b.setKilo(Json.sayiAl(m, 0, "kilo"));
        b.setBoy(Json.sayiAl(m, 0, "boy"));
        b.setBasCevresi(Json.sayiAl(m, 0, "basCevresi", "bascevresi"));
        b.not = Json.metinAl(m, "", "not");
        return b;
    }

    @Override
    public String toString() {
        return tarih.format(TARIH_BICIMI) + " " + kilo + " kg / " + boy + " cm";
    }
}
