package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.ui.bilesen.BosDurum;
import com.akansel.bebektakip.ui.bilesen.DuzButon;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.PanelBasligi;
import com.akansel.bebektakip.ui.bilesen.TakvimSecici;
import com.akansel.bebektakip.ui.tablo.Hucreler;
import com.akansel.bebektakip.ui.tablo.SadeTablo;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Uyku ekranı: "Uyku Başlat" o anın saatiyle açık bir kayıt açar,
 * "Uykuyu Bitir" son açık kaydı kapatır. Saatler ve not tabloda elle
 * düzeltilebilir; gece yarısını aşan uykular doğru hesaplanır.
 */
public class UykuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SUTUN_TARIH = 0;
    private static final int SUTUN_BASLANGIC = 1;
    private static final int SUTUN_BITIS = 2;
    private static final int SUTUN_SURE = 3;
    private static final int SUTUN_NOT = 4;
    private static final int SUTUN_SIL = 5;

    private final KayitDeposu depo;
    private final Model modeli = new Model();
    private final SadeTablo tablo;
    private final PanelBasligi baslik;
    private final BosDurum bosDurum = new BosDurum(
            "Henüz uyku kaydı yok.\n\"Uyku Başlat\" ile ilk kaydı açın.", Ikonlar::ay);
    private final CardLayout duzen = new CardLayout();
    private final JPanel kap = new JPanel(duzen);

    public UykuPanel(KayitDeposu depo) {
        super(new BorderLayout());
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        tablo = new SadeTablo(modeli);
        ciziciKur();
        fareKur();

        DuzButon bitirButonu = new DuzButon("Uykuyu Bitir", null, DuzButon.Bicim.CERCEVELI);
        bitirButonu.bosluk(13, 8);
        bitirButonu.addActionListener(e -> uykuyuBitir());

        DuzButon baslatButonu = new DuzButon("Uyku Başlat", Ikonlar::ay, DuzButon.Bicim.DOLU);
        baslatButonu.bosluk(13, 8);
        baslatButonu.addActionListener(e -> uykuBaslat());

        Box dugmeler = Box.createHorizontalBox();
        dugmeler.add(bitirButonu);
        dugmeler.add(Box.createHorizontalStrut(8));
        dugmeler.add(baslatButonu);

        baslik = new PanelBasligi("Uyku Takibi", dugmeler);

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
        tablo.getColumnModel().getColumn(SUTUN_BASLANGIC)
                .setCellRenderer(new Hucreler.SaatCizici());
        tablo.getColumnModel().getColumn(SUTUN_BITIS)
                .setCellRenderer(new Hucreler.MetinCizici("uyuyor…", false));
        tablo.getColumnModel().getColumn(SUTUN_SURE)
                .setCellRenderer(new Hucreler.MetinCizici("—", true));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellRenderer(new Hucreler.MetinCizici("Not…", false));
        tablo.getColumnModel().getColumn(SUTUN_SIL)
                .setCellRenderer(new Hucreler.SilCizici());

        tablo.getColumnModel().getColumn(SUTUN_BASLANGIC)
                .setCellEditor(new Hucreler.MetinDuzenleyici(true));
        tablo.getColumnModel().getColumn(SUTUN_BITIS)
                .setCellEditor(new Hucreler.MetinDuzenleyici(true));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));

        tablo.sabitSutun(SUTUN_TARIH, 100);
        tablo.sabitSutun(SUTUN_BASLANGIC, 92);
        tablo.sabitSutun(SUTUN_BITIS, 92);
        tablo.sabitSutun(SUTUN_SURE, 96);
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
        UykuKayit u = modeli.satirdaki(satir);
        if (u == null) {
            return;
        }
        Rectangle r = tablo.getCellRect(satir, SUTUN_TARIH, true);
        TakvimSecici.goster(tablo, r.x, r.y + r.height, u.getTarih(), tarih -> {
            u.setTarih(tarih);
            depo.kaydetUyku();
            modeli.fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        });
    }

    private void silmeyiSor(int satir) {
        UykuKayit u = modeli.satirdaki(satir);
        if (u == null) {
            return;
        }
        int cevap = JOptionPane.showConfirmDialog(this,
                "Bu uyku kaydı silinsin mi?\n\n" + Tema.tamTarih(u.getTarih())
                        + " " + Tema.saat(u.getBaslangic()),
                "Kaydı sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap == JOptionPane.YES_OPTION) {
            depo.silUyku(u);
            yenile();
        }
    }

    /** Süren (bitişi girilmemiş) en yeni uyku kaydı; yoksa null. */
    private UykuKayit acikKayit() {
        for (UykuKayit u : depo.getUykular()) {
            if (u.devamEdiyor()) {
                return u;
            }
        }
        return null;
    }

    private void uykuBaslat() {
        if (acikKayit() != null) {
            JOptionPane.showMessageDialog(this,
                    "Zaten süren bir uyku kaydı var.\nÖnce \"Uykuyu Bitir\" ile kapatın.",
                    "Uyku Takibi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        depo.ekleUyku(new UykuKayit());
        yenile();
        if (modeli.getRowCount() > 0) {
            tablo.setRowSelectionInterval(0, 0);
        }
    }

    private void uykuyuBitir() {
        UykuKayit acik = acikKayit();
        if (acik == null) {
            JOptionPane.showMessageDialog(this,
                    "Süren bir uyku kaydı yok.",
                    "Uyku Takibi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        acik.setBitis(LocalTime.now());
        depo.kaydetUyku();
        yenile();
    }

    /** Liste dışarıdan değiştiğinde tabloyu ve başlığı tazeler. */
    public void yenile() {
        tablo.duzenlemeyiBitir();
        modeli.fireTableDataChanged();
        duzen.show(kap, modeli.getRowCount() == 0 ? "bos" : "tablo");
        basligiGuncelle();
    }

    private void basligiGuncelle() {
        long bugun = depo.gununUykuSuresi(LocalDate.now());
        baslik.setBaslik(bugun > 0
                ? "Uyku Takibi · bugün " + Tema.sure(bugun)
                : "Uyku Takibi");
    }

    /** Uyku listesinin tablo modeli; doğrudan depodaki listeyi gösterir. */
    private class Model extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] basliklar = {
                "Tarih", "Başlangıç", "Bitiş", "Süre", "Not", ""
        };

        UykuKayit satirdaki(int satir) {
            if (satir < 0 || satir >= depo.getUykular().size()) {
                return null;
            }
            return depo.getUykular().get(satir);
        }

        @Override
        public int getRowCount() {
            return depo.getUykular().size();
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
            return sutun == SUTUN_BASLANGIC || sutun == SUTUN_BITIS || sutun == SUTUN_NOT;
        }

        @Override
        public Object getValueAt(int satir, int sutun) {
            UykuKayit u = satirdaki(satir);
            if (u == null) {
                return null;
            }
            switch (sutun) {
                case SUTUN_TARIH:
                    return u.getTarih();
                case SUTUN_BASLANGIC:
                    return u.getBaslangic();
                case SUTUN_BITIS:
                    return u.getBitis();
                case SUTUN_SURE:
                    return u.devamEdiyor() ? "" : Tema.sure(u.sureDakika());
                case SUTUN_NOT:
                    return u.getNot();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object deger, int satir, int sutun) {
            UykuKayit u = satirdaki(satir);
            if (u == null) {
                return;
            }
            switch (sutun) {
                case SUTUN_BASLANGIC: {
                    LocalTime s = Kayit.saatCoz(String.valueOf(deger));
                    if (s == null) {
                        return;
                    }
                    u.setBaslangic(s);
                    break;
                }
                case SUTUN_BITIS: {
                    String metin = deger == null ? "" : String.valueOf(deger).trim();
                    if (metin.isEmpty()) {
                        u.setBitis(null);
                    } else {
                        LocalTime s = Kayit.saatCoz(metin);
                        if (s == null) {
                            return;
                        }
                        u.setBitis(s);
                    }
                    break;
                }
                case SUTUN_NOT:
                    u.setNot(deger == null ? "" : String.valueOf(deger));
                    break;
                default:
                    return;
            }
            depo.kaydetUyku();
            fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        }
    }
}
