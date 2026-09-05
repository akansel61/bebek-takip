import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.ui.AnaPencere;
import com.akansel.bebektakip.ui.YanMenu;
import com.akansel.bebektakip.ui.tablo.KayitTabloModeli;
import com.akansel.bebektakip.ui.tablo.KayitTablosu;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
