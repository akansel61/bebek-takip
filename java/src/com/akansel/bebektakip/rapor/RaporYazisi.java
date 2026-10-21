package com.akansel.bebektakip.rapor;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Raporun düz ve kalın yazı tipi.
 *
 * PDF'e gömülebilmesi için yazı tipinin dosyası gerekiyor; sistemin yazı
 * tipi klasöründe sırayla Segoe UI, Arial, Tahoma ve Calibri aranır.
 * Ölçüm ve ekran çizimi aynı dosyadan yüklenen AWT yazı tipiyle yapılır,
 * böylece önizlemedeki satır kırılımları PDF'tekiyle aynı çıkar.
 *
 * Hiçbiri bulunamazsa Java'nın mantıksal yazı tipine düşülür: önizleme ve
 * yazdırma çalışır, PDF kaydetme kapatılır.
 */
public final class RaporYazisi {

    private static final String[][] ADAYLAR = {
            {"segoeui.ttf", "segoeuib.ttf"},
            {"arial.ttf", "arialbd.ttf"},
            {"tahoma.ttf", "tahomabd.ttf"},
            {"calibri.ttf", "calibrib.ttf"},
            {"DejaVuSans.ttf", "DejaVuSans-Bold.ttf"},
            {"Arial.ttf", "Arial Bold.ttf"},
    };

    private static RaporYazisi varsayilan;

    private final Font duz;
    private final Font kalin;
    private final TrueTypeYazi duzDosya;
    private final TrueTypeYazi kalinDosya;
    private final String aciklama;
    private final FontRenderContext olcum = new FontRenderContext(null, true, true);
    private final Map<Double, Font> duzOnbellek = new HashMap<>();
    private final Map<Double, Font> kalinOnbellek = new HashMap<>();

    private RaporYazisi(Font duz, Font kalin, TrueTypeYazi duzDosya, TrueTypeYazi kalinDosya,
                        String aciklama) {
        this.duz = duz;
        this.kalin = kalin;
        this.duzDosya = duzDosya;
        this.kalinDosya = kalinDosya;
        this.aciklama = aciklama;
    }

    /** İlk çağrıda yazı tipi dosyalarını arar; sonra hep aynı nesne döner. */
    public static synchronized RaporYazisi varsayilan() {
        if (varsayilan == null) {
            varsayilan = bul();
        }
        return varsayilan;
    }

    private static RaporYazisi bul() {
        for (Path klasor : klasorler()) {
            for (String[] cift : ADAYLAR) {
                Path d = klasor.resolve(cift[0]);
                Path k = klasor.resolve(cift[1]);
                if (!Files.isRegularFile(d) || !Files.isRegularFile(k)) {
                    continue;
                }
                try {
                    TrueTypeYazi duzTt = TrueTypeYazi.oku(d);
                    TrueTypeYazi kalinTt = TrueTypeYazi.oku(k);
                    Font duzAwt = Font.createFont(Font.TRUETYPE_FONT, d.toFile());
                    Font kalinAwt = Font.createFont(Font.TRUETYPE_FONT, k.toFile());
                    return new RaporYazisi(duzAwt, kalinAwt, duzTt, kalinTt, duzTt.ad);
                } catch (IOException | FontFormatException | RuntimeException e) {
                    // bu çift okunamadı, sıradakine geç
                }
            }
        }
        return new RaporYazisi(new Font(Font.SANS_SERIF, Font.PLAIN, 1),
                new Font(Font.SANS_SERIF, Font.BOLD, 1), null, null, "SansSerif");
    }

    private static List<Path> klasorler() {
        List<Path> liste = new ArrayList<>();
        String windir = System.getenv("WINDIR");
        liste.add(Paths.get(windir == null ? "C:\\Windows" : windir, "Fonts"));
        String yerel = System.getenv("LOCALAPPDATA");
        if (yerel != null) {
            liste.add(Paths.get(yerel, "Microsoft", "Windows", "Fonts"));
        }
        liste.add(Paths.get("/usr/share/fonts/truetype/dejavu"));
        liste.add(Paths.get("/Library/Fonts"));
        liste.add(Paths.get("/System/Library/Fonts/Supplemental"));
        return liste;
    }

    /** PDF için gömülebilir yazı tipi bulundu mu. */
    public boolean pdfYazilabilir() {
        return duzDosya != null && kalinDosya != null;
    }

    /** Kullanılan yazı tipinin adı; hata mesajlarında ve testlerde gösterilir. */
    public String getAciklama() {
        return aciklama;
    }

    TrueTypeYazi dosya(boolean kalinMi) {
        return kalinMi ? kalinDosya : duzDosya;
    }

    /** Verilen boyutta AWT yazı tipi; ekrana ve yazıcıya çizerken kullanılır. */
    public synchronized Font font(boolean kalinMi, double boyut) {
        Map<Double, Font> onbellek = kalinMi ? kalinOnbellek : duzOnbellek;
        return onbellek.computeIfAbsent(boyut,
                b -> (kalinMi ? kalin : duz).deriveFont((float) (double) b));
    }

    /** Metnin punto cinsinden genişliği. */
    public double genislik(String metin, double boyut, boolean kalinMi) {
        if (metin == null || metin.isEmpty()) {
            return 0;
        }
        return font(kalinMi, boyut).getStringBounds(metin, olcum).getWidth();
    }
}
