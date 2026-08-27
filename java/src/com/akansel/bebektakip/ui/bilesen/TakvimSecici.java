package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Ikonlar;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Açılır takvim: ay gezinme, gün seçme ve "Bugün" kısayolu. */
public class TakvimSecici extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int KENAR = 10;
    private static final int HUCRE = 34;
    private static final int BASLIK_Y = 34;
    private static final int GUN_ADI_Y = 26;
    private static final int ALT_Y = 34;
    private static final int SATIR = 6;

    private static final DateTimeFormatter AY_YIL =
            DateTimeFormatter.ofPattern("MMMM yyyy", Tema.TR);
    private static final String[] GUN_BASLIKLARI = {"Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pa"};

    private LocalDate gosterilenAy;
    private LocalDate secili;
    private final Consumer<LocalDate> geriCagri;

    private int uzerindekiGun = -1;
    private boolean uzerindeGeri;
    private boolean uzerindeIleri;
    private boolean uzerindeBugun;

    public TakvimSecici(LocalDate secili, Consumer<LocalDate> geriCagri) {
        this.secili = secili == null ? LocalDate.now() : secili;
        this.gosterilenAy = this.secili.withDayOfMonth(1);
        this.geriCagri = geriCagri;
        setOpaque(true);
        setBackground(Tema.YUZEY);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int genislik = KENAR * 2 + HUCRE * 7;
        int yukseklik = KENAR * 2 + BASLIK_Y + GUN_ADI_Y + HUCRE * SATIR + ALT_Y;
        setPreferredSize(new Dimension(genislik, yukseklik));

        MouseAdapter fare = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                guncelleUzerinde(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                guncelleUzerinde(-100, -100);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                tiklandi(e.getX(), e.getY());
            }
        };
        addMouseListener(fare);
        addMouseMotionListener(fare);
    }

    private int izgaraUst() {
        return KENAR + BASLIK_Y + GUN_ADI_Y;
    }

    /** Ayın ilk gününün ızgaradaki sütunu (pazartesi = 0). */
    private int ilkSutun() {
        DayOfWeek g = gosterilenAy.getDayOfWeek();
        return g.getValue() - 1;
    }

    /** Verilen noktadaki gün numarası; gün yoksa -1. */
    private int gunBul(int x, int y) {
        int ust = izgaraUst();
        if (x < KENAR || x >= KENAR + HUCRE * 7 || y < ust || y >= ust + HUCRE * SATIR) {
            return -1;
        }
        int sutun = (x - KENAR) / HUCRE;
        int satir = (y - ust) / HUCRE;
        int sira = satir * 7 + sutun - ilkSutun() + 1;
        int ayGun = gosterilenAy.lengthOfMonth();
        return (sira >= 1 && sira <= ayGun) ? sira : -1;
    }

    private boolean geriAlaninda(int x, int y) {
        return y >= KENAR && y < KENAR + BASLIK_Y && x >= KENAR && x < KENAR + 30;
    }

    private boolean ileriAlaninda(int x, int y) {
        int sag = KENAR + HUCRE * 7;
        return y >= KENAR && y < KENAR + BASLIK_Y && x >= sag - 30 && x < sag;
    }

    private boolean bugunAlaninda(int x, int y) {
        int ust = izgaraUst() + HUCRE * SATIR;
        return y >= ust && y < ust + ALT_Y;
    }

    private void guncelleUzerinde(int x, int y) {
        int g = gunBul(x, y);
        boolean geri = geriAlaninda(x, y);
        boolean ileri = ileriAlaninda(x, y);
        boolean bugun = bugunAlaninda(x, y);
        if (g != uzerindekiGun || geri != uzerindeGeri
                || ileri != uzerindeIleri || bugun != uzerindeBugun) {
            uzerindekiGun = g;
            uzerindeGeri = geri;
            uzerindeIleri = ileri;
            uzerindeBugun = bugun;
            repaint();
        }
    }

    private void tiklandi(int x, int y) {
        if (geriAlaninda(x, y)) {
            gosterilenAy = gosterilenAy.minusMonths(1);
            repaint();
            return;
        }
        if (ileriAlaninda(x, y)) {
            gosterilenAy = gosterilenAy.plusMonths(1);
            repaint();
            return;
        }
        if (bugunAlaninda(x, y)) {
            sec(LocalDate.now());
            return;
        }
        int g = gunBul(x, y);
        if (g > 0) {
            sec(gosterilenAy.withDayOfMonth(g));
        }
    }

    private void sec(LocalDate tarih) {
        secili = tarih;
        if (geriCagri != null) {
            geriCagri.accept(tarih);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int genislik = getWidth();

            // başlık
            g2.setFont(Tema.fontKalin(14));
            g2.setColor(Tema.METIN);
            FontMetrics fmB = g2.getFontMetrics();
            String baslik = gosterilenAy.format(AY_YIL);
            g2.drawString(baslik, (genislik - fmB.stringWidth(baslik)) / 2,
                    KENAR + BASLIK_Y / 2 + fmB.getAscent() / 2 - 2);

            cizGezinme(g2, KENAR, KENAR + (BASLIK_Y - 22) / 2, uzerindeGeri, true);
            cizGezinme(g2, KENAR + HUCRE * 7 - 26, KENAR + (BASLIK_Y - 22) / 2,
                    uzerindeIleri, false);

            // gün başlıkları
            g2.setFont(Tema.font(11));
            g2.setColor(Tema.METIN_SOLUK);
            FontMetrics fmG = g2.getFontMetrics();
            for (int i = 0; i < 7; i++) {
                String ad = GUN_BASLIKLARI[i];
                int x = KENAR + i * HUCRE + (HUCRE - fmG.stringWidth(ad)) / 2;
                g2.drawString(ad, x, KENAR + BASLIK_Y + fmG.getAscent() + 2);
            }

            // günler
            int ust = izgaraUst();
            int ayGun = gosterilenAy.lengthOfMonth();
            int ilk = ilkSutun();
            LocalDate bugun = LocalDate.now();
            g2.setFont(Tema.font(13));
            FontMetrics fmD = g2.getFontMetrics();

            for (int gun = 1; gun <= ayGun; gun++) {
                int sira = ilk + gun - 1;
                int satir = sira / 7;
                int sutun = sira % 7;
                int hx = KENAR + sutun * HUCRE;
                int hy = ust + satir * HUCRE;
                LocalDate tarih = gosterilenAy.withDayOfMonth(gun);

                boolean seciliMi = tarih.equals(secili);
                boolean bugunMu = tarih.equals(bugun);
                boolean uzerindeMi = gun == uzerindekiGun;

                if (seciliMi) {
                    g2.setColor(Tema.VURGU);
                    g2.fill(new RoundRectangle2D.Double(hx + 3, hy + 3,
                            HUCRE - 6, HUCRE - 6, 9, 9));
                } else if (uzerindeMi) {
                    g2.setColor(Tema.ARKA);
                    g2.fill(new RoundRectangle2D.Double(hx + 3, hy + 3,
                            HUCRE - 6, HUCRE - 6, 9, 9));
                }

                Color yazi = seciliMi ? Color.WHITE : (bugunMu ? Tema.MAVI : Tema.METIN);
                g2.setFont(bugunMu || seciliMi ? Tema.fontKalin(13) : Tema.font(13));
                g2.setColor(yazi);
                String metin = String.valueOf(gun);
                int mx = hx + (HUCRE - g2.getFontMetrics().stringWidth(metin)) / 2;
                int my = hy + HUCRE / 2 + fmD.getAscent() / 2 - 1;
                g2.drawString(metin, mx, my);
            }

            // alt satır
            int altUst = ust + HUCRE * SATIR;
            g2.setColor(Tema.CIZGI);
            g2.drawLine(KENAR, altUst, genislik - KENAR, altUst);
            if (uzerindeBugun) {
                g2.setColor(Tema.ARKA);
                g2.fill(new RoundRectangle2D.Double(KENAR, altUst + 4,
                        genislik - KENAR * 2, ALT_Y - 8, 8, 8));
            }
            g2.setFont(Tema.font(12));
            g2.setColor(uzerindeBugun ? Tema.METIN : Tema.MAVI);
            FontMetrics fmA = g2.getFontMetrics();
            String altMetin = "Bugün: " + Tema.tamTarih(bugun);
            g2.drawString(altMetin, (genislik - fmA.stringWidth(altMetin)) / 2,
                    altUst + ALT_Y / 2 + fmA.getAscent() / 2);
        } finally {
            g2.dispose();
        }
    }

    private void cizGezinme(Graphics2D g2, int x, int y, boolean uzerinde, boolean sol) {
        if (uzerinde) {
            g2.setColor(Tema.ARKA);
            g2.fill(new RoundRectangle2D.Double(x, y, 26, 22, 7, 7));
        }
        Color renk = uzerinde ? Tema.METIN : Tema.METIN_SOLUK;
        if (sol) {
            Ikonlar.okSol(g2, x + 5, y + 3, 16, renk);
        } else {
            Ikonlar.okSag(g2, x + 5, y + 3, 16, renk);
        }
    }

    /** Takvimi verilen bileşenin altında açar. */
    public static void goster(Component sahip, int x, int y,
                              LocalDate secili, Consumer<LocalDate> geriCagri) {
        final JPopupMenu acilir = new JPopupMenu();
        acilir.setBorder(BorderFactory.createLineBorder(Tema.CIZGI));
        acilir.setBackground(Tema.YUZEY);
        TakvimSecici takvim = new TakvimSecici(secili, tarih -> {
            geriCagri.accept(tarih);
        });
        acilir.add(takvim);
        takvim.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (takvim.gunBul(e.getX(), e.getY()) > 0
                        || takvim.bugunAlaninda(e.getX(), e.getY())) {
                    acilir.setVisible(false);
                }
            }
        });
        acilir.show(sahip, x, y);
    }
}
