package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.ui.bilesen.AramaAlani;
import com.akansel.bebektakip.ui.bilesen.BosDurum;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.PanelBasligi;
import com.akansel.bebektakip.ui.tablo.KayitTabloModeli;
import com.akansel.bebektakip.ui.tablo.KayitTablosu;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Rectangle;
import java.util.function.Consumer;

/** Tüm kayıtların düzenlenebilir tablosu ve arama alanı. */
public class KayitlarPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final KayitTabloModeli modeli;
    private final KayitTablosu tablo;
    private final PanelBasligi baslik;
    private final AramaAlani arama;
    private final BosDurum bosDurum = new BosDurum(
            "Henüz kayıt yok.\n\"Yeni Kayıt\" butonuna basarak başlayın.");
    private final CardLayout duzen = new CardLayout();
    private final JPanel kap = new JPanel(duzen);

    public KayitlarPanel(KayitDeposu depo, Consumer<Kayit> silme, Runnable degisince) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        modeli = new KayitTabloModeli(depo.getKayitlar(), true, false);
        modeli.setDegisiklikDinleyici(degisince);
        tablo = new KayitTablosu(modeli, false);
        tablo.setSilmeIstegi(silme);

        arama = new AramaAlani("Ara…", 200, metin -> {
            modeli.setFiltre(metin);
            durumGuncelle();
        });

        JScrollPane kaydirma = new JScrollPane(tablo);
        kaydirma.setBorder(BorderFactory.createEmptyBorder());
        kaydirma.getViewport().setBackground(Tema.YUZEY);
        kaydirma.setBackground(Tema.YUZEY);
        kaydirma.getVerticalScrollBar().setUnitIncrement(24);
        JViewport ustBaslik = kaydirma.getColumnHeader();
        if (ustBaslik != null) {
            ustBaslik.setBackground(Tema.YUZEY);
        }

        kap.setOpaque(false);
        kap.add(kaydirma, "tablo");
        kap.add(bosDurum, "bos");

        baslik = new PanelBasligi("Tüm Kayıtlar", arama);

        Kart kart = new Kart(new BorderLayout());
        kart.add(baslik, BorderLayout.NORTH);
        kart.add(kap, BorderLayout.CENTER);
        add(kart, BorderLayout.CENTER);
    }

    /** Kaynak liste değiştiğinde tabloyu tazeler. */
    public void yenile() {
        modeli.yenile();
        durumGuncelle();
    }

    private void durumGuncelle() {
        boolean filtreVar = !modeli.getFiltre().isEmpty();
        if (modeli.getRowCount() == 0) {
            bosDurum.setMesaj(filtreVar
                    ? "Aramanıza uyan kayıt bulunamadı."
                    : "Henüz kayıt yok.\n\"Yeni Kayıt\" butonuna basarak başlayın.");
            duzen.show(kap, "bos");
        } else {
            duzen.show(kap, "tablo");
        }
        baslik.setBaslik(filtreVar
                ? "Arama sonucu · " + modeli.getRowCount() + " kayıt"
                : "Tüm Kayıtlar");
    }

    /** Yeni eklenen kaydın satırını seçip görünür alana getirir. */
    public void kaydaGit(Kayit kayit) {
        arama.temizle();
        modeli.yenile();
        durumGuncelle();
        for (int satir = 0; satir < modeli.getRowCount(); satir++) {
            if (modeli.satirdaki(satir) == kayit) {
                final int hedef = satir;
                SwingUtilities.invokeLater(() -> {
                    tablo.setRowSelectionInterval(hedef, hedef);
                    Rectangle r = tablo.getCellRect(hedef, 0, true);
                    tablo.scrollRectToVisible(r);
                    tablo.requestFocusInWindow();
                });
                return;
            }
        }
    }

    /** Arama kutusuna odaklanır (Ctrl+F). */
    public void aramayaOdaklan() {
        arama.requestFocusInWindow();
        for (java.awt.Component c : arama.getComponents()) {
            c.requestFocusInWindow();
            break;
        }
    }
}
