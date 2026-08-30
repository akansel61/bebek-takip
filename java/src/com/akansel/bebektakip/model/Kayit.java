package com.akansel.bebektakip.model;

import com.akansel.bebektakip.depo.Json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Tek bir beslenme / bez kaydı. */
public class Kayit {

    private static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SAAT_BICIMI = DateTimeFormatter.ofPattern("HH:mm");

    private String id;
    private LocalDate tarih;
    private LocalTime saat;
    private boolean cis;
    private boolean kaka;
    private boolean sagMeme;
    private boolean solMeme;
    private boolean mama;
    private String mamaNotu;
    private String not;

    public Kayit() {
        LocalDateTime simdi = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.tarih = simdi.toLocalDate();
        this.saat = simdi.toLocalTime().withSecond(0).withNano(0);
        this.mamaNotu = "";
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

    public boolean isCis() {
        return cis;
    }

    public void setCis(boolean cis) {
        this.cis = cis;
    }

    public boolean isKaka() {
        return kaka;
    }

    public void setKaka(boolean kaka) {
        this.kaka = kaka;
    }

    public boolean isSagMeme() {
        return sagMeme;
    }

    public void setSagMeme(boolean sagMeme) {
        this.sagMeme = sagMeme;
    }

    public boolean isSolMeme() {
        return solMeme;
    }

    public void setSolMeme(boolean solMeme) {
        this.solMeme = solMeme;
    }

    public boolean isMama() {
        return mama;
    }

    public void setMama(boolean mama) {
        this.mama = mama;
    }

    public String getMamaNotu() {
        return mamaNotu == null ? "" : mamaNotu;
    }

    public void setMamaNotu(String mamaNotu) {
        this.mamaNotu = mamaNotu == null ? "" : mamaNotu;
    }

    public String getNot() {
        return not == null ? "" : not;
    }

    public void setNot(String not) {
        this.not = not == null ? "" : not;
    }

    /** Emzirme var mı (sağ ya da sol meme). */
    public boolean isEmzirme() {
        return sagMeme || solMeme;
    }

    /** Tarih + saat birleşimi; sıralamada kullanılır. */
    public LocalDateTime zaman() {
        return LocalDateTime.of(tarih, saat);
    }

    /**
     * Mama notundaki sayısal miktar. "120", "120 ml", "90ml", "7,5" gibi
     * yazımları tanır, sayı bulamazsa 0 döner. Mama işaretli değilse
     * miktar toplama katılmaz.
     */
    public double mamaMiktari() {
        if (!mama) {
            return 0;
        }
        return sayiyiCoz(getMamaNotu());
    }

    /** Metindeki ilk sayıyı çözer; büyüme ölçüleri de aynı yazımları kullanır. */
    public static double sayiyiCoz(String metin) {
        if (metin == null) {
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        boolean basladi = false;
        boolean noktaVar = false;
        for (int i = 0; i < metin.length(); i++) {
            char c = metin.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
                basladi = true;
            } else if ((c == '.' || c == ',') && basladi && !noktaVar) {
                sb.append('.');
                noktaVar = true;
            } else if (c == '-' && !basladi && sb.length() == 0) {
                sb.append(c);
            } else if (basladi) {
                break;
            } else if (!Character.isWhitespace(c)) {
                sb.setLength(0);
            }
        }
        if (!basladi) {
            return 0;
        }
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Map<String, Object> jsonaCevir() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tarih", tarih.format(TARIH_BICIMI));
        m.put("saat", saat.format(SAAT_BICIMI));
        m.put("cis", cis);
        m.put("kaka", kaka);
        m.put("sagMeme", sagMeme);
        m.put("solMeme", solMeme);
        m.put("mama", mama);
        m.put("mamaNotu", getMamaNotu());
        m.put("not", getNot());
        return m;
    }

    /**
     * JSON nesnesinden kayıt üretir. Hem bu uygulamanın biçimini ("tarih",
     * "saat") hem de tarayıcı sürümünün biçimini ("date", "time") kabul eder.
     */
    public static Kayit jsondan(Map<?, ?> m) {
        Kayit k = new Kayit();
        String id = Json.metinAl(m, "", "id");
        if (!id.isEmpty()) {
            k.id = id;
        }
        LocalDate t = tarihCoz(Json.metinAl(m, "", "tarih", "date"));
        if (t != null) {
            k.tarih = t;
        }
        LocalTime s = saatCoz(Json.metinAl(m, "", "saat", "time"));
        if (s != null) {
            k.saat = s;
        }
        k.cis = Json.mantiksalAl(m, false, "cis");
        k.kaka = Json.mantiksalAl(m, false, "kaka");
        k.sagMeme = Json.mantiksalAl(m, false, "sagMeme", "sag", "sagmeme");
        k.solMeme = Json.mantiksalAl(m, false, "solMeme", "sol", "solmeme");
        k.mama = Json.mantiksalAl(m, false, "mama");
        k.mamaNotu = Json.metinAl(m, "", "mamaNotu", "mamanotu");
        k.not = Json.metinAl(m, "", "not", "note");
        return k;
    }

    /** "2025-08-26", "26.08.2025" ve "26/08/2025" yazımlarını çözer. */
    public static LocalDate tarihCoz(String metin) {
        if (metin == null) {
            return null;
        }
        String t = metin.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() >= 10 && t.charAt(4) == '-') {
            try {
                return LocalDate.parse(t.substring(0, 10), TARIH_BICIMI);
            } catch (RuntimeException e) {
                return null;
            }
        }
        String[] p = t.split("[./\\-]");
        if (p.length == 3) {
            try {
                int gun = Integer.parseInt(p[0].trim());
                int ay = Integer.parseInt(p[1].trim());
                int yil = Integer.parseInt(p[2].trim());
                if (yil < 100) {
                    yil += 2000;
                }
                return LocalDate.of(yil, ay, gun);
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    /** "14:30" ve "14:30:00" yazımlarını çözer. */
    public static LocalTime saatCoz(String metin) {
        if (metin == null) {
            return null;
        }
        String t = metin.trim().replace('.', ':');
        if (t.isEmpty()) {
            return null;
        }
        String[] p = t.split(":");
        if (p.length < 2) {
            return null;
        }
        try {
            int saat = Integer.parseInt(p[0].trim());
            int dakika = Integer.parseInt(p[1].trim());
            if (saat < 0 || saat > 23 || dakika < 0 || dakika > 59) {
                return null;
            }
            return LocalTime.of(saat, dakika);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public String tarihMetni() {
        return tarih.format(TARIH_BICIMI);
    }

    public String saatMetni() {
        return saat.format(SAAT_BICIMI);
    }

    @Override
    public String toString() {
        return tarihMetni() + " " + saatMetni();
    }
}
