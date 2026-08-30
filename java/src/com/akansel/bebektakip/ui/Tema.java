package com.akansel.bebektakip.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Uygulamanın renk, yazı tipi ve biçimlendirme sabitleri. */
public final class Tema {

    private Tema() {
    }

    public static final Color ARKA = new Color(0xF5F5F7);
    public static final Color YUZEY = Color.WHITE;
    public static final Color CIZGI = new Color(0xE5E5E7);
    public static final Color CIZGI_ACIK = new Color(0xF0F0F2);
    public static final Color METIN = new Color(0x1D1D1F);
    public static final Color METIN_SOLUK = new Color(0x86868B);
    public static final Color VURGU = new Color(0x1D1D1F);
    public static final Color YESIL = new Color(0x34C759);
    public static final Color KIRMIZI = new Color(0xFF3B30);
    public static final Color MAVI = new Color(0x0071E3);
    public static final Color TURUNCU = new Color(0xFF9500);
    public static final Color MOR = new Color(0xAF52DE);
    public static final Color KAHVE = new Color(0xA2845E);
    public static final Color SATIR_USTU = new Color(0xF9F9FB);
    public static final Color PASIF_KUTU = new Color(0xC7C7CC);
    public static final Color SECILI = new Color(0xEDEDF0);

    private static final String AILE = yaziAilesiSec();

    private static String yaziAilesiSec() {
        Set<String> mevcut = new HashSet<>();
        try {
            String[] adlar = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames(Locale.ENGLISH);
            for (String a : adlar) {
                mevcut.add(a);
            }
        } catch (RuntimeException e) {
            return Font.SANS_SERIF;
        }
        String[] tercihler = {"Segoe UI", "Inter", "Helvetica Neue", "Roboto", "Arial"};
        for (String t : tercihler) {
            if (mevcut.contains(t)) {
                return t;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int boyut) {
        return new Font(AILE, Font.PLAIN, boyut);
    }

    public static Font fontKalin(int boyut) {
        return new Font(AILE, Font.BOLD, boyut);
    }

    /** Rakamların eşit genişlikte görünmesi için sayısal yazı tipi. */
    public static Font fontSayi(int boyut) {
        return new Font(AILE, Font.BOLD, boyut);
    }

    public static final Locale TR = new Locale("tr", "TR");

    /** Telif satırı; arayüzde ve Hakkında penceresinde aynı metin kullanılır. */
    public static final String TELIF = "© 2026 by AKANSEL";

    private static final DateTimeFormatter UZUN_TARIH =
            DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR);
    private static final DateTimeFormatter KISA_TARIH =
            DateTimeFormatter.ofPattern("d MMM", TR);
    private static final DateTimeFormatter TAM_TARIH =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", TR);
    private static final DateTimeFormatter GUN_ADI =
            DateTimeFormatter.ofPattern("EEE", TR);
    private static final DateTimeFormatter SAAT =
            DateTimeFormatter.ofPattern("HH:mm", TR);

    public static String uzunTarih(LocalDate d) {
        return d.format(UZUN_TARIH);
    }

    public static String kisaTarih(LocalDate d) {
        return d.format(KISA_TARIH);
    }

    public static String tamTarih(LocalDate d) {
        return d.format(TAM_TARIH);
    }

    public static String gunAdi(LocalDate d) {
        return d.format(GUN_ADI);
    }

    public static String saat(LocalTime t) {
        return t.format(SAAT);
    }

    /** Ondalık kısmı yoksa tam sayı olarak yazar (120,5 / 120). */
    public static String sayi(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.0001) {
            return String.valueOf((long) Math.rint(d));
        }
        return String.format(TR, "%.1f", d);
    }

    /** Dakikayı okunur süreye çevirir: 45 dk, 2 sa, 2 sa 15 dk. */
    public static String sure(long dakika) {
        if (dakika < 60) {
            return dakika + " dk";
        }
        long sa = dakika / 60;
        long dk = dakika % 60;
        return dk == 0 ? sa + " sa" : sa + " sa " + dk + " dk";
    }

    /** Çizim kalitesi ayarları; her paintComponent başında çağrılır. */
    public static Graphics2D kaliteAyarla(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    /** İki rengi verilen oranda karıştırır (0 = a, 1 = b). */
    public static Color karistir(Color a, Color b, double oran) {
        double o = Math.max(0, Math.min(1, oran));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * o),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * o),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * o));
    }

    /** Metin verilen genişliğe sığmıyorsa sonuna üç nokta koyup kısaltır. */
    public static String kisalt(Graphics2D g, String metin, int genislik) {
        if (metin == null) {
            return "";
        }
        java.awt.FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(metin) <= genislik) {
            return metin;
        }
        String son = "…";
        int sonGenislik = fm.stringWidth(son);
        StringBuilder sb = new StringBuilder();
        int toplam = 0;
        for (int i = 0; i < metin.length(); i++) {
            int w = fm.charWidth(metin.charAt(i));
            if (toplam + w + sonGenislik > genislik) {
                break;
            }
            sb.append(metin.charAt(i));
            toplam += w;
        }
        return sb.append(son).toString();
    }
}
