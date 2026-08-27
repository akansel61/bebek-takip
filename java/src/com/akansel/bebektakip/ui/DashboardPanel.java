package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.store.KayitDeposu;
import com.akansel.bebektakip.ui.bilesen.BosDurum;
import com.akansel.bebektakip.ui.bilesen.DuzButon;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.KartIzgara;
import com.akansel.bebektakip.ui.bilesen.KaydirilabilirPanel;
import com.akansel.bebektakip.ui.bilesen.IstatistikKarti;
import com.akansel.bebektakip.ui.bilesen.PanelBasligi;
import com.akansel.bebektakip.ui.tablo.KayitTabloModeli;
import com.akansel.bebektakip.ui.tablo.KayitTablosu;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/** Özet ekranı: günlük ölçümler ve son kayıtlar. */
public class DashboardPanel extends KaydirilabilirPanel {

    private static final long serialVersionUID = 1L;

    private static final int SON_KAYIT_SAYISI = 5;

    private final KayitDeposu depo;

    private final IstatistikKarti kartKayit =
            new IstatistikKarti("Bugünkü kayıt", "", Tema.METIN);
    private final IstatistikKarti kartMama =
            new IstatistikKarti("Toplam mama", "ml / gr", Tema.TURUNCU);
    private final IstatistikKarti kartCis =
            new IstatistikKarti("Çiş sayısı", "", Tema.MAVI);
    private final IstatistikKarti kartKaka =
            new IstatistikKarti("Kaka sayısı", "", Tema.KAHVE);
    private final IstatistikKarti kartEmzirme =
            new IstatistikKarti("Emzirme", "kez", Tema.MOR);

    private final KayitTabloModeli modeli;
    private final KayitTablosu tablo;
    private final CardLayout tabloDuzeni = new CardLayout();
    private final JPanel tabloKabi = new JPanel(tabloDuzeni);

    public DashboardPanel(KayitDeposu depo, Runnable tumunuGor, Consumer<Kayit> silme) {
        super(new BorderLayout(0, 20));
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        KartIzgara izgara = new KartIzgara(145, 16);
        izgara.add(kartKayit);
        izgara.add(kartMama);
        izgara.add(kartCis);
        izgara.add(kartKaka);
        izgara.add(kartEmzirme);
        add(izgara, BorderLayout.NORTH);

        modeli = new KayitTabloModeli(depo.getKayitlar(), false, true);
        modeli.setEnFazlaSatir(SON_KAYIT_SAYISI);
        tablo = new KayitTablosu(modeli, true);
        tablo.setSilmeIstegi(silme);

        DuzButon tumunuGorButonu = new DuzButon("Tümünü gör", Ikonlar::okSag,
                DuzButon.Bicim.YALIN);
        tumunuGorButonu.ikonSagda(true).ikonBoyutu(13).bosluk(10, 6);
        tumunuGorButonu.addActionListener(e -> tumunuGor.run());

        JPanel tabloAlani = new JPanel(new BorderLayout());
        tabloAlani.setOpaque(false);
        tabloAlani.add(tablo.getTableHeader(), BorderLayout.NORTH);
        tabloAlani.add(tablo, BorderLayout.CENTER);

        tabloKabi.setOpaque(false);
        tabloKabi.add(tabloAlani, "tablo");
        BosDurum bos = new BosDurum("Henüz kayıt yok.\n\"Yeni Kayıt\" ile başlayın.");
        bos.setPreferredSize(new Dimension(200, 170));
        tabloKabi.add(bos, "bos");

        Kart kart = new Kart(new BorderLayout());
        kart.add(new PanelBasligi("Son Kayıtlar", tumunuGorButonu), BorderLayout.NORTH);
        kart.add(tabloKabi, BorderLayout.CENTER);
        add(kart, BorderLayout.CENTER);
    }

    /** Ölçümleri ve son kayıt tablosunu yeniden hesaplar. */
    public void yenile() {
        modeli.yenile();
        tabloDuzeni.show(tabloKabi, modeli.getRowCount() == 0 ? "bos" : "tablo");

        List<Kayit> bugun = depo.gununKayitlari(LocalDate.now());
        kartKayit.setDeger(String.valueOf(bugun.size()));
        kartMama.setDeger(Tema.sayi(KayitDeposu.mamaToplami(bugun)));
        kartCis.setDeger(String.valueOf(KayitDeposu.say(bugun, Kayit::isCis)));
        kartKaka.setDeger(String.valueOf(KayitDeposu.say(bugun, Kayit::isKaka)));
        kartEmzirme.setDeger(String.valueOf(KayitDeposu.say(bugun, Kayit::isEmzirme)));

        revalidate();
        repaint();
    }
}
