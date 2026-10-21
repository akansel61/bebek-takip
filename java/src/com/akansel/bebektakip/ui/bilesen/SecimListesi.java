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
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Hücrenin altında açılan seçim listesi. Çoklu biçimde (belirtiler) her
 * tıklama bir satırı işaretler ya da kaldırır ve liste açık kalır; tekli
 * biçimde (ölçüm yeri) seçim yapılınca liste kapanır.
 */
public class SecimListesi extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SATIR = 30;
    private static final int KENAR = 6;

    private final List<String> secenekler = new ArrayList<>();
    private final Set<String> secili = new LinkedHashSet<>();
    private final boolean coklu;
    private final Consumer<List<String>> degisince;
    private int uzerindeki = -1;
    private Runnable kapat = () -> { };

    private SecimListesi(List<String> secenekler, Collection<String> seciliOlanlar,
                         boolean coklu, Consumer<List<String>> degisince) {
        this.secenekler.addAll(secenekler);
        // Listede olmayan eski seçimler de görünsün ki kaldırılabilsin.
        for (String s : seciliOlanlar) {
            if (!this.secenekler.contains(s)) {
                this.secenekler.add(s);
            }
        }
        this.secili.addAll(seciliOlanlar);
        this.coklu = coklu;
        this.degisince = degisince;
        setOpaque(true);
        setBackground(Tema.YUZEY);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter fare = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                uzerindeyiGuncelle(satirBul(e.getY()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                uzerindeyiGuncelle(-1);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int satir = satirBul(e.getY());
                if (satir >= 0) {
                    tiklandi(satir);
                }
            }
        };
        addMouseListener(fare);
        addMouseMotionListener(fare);
    }

    private int satirBul(int y) {
        int satir = (y - KENAR) / SATIR;
        return y >= KENAR && satir < secenekler.size() ? satir : -1;
    }

    private void uzerindeyiGuncelle(int satir) {
        if (satir != uzerindeki) {
            uzerindeki = satir;
            repaint();
        }
    }

    private void tiklandi(int satir) {
        String s = secenekler.get(satir);
        if (coklu) {
            if (!secili.remove(s)) {
                secili.add(s);
            }
            repaint();
        } else {
            secili.clear();
            secili.add(s);
        }
        degisince.accept(sirali());
        if (!coklu) {
            kapat.run();
        }
    }

    /** Seçilenler, listedeki sırayla. */
    private List<String> sirali() {
        List<String> sonuc = new ArrayList<>();
        for (String s : secenekler) {
            if (secili.contains(s)) {
                sonuc.add(s);
            }
        }
        return sonuc;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Tema.font(13));
        int en = 0;
        for (String s : secenekler) {
            en = Math.max(en, fm.stringWidth(s));
        }
        return new Dimension(Math.max(200, en + 64), secenekler.size() * SATIR + KENAR * 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            g2.setFont(Tema.font(13));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < secenekler.size(); i++) {
                int y = KENAR + i * SATIR;
                String s = secenekler.get(i);
                boolean isaretli = secili.contains(s);
                if (i == uzerindeki) {
                    g2.setColor(Tema.ARKA);
                    g2.fill(new RoundRectangle2D.Double(KENAR, y, getWidth() - KENAR * 2.0,
                            SATIR, 8, 8));
                }
                double kutuY = y + (SATIR - 18) / 2.0;
                if (coklu) {
                    RoundRectangle2D kutu = new RoundRectangle2D.Double(16, kutuY, 18, 18, 5, 5);
                    if (isaretli) {
                        g2.setColor(Tema.YESIL);
                        g2.fill(kutu);
                        Ikonlar.onay(g2, 19, kutuY + 3, 12, Color.WHITE);
                    } else {
                        g2.setColor(Tema.PASIF_KUTU);
                        g2.draw(kutu);
                    }
                } else {
                    Ellipse2D daire = new Ellipse2D.Double(16, kutuY, 18, 18);
                    g2.setColor(isaretli ? Tema.VURGU : Tema.PASIF_KUTU);
                    g2.draw(daire);
                    if (isaretli) {
                        g2.fill(new Ellipse2D.Double(20.5, kutuY + 4.5, 9, 9));
                    }
                }
                g2.setColor(Tema.METIN);
                g2.drawString(s, 44, y + (SATIR - fm.getHeight()) / 2 + fm.getAscent());
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Listeyi verilen bileşenin üstünde, belirtilen noktada açar. Her
     * değişiklikte seçilenlerin güncel listesi geri çağrıya verilir.
     */
    public static void goster(Component sahip, int x, int y, List<String> secenekler,
                              Collection<String> seciliOlanlar, boolean coklu,
                              Consumer<List<String>> degisince) {
        JPopupMenu acilir = new JPopupMenu();
        acilir.setBorder(BorderFactory.createLineBorder(Tema.CIZGI));
        acilir.setBackground(Tema.YUZEY);
        SecimListesi liste = new SecimListesi(secenekler, seciliOlanlar, coklu, degisince);
        liste.kapat = () -> acilir.setVisible(false);
        acilir.add(liste);
        acilir.show(sahip, x, y);
    }
}
