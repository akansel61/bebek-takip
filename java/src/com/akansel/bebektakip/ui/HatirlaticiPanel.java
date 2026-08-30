package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.Hatirlatici;
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
import java.time.LocalTime;

/**
 * Hatırlatıcı ekranı: aşı, ilaç, kontrol randevusu gibi işler. Liste en
 * yakın tarihli iş üstte olacak biçimde sıralanır; soldaki kutu işaretlenen
 * hatırlatma tamamlanmış sayılır.
 */
public class HatirlaticiPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SUTUN_TAMAM = 0;
    private static final int SUTUN_TARIH = 1;
    private static final int SUTUN_SAAT = 2;
    private static final int SUTUN_BASLIK = 3;
    private static final int SUTUN_NOT = 4;
    private static final int SUTUN_SIL = 5;

    private final KayitDeposu depo;
    private final Model modeli = new Model();
    private final SadeTablo tablo;
    private final PanelBasligi baslik;
    private final BosDurum bosDurum = new BosDurum(
            "Henüz hatırlatıcı yok.\nAşı ve kontrol günlerini buraya ekleyin.", Ikonlar::zil);
    private final CardLayout duzen = new CardLayout();
    private final JPanel kap = new JPanel(duzen);

    public HatirlaticiPanel(KayitDeposu depo) {
        super(new BorderLayout());
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        tablo = new SadeTablo(modeli);
        ciziciKur();
        fareKur();

        DuzButon yeniButon = new DuzButon("Yeni Hatırlatıcı", Ikonlar::zil, DuzButon.Bicim.DOLU);
        yeniButon.bosluk(13, 8);
        yeniButon.addActionListener(e -> yeniHatirlatici());

        baslik = new PanelBasligi("Hatırlatıcılar", yeniButon);

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
        tablo.getColumnModel().getColumn(SUTUN_TAMAM)
                .setCellRenderer(new Hucreler.OnayCizici(true));
        tablo.getColumnModel().getColumn(SUTUN_TARIH)
                .setCellRenderer(new Hucreler.TarihCizici(false, true));
        tablo.getColumnModel().getColumn(SUTUN_SAAT)
                .setCellRenderer(new Hucreler.SaatCizici());
        tablo.getColumnModel().getColumn(SUTUN_BASLIK)
                .setCellRenderer(new Hucreler.MetinCizici("Başlık…", false));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellRenderer(new Hucreler.MetinCizici("Not…", false));
        tablo.getColumnModel().getColumn(SUTUN_SIL)
                .setCellRenderer(new Hucreler.SilCizici());

        tablo.getColumnModel().getColumn(SUTUN_SAAT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(true));
        tablo.getColumnModel().getColumn(SUTUN_BASLIK)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));

        tablo.sabitSutun(SUTUN_TAMAM, 66);
        tablo.sabitSutun(SUTUN_TARIH, 100);
        tablo.sabitSutun(SUTUN_SAAT, 70);
        tablo.esnekSutun(SUTUN_BASLIK, 130, 210);
        tablo.esnekSutun(SUTUN_NOT, 140, 240);
        tablo.sabitSutun(SUTUN_SIL, 42);
    }

    private void fareKur() {
        MouseAdapter fare = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int sutun = tablo.columnAtPoint(e.getPoint());
                boolean elle = tablo.rowAtPoint(e.getPoint()) >= 0
                        && (sutun == SUTUN_TAMAM || sutun == SUTUN_TARIH
                        || sutun == SUTUN_SIL);
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
                } else if (sutun == SUTUN_TAMAM) {
                    tablo.duzenlemeyiBitir();
                    tablo.setRowSelectionInterval(satir, satir);
                    modeli.isaretiDegistir(satir);
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
        Hatirlatici h = modeli.satirdaki(satir);
        if (h == null) {
            return;
        }
        Rectangle r = tablo.getCellRect(satir, SUTUN_TARIH, true);
        TakvimSecici.goster(tablo, r.x, r.y + r.height, h.getTarih(), tarih -> {
            h.setTarih(tarih);
            depo.kaydetHatirlatici();
            modeli.fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        });
    }

    private void silmeyiSor(int satir) {
        Hatirlatici h = modeli.satirdaki(satir);
        if (h == null) {
            return;
        }
        String ad = h.getBaslik().isEmpty() ? Tema.tamTarih(h.getTarih()) : h.getBaslik();
        int cevap = JOptionPane.showConfirmDialog(this,
                "Bu hatırlatıcı silinsin mi?\n\n" + ad,
                "Kaydı sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap == JOptionPane.YES_OPTION) {
            depo.silHatirlatici(h);
            yenile();
        }
    }

    private void yeniHatirlatici() {
        Hatirlatici h = new Hatirlatici();
        depo.ekleHatirlatici(h);
        yenile();
        // liste tarihe göre sıralandığı için yeni kayıt her yerde olabilir
        int satir = depo.getHatirlaticilar().indexOf(h);
        if (satir >= 0) {
            tablo.setRowSelectionInterval(satir, satir);
            tablo.scrollRectToVisible(tablo.getCellRect(satir, 0, true));
            final int hedefSatir = satir;
            SwingUtilities.invokeLater(() ->
                    tablo.editCellAt(hedefSatir, SUTUN_BASLIK));
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
        int bekleyen = depo.bekleyenHatirlatici();
        baslik.setBaslik(bekleyen > 0
                ? "Hatırlatıcılar · " + bekleyen + " bekleyen"
                : "Hatırlatıcılar");
    }

    /** Hatırlatıcı listesinin tablo modeli; doğrudan depodaki listeyi gösterir. */
    private class Model extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] basliklar = {
                "Tamam", "Tarih", "Saat", "Başlık", "Not", ""
        };

        Hatirlatici satirdaki(int satir) {
            if (satir < 0 || satir >= depo.getHatirlaticilar().size()) {
                return null;
            }
            return depo.getHatirlaticilar().get(satir);
        }

        void isaretiDegistir(int satir) {
            Hatirlatici h = satirdaki(satir);
            if (h != null) {
                h.setTamamlandi(!h.isTamamlandi());
                depo.kaydetHatirlatici();
                fireTableRowsUpdated(satir, satir);
                basligiGuncelle();
            }
        }

        @Override
        public int getRowCount() {
            return depo.getHatirlaticilar().size();
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
            return sutun == SUTUN_SAAT || sutun == SUTUN_BASLIK || sutun == SUTUN_NOT;
        }

        @Override
        public Object getValueAt(int satir, int sutun) {
            Hatirlatici h = satirdaki(satir);
            if (h == null) {
                return null;
            }
            switch (sutun) {
                case SUTUN_TAMAM:
                    return h.isTamamlandi();
                case SUTUN_TARIH:
                    return h.getTarih();
                case SUTUN_SAAT:
                    return h.getSaat();
                case SUTUN_BASLIK:
                    return h.getBaslik();
                case SUTUN_NOT:
                    return h.getNot();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object deger, int satir, int sutun) {
            Hatirlatici h = satirdaki(satir);
            if (h == null) {
                return;
            }
            switch (sutun) {
                case SUTUN_SAAT: {
                    LocalTime s = Kayit.saatCoz(String.valueOf(deger));
                    if (s == null) {
                        return;
                    }
                    h.setSaat(s);
                    break;
                }
                case SUTUN_BASLIK:
                    h.setBaslik(deger == null ? "" : String.valueOf(deger));
                    break;
                case SUTUN_NOT:
                    h.setNot(deger == null ? "" : String.valueOf(deger));
                    break;
                default:
                    return;
            }
            depo.kaydetHatirlatici();
            fireTableRowsUpdated(satir, satir);
            basligiGuncelle();
        }
    }
}
