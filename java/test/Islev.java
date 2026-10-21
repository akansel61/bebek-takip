import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.rapor.Donem;
import com.akansel.bebektakip.rapor.RaporVerisi;
import com.akansel.bebektakip.ui.AnaPencere;
import com.akansel.bebektakip.ui.RaporPanel;
import com.akansel.bebektakip.ui.YanMenu;
import com.akansel.bebektakip.ui.bilesen.DuzButon;
import com.akansel.bebektakip.ui.tablo.KayitTabloModeli;
import com.akansel.bebektakip.ui.tablo.KayitTablosu;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/** Gerçek pencere üzerinden işlevsel doğrulama. */
public class Islev {

    static int basarili = 0;
    static int basarisiz = 0;

    static void kontrol(String ad, Object beklenen, Object gercek) {
        if (beklenen == null ? gercek == null : beklenen.equals(gercek)) {
            basarili++;
        } else {
            basarisiz++;
            System.out.println("  BAŞARISIZ " + ad + ": beklenen=<" + beklenen
                    + "> gerçek=<" + gercek + ">");
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception yoksay) {
                // varsayılanla devam
            }
        });

        Path dizin = Files.createTempDirectory("islev");
        KayitDeposu depo = new KayitDeposu(dizin);
        depo.yukle();

        final AnaPencere[] kutu = new AnaPencere[1];
        SwingUtilities.invokeAndWait(() -> {
            AnaPencere p = new AnaPencere(depo);
            p.setSize(1220, 780);
            p.setLocation(-1500, 60);
            p.setVisible(true);
            kutu[0] = p;
        });
        Thread.sleep(500);
        AnaPencere pencere = kutu[0];

        // yeni kayıt ekleme
        System.out.println("[Yeni kayıt]");
        eylem(pencere, "yeniKayit");
        eylem(pencere, "yeniKayit");
        eylem(pencere, "yeniKayit");
        Thread.sleep(200);
        kontrol("depo 3 kayıt", 3, depo.sayi());
        kontrol("dosyaya yazıldı", true, Files.exists(depo.getDosya()));

        List<KayitTablosu> tablolar = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> topla(pencere.getContentPane(), tablolar));
        kontrol("iki tablo var", 2, tablolar.size());

        KayitTablosu duzenlenebilir = null;
        KayitTablosu ozet = null;
        for (KayitTablosu t : tablolar) {
            KayitTabloModeli m = (KayitTabloModeli) t.getModel();
            if (m.isDuzenlenebilir()) {
                duzenlenebilir = t;
            } else {
                ozet = t;
            }
        }
        kontrol("düzenlenebilir tablo bulundu", true, duzenlenebilir != null);
        kontrol("özet tablosu bulundu", true, ozet != null);

        final KayitTabloModeli model = (KayitTabloModeli) duzenlenebilir.getModel();
        final KayitTabloModeli ozetModel = (KayitTabloModeli) ozet.getModel();
        kontrol("tabloda 3 satır", 3, model.getRowCount());
        kontrol("özet tablosunda 3 satır", 3, ozetModel.getRowCount());

        // işaretleme ve düzenleme
        System.out.println("[İşaretleme ve düzenleme]");
        SwingUtilities.invokeAndWait(() -> {
            model.tersineCevir(0, KayitTabloModeli.SUTUN_CIS);
            model.tersineCevir(0, KayitTabloModeli.SUTUN_MAMA);
            model.setValueAt("150 ml", 0, KayitTabloModeli.SUTUN_MAMA_NOTU);
            model.setValueAt("Gece beslenmesi", 0, KayitTabloModeli.SUTUN_NOT);
            model.setValueAt("03:45", 0, KayitTabloModeli.SUTUN_SAAT);
            model.tersineCevir(1, KayitTabloModeli.SUTUN_KAKA);
            model.tersineCevir(2, KayitTabloModeli.SUTUN_SAG);
        });
        Kayit ilk = model.satirdaki(0);
        kontrol("çiş işaretlendi", true, ilk.isCis());
        kontrol("mama işaretlendi", true, ilk.isMama());
        kontrol("mama notu", "150 ml", ilk.getMamaNotu());
        kontrol("mama miktarı", 150.0, ilk.mamaMiktari());
        kontrol("not", "Gece beslenmesi", ilk.getNot());
        kontrol("saat değişti", "03:45", ilk.saatMetni());
        kontrol("geçersiz saat yok sayıldı", "03:45", saatDene(model, ilk));
        kontrol("emzirme (3. satır)", true, model.satirdaki(2).isEmzirme());

        // gecikmeli kaydın diske yansıması
        System.out.println("[Gecikmeli kayıt]");
        Thread.sleep(1200);
        KayitDeposu tekrar = new KayitDeposu(dizin);
        tekrar.yukle();
        kontrol("diskte 3 kayıt", 3, tekrar.sayi());
        boolean notBulundu = false;
        for (Kayit k : tekrar.getKayitlar()) {
            if ("Gece beslenmesi".equals(k.getNot()) && k.isMama()
                    && "150 ml".equals(k.getMamaNotu())) {
                notBulundu = true;
            }
        }
        kontrol("düzenleme diske yazıldı", true, notBulundu);

        // arama
        System.out.println("[Arama]");
        SwingUtilities.invokeAndWait(() -> model.setFiltre("Gece"));
        kontrol("filtre 1 sonuc", 1, model.getRowCount());
        SwingUtilities.invokeAndWait(() -> model.setFiltre("gece"));
        kontrol("küçük harf duyarsız", 1, model.getRowCount());
        SwingUtilities.invokeAndWait(() -> model.setFiltre("150"));
        kontrol("mama notunda arama", 1, model.getRowCount());
        SwingUtilities.invokeAndWait(() -> model.setFiltre("03:45"));
        kontrol("saatte arama", 1, model.getRowCount());
        SwingUtilities.invokeAndWait(() -> model.setFiltre("bulunmayan"));
        kontrol("sonuçsuz arama", 0, model.getRowCount());
        SwingUtilities.invokeAndWait(() -> model.setFiltre(""));
        kontrol("filtre temizlendi", 3, model.getRowCount());

        // Tarayıcı sürümünde arama açıkken hücre düzenlemek, aramaya uymayan tüm
        // kayıtları siliyordu: collectSave yalnızca görünen satırları yazıyordu.
        System.out.println("[Filtre açıkken düzenleme - veri kaybı olmamalı]");
        SwingUtilities.invokeAndWait(() -> {
            model.setFiltre("Gece");
            model.setValueAt("Gece beslenmesi - guncellendi", 0,
                    KayitTabloModeli.SUTUN_NOT);
            model.tersineCevir(0, KayitTabloModeli.SUTUN_KAKA);
        });
        kontrol("filtreli görünümde 1 satır", 1, model.getRowCount());
        kontrol("depo bozulmadı", 3, depo.sayi());
        SwingUtilities.invokeAndWait(() -> model.setFiltre(""));
        kontrol("filtre kalkınca 3 satır", 3, model.getRowCount());
        Thread.sleep(1200);
        KayitDeposu filtreSonrasi = new KayitDeposu(dizin);
        filtreSonrasi.yukle();
        kontrol("diskte hala 3 kayıt", 3, filtreSonrasi.sayi());
        boolean guncelBulundu = false;
        for (Kayit k : filtreSonrasi.getKayitlar()) {
            if ("Gece beslenmesi - guncellendi".equals(k.getNot())) {
                guncelBulundu = true;
            }
        }
        kontrol("düzenleme kaydedildi", true, guncelBulundu);

        // silme
        System.out.println("[Silme]");
        Kayit silinecek = model.satirdaki(1);
        SwingUtilities.invokeAndWait(() -> depo.sil(silinecek));
        SwingUtilities.invokeAndWait(model::yenile);
        kontrol("depoda 2 kayıt", 2, depo.sayi());
        kontrol("tabloda 2 satır", 2, model.getRowCount());
        KayitDeposu silmeSonrasi = new KayitDeposu(dizin);
        silmeSonrasi.yukle();
        kontrol("silme diske yansıdı", 2, silmeSonrasi.sayi());

        // görünüm geçişleri
        System.out.println("[Görünüm geçişi]");
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_ISTATISTIK));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_OZET));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_KAYITLAR));
        kontrol("geçişler sorunsuz", 2, model.getRowCount());

        // uyku, büyüme ve hatırlatıcı akışları
        System.out.println("[Uyku / Büyüme / Hatırlatıcı]");
        SwingUtilities.invokeAndWait(() -> {
            UykuKayit u = new UykuKayit();
            u.setBaslangic(LocalTime.of(13, 0));
            u.setBitis(LocalTime.of(14, 30));
            depo.ekleUyku(u);
            BuyumeKayit b = new BuyumeKayit();
            b.setKilo(4.2);
            b.setBoy(56);
            depo.ekleBuyume(b);
            Hatirlatici h = new Hatirlatici();
            h.setBaslik("Aşı randevusu");
            depo.ekleHatirlatici(h);
            IlacKayit i = new IlacKayit();
            i.setAd("D vitamini");
            i.setDoz("3 damla");
            depo.ekleIlac(i);
        });
        kontrol("uyku kaydı eklendi", 1, depo.getUykular().size());
        kontrol("uyku süresi doğru", 90L, depo.getUykular().get(0).sureDakika());
        kontrol("ölçüm eklendi", 1, depo.getBuyumeler().size());
        kontrol("bekleyen hatırlatıcı", 1, depo.bekleyenHatirlatici());
        kontrol("ilaç kaydı eklendi", 1, depo.getIlaclar().size());
        kontrol("uyku dosyası yazıldı", true,
                Files.exists(dizin.resolve("uykular.json")));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_UYKU));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_BUYUME));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_ILAC));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_HATIRLATICI));
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_KAYITLAR));
        kontrol("yeni ekran geçişleri sorunsuz", 2, model.getRowCount());

        // özet tablosu en fazla 5 satır göstermeli
        System.out.println("[Özet sınırı]");
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < 8; i++) {
                depo.getKayitlar().add(new Kayit());
            }
            depo.guncellendi();
            pencere.gorunumeGec(YanMenu.GORUNUM_OZET);
        });
        kontrol("depoda 10 kayıt", 10, depo.sayi());
        kontrol("özet en fazla 5", 5, ozetModel.getRowCount());

        // ateş takibi
        System.out.println("[Ateş takibi]");
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_ATES));
        DuzButon olc = dugme(pencere, "Ateş Ölç");
        SwingUtilities.invokeAndWait(olc::doClick);
        Thread.sleep(150);
        kontrol("ölçüm satırı açıldı", 1, depo.getAtesler().size());
        JTable atesTablosu = tablo(pencere, t -> t.getColumnCount() == 7
                && "Derece".equals(t.getColumnName(2)));
        kontrol("ateş tablosu bulundu", true, atesTablosu != null);
        SwingUtilities.invokeAndWait(() -> {
            if (atesTablosu.isEditing()) {
                atesTablosu.getCellEditor().cancelCellEditing();
            }
            atesTablosu.getModel().setValueAt("38,6", 0, 2);
            atesTablosu.getModel().setValueAt("çok", 0, 2);
            atesTablosu.getModel().setValueAt("Ateş düşürücü verildi", 0, 5);
        });
        AtesKayit olcum = depo.getAtesler().get(0);
        kontrol("derece girildi", 38.6, olcum.getDerece());
        kontrol("geçersiz derece yok sayıldı", 38.6, olcum.getDerece());
        kontrol("ateş seviyesi", AtesKayit.Seviye.ATES, olcum.seviye());
        kontrol("tabloda virgüllü derece", "38,6", atesTablosu.getModel().getValueAt(0, 2));
        KayitDeposu atesSonrasi = new KayitDeposu(dizin);
        atesSonrasi.yukle();
        kontrol("ateş diske yazıldı", 38.6, atesSonrasi.getAtesler().get(0).getDerece());
        kontrol("ateş notu diske yazıldı", "Ateş düşürücü verildi",
                atesSonrasi.getAtesler().get(0).getNot());

        // vitamin / ilaç ayrımı
        System.out.println("[Vitamin / ilaç türü]");
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_ILAC));
        SwingUtilities.invokeAndWait(dugme(pencere, "İlaç Ekle")::doClick);
        Thread.sleep(150);
        kontrol("ilaç türünde eklendi", IlacKayit.TUR_ILAC, depo.getIlaclar().get(0).getTur());
        SwingUtilities.invokeAndWait(dugme(pencere, "Vitamin Ekle")::doClick);
        Thread.sleep(150);
        JTable ilacTablosu = tablo(pencere, t -> t.getColumnCount() == 7
                && "Tür".equals(t.getColumnName(2)));
        SwingUtilities.invokeAndWait(() -> {
            if (ilacTablosu.isEditing()) {
                ilacTablosu.getCellEditor().stopCellEditing();
            }
        });
        IlacKayit vitamin = depo.getIlaclar().get(0);
        kontrol("vitamin türünde eklendi", true, vitamin.isVitamin());
        kontrol("son vitaminin adı hazır geldi", "D vitamini", vitamin.getAd());
        kontrol("son vitaminin dozu hazır geldi", "3 damla", vitamin.getDoz());
        kontrol("tabloda tür", "Vitamin", ilacTablosu.getModel().getValueAt(0, 2));

        // rapor ekranı
        System.out.println("[Rapor]");
        eylem(pencere, "gorunum9");
        Thread.sleep(300);
        RaporPanel rapor = bul(pencere.getContentPane(), RaporPanel.class);
        kontrol("rapor ekranı bulundu", true, rapor != null);
        final int[] sayfa = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            rapor.setDonem(Donem.gunluk(LocalDate.now()));
            sayfa[0] = rapor.sayfaSayisi();
        });
        kontrol("günlük rapor tek sayfa", 1, sayfa[0]);
        Path pdf = dizin.resolve("rapor.pdf");
        Path xlsx = dizin.resolve("rapor.xlsx");
        final Exception[] hata = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                rapor.pdfKaydet(pdf);
                rapor.excelKaydet(xlsx);
            } catch (Exception e) {
                hata[0] = e;
            }
        });
        kontrol("rapor yazılırken hata yok", null, hata[0]);
        byte[] pdfBaslik = Files.readAllBytes(pdf);
        kontrol("PDF yazıldı", "%PDF", new String(pdfBaslik, 0, 4,
                java.nio.charset.StandardCharsets.ISO_8859_1));
        byte[] zipBaslik = Files.readAllBytes(xlsx);
        kontrol("Excel (zip) yazıldı", true, zipBaslik[0] == 'P' && zipBaslik[1] == 'K');
        SwingUtilities.invokeAndWait(() -> {
            rapor.setBolumler(EnumSet.noneOf(RaporVerisi.Bolum.class));
            sayfa[0] = rapor.sayfaSayisi();
        });
        kontrol("bölüm seçilmeyince sayfa yok", 0, sayfa[0]);
        SwingUtilities.invokeAndWait(() -> {
            rapor.setBolumler(EnumSet.allOf(RaporVerisi.Bolum.class));
            rapor.setDonem(rapor.getDonem().onceki());
        });
        kontrol("önceki güne geçildi", LocalDate.now().minusDays(1),
                rapor.getDonem().getBaslangic());
        SwingUtilities.invokeAndWait(() -> pencere.gorunumeGec(YanMenu.GORUNUM_OZET));

        SwingUtilities.invokeAndWait(pencere::dispose);
        System.out.println();
        System.out.println("Başarılı: " + basarili + "  Başarısız: " + basarisiz);
        System.exit(basarisiz > 0 ? 1 : 0);
    }

    static String saatDene(KayitTabloModeli model, Kayit k) throws Exception {
        SwingUtilities.invokeAndWait(() ->
                model.setValueAt("99:99", 0, KayitTabloModeli.SUTUN_SAAT));
        return k.saatMetni();
    }

    static void eylem(AnaPencere pencere, String ad) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ActionMap am = ((JComponent) pencere.getContentPane()).getActionMap();
            javax.swing.Action a = am.get(ad);
            if (a == null) {
                throw new IllegalStateException("eylem yok: " + ad);
            }
            a.actionPerformed(new ActionEvent(pencere, ActionEvent.ACTION_PERFORMED, ad));
        });
        Thread.sleep(120);
    }

    /** Pencerede verilen metinli ilk düğme. */
    static DuzButon dugme(AnaPencere pencere, String metin) throws Exception {
        final DuzButon[] kutu = new DuzButon[1];
        SwingUtilities.invokeAndWait(() -> kutu[0] = bul(pencere.getContentPane(), DuzButon.class,
                d -> metin.equals(d.getText()) && d.isShowing()));
        if (kutu[0] == null) {
            throw new IllegalStateException("düğme yok: " + metin);
        }
        return kutu[0];
    }

    static JTable tablo(AnaPencere pencere, Predicate<JTable> kosul) throws Exception {
        final JTable[] kutu = new JTable[1];
        SwingUtilities.invokeAndWait(() ->
                kutu[0] = bul(pencere.getContentPane(), JTable.class, kosul));
        return kutu[0];
    }

    static <T> T bul(Container k, Class<T> tur) {
        return bul(k, tur, t -> true);
    }

    static <T> T bul(Container k, Class<T> tur, Predicate<T> kosul) {
        for (Component c : k.getComponents()) {
            if (tur.isInstance(c) && kosul.test(tur.cast(c))) {
                return tur.cast(c);
            }
            if (c instanceof Container) {
                T t = bul((Container) c, tur, kosul);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    static void topla(Container k, List<KayitTablosu> hedef) {
        for (Component c : k.getComponents()) {
            if (c instanceof KayitTablosu) {
                hedef.add((KayitTablosu) c);
            }
            if (c instanceof Container) {
                topla((Container) c, hedef);
            }
        }
    }
}
