package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.ui.bilesen.AtesGrafigi;
import com.akansel.bebektakip.ui.bilesen.BosDurum;
import com.akansel.bebektakip.ui.bilesen.DuzButon;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.PanelBasligi;
import com.akansel.bebektakip.ui.bilesen.SecimListesi;
import com.akansel.bebektakip.ui.bilesen.TakvimSecici;
import com.akansel.bebektakip.ui.tablo.Hucreler;
import com.akansel.bebektakip.ui.tablo.SadeTablo;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Ateş takibi ekranı. "Ateş Ölç" o anın tarih ve saatiyle yeni bir satır
 * açar ve doğrudan derece hücresine geçer. Ölçüm yeri ve belirtiler
 * hücreye tıklayınca açılan listeden seçilir. Üstteki grafik son üç günün
 * seyrini gösterir; hastalık günlerinde ateşin düşüp düşmediği bir bakışta
 * görülsün diye.
 */
public class AtesPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int SUTUN_TARIH = 0;
    private static final int SUTUN_SAAT = 1;
    private static final int SUTUN_DERECE = 2;
    private static final int SUTUN_YER = 3;
    private static final int SUTUN_BELIRTI = 4;
    private static final int SUTUN_NOT = 5;
    private static final int SUTUN_SIL = 6;

    private final KayitDeposu depo;
    private final Model modeli = new Model();
    private final SadeTablo tablo;
    private final PanelBasligi baslik;
    private final JLabel grafikOzeti = new JLabel();
    private final AtesGrafigi grafik = new AtesGrafigi();
    private final BosDurum bosDurum = new BosDurum(
            "Henüz ateş ölçümü yok.\n\"Ateş Ölç\" ile ilk ölçümü girin.", Ikonlar::termometre);
    private final CardLayout duzen = new CardLayout();
    private final JPanel kap = new JPanel(duzen);

    public AtesPanel(KayitDeposu depo) {
        super(new BorderLayout(0, 16));
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        tablo = new SadeTablo(modeli);
        ciziciKur();
        fareKur();

        grafikOzeti.setFont(Tema.font(12));
        grafikOzeti.setForeground(Tema.METIN_SOLUK);
        Kart grafikKarti = new Kart(new BorderLayout());
        grafikKarti.add(new PanelBasligi("Ateş Seyri · son 3 gün", grafikOzeti),
                BorderLayout.NORTH);
        grafik.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 20));
        grafikKarti.add(grafik, BorderLayout.CENTER);
        add(grafikKarti, BorderLayout.NORTH);

        DuzButon olcButonu = new DuzButon("Ateş Ölç", Ikonlar::termometre, DuzButon.Bicim.DOLU);
        olcButonu.bosluk(13, 8);
        olcButonu.addActionListener(e -> yeniOlcum());
        baslik = new PanelBasligi("Ateş Ölçümleri", olcButonu);

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

        add(bilgiNotu(), BorderLayout.SOUTH);

        yenile();
    }

    private Component bilgiNotu() {
        JTextArea not = new JTextArea(
                "37,5 °C altı normal · 37,5–37,9 °C hafif yüksek · 38 °C ve üstü ateş · "
                        + "39 °C ve üstü yüksek ateş. Bu ekran tıbbi tavsiye yerine geçmez: "
                        + "3 aydan küçük bebekte 38 °C ve üstü ateşte, havale, nefes darlığı, "
                        + "morarma, yaygın döküntü ya da 3 günü aşan ateşte vakit kaybetmeden "
                        + "doktora başvurun.");
        not.setFont(Tema.font(12));
        not.setForeground(Tema.METIN_SOLUK);
        not.setOpaque(false);
        not.setEditable(false);
        not.setFocusable(false);
        not.setLineWrap(true);
        not.setWrapStyleWord(true);
        not.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        return not;
    }

    private void ciziciKur() {
        tablo.getColumnModel().getColumn(SUTUN_TARIH)
                .setCellRenderer(new Hucreler.TarihCizici(false, true));
        tablo.getColumnModel().getColumn(SUTUN_SAAT)
                .setCellRenderer(new Hucreler.SaatCizici());
        tablo.getColumnModel().getColumn(SUTUN_DERECE).setCellRenderer(new Hucreler.RozetCizici(
                d -> d.toString().isEmpty() ? "" : d + " °C",
                d -> AtesGrafigi.seviyeRengi(AtesKayit.seviye(AtesKayit.dereceCoz(d.toString()))),
                "°C girin…"));
        tablo.getColumnModel().getColumn(SUTUN_YER)
                .setCellRenderer(new Hucreler.MetinCizici("Seçin…", false));
        tablo.getColumnModel().getColumn(SUTUN_BELIRTI)
                .setCellRenderer(new Hucreler.MetinCizici("Belirti seçin…", false));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellRenderer(new Hucreler.MetinCizici("Not…", false));
        tablo.getColumnModel().getColumn(SUTUN_SIL)
                .setCellRenderer(new Hucreler.SilCizici());

        tablo.getColumnModel().getColumn(SUTUN_SAAT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(true));
        tablo.getColumnModel().getColumn(SUTUN_DERECE)
                .setCellEditor(new Hucreler.MetinDuzenleyici(
                        metin -> AtesKayit.dereceCoz(metin) > 0));
        tablo.getColumnModel().getColumn(SUTUN_NOT)
                .setCellEditor(new Hucreler.MetinDuzenleyici(false));

        tablo.sabitSutun(SUTUN_TARIH, 100);
        tablo.sabitSutun(SUTUN_SAAT, 70);
        tablo.sabitSutun(SUTUN_DERECE, 112);
        tablo.sabitSutun(SUTUN_YER, 112);
        tablo.esnekSutun(SUTUN_BELIRTI, 160, 280);
        tablo.esnekSutun(SUTUN_NOT, 120, 200);
        tablo.sabitSutun(SUTUN_SIL, 42);
    }

    private void fareKur() {
        MouseAdapter fare = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int sutun = tablo.columnAtPoint(e.getPoint());
                boolean elle = tablo.rowAtPoint(e.getPoint()) >= 0
                        && (sutun == SUTUN_TARIH || sutun == SUTUN_YER
                        || sutun == SUTUN_BELIRTI || sutun == SUTUN_SIL);
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
                } else if (sutun == SUTUN_TARIH || sutun == SUTUN_YER || sutun == SUTUN_BELIRTI) {
                    tablo.duzenlemeyiBitir();
                    tablo.setRowSelectionInterval(satir, satir);
                    if (sutun == SUTUN_TARIH) {
                        takvimiAc(satir);
                    } else {
                        listeyiAc(satir, sutun);
                    }
                }
            }
        };
        tablo.addMouseListener(fare);
        tablo.addMouseMotionListener(fare);
    }

    private void takvimiAc(int satir) {
        AtesKayit a = modeli.satirdaki(satir);
        if (a == null) {
            return;
        }
        Rectangle r = tablo.getCellRect(satir, SUTUN_TARIH, true);
        TakvimSecici.goster(tablo, r.x, r.y + r.height, a.getTarih(), tarih -> {
            a.setTarih(tarih);
            kaydetVeTazele();
        });
    }

    /** Ölçüm yeri tek seçimli, belirtiler çok seçimli listeyle seçilir. */
    private void listeyiAc(int satir, int sutun) {
        AtesKayit a = modeli.satirdaki(satir);
        if (a == null) {
            return;
        }
        Rectangle r = tablo.getCellRect(satir, sutun, true);
        if (sutun == SUTUN_YER) {
            SecimListesi.goster(tablo, r.x, r.y + r.height, AtesKayit.OLCUM_YERLERI,
                    a.getOlcumYeri().isEmpty() ? List.of() : List.of(a.getOlcumYeri()),
                    false, secilen -> {
                        a.setOlcumYeri(secilen.isEmpty() ? "" : secilen.get(0));
                        kaydetVeTazele();
                    });
        } else {
            SecimListesi.goster(tablo, r.x, r.y + r.height, AtesKayit.BELIRTILER,
                    a.getBelirtiler(), true, secilen -> {
                        a.setBelirtiler(secilen);
                        kaydetVeTazele();
                    });
        }
    }

    private void silmeyiSor(int satir) {
        AtesKayit a = modeli.satirdaki(satir);
        if (a == null) {
            return;
        }
        String ozet = Tema.tamTarih(a.getTarih()) + " " + Tema.saat(a.getSaat())
                + (a.getDerece() > 0 ? " · " + dereceMetni(a.getDerece()) + " °C" : "");
        int cevap = JOptionPane.showConfirmDialog(this,
                "Bu ölçüm silinsin mi?\n\n" + ozet,
                "Kaydı sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap == JOptionPane.YES_OPTION) {
            depo.silAtes(a);
            yenile();
        }
    }

    private void yeniOlcum() {
        tablo.duzenlemeyiBitir();
        depo.ekleAtes(new AtesKayit());
        yenile();
        if (modeli.getRowCount() > 0) {
            tablo.setRowSelectionInterval(0, 0);
            SwingUtilities.invokeLater(() -> {
                if (tablo.editCellAt(0, SUTUN_DERECE) && tablo.getEditorComponent() != null) {
                    tablo.getEditorComponent().requestFocusInWindow();
                }
            });
        }
    }

    private void kaydetVeTazele() {
        depo.sirala();
        depo.kaydetAtes();
        modeli.fireTableDataChanged();
        ozetleriGuncelle();
    }

    /** Liste dışarıdan değiştiğinde tabloyu, başlığı ve grafiği tazeler. */
    public void yenile() {
        tablo.duzenlemeyiBitir();
        modeli.fireTableDataChanged();
        duzen.show(kap, modeli.getRowCount() == 0 ? "bos" : "tablo");
        ozetleriGuncelle();
    }

    private void ozetleriGuncelle() {
        LocalDateTime simdi = LocalDateTime.now();
        AtesKayit son = depo.sonAtes(simdi, 24);
        baslik.setBaslik(son == null ? "Ateş Ölçümleri"
                : "Ateş Ölçümleri · son: " + dereceMetni(son.getDerece()) + " °C ("
                + Tema.saat(son.getSaat()) + ")");

        grafik.setOlcumler(depo.getAtesler(), simdi);
        double enYuksek = 0;
        for (AtesKayit a : depo.getAtesler()) {
            if (!a.zaman().isBefore(simdi.minusHours(AtesGrafigi.SAAT))
                    && !a.zaman().isAfter(simdi)) {
                enYuksek = Math.max(enYuksek, a.getDerece());
            }
        }
        grafikOzeti.setText(grafik.olcumSayisi() == 0 ? ""
                : grafik.olcumSayisi() + " ölçüm · en yüksek " + dereceMetni(enYuksek) + " °C");
    }

    private static String dereceMetni(double d) {
        return String.format(Tema.TR, "%.1f", d);
    }

    /** Ölçüm listesinin tablo modeli; doğrudan depodaki listeyi gösterir. */
    private class Model extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] basliklar = {
                "Tarih", "Saat", "Derece", "Ölçüm yeri", "Belirtiler", "Not", ""
        };

        AtesKayit satirdaki(int satir) {
            if (satir < 0 || satir >= depo.getAtesler().size()) {
                return null;
            }
            return depo.getAtesler().get(satir);
        }

        @Override
        public int getRowCount() {
            return depo.getAtesler().size();
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
            return sutun == SUTUN_SAAT || sutun == SUTUN_DERECE || sutun == SUTUN_NOT;
        }

        @Override
        public Object getValueAt(int satir, int sutun) {
            AtesKayit a = satirdaki(satir);
            if (a == null) {
                return null;
            }
            switch (sutun) {
                case SUTUN_TARIH:
                    return a.getTarih();
                case SUTUN_SAAT:
                    return a.getSaat();
                case SUTUN_DERECE:
                    // düzenleyici de bu metni gösterir: "38,4"
                    return a.getDerece() > 0 ? dereceMetni(a.getDerece()) : "";
                case SUTUN_YER:
                    return a.getOlcumYeri();
                case SUTUN_BELIRTI:
                    return a.belirtiMetni();
                case SUTUN_NOT:
                    return a.getNot();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object deger, int satir, int sutun) {
            AtesKayit a = satirdaki(satir);
            if (a == null) {
                return;
            }
            String metin = deger == null ? "" : String.valueOf(deger);
            switch (sutun) {
                case SUTUN_SAAT: {
                    LocalTime s = Kayit.saatCoz(metin);
                    if (s == null) {
                        return;
                    }
                    a.setSaat(s);
                    break;
                }
                case SUTUN_DERECE: {
                    if (metin.trim().isEmpty()) {
                        a.setDerece(0);
                        break;
                    }
                    double d = AtesKayit.dereceCoz(metin);
                    if (d <= 0) {
                        return;
                    }
                    a.setDerece(d);
                    break;
                }
                case SUTUN_NOT:
                    a.setNot(metin);
                    break;
                default:
                    return;
            }
            depo.kaydetAtes();
            fireTableRowsUpdated(satir, satir);
            ozetleriGuncelle();
        }
    }
}
