import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.ui.AnaPencere;
import com.akansel.bebektakip.ui.YanMenu;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pencereyi ekranın dışında gerçekleştirip PNG'ye çizer; böylece yerleşim
 * gerçek koşullarda hesaplanır ama ekranda bir şey görünmez.
 */
public class Onizleme {

    private static final int GENISLIK = 1220;
    private static final int YUKSEKLIK = 780;

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Path cikti = Path.of(args.length > 0 ? args[0] : "build/onizleme");
        Files.createDirectories(cikti);

        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception yoksay) {
                // varsayılan görünümle devam
            }
        });

        Path bosDizin = Files.createTempDirectory("onizleme-bos");
        KayitDeposu bos = new KayitDeposu(bosDizin);
        bos.yukle();
        cek(bos, new String[]{YanMenu.GORUNUM_OZET, YanMenu.GORUNUM_KAYITLAR},
                new Path[]{cikti.resolve("1-bos-ozet.png"),
                        cikti.resolve("2-bos-kayitlar.png")});

        Path doluDizin = Files.createTempDirectory("onizleme-dolu");
        KayitDeposu depo = new KayitDeposu(doluDizin);
        depo.yukle();
        ornekVeri(depo);
        ornekDigerVeri(depo);
        cek(depo, new String[]{YanMenu.GORUNUM_OZET, YanMenu.GORUNUM_KAYITLAR,
                        YanMenu.GORUNUM_UYKU, YanMenu.GORUNUM_BUYUME,
                        YanMenu.GORUNUM_ILAC, YanMenu.GORUNUM_HATIRLATICI,
                        YanMenu.GORUNUM_ISTATISTIK},
                new Path[]{cikti.resolve("3-ozet.png"),
                        cikti.resolve("4-kayitlar.png"),
                        cikti.resolve("5-uyku.png"),
                        cikti.resolve("6-buyume.png"),
                        cikti.resolve("7-vitamin-ilac.png"),
                        cikti.resolve("8-hatirlatici.png"),
                        cikti.resolve("9-istatistikler.png")});

        System.exit(0);
    }

    static void cek(KayitDeposu depo, String[] gorunumler, Path[] hedefler) throws Exception {
        final AnaPencere[] kutu = new AnaPencere[1];
        SwingUtilities.invokeAndWait(() -> {
            AnaPencere p = new AnaPencere(depo);
            p.setSize(GENISLIK, YUKSEKLIK);
            // ekranın solunda, görünmeyen bir konum
            p.setLocation(-GENISLIK - 200, 60);
            p.setVisible(true);
            kutu[0] = p;
        });
        Thread.sleep(700);

        for (int i = 0; i < gorunumler.length; i++) {
            final String gorunum = gorunumler[i];
            SwingUtilities.invokeAndWait(() -> kutu[0].gorunumeGec(gorunum));
            Thread.sleep(350);
            final Path hedef = hedefler[i];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    yaz(kutu[0], hedef);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        SwingUtilities.invokeAndWait(() -> kutu[0].dispose());
        Thread.sleep(200);
    }

    static void yaz(AnaPencere pencere, Path hedef) throws Exception {
        Container icerik = pencere.getContentPane();
        // Ekranın DPI ölçeğiyle çiz; yoksa yazı ölçüleri gerçekte olduğundan farklı çıkar.
        double olcek = pencere.getGraphicsConfiguration()
                .getDefaultTransform().getScaleX();
        BufferedImage img = new BufferedImage(
                (int) Math.round(icerik.getWidth() * olcek),
                (int) Math.round(icerik.getHeight() * olcek),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.scale(olcek, olcek);
            icerik.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(img, "png", new File(hedef.toString()));
        System.out.println("yazıldı: " + hedef + "  "
                + img.getWidth() + "x" + img.getHeight());
    }

    static void ornekVeri(KayitDeposu depo) {
        LocalDate bugun = LocalDate.now();
        int[][] plan = {
                {0, 6, 30}, {0, 9, 15}, {0, 11, 40}, {0, 14, 5}, {0, 17, 20}, {0, 20, 0},
                {1, 7, 0}, {1, 10, 30}, {1, 13, 15}, {1, 16, 45}, {1, 21, 10},
                {2, 8, 20}, {2, 12, 0}, {2, 15, 30}, {2, 19, 50},
                {3, 7, 45}, {3, 11, 10}, {3, 14, 40}, {3, 18, 25}, {3, 22, 0},
                {4, 6, 55}, {4, 10, 5}, {4, 13, 50},
                {5, 9, 30}, {5, 12, 45}, {5, 16, 0}, {5, 20, 15},
                {6, 8, 0}, {6, 11, 25},
        };
        String[] notlar = {"", "Uyuyarak içti", "Gaz sancısı vardı", "", "Çok iştahlıydı",
                "", "Yarısını bıraktı", "", "Kusmadı", ""};
        int i = 0;
        for (int[] p : plan) {
            Kayit k = new Kayit();
            k.setTarih(bugun.minusDays(p[0]));
            k.setSaat(LocalTime.of(p[1], p[2]));
            k.setCis(i % 3 != 2);
            k.setKaka(i % 4 == 0);
            boolean mamaMi = i % 3 == 0;
            k.setMama(mamaMi);
            if (mamaMi) {
                k.setMamaNotu((80 + (i % 5) * 20) + " ml");
            } else {
                k.setSagMeme(i % 2 == 0);
                k.setSolMeme(i % 2 == 1);
            }
            k.setNot(notlar[i % notlar.length]);
            depo.getKayitlar().add(k);
            i++;
        }
        depo.sirala();
        depo.kaydet();
    }

    static void ornekDigerVeri(KayitDeposu depo) {
        LocalDate bugun = LocalDate.now();

        int[][] uykular = {
                {0, 9, 15, 10, 5}, {0, 13, 0, 14, 30},
                {1, 12, 30, 14, 10}, {1, 20, 45, 6, 20},
                {2, 13, 10, 15, 0}, {3, 12, 50, 14, 25},
                {5, 13, 20, 15, 5},
        };
        for (int[] p : uykular) {
            UykuKayit u = new UykuKayit();
            u.setTarih(bugun.minusDays(p[0]));
            u.setBaslangic(LocalTime.of(p[1], p[2]));
            u.setBitis(LocalTime.of(p[3], p[4]));
            depo.getUykular().add(u);
        }

        double[][] olcumler = {{0, 5.1, 58, 39.5}, {14, 4.6, 56, 38.7}, {30, 4.1, 54, 37.8}};
        for (double[] o : olcumler) {
            BuyumeKayit b = new BuyumeKayit();
            b.setTarih(bugun.minusDays((long) o[0]));
            b.setKilo(o[1]);
            b.setBoy(o[2]);
            b.setBasCevresi(o[3]);
            depo.getBuyumeler().add(b);
        }

        String[] basliklar = {"D vitamini", "5'li karma aşı", "Kontrol randevusu"};
        int[] gunler = {0, 3, 12};
        for (int i = 0; i < basliklar.length; i++) {
            Hatirlatici h = new Hatirlatici();
            h.setBaslik(basliklar[i]);
            h.setTarih(bugun.plusDays(gunler[i]));
            h.setSaat(LocalTime.of(10, 0));
            depo.getHatirlaticilar().add(h);
        }

        String[][] ilaclar = {
                {"0", "8", "30", "D vitamini", "3 damla", ""},
                {"0", "20", "0", "Demir şurubu", "1 ml", "Akşam beslenmesinden sonra"},
                {"1", "8", "15", "D vitamini", "3 damla", ""},
                {"2", "8", "45", "D vitamini", "3 damla", "Kahvaltıdan önce"},
        };
        for (String[] p : ilaclar) {
            IlacKayit ilac = new IlacKayit();
            ilac.setTarih(bugun.minusDays(Integer.parseInt(p[0])));
            ilac.setSaat(LocalTime.of(Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            ilac.setAd(p[3]);
            ilac.setDoz(p[4]);
            ilac.setNot(p[5]);
            depo.getIlaclar().add(ilac);
        }

        depo.sirala();
        depo.kaydetUyku();
        depo.kaydetBuyume();
        depo.kaydetHatirlatici();
        depo.kaydetIlac();
    }
}
