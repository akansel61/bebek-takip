package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Son üç günün ateş seyri. Arka planda 37,5 ve 38 °C eşikleri renkli bant
 * olarak durur; her ölçüm seviyesine göre renklenmiş bir nokta, noktalar
 * da zaman sırasıyla birbirine bağlanır. Noktanın üstüne gelince saat ve
 * derece görünür.
 */
public class AtesGrafigi extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final int SAAT = 72;

    private static final int SOL = 42;
    private static final int SAG = 14;
    private static final int UST = 12;
    private static final int ALT = 26;

    private static final DateTimeFormatter GUN = DateTimeFormatter.ofPattern("d MMM EEE", Tema.TR);
    private static final DateTimeFormatter IPUCU =
            DateTimeFormatter.ofPattern("d MMMM HH:mm", Tema.TR);

    private final List<AtesKayit> olcumler = new ArrayList<>();
    private LocalDateTime bitis = LocalDateTime.now();

    public AtesGrafigi() {
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /** Derecesi girilmiş ve son 72 saate düşen ölçümleri alır. */
    public void setOlcumler(List<AtesKayit> tumu, LocalDateTime simdi) {
        bitis = simdi;
        LocalDateTime baslangic = simdi.minusHours(SAAT);
        olcumler.clear();
        for (AtesKayit a : tumu) {
            if (a.getDerece() > 0 && !a.zaman().isBefore(baslangic) && !a.zaman().isAfter(simdi)) {
                olcumler.add(a);
            }
        }
        olcumler.sort(Comparator.comparing(AtesKayit::zaman));
        repaint();
    }

    public int olcumSayisi() {
        return olcumler.size();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 170);
    }

    private double altSinir() {
        double en = 36.0;
        for (AtesKayit a : olcumler) {
            en = Math.min(en, Math.floor((a.getDerece() - 0.2) * 2) / 2);
        }
        return Math.max(34.0, en);
    }

    private double ustSinir() {
        double en = 39.5;
        for (AtesKayit a : olcumler) {
            en = Math.max(en, Math.ceil((a.getDerece() + 0.2) * 2) / 2);
        }
        return Math.min(43.0, en);
    }

    private double x(LocalDateTime t) {
        double alan = getWidth() - SOL - SAG;
        double gecen = Duration.between(bitis.minusHours(SAAT), t).toMinutes();
        return SOL + alan * gecen / (SAAT * 60.0);
    }

    private double y(double derece, double alt, double ust) {
        double alan = getHeight() - UST - ALT;
        return UST + alan * (ust - derece) / (ust - alt);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        double alt = altSinir();
        double ust = ustSinir();
        for (AtesKayit a : olcumler) {
            double dx = x(a.zaman()) - e.getX();
            double dy = y(a.getDerece(), alt, ust) - e.getY();
            if (dx * dx + dy * dy <= 64) {
                return a.zaman().format(IPUCU) + " · "
                        + String.format(Tema.TR, "%.1f °C", a.getDerece())
                        + " · " + a.seviye().getAd();
            }
        }
        return null;
    }

    /** Seviyenin grafik ve tablo rengi. */
    public static Color seviyeRengi(AtesKayit.Seviye s) {
        switch (s) {
            case YUKSEK:
                return new Color(0xC4161C);
            case ATES:
                return Tema.KIRMIZI;
            case HAFIF:
                return Tema.TURUNCU;
            case NORMAL:
                return Tema.YESIL;
            default:
                return Tema.PASIF_KUTU;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int w = getWidth();
            int h = getHeight();
            double alt = altSinir();
            double ust = ustSinir();
            double sol = SOL;
            double sag = w - SAG;

            // eşik bantları
            g2.setColor(Tema.karistir(Tema.KIRMIZI, Color.WHITE, 0.93));
            g2.fill(new Rectangle2D.Double(sol, y(ust, alt, ust), sag - sol,
                    y(AtesKayit.ATES_SINIRI, alt, ust) - y(ust, alt, ust)));
            g2.setColor(Tema.karistir(Tema.TURUNCU, Color.WHITE, 0.9));
            g2.fill(new Rectangle2D.Double(sol, y(AtesKayit.ATES_SINIRI, alt, ust), sag - sol,
                    y(AtesKayit.HAFIF_SINIR, alt, ust) - y(AtesKayit.ATES_SINIRI, alt, ust)));

            // yatay ızgara ve derece etiketleri
            g2.setFont(Tema.font(11));
            FontMetrics fm = g2.getFontMetrics();
            for (double d = Math.ceil(alt); d <= ust; d += 1) {
                double yy = y(d, alt, ust);
                g2.setColor(Tema.CIZGI_ACIK);
                g2.draw(new Line2D.Double(sol, yy, sag, yy));
                g2.setColor(Tema.METIN_SOLUK);
                String etiket = (int) d + "°";
                g2.drawString(etiket, SOL - 8 - fm.stringWidth(etiket),
                        (float) (yy + fm.getAscent() / 2.0 - 1));
            }
            double esik = y(AtesKayit.ATES_SINIRI, alt, ust);
            g2.setColor(Tema.karistir(Tema.KIRMIZI, Color.WHITE, 0.45));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{4f, 3f}, 0f));
            g2.draw(new Line2D.Double(sol, esik, sag, esik));

            // gün sınırları ve gün adları
            LocalDateTime bas = bitis.minusHours(SAAT);
            LocalDateTime parca = bas;
            LocalDate bugun = bitis.toLocalDate();
            while (parca.isBefore(bitis)) {
                LocalDateTime gece = parca.toLocalDate().plusDays(1).atStartOfDay();
                LocalDateTime son = gece.isBefore(bitis) ? gece : bitis;
                double x1 = x(parca);
                double x2 = x(son);
                if (gece.isBefore(bitis)) {
                    g2.setColor(Tema.CIZGI);
                    g2.draw(new Line2D.Double(x2, UST, x2, h - ALT));
                }
                String gun = parca.toLocalDate().equals(bugun) ? "Bugün"
                        : parca.toLocalDate().format(GUN);
                int gw = fm.stringWidth(gun);
                if (x2 - x1 > gw + 10) {
                    g2.setColor(Tema.METIN_SOLUK);
                    g2.drawString(gun, (float) ((x1 + x2 - gw) / 2), h - ALT + 17);
                }
                parca = son;
            }
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Tema.CIZGI);
            g2.draw(new Line2D.Double(sol, h - ALT, sag, h - ALT));

            if (olcumler.isEmpty()) {
                g2.setFont(Tema.font(13));
                FontMetrics fmB = g2.getFontMetrics();
                String mesaj = "Son 3 günde ateş ölçümü yok";
                g2.setColor(Tema.METIN_SOLUK);
                g2.drawString(mesaj, (float) ((sol + sag - fmB.stringWidth(mesaj)) / 2),
                        (float) ((UST + h - ALT) / 2.0 + fmB.getAscent() / 2.0));
                return;
            }

            // ölçümleri birleştiren çizgi
            if (olcumler.size() > 1) {
                Path2D yol = new Path2D.Double();
                for (int i = 0; i < olcumler.size(); i++) {
                    AtesKayit a = olcumler.get(i);
                    double px = x(a.zaman());
                    double py = y(a.getDerece(), alt, ust);
                    if (i == 0) {
                        yol.moveTo(px, py);
                    } else {
                        yol.lineTo(px, py);
                    }
                }
                g2.setColor(Tema.karistir(Tema.METIN_SOLUK, Color.WHITE, 0.25));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(yol);
            }

            AtesKayit enYuksek = olcumler.get(0);
            for (AtesKayit a : olcumler) {
                if (a.getDerece() > enYuksek.getDerece()) {
                    enYuksek = a;
                }
                double px = x(a.zaman());
                double py = y(a.getDerece(), alt, ust);
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(px - 6, py - 6, 12, 12));
                g2.setColor(seviyeRengi(a.seviye()));
                g2.fill(new Ellipse2D.Double(px - 4.5, py - 4.5, 9, 9));
            }

            // en yüksek ölçümün değeri noktanın üstünde
            g2.setFont(Tema.fontKalin(11));
            FontMetrics fmK = g2.getFontMetrics();
            String deger = String.format(Tema.TR, "%.1f°", enYuksek.getDerece());
            double px = x(enYuksek.zaman());
            double py = y(enYuksek.getDerece(), alt, ust);
            double lx = Math.max(sol, Math.min(sag - fmK.stringWidth(deger),
                    px - fmK.stringWidth(deger) / 2.0));
            g2.setColor(Tema.karistir(seviyeRengi(enYuksek.seviye()), Color.BLACK, 0.15));
            g2.drawString(deger, (float) lx, (float) Math.max(UST + fmK.getAscent(), py - 9));
        } finally {
            g2.dispose();
        }
    }
}
