package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.ui.bilesen.BosDurum;
import com.akansel.bebektakip.ui.bilesen.DuzButon;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.PanelBasligi;
import com.akansel.bebektakip.ui.bilesen.TakvimSecici;
import com.akansel.bebektakip.ui.tablo.Hucreler;
import com.akansel.bebektakip.ui.tablo.SadeTablo;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Büyüme ekranı: kilo, boy ve baş çevresi ölçümleri. "Yeni Ölçüm" günün
 * tarihiyle boş bir satır açar; değerler tabloda elle yazılır, "4,2" ve
 * "4.2" yazımlarının ikisi de kabul edilir.
 */
public class BuyumePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SUTUN_TARIH = 0;
    private static final int SUTUN_KILO = 1;
    private static final int SUTUN_BOY = 2;
    private static final int SUTUN_BAS = 3;
    private static final int SUTUN_NOT = 4;
    private static final int SUTUN_SIL = 5;

    private final KayitDeposu depo;
    private final Model modeli = new Model();
    private final SadeTablo tablo;
    private final PanelBasligi baslik;
    private final BosDurum bosDurum = new BosDurum(
            "Henüz ölçüm yok.\n\"Yeni Ölçüm\" ile ilk kaydı ekleyin.", Ikonlar::cetvel);
    private final CardLayout duzen = new CardLayout();
    private final JPanel kap = new JPanel(duzen);

    public BuyumePanel(KayitDeposu depo) {
        super(new BorderLayout());
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        tablo = new SadeTablo(modeli);
        ciziciKur();
        fareKur();

        DuzButon yeniButon = new DuzButon("Yeni Ölçüm", Ikonlar::cetvel, DuzButon.Bicim.DOLU);
        yeniButon.bosluk(13, 8);
        yeniButon.addActionListener(e -> yeniOlcum());

        baslik = new PanelBasligi("Büyüme Takibi", yeniButon);

        JScrollPane kaydirma = new JScrollPane(tablo);
        kaydirma.setBorder(BorderFactory.createEmptyBorder());
        kaydirma.getViewport().setBackground(Tema.YUZEY);
        kaydirma.setBackground(Tema.YUZEY);
        kaydirma.getVerticalScrollBar().setUnitIncrement(24);

        kap.setOpaque(false);
        kap.add(kaydirma, "tablo");
        kap.add(bosDurum, "bos");

        Kart kart = new Kart(new BorderLayout());
        kart.add(baslik, BorderLayout.NORTH);
        kart.add(kap, BorderLayout.CENTER);
        add(kart, BorderLayout.CENTER);

        yenile();
    }

    private void ciziciKur() {
        tablo.getColumnModel().getColumn(SUTUN_TARIH)
                .setCellRenderer(new Hucreler.TarihCizici(false, true));
        tablo.getColumnModel().getColumn(SUTUN_KILO)
                .setCellRenderer(new Hucreler.MetinCizici("—", false));
        tablo.getColumnModel().getColumn(SUTUN_BOY)
                .setCellRenderer(new Hucreler.MetinCizici("—", false));
        tablo.getColumnModel().getColumn(SUTUN_BAS)
                .setCellRenderer(new Hucreler.MetinCizici("—", false));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellRenderer(new Hucreler.MetinCizici("Not…", false));
        tablo.getColumnModel().getColumn(SUTUN_SIL)
                .setCellRenderer(new Hucreler.SilCizici());

        for (int s = SUTUN_KILO; s <= SUTUN_NOT; s++) {
            tablo.getColumnModel().getColumn(s)
                    .setCellEditor(new Hucreler.MetinDuzenleyici(false));
        }

        tablo.sabitSutun(SUTUN_TARIH, 100);
        tablo.sabitSutun(SUTUN_KILO, 92);
        tablo.sabitSutun(SUTUN_BOY, 92);
        tablo.sabitSutun(SUTUN_BAS, 118);
        tablo.esnekSutun(SUTUN_NOT, 140, 240);
        tablo.sabitSutun(SUTUN_SIL, 42);
    }

    private void fareKur() {
        MouseAdapter fare = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int sutun = tablo.columnAtPoint(e.getPoint());
                boolean elle = tablo.rowAtPoint(e.getPoint()) >= 0
                        && (sutun == SUTUN_TARIH || sutun == SUTUN_SIL);
                tablo.setCursor(Cursor.getPredefinedCursor(
                        elle ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int satir = tablo.rowAtPoint(e.getPoint());
                int sutun = tablo.columnAtPoint(e.getPoint());
                if (satir < 0 || sutun < 0) {
                    return;
                }
                if (sutun == SUTUN_SIL) {
                    tablo.duzenlemeyiBitir();
                    silmeyiSor(satir);
                } else if (sutun == SUTUN_TARIH) {
                    tablo.duzenlemeyiBitir();
                    tablo.setRowSelectionInterval(satir, satir);
                    takvimiAc(satir);
                }
            }
        };
        tablo.addMouseListener(fare);
        tablo.addMouseMotionListener(fare);
    }

    private void takvimiAc(int satir) {
        BuyumeKayit b = modeli.satirdaki(satir);
        if (b == null) {
            return;
        }
        Rectangle r = tablo.getCellRect(satir, SUTUN_TARIH, true);
        TakvimSecici.goster(tablo, r.x, r.y + r.height, b.getTarih(), tarih -> {
            b.setTarih(tarih);
            depo.kaydetBuyume();
            modeli.fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        });
    }

    private void silmeyiSor(int satir) {
        BuyumeKayit b = modeli.satirdaki(satir);
        if (b == null) {
            return;
        }
        int cevap = JOptionPane.showConfirmDialog(this,
                "Bu ölçüm silinsin mi?\n\n" + Tema.tamTarih(b.getTarih()),
                "Kaydı sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap == JOptionPane.YES_OPTION) {
            depo.silBuyume(b);
            yenile();
        }
    }

    private void yeniOlcum() {
        depo.ekleBuyume(new BuyumeKayit());
        yenile();
        if (modeli.getRowCount() > 0) {
            tablo.setRowSelectionInterval(0, 0);
        }
    }

    /** Liste dışarıdan değiştiğinde tabloyu ve başlığı tazeler. */
    public void yenile() {
        tablo.duzenlemeyiBitir();
        modeli.fireTableDataChanged();
        duzen.show(kap, modeli.getRowCount() == 0 ? "bos" : "tablo");
        basligiGuncelle();
    }

    private void basligiGuncelle() {
        BuyumeKayit son = depo.sonBuyume();
        if (son == null || (son.getKilo() <= 0 && son.getBoy() <= 0)) {
            baslik.setBaslik("Büyüme Takibi");
            return;
        }
        StringBuilder ozet = new StringBuilder("Büyüme Takibi · son:");
        if (son.getKilo() > 0) {
            ozet.append(' ').append(Tema.sayi(son.getKilo())).append(" kg");
        }
        if (son.getBoy() > 0) {
            if (son.getKilo() > 0) {
                ozet.append(" /");
            }
            ozet.append(' ').append(Tema.sayi(son.getBoy())).append(" cm");
        }
        baslik.setBaslik(ozet.toString());
    }

    /** Ölçüm listesinin tablo modeli; doğrudan depodaki listeyi gösterir. */
    private class Model extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] basliklar = {
                "Tarih", "Kilo (kg)", "Boy (cm)", "Baş çevresi (cm)", "Not", ""
        };

        BuyumeKayit satirdaki(int satir) {
            if (satir < 0 || satir >= depo.getBuyumeler().size()) {
                return null;
            }
            return depo.getBuyumeler().get(satir);
        }

        @Override
        public int getRowCount() {
            return depo.getBuyumeler().size();
        }

        @Override
        public int getColumnCount() {
            return basliklar.length;
        }

        @Override
        public String getColumnName(int sutun) {
            return basliklar[sutun];
        }

        @Override
        public boolean isCellEditable(int satir, int sutun) {
            return sutun >= SUTUN_KILO && sutun <= SUTUN_NOT;
        }

        @Override
        public Object getValueAt(int satir, int sutun) {
            BuyumeKayit b = satirdaki(satir);
            if (b == null) {
                return null;
            }
            switch (sutun) {
                case SUTUN_TARIH:
                    return b.getTarih();
                case SUTUN_KILO:
                    return b.getKilo() > 0 ? Tema.sayi(b.getKilo()) : "";
                case SUTUN_BOY:
                    return b.getBoy() > 0 ? Tema.sayi(b.getBoy()) : "";
                case SUTUN_BAS:
                    return b.getBasCevresi() > 0 ? Tema.sayi(b.getBasCevresi()) : "";
                case SUTUN_NOT:
                    return b.getNot();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object deger, int satir, int sutun) {
            BuyumeKayit b = satirdaki(satir);
            if (b == null) {
                return;
            }
            if (sutun == SUTUN_NOT) {
                b.setNot(deger == null ? "" : String.valueOf(deger));
            } else if (sutun >= SUTUN_KILO && sutun <= SUTUN_BAS) {
                String metin = deger == null ? "" : String.valueOf(deger).trim();
                double d = metin.isEmpty() ? 0 : Kayit.sayiyiCoz(metin);
                if (!metin.isEmpty() && d <= 0) {
                    // sayı çözülemedi; eski değeri koru
                    return;
                }
                if (sutun == SUTUN_KILO) {
                    b.setKilo(d);
                } else if (sutun == SUTUN_BOY) {
                    b.setBoy(d);
                } else {
                    b.setBasCevresi(d);
                }
            } else {
                return;
            }
            depo.kaydetBuyume();
            fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        }
    }
}
