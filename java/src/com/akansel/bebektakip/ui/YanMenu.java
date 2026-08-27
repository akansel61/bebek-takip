package com.akansel.bebektakip.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Sol taraftaki gezinme şeridi. */
public class YanMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final String GORUNUM_DASHBOARD = "dashboard";
    public static final String GORUNUM_KAYITLAR = "kayitlar";
    public static final String GORUNUM_ISTATISTIK = "istatistik";

    public static final int GENISLIK = 220;

    private final List<MenuOgesi> ogeler = new ArrayList<>();
    private final JLabel sonGuncelleme = new JLabel("—");

    public YanMenu(String surum, Consumer<String> secim) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(Tema.YUZEY);
        setPreferredSize(new Dimension(GENISLIK, 0));
        setMinimumSize(new Dimension(GENISLIK, 0));
        setMaximumSize(new Dimension(GENISLIK, Integer.MAX_VALUE));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.CIZGI));

        add(logoAlani(surum), BorderLayout.NORTH);

        JPanel gezinme = new JPanel();
        gezinme.setOpaque(false);
        gezinme.setLayout(new BoxLayout(gezinme, BoxLayout.Y_AXIS));
        gezinme.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        gezinme.add(oge(GORUNUM_DASHBOARD, "Dashboard", Ikonlar::grafik, secim));
        gezinme.add(Box.createVerticalStrut(4));
        gezinme.add(oge(GORUNUM_KAYITLAR, "Kayıtlar", Ikonlar::liste, secim));
        gezinme.add(Box.createVerticalStrut(4));
        gezinme.add(oge(GORUNUM_ISTATISTIK, "İstatistikler", Ikonlar::trend, secim));
        gezinme.add(Box.createVerticalGlue());

        JPanel orta = new JPanel(new BorderLayout());
        orta.setOpaque(false);
        orta.add(gezinme, BorderLayout.NORTH);
        add(orta, BorderLayout.CENTER);

        add(altBilgi(), BorderLayout.SOUTH);
        setAktif(GORUNUM_DASHBOARD);
    }

    private MenuOgesi oge(String anahtar, String metin, IkonCizici ikon,
                          Consumer<String> secim) {
        MenuOgesi o = new MenuOgesi(anahtar, metin, ikon, secim);
        ogeler.add(o);
        return o;
    }

    private JComponent logoAlani(String surum) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.CIZGI),
                BorderFactory.createEmptyBorder(18, 16, 18, 16)));

        JComponent simge = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    Tema.kaliteAyarla(g2);
                    g2.setColor(Tema.VURGU);
                    g2.fill(new RoundRectangle2D.Double(0, 0, 38, 38, 11, 11));
                    Ikonlar.bebek(g2, 6, 6, 26, Color.WHITE);
                    Ikonlar.bebekYuzu(g2, 6, 6, 26, Tema.VURGU);
                } finally {
                    g2.dispose();
                }
            }
        };
        p.add(simge, BorderLayout.WEST);

        JPanel yazi = new JPanel();
        yazi.setOpaque(false);
        yazi.setLayout(new BoxLayout(yazi, BoxLayout.Y_AXIS));
        JLabel ad = new JLabel("Bebek Takip");
        ad.setFont(Tema.fontKalin(15));
        ad.setForeground(Tema.METIN);
        ad.setAlignmentX(LEFT_ALIGNMENT);
        JLabel s = new JLabel(surum);
        s.setFont(Tema.font(11));
        s.setForeground(Tema.METIN_SOLUK);
        s.setAlignmentX(LEFT_ALIGNMENT);
        yazi.add(Box.createVerticalGlue());
        yazi.add(ad);
        yazi.add(Box.createVerticalStrut(1));
        yazi.add(s);
        yazi.add(Box.createVerticalGlue());
        p.add(yazi, BorderLayout.CENTER);
        return p;
    }

    private JComponent altBilgi() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.CIZGI),
                BorderFactory.createEmptyBorder(12, 16, 14, 16)));

        JLabel baslik = new JLabel("Son güncelleme");
        baslik.setFont(Tema.font(12));
        baslik.setForeground(Tema.METIN_SOLUK);
        baslik.setAlignmentX(LEFT_ALIGNMENT);

        sonGuncelleme.setFont(Tema.font(12));
        sonGuncelleme.setForeground(Tema.METIN);
        sonGuncelleme.setAlignmentX(LEFT_ALIGNMENT);

        JLabel telif = new JLabel(Tema.TELIF);
        telif.setFont(Tema.font(11));
        telif.setForeground(Tema.PASIF_KUTU);
        telif.setAlignmentX(LEFT_ALIGNMENT);

        p.add(baslik);
        p.add(Box.createVerticalStrut(3));
        p.add(sonGuncelleme);
        p.add(Box.createVerticalStrut(12));
        p.add(telif);
        return p;
    }

    public void setAktif(String anahtar) {
        for (MenuOgesi o : ogeler) {
            o.setAktif(o.anahtar.equals(anahtar));
        }
    }

    public void setSonGuncelleme(String metin) {
        sonGuncelleme.setText(metin);
    }

    /** Tek gezinme satırı. */
    private static class MenuOgesi extends JComponent {

        private static final long serialVersionUID = 1L;

        private final String anahtar;
        private final String metin;
        private final IkonCizici ikon;
        private boolean aktif;
        private boolean uzerinde;

        MenuOgesi(String anahtar, String metin, IkonCizici ikon, Consumer<String> secim) {
            this.anahtar = anahtar;
            this.metin = metin;
            this.ikon = ikon;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    uzerinde = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    uzerinde = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    secim.accept(anahtar);
                }
            });
        }

        void setAktif(boolean aktif) {
            if (this.aktif != aktif) {
                this.aktif = aktif;
                repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(GENISLIK - 16, 40);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 40);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Tema.kaliteAyarla(g2);
                if (aktif || uzerinde) {
                    g2.setColor(aktif ? Tema.SECILI : Tema.ARKA);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 9, 9));
                }
                Color renk = aktif || uzerinde ? Tema.METIN : Tema.METIN_SOLUK;
                ikon.ciz(g2, 12, (getHeight() - 18) / 2.0, 18, renk);
                g2.setFont(aktif ? Tema.fontKalin(14) : Tema.font(14));
                g2.setColor(renk);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(metin, 40, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
