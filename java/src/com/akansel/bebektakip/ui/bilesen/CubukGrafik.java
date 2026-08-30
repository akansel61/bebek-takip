package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Tema;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Yatay çubuk grafik: solda etiket, ortada çubuk, sağda değer. */
public class CubukGrafik extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SATIR_YUKSEKLIGI = 34;
    private static final int ETIKET_GENISLIGI = 52;
    private static final int DEGER_GENISLIGI = 78;
    private static final int CUBUK_KALINLIGI = 9;

    /** Grafikteki tek satır. */
    public static class Satir {
        public final String etiket;
        public final double deger;
        public final String degerMetni;
        public final boolean vurgulu;

        public Satir(String etiket, double deger, String degerMetni, boolean vurgulu) {
            this.etiket = etiket;
            this.deger = deger;
            this.degerMetni = degerMetni;
            this.vurgulu = vurgulu;
        }
    }

    private final List<Satir> satirlar = new ArrayList<>();
    private Color cubukRengi = Tema.VURGU;
    private String bosMesaj = "Gösterilecek veri yok";

    public CubukGrafik() {
        setOpaque(false);
    }

    public CubukGrafik cubukRengi(Color renk) {
        this.cubukRengi = renk;
        return this;
    }

    public void setSatirlar(List<Satir> yeni) {
        satirlar.clear();
        if (yeni != null) {
            satirlar.addAll(yeni);
        }
        revalidate();
        repaint();
    }

    public void setBosMesaj(String bosMesaj) {
        this.bosMesaj = bosMesaj;
    }

    @Override
    public Dimension getPreferredSize() {
        int satir = Math.max(satirlar.size(), 1);
        return new Dimension(280, satir * SATIR_YUKSEKLIGI);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            int w = getWidth();

            if (satirlar.isEmpty()) {
                g2.setFont(Tema.font(13));
                g2.setColor(Tema.METIN_SOLUK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(bosMesaj, (w - fm.stringWidth(bosMesaj)) / 2,
                        SATIR_YUKSEKLIGI / 2 + fm.getAscent() / 2);
                return;
            }

            double enBuyuk = 0;
            for (Satir s : satirlar) {
                enBuyuk = Math.max(enBuyuk, s.deger);
            }

            int cubukAlani = Math.max(20, w - ETIKET_GENISLIGI - DEGER_GENISLIGI - 20);

            for (int i = 0; i < satirlar.size(); i++) {
                Satir s = satirlar.get(i);
                int ust = i * SATIR_YUKSEKLIGI;
                int orta = ust + SATIR_YUKSEKLIGI / 2;

                g2.setFont(s.vurgulu ? Tema.fontKalin(12) : Tema.font(12));
                g2.setColor(s.vurgulu ? Tema.METIN : Tema.METIN_SOLUK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s.etiket, 0, orta + fm.getAscent() / 2 - 1);

                double oran = enBuyuk > 0 ? s.deger / enBuyuk : 0;
                int uzunluk = (int) Math.round(cubukAlani * oran);

                g2.setColor(Tema.CIZGI_ACIK);
                g2.fill(new RoundRectangle2D.Double(ETIKET_GENISLIGI,
                        orta - CUBUK_KALINLIGI / 2.0, cubukAlani, CUBUK_KALINLIGI,
                        CUBUK_KALINLIGI, CUBUK_KALINLIGI));

                if (uzunluk > 0) {
                    Color renk = Tema.karistir(cubukRengi, Color.WHITE,
                            0.55 * (1 - Math.min(1, oran + 0.15)));
                    g2.setColor(renk);
                    g2.fill(new RoundRectangle2D.Double(ETIKET_GENISLIGI,
                            orta - CUBUK_KALINLIGI / 2.0,
                            Math.max(CUBUK_KALINLIGI, uzunluk), CUBUK_KALINLIGI,
                            CUBUK_KALINLIGI, CUBUK_KALINLIGI));
                }

                g2.setFont(Tema.font(12));
                g2.setColor(Tema.METIN_SOLUK);
                FontMetrics fmD = g2.getFontMetrics();
                String metin = s.degerMetni == null ? "" : s.degerMetni;
                g2.drawString(metin, w - fmD.stringWidth(metin), orta + fmD.getAscent() / 2 - 1);

                if (i < satirlar.size() - 1) {
                    g2.setColor(Tema.CIZGI_ACIK);
                    g2.drawLine(0, ust + SATIR_YUKSEKLIGI - 1, w, ust + SATIR_YUKSEKLIGI - 1);
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
