package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.IlacKayit;
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
import javax.swing.SwingUtilities;
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
 * Vitamin ve ilaç ekranı: bebeğe ne verildiyse buraya işlenir. "Ekle" o anın
 * tarih ve saatiyle boş bir satır açar; ad, doz ve not hücrede yazılır.
 */
public class IlacPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SUTUN_TARIH = 0;
    private static final int SUTUN_SAAT = 1;
    private static final int SUTUN_AD = 2;
    private static final int SUTUN_DOZ = 3;
    private static final int SUTUN_NOT = 4;
    private static final int SUTUN_SIL = 5;

    private final KayitDeposu depo;
    private final Model modeli = new Model();
    private final SadeTablo tablo;
    private final PanelBasligi baslik;
    private final BosDurum bosDurum = new BosDurum(
            "Henüz vitamin veya ilaç kaydı yok.\nVerilenleri buraya işleyin.", Ikonlar::hap);
    private final CardLayout duzen = new CardLayout();
    private final JPanel kap = new JPanel(duzen);

    public IlacPanel(KayitDeposu depo) {
        super(new BorderLayout());
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        tablo = new SadeTablo(modeli);
        ciziciKur();
        fareKur();

        DuzButon yeniButon = new DuzButon("Vitamin / İlaç Ekle", Ikonlar::hap,
                DuzButon.Bicim.DOLU);
        yeniButon.bosluk(13, 8);
        yeniButon.addActionListener(e -> yeniKayit());

        baslik = new PanelBasligi("Vitamin ve İlaçlar", yeniButon);

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
        tablo.getColumnModel().getColumn(SUTUN_SAAT)
                .setCellRenderer(new Hucreler.SaatCizici());
        tablo.getColumnModel().getColumn(SUTUN_AD)
                .setCellRenderer(new Hucreler.MetinCizici("Vitamin / ilaç adı…", false));
        tablo.getColumnModel().getColumn(SUTUN_DOZ)
                .setCellRenderer(new Hucreler.MetinCizici("Doz…", false));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellRenderer(new Hucreler.MetinCizici("Not…", false));
        tablo.getColumnModel().getColumn(SUTUN_SIL)
                .setCellRenderer(new Hucreler.SilCizici());

        tablo.getColumnModel().getColumn(SUTUN_SAAT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(true));
        tablo.getColumnModel().getColumn(SUTUN_AD)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));
        tablo.getColumnModel().getColumn(SUTUN_DOZ)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));

        tablo.sabitSutun(SUTUN_TARIH, 100);
        tablo.sabitSutun(SUTUN_SAAT, 70);
        tablo.esnekSutun(SUTUN_AD, 130, 220);
        tablo.sabitSutun(SUTUN_DOZ, 110);
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
        IlacKayit kayit = modeli.satirdaki(satir);
        if (kayit == null) {
            return;
        }
        Rectangle r = tablo.getCellRect(satir, SUTUN_TARIH, true);
        TakvimSecici.goster(tablo, r.x, r.y + r.height, kayit.getTarih(), tarih -> {
            kayit.setTarih(tarih);
            depo.kaydetIlac();
            modeli.fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        });
    }

    private void silmeyiSor(int satir) {
        IlacKayit kayit = modeli.satirdaki(satir);
        if (kayit == null) {
            return;
        }
        String ad = kayit.getAd().isEmpty() ? Tema.tamTarih(kayit.getTarih()) : kayit.getAd();
        int cevap = JOptionPane.showConfirmDialog(this,
                "Bu kayıt silinsin mi?\n\n" + ad,
                "Kaydı sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap == JOptionPane.YES_OPTION) {
            depo.silIlac(kayit);
            yenile();
        }
    }

    private void yeniKayit() {
        depo.ekleIlac(new IlacKayit());
        yenile();
        if (modeli.getRowCount() > 0) {
            tablo.setRowSelectionInterval(0, 0);
            SwingUtilities.invokeLater(() -> tablo.editCellAt(0, SUTUN_AD));
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
        int bugun = 0;
        LocalDate simdi = LocalDate.now();
        for (IlacKayit i : depo.getIlaclar()) {
            if (i.getTarih().equals(simdi)) {
                bugun++;
            }
        }
        baslik.setBaslik(bugun > 0
                ? "Vitamin ve İlaçlar · bugün " + bugun
                : "Vitamin ve İlaçlar");
    }

    /** Vitamin/ilaç listesinin tablo modeli; doğrudan depodaki listeyi gösterir. */
    private class Model extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] basliklar = {
                "Tarih", "Saat", "Vitamin / İlaç", "Doz", "Not", ""
        };

        IlacKayit satirdaki(int satir) {
            if (satir < 0 || satir >= depo.getIlaclar().size()) {
                return null;
            }
            return depo.getIlaclar().get(satir);
        }

        @Override
        public int getRowCount() {
            return depo.getIlaclar().size();
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
            return sutun == SUTUN_SAAT || sutun == SUTUN_AD
                    || sutun == SUTUN_DOZ || sutun == SUTUN_NOT;
        }

        @Override
        public Object getValueAt(int satir, int sutun) {
            IlacKayit kayit = satirdaki(satir);
            if (kayit == null) {
                return null;
            }
            switch (sutun) {
                case SUTUN_TARIH:
                    return kayit.getTarih();
                case SUTUN_SAAT:
                    return kayit.getSaat();
                case SUTUN_AD:
                    return kayit.getAd();
                case SUTUN_DOZ:
                    return kayit.getDoz();
                case SUTUN_NOT:
                    return kayit.getNot();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object deger, int satir, int sutun) {
            IlacKayit kayit = satirdaki(satir);
            if (kayit == null) {
                return;
            }
            switch (sutun) {
                case SUTUN_SAAT: {
                    LocalTime s = Kayit.saatCoz(String.valueOf(deger));
                    if (s == null) {
                        return;
                    }
                    kayit.setSaat(s);
                    break;
                }
                case SUTUN_AD:
                    kayit.setAd(deger == null ? "" : String.valueOf(deger));
                    break;
                case SUTUN_DOZ:
                    kayit.setDoz(deger == null ? "" : String.valueOf(deger));
                    break;
                case SUTUN_NOT:
                    kayit.setNot(deger == null ? "" : String.valueOf(deger));
                    break;
                default:
                    return;
            }
            depo.kaydetIlac();
            fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        }
    }
}
