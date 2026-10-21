package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.rapor.Donem;
import com.akansel.bebektakip.rapor.ExcelYazici;
import com.akansel.bebektakip.rapor.PdfYazici;
import com.akansel.bebektakip.rapor.RaporDizici;
import com.akansel.bebektakip.rapor.RaporVerisi;
import com.akansel.bebektakip.rapor.RaporYazisi;
import com.akansel.bebektakip.rapor.Sayfa;
import com.akansel.bebektakip.rapor.SayfaCizici;
import com.akansel.bebektakip.ui.bilesen.DuzButon;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.PanelBasligi;
import com.akansel.bebektakip.ui.bilesen.SayfaOnizleme;
import com.akansel.bebektakip.ui.bilesen.TakvimSecici;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Rapor ekranı. Dönem (günlük, haftalık, aylık ya da özel aralık) ve
 * rapora girecek bölümler seçilir; sayfalar anında önizlenir. Aynı sayfalar
 * PDF olarak kaydedilir ya da yazıcıya gönderilir, Excel ise ayrı
 * sayfalarda düz tablo olarak üretilir.
 */
public class RaporPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final KayitDeposu depo;
    private final String imza;
    private Donem donem = Donem.haftalik(LocalDate.now());
    private final Set<RaporVerisi.Bolum> bolumler = EnumSet.allOf(RaporVerisi.Bolum.class);

    private final Map<Donem.Tur, DuzButon> turDugmeleri = new EnumMap<>(Donem.Tur.class);
    private final Map<RaporVerisi.Bolum, DuzButon> bolumDugmeleri =
            new EnumMap<>(RaporVerisi.Bolum.class);
    private final DuzButon tarihDugmesi;
    private final DuzButon baslangicDugmesi;
    private final DuzButon bitisDugmesi;
    private final CardLayout tarihDuzeni = new CardLayout();
    private final JPanel tarihKabi = new JPanel(tarihDuzeni) {
        private static final long serialVersionUID = 1L;

        // CardLayout en geniş kartın boyunu ister; ok düğmeleri görünen
        // kartın hemen yanında dursun diye yalnızca o kart ölçülür.
        @Override
        public Dimension getPreferredSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    return c.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    };
    private final JPanel bolumSatiri = new JPanel();
    private final JLabel durum = new JLabel();
    private final SayfaOnizleme onizleme = new SayfaOnizleme();
    private final DuzButon pdfDugmesi;
    private final DuzButon excelDugmesi;
    private final DuzButon yazdirDugmesi;

    private List<Sayfa> sayfalar = new ArrayList<>();
    private RaporVerisi veri;
    private boolean eskimis = true;

    public RaporPanel(KayitDeposu depo, String imza) {
        super(new BorderLayout());
        this.depo = depo;
        this.imza = imza;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        yazdirDugmesi = new DuzButon("Yazdır", Ikonlar::yazici, DuzButon.Bicim.CERCEVELI);
        yazdirDugmesi.bosluk(13, 8);
        yazdirDugmesi.addActionListener(e -> yazdir());
        excelDugmesi = new DuzButon("Excel Kaydet", Ikonlar::tablo, DuzButon.Bicim.CERCEVELI);
        excelDugmesi.bosluk(13, 8);
        excelDugmesi.addActionListener(e -> excelKaydetSor());
        pdfDugmesi = new DuzButon("PDF Kaydet", Ikonlar::belge, DuzButon.Bicim.DOLU);
        pdfDugmesi.bosluk(13, 8);
        pdfDugmesi.addActionListener(e -> pdfKaydetSor());

        Box dugmeler = Box.createHorizontalBox();
        dugmeler.add(yazdirDugmesi);
        dugmeler.add(Box.createHorizontalStrut(8));
        dugmeler.add(excelDugmesi);
        dugmeler.add(Box.createHorizontalStrut(8));
        dugmeler.add(pdfDugmesi);

        tarihDugmesi = new DuzButon("", Ikonlar::takvim, DuzButon.Bicim.CERCEVELI);
        tarihDugmesi.bosluk(12, 7);
        tarihDugmesi.addActionListener(e -> takvimAc(tarihDugmesi, donem.getBaslangic(),
                tarih -> setDonem(Donem.olustur(donem.getTur(), tarih))));
        baslangicDugmesi = new DuzButon("", Ikonlar::takvim, DuzButon.Bicim.CERCEVELI);
        baslangicDugmesi.bosluk(12, 7);
        baslangicDugmesi.addActionListener(e -> takvimAc(baslangicDugmesi, donem.getBaslangic(),
                tarih -> setDonem(Donem.ozel(tarih, donem.getBitis()))));
        bitisDugmesi = new DuzButon("", Ikonlar::takvim, DuzButon.Bicim.CERCEVELI);
        bitisDugmesi.bosluk(12, 7);
        bitisDugmesi.addActionListener(e -> takvimAc(bitisDugmesi, donem.getBitis(),
                tarih -> setDonem(Donem.ozel(donem.getBaslangic(), tarih))));

        JScrollPane kaydirma = new JScrollPane(onizleme);
        kaydirma.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.CIZGI));
        kaydirma.getViewport().setBackground(Tema.ARKA);
        kaydirma.getVerticalScrollBar().setUnitIncrement(32);
        kaydirma.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel govde = new JPanel(new BorderLayout());
        govde.setOpaque(false);
        govde.add(ayarlar(), BorderLayout.NORTH);
        govde.add(kaydirma, BorderLayout.CENTER);

        Kart kart = new Kart(new BorderLayout());
        kart.add(new PanelBasligi("Rapor", dugmeler), BorderLayout.NORTH);
        kart.add(govde, BorderLayout.CENTER);
        add(kart, BorderLayout.CENTER);

        secimleriGoster();
    }

    private JComponent ayarlar() {
        JPanel donemSatiri = satir();
        donemSatiri.add(etiket("Dönem"));
        for (Donem.Tur tur : Donem.Tur.values()) {
            DuzButon d = new DuzButon(tur.getAd(), null, DuzButon.Bicim.CERCEVELI);
            d.bosluk(12, 7);
            d.addActionListener(e -> turSec(tur));
            turDugmeleri.put(tur, d);
            donemSatiri.add(d);
            donemSatiri.add(Box.createHorizontalStrut(6));
        }
        donemSatiri.add(Box.createHorizontalStrut(14));

        DuzButon geri = new DuzButon("", Ikonlar::okSol, DuzButon.Bicim.YALIN);
        geri.bosluk(8, 8);
        geri.setToolTipText("Önceki dönem");
        geri.addActionListener(e -> setDonem(donem.onceki()));
        DuzButon ileri = new DuzButon("", Ikonlar::okSag, DuzButon.Bicim.YALIN);
        ileri.bosluk(8, 8);
        ileri.setToolTipText("Sonraki dönem");
        ileri.addActionListener(e -> setDonem(donem.sonraki()));

        JPanel tek = satir();
        tek.add(tarihDugmesi);
        JPanel aralik = satir();
        aralik.add(baslangicDugmesi);
        JLabel tire = new JLabel("–");
        tire.setFont(Tema.font(13));
        tire.setForeground(Tema.METIN_SOLUK);
        tire.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        aralik.add(tire);
        aralik.add(bitisDugmesi);
        tarihKabi.setOpaque(false);
        tarihKabi.add(tek, "tek");
        tarihKabi.add(aralik, "aralik");

        donemSatiri.add(geri);
        donemSatiri.add(Box.createHorizontalStrut(4));
        donemSatiri.add(tarihKabi);
        donemSatiri.add(Box.createHorizontalStrut(4));
        donemSatiri.add(ileri);
        donemSatiri.add(Box.createHorizontalGlue());

        bolumSatiri.setOpaque(false);
        bolumSatiri.setLayout(new BoxLayout(bolumSatiri, BoxLayout.X_AXIS));
        bolumSatiri.setAlignmentX(LEFT_ALIGNMENT);
        bolumSatiri.add(etiket("İçerik"));
        for (RaporVerisi.Bolum b : RaporVerisi.Bolum.values()) {
            DuzButon d = new DuzButon(b.getAd(), null, DuzButon.Bicim.CERCEVELI);
            d.bosluk(12, 7).ikonBoyutu(13).renk(bolumRengi(b));
            d.addActionListener(e -> bolumDegistir(b));
            bolumDugmeleri.put(b, d);
            bolumSatiri.add(d);
            bolumSatiri.add(Box.createHorizontalStrut(6));
        }
        bolumSatiri.add(Box.createHorizontalGlue());
        durum.setFont(Tema.font(12));
        durum.setForeground(Tema.METIN_SOLUK);
        bolumSatiri.add(durum);

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        p.add(donemSatiri);
        p.add(Box.createVerticalStrut(10));
        p.add(bolumSatiri);
        return p;
    }

    private static JPanel satir() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private static JLabel etiket(String metin) {
        JLabel l = new JLabel(metin);
        l.setFont(Tema.font(12));
        l.setForeground(Tema.METIN_SOLUK);
        Dimension d = new Dimension(64, l.getPreferredSize().height);
        l.setPreferredSize(d);
        l.setMinimumSize(d);
        l.setMaximumSize(d);
        return l;
    }

    private static Color bolumRengi(RaporVerisi.Bolum b) {
        switch (b) {
            case MAMA:
                return Tema.TURUNCU;
            case VITAMIN:
                return Tema.YESIL;
            case ILAC:
                return Tema.MAVI;
            default:
                return Tema.KIRMIZI;
        }
    }

    private void takvimAc(JComponent sahip, LocalDate secili, Consumer<LocalDate> secilince) {
        TakvimSecici.goster(sahip, 0, sahip.getHeight() + 4, secili, secilince);
    }

    /**
     * Dönem türü değişince tarih kaybolmasın: içinde bulunulan dönem bugünü
     * kapsıyorsa bugünden, kapsamıyorsa dönemin başından kurulur. Özel
     * aralığa geçerken mevcut dönemin tarihleri olduğu gibi alınır.
     */
    private void turSec(Donem.Tur tur) {
        if (tur == donem.getTur()) {
            return;
        }
        if (tur == Donem.Tur.OZEL) {
            setDonem(Donem.ozel(donem.getBaslangic(), donem.getBitis()));
            return;
        }
        LocalDate bugun = LocalDate.now();
        LocalDate dayanak = donem.icerir(bugun) ? bugun
                : (donem.getTur() == Donem.Tur.OZEL ? donem.getBitis() : donem.getBaslangic());
        setDonem(Donem.olustur(tur, dayanak));
    }

    private void bolumDegistir(RaporVerisi.Bolum b) {
        if (!bolumler.remove(b)) {
            bolumler.add(b);
        }
        secimleriGoster();
        yenile();
    }

    public Donem getDonem() {
        return donem;
    }

    public void setDonem(Donem yeni) {
        donem = yeni;
        secimleriGoster();
        yenile();
    }

    public void setBolumler(Set<RaporVerisi.Bolum> yeni) {
        bolumler.clear();
        bolumler.addAll(yeni);
        secimleriGoster();
        yenile();
    }

    public int sayfaSayisi() {
        if (eskimis) {
            olustur();
        }
        return sayfalar.size();
    }

    private void secimleriGoster() {
        for (Map.Entry<Donem.Tur, DuzButon> e : turDugmeleri.entrySet()) {
            e.getValue().bicim(e.getKey() == donem.getTur()
                    ? DuzButon.Bicim.DOLU : DuzButon.Bicim.CERCEVELI);
            e.getValue().repaint();
        }
        for (Map.Entry<RaporVerisi.Bolum, DuzButon> e : bolumDugmeleri.entrySet()) {
            boolean secili = bolumler.contains(e.getKey());
            e.getValue().bicim(secili ? DuzButon.Bicim.DOLU : DuzButon.Bicim.CERCEVELI)
                    .ikon(secili ? Ikonlar::onay : null);
            e.getValue().revalidate();
            e.getValue().repaint();
        }
        tarihDugmesi.setText(donem.aralikMetni());
        baslangicDugmesi.setText(Tema.tamTarih(donem.getBaslangic()));
        bitisDugmesi.setText(Tema.tamTarih(donem.getBitis()));
        tarihDuzeni.show(tarihKabi, donem.getTur() == Donem.Tur.OZEL ? "aralik" : "tek");
        tarihKabi.revalidate();
        bolumSatiri.revalidate();
        repaint();
    }

    /**
     * Rapor ekranda değilken veri değişirse yalnızca "eskimiş" diye
     * işaretlenir; sayfalar ekrana gelince bir kez hazırlanır.
     */
    public void yenile() {
        if (!isShowing()) {
            eskimis = true;
            return;
        }
        olustur();
    }

    private void olustur() {
        eskimis = false;
        RaporYazisi yazi = RaporYazisi.varsayilan();
        veri = RaporVerisi.hazirla(depo, donem, bolumler);
        if (bolumler.isEmpty()) {
            sayfalar = new ArrayList<>();
            onizleme.setBosMesaj("Rapora eklenecek en az bir bölüm seçin.");
        } else {
            sayfalar = RaporDizici.diz(veri, yazi, imza);
        }
        onizleme.setSayfalar(sayfalar, yazi);

        int kayit = 0;
        if (bolumler.contains(RaporVerisi.Bolum.MAMA)) {
            kayit += veri.getMamalar().size();
        }
        if (bolumler.contains(RaporVerisi.Bolum.VITAMIN)) {
            kayit += veri.getVitaminler().size();
        }
        if (bolumler.contains(RaporVerisi.Bolum.ILAC)) {
            kayit += veri.getIlaclar().size();
        }
        if (bolumler.contains(RaporVerisi.Bolum.ATES)) {
            kayit += veri.getAtesler().size();
        }
        durum.setText(sayfalar.isEmpty() ? "" : sayfalar.size() + " sayfa · " + kayit + " kayıt");

        boolean var = !sayfalar.isEmpty();
        yazdirDugmesi.setEnabled(var);
        excelDugmesi.setEnabled(var);
        pdfDugmesi.setEnabled(var && yazi.pdfYazilabilir());
        pdfDugmesi.setToolTipText(yazi.pdfYazilabilir() ? null
                : "PDF için gerekli yazı tipi dosyası bulunamadı; "
                + "Yazdır ile PDF yazıcısı seçilebilir.");
    }

    private String dosyaAdi() {
        String tur;
        switch (donem.getTur()) {
            case GUNLUK:
                tur = "gunluk";
                break;
            case HAFTALIK:
                tur = "haftalik";
                break;
            case AYLIK:
                tur = "aylik";
                break;
            default:
                tur = "donem";
        }
        return "bebek-takip-" + tur + "-rapor-" + donem.dosyaEki();
    }

    private String belgeAdi() {
        return "Bebek Takip – " + donem.raporAdi() + " (" + donem.aralikMetni() + ")";
    }

    /** Önizlemedeki sayfaları PDF olarak yazar. */
    public void pdfKaydet(Path hedef) throws IOException {
        if (eskimis) {
            olustur();
        }
        PdfYazici.yaz(hedef, sayfalar, RaporYazisi.varsayilan(), belgeAdi());
    }

    public void excelKaydet(Path hedef) throws IOException {
        if (eskimis) {
            olustur();
        }
        ExcelYazici.yaz(hedef, veri);
    }

    private void pdfKaydetSor() {
        File hedef = DosyaSecici.kaydetmeYeri(this, "PDF olarak kaydet", dosyaAdi() + ".pdf",
                "PDF dosyası (*.pdf)", "pdf");
        if (hedef == null) {
            return;
        }
        try {
            pdfKaydet(hedef.toPath());
            DosyaSecici.kaydedildi(this, hedef);
        } catch (IOException e) {
            hata("PDF yazılamadı: " + e.getMessage());
        }
    }

    private void excelKaydetSor() {
        File hedef = DosyaSecici.kaydetmeYeri(this, "Excel olarak kaydet", dosyaAdi() + ".xlsx",
                "Excel çalışma kitabı (*.xlsx)", "xlsx");
        if (hedef == null) {
            return;
        }
        try {
            excelKaydet(hedef.toPath());
            DosyaSecici.kaydedildi(this, hedef);
        } catch (IOException e) {
            hata("Excel dosyası yazılamadı: " + e.getMessage());
        }
    }

    private void yazdir() {
        if (eskimis) {
            olustur();
        }
        try {
            SayfaCizici.yazdir(sayfalar, RaporYazisi.varsayilan(), belgeAdi());
        } catch (PrinterException e) {
            hata("Yazdırılamadı: " + e.getMessage());
        }
    }

    private void hata(String mesaj) {
        JOptionPane.showMessageDialog(this, mesaj, "Hata", JOptionPane.ERROR_MESSAGE);
    }
}
