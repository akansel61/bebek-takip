package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.DisaAktarim;
import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.BuyumeKayit;
import com.akansel.bebektakip.model.Hatirlatici;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.ui.bilesen.DuzButon;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Ana pencere; bütün ekranların birleştiği yer. */
public class AnaPencere extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String SURUM = "v1.2";

    private static final DateTimeFormatter SAAT_BICIMI =
            DateTimeFormatter.ofPattern("HH:mm:ss", Tema.TR);

    private final KayitDeposu depo;

    private final YanMenu yanMenu;
    private final CardLayout gorunumDuzeni = new CardLayout();
    private final JPanel gorunumKabi = new JPanel(gorunumDuzeni);
    private final OzetPanel ozetPanel;
    private final KayitlarPanel kayitlarPanel;
    private final UykuPanel uykuPanel;
    private final BuyumePanel buyumePanel;
    private final IlacPanel ilacPanel;
    private final HatirlaticiPanel hatirlaticiPanel;
    private final IstatistiklerPanel istatistiklerPanel;

    private final JLabel tarihEtiketi = new JLabel();
    private final Timer kaydetZamanlayici;
    private final Timer saatZamanlayici;
    private TrayIcon tepsiSimgesi;

    private LocalDate gosterilenGun = LocalDate.now();
    private String aktifGorunum = YanMenu.GORUNUM_OZET;

    public AnaPencere(KayitDeposu depo) {
        this.depo = depo;

        setTitle("Bebek Beslenme Takibi");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImages(UygulamaIkonu.tumBoyutlar());
        setMinimumSize(new Dimension(1060, 680));
        setSize(new Dimension(1220, 780));
        setLocationRelativeTo(null);

        kaydetZamanlayici = new Timer(600, e -> kaydetVeOzetiTazele());
        kaydetZamanlayici.setRepeats(false);

        ozetPanel = new OzetPanel(depo, () -> gorunumeGec(YanMenu.GORUNUM_KAYITLAR),
                this::silmeyiOnayla);
        kayitlarPanel = new KayitlarPanel(depo, this::silmeyiOnayla, this::veriDegisti);
        uykuPanel = new UykuPanel(depo);
        buyumePanel = new BuyumePanel(depo);
        ilacPanel = new IlacPanel(depo);
        hatirlaticiPanel = new HatirlaticiPanel(depo);
        istatistiklerPanel = new IstatistiklerPanel(depo);

        yanMenu = new YanMenu(SURUM, this::gorunumeGec);

        gorunumKabi.setOpaque(true);
        gorunumKabi.setBackground(Tema.ARKA);
        gorunumKabi.add(kaydirmaya(ozetPanel), YanMenu.GORUNUM_OZET);
        gorunumKabi.add(kayitlarPanel, YanMenu.GORUNUM_KAYITLAR);
        gorunumKabi.add(uykuPanel, YanMenu.GORUNUM_UYKU);
        gorunumKabi.add(buyumePanel, YanMenu.GORUNUM_BUYUME);
        gorunumKabi.add(ilacPanel, YanMenu.GORUNUM_ILAC);
        gorunumKabi.add(hatirlaticiPanel, YanMenu.GORUNUM_HATIRLATICI);
        gorunumKabi.add(kaydirmaya(istatistiklerPanel), YanMenu.GORUNUM_ISTATISTIK);

        JPanel ana = new JPanel(new BorderLayout());
        ana.setOpaque(true);
        ana.setBackground(Tema.ARKA);
        ana.add(ustSerit(), BorderLayout.NORTH);
        ana.add(gorunumKabi, BorderLayout.CENTER);

        JPanel kok = new JPanel(new BorderLayout());
        kok.setBackground(Tema.ARKA);
        kok.add(yanMenu, BorderLayout.WEST);
        kok.add(ana, BorderLayout.CENTER);
        setContentPane(kok);

        kisayollariKur();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                kapat();
            }
        });

        saatZamanlayici = new Timer(30_000, e -> gunuKontrolEt());
        saatZamanlayici.start();

        sistemTepsisiniKur();

        tarihiGuncelle();
        tumunuYenile();
        gorunumeGec(YanMenu.GORUNUM_OZET);

        if (depo.getSonHata() != null) {
            JOptionPane.showMessageDialog(this, depo.getSonHata(),
                    "Uyarı", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JScrollPane kaydirmaya(JComponent icerik) {
        JScrollPane k = new JScrollPane(icerik);
        k.setBorder(BorderFactory.createEmptyBorder());
        k.getViewport().setBackground(Tema.ARKA);
        k.setBackground(Tema.ARKA);
        k.getVerticalScrollBar().setUnitIncrement(24);
        k.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return k;
    }

    private JComponent ustSerit() {
        JPanel serit = new JPanel(new BorderLayout(16, 0));
        serit.setOpaque(true);
        serit.setBackground(Tema.YUZEY);
        serit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.CIZGI),
                BorderFactory.createEmptyBorder(0, 24, 0, 20)));
        serit.setPreferredSize(new Dimension(0, 60));

        JLabel baslik = new JLabel("Bebek Beslenme Takibi");
        baslik.setFont(Tema.fontKalin(17));
        baslik.setForeground(Tema.METIN);
        serit.add(baslik, BorderLayout.WEST);

        tarihEtiketi.setFont(Tema.font(13));
        tarihEtiketi.setForeground(Tema.METIN_SOLUK);

        DuzButon disaAktarButonu = new DuzButon("Dışa Aktar", Ikonlar::disaAktar,
                DuzButon.Bicim.CERCEVELI);
        disaAktarButonu.bosluk(13, 8);
        disaAktarButonu.addActionListener(e ->
                menuyuGoster(disaAktarButonu));

        DuzButon yeniButon = new DuzButon("Yeni Kayıt", Ikonlar::arti, DuzButon.Bicim.DOLU);
        yeniButon.bosluk(15, 8);
        yeniButon.addActionListener(e -> yeniKayit());

        JPanel sag = new JPanel();
        sag.setOpaque(false);
        sag.setLayout(new javax.swing.BoxLayout(sag, javax.swing.BoxLayout.X_AXIS));
        sag.add(tarihEtiketi);
        sag.add(javax.swing.Box.createHorizontalStrut(18));
        sag.add(disaAktarButonu);
        sag.add(javax.swing.Box.createHorizontalStrut(10));
        sag.add(yeniButon);

        JPanel sagKap = new JPanel(new BorderLayout());
        sagKap.setOpaque(false);
        sagKap.add(sag, BorderLayout.EAST);
        serit.add(sagKap, BorderLayout.CENTER);
        return serit;
    }

    private void menuyuGoster(JComponent sahip) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuOgesi("CSV olarak dışa aktar…", e -> csvDisaAktar()));
        menu.add(menuOgesi("JSON yedek al…", e -> jsonDisaAktar()));
        menu.add(menuOgesi("JSON'dan içe aktar…", e -> jsonIceAktar()));
        menu.addSeparator();
        menu.add(menuOgesi("Veri klasörünü aç", e -> veriKlasorunuAc()));
        menu.addSeparator();
        menu.add(menuOgesi("Hakkında", e -> hakkinda()));
        menu.show(sahip, 0, sahip.getHeight() + 6);
    }

    private JMenuItem menuOgesi(String metin, java.awt.event.ActionListener eylem) {
        JMenuItem oge = new JMenuItem(metin);
        oge.setFont(Tema.font(13));
        oge.addActionListener(eylem);
        return oge;
    }

    /**
     * Görev çubuğu tepsisine küçük bir simge koyar: çift tıklama pencereyi
     * açar, sağ tık menüsünden yeni kayıt eklenip çıkılabilir. Tepsi
     * desteklenmiyorsa uygulama sessizce tepsisiz çalışır.
     */
    private void sistemTepsisiniKur() {
        if (!SystemTray.isSupported()) {
            return;
        }
        PopupMenu menu = new PopupMenu();

        MenuItem goster = new MenuItem("Pencereyi Göster");
        goster.addActionListener(e -> pencereyiGoster());
        menu.add(goster);
        menu.addSeparator();

        MenuItem yeniOge = new MenuItem("Yeni Kayıt");
        yeniOge.addActionListener(e -> {
            pencereyiGoster();
            yeniKayit();
        });
        menu.add(yeniOge);

        MenuItem uyku = new MenuItem("Uyku Takibi");
        uyku.addActionListener(e -> {
            pencereyiGoster();
            gorunumeGec(YanMenu.GORUNUM_UYKU);
        });
        menu.add(uyku);
        menu.addSeparator();

        MenuItem cikis = new MenuItem("Çıkış");
        cikis.addActionListener(e -> kapat());
        menu.add(cikis);

        TrayIcon simge = new TrayIcon(UygulamaIkonu.olustur(16), "Bebek Takip", menu);
        simge.setImageAutoSize(true);
        simge.addActionListener(e -> pencereyiGoster());
        try {
            SystemTray.getSystemTray().add(simge);
            tepsiSimgesi = simge;
        } catch (AWTException e) {
            tepsiSimgesi = null;
        }
    }

    private void pencereyiGoster() {
        setVisible(true);
        setState(NORMAL);
        toFront();
        requestFocus();
    }

    public void gorunumeGec(String anahtar) {
        aktifGorunum = anahtar;
        yanMenu.setAktif(anahtar);
        gorunumDuzeni.show(gorunumKabi, anahtar);
        if (YanMenu.GORUNUM_OZET.equals(anahtar)) {
            ozetPanel.yenile();
        } else if (YanMenu.GORUNUM_KAYITLAR.equals(anahtar)) {
            depo.sirala();
            kayitlarPanel.yenile();
        } else if (YanMenu.GORUNUM_UYKU.equals(anahtar)) {
            depo.sirala();
            uykuPanel.yenile();
        } else if (YanMenu.GORUNUM_BUYUME.equals(anahtar)) {
            depo.sirala();
            buyumePanel.yenile();
        } else if (YanMenu.GORUNUM_ILAC.equals(anahtar)) {
            depo.sirala();
            ilacPanel.yenile();
        } else if (YanMenu.GORUNUM_HATIRLATICI.equals(anahtar)) {
            depo.sirala();
            hatirlaticiPanel.yenile();
        } else {
            istatistiklerPanel.yenile();
        }
    }

    private void tumunuYenile() {
        ozetPanel.yenile();
        kayitlarPanel.yenile();
        uykuPanel.yenile();
        buyumePanel.yenile();
        ilacPanel.yenile();
        hatirlaticiPanel.yenile();
        istatistiklerPanel.yenile();
        sonGuncellemeyiYaz();
    }

    private void sonGuncellemeyiYaz() {
        LocalDateTime t = depo.getSonKayitZamani();
        yanMenu.setSonGuncelleme(t == null ? "—" : t.format(SAAT_BICIMI));
    }

    /** Hücre değiştikçe çağrılır; art arda gelen değişiklikleri tek yazmada toplar. */
    private void veriDegisti() {
        kaydetZamanlayici.restart();
    }

    private void kaydetVeOzetiTazele() {
        if (!depo.kaydet()) {
            hataGoster(depo.getSonHata());
            return;
        }
        sonGuncellemeyiYaz();
        ozetPanel.yenile();
        istatistiklerPanel.yenile();
    }

    /** Bekleyen değişiklikleri beklemeden diske yazar. */
    private void bekleyeniYaz() {
        if (kaydetZamanlayici.isRunning()) {
            kaydetZamanlayici.stop();
            depo.kaydet();
            sonGuncellemeyiYaz();
        }
    }

    private void yeniKayit() {
        bekleyeniYaz();
        Kayit k = new Kayit();
        depo.ekle(k);
        tumunuYenile();
        if (!YanMenu.GORUNUM_KAYITLAR.equals(aktifGorunum)) {
            gorunumeGec(YanMenu.GORUNUM_KAYITLAR);
        }
        kayitlarPanel.kaydaGit(k);
    }

    private void silmeyiOnayla(Kayit kayit) {
        String ozet = Tema.tamTarih(kayit.getTarih()) + " " + kayit.saatMetni();
        int cevap = JOptionPane.showConfirmDialog(this,
                "Bu kayıt silinsin mi?\n\n" + ozet,
                "Kaydı sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (cevap != JOptionPane.YES_OPTION) {
            return;
        }
        kaydetZamanlayici.stop();
        depo.sil(kayit);
        tumunuYenile();
    }

    private void csvDisaAktar() {
        bekleyeniYaz();
        File hedef = dosyaSec("CSV olarak dışa aktar", "bebek-takip-"
                + LocalDate.now() + ".csv", "CSV dosyası (*.csv)", "csv");
        if (hedef == null) {
            return;
        }
        try {
            DisaAktarim.csvYaz(hedef.toPath(), depo.getKayitlar());
            bilgiGoster(depo.sayi() + " kayıt aktarıldı:\n" + hedef.getAbsolutePath());
        } catch (IOException e) {
            hataGoster("CSV yazılamadı: " + e.getMessage());
        }
    }

    private void jsonDisaAktar() {
        bekleyeniYaz();
        File hedef = dosyaSec("JSON yedek al", "bebek-takip-yedek-"
                + LocalDate.now() + ".json", "JSON dosyası (*.json)", "json");
        if (hedef == null) {
            return;
        }
        try {
            DisaAktarim.yedekYaz(hedef.toPath(), depo);
            int toplam = depo.sayi() + depo.getUykular().size()
                    + depo.getBuyumeler().size() + depo.getHatirlaticilar().size()
                    + depo.getIlaclar().size();
            bilgiGoster(toplam + " kayıt yedeklendi:\n" + hedef.getAbsolutePath());
        } catch (IOException e) {
            hataGoster("Yedek yazılamadı: " + e.getMessage());
        }
    }

    private void jsonIceAktar() {
        bekleyeniYaz();
        JFileChooser secici = new JFileChooser();
        secici.setDialogTitle("JSON'dan içe aktar");
        secici.setFileFilter(new FileNameExtensionFilter("JSON dosyası (*.json)", "json"));
        if (secici.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String metin;
        try {
            metin = DisaAktarim.metinOku(secici.getSelectedFile().toPath());
        } catch (IOException e) {
            hataGoster("Dosya okunamadı: " + e.getMessage());
            return;
        }

        // Her bölüm ayrı çözülür; eski yedeklerde yalnızca beslenme bulunur.
        List<Kayit> gelenKayit = new ArrayList<>();
        try {
            gelenKayit = KayitDeposu.metniCoz(metin);
        } catch (RuntimeException yoksay) {
            // beslenme bölümü yoksa öbür bölümlere bakılır
        }
        List<UykuKayit> gelenUyku = new ArrayList<>();
        List<BuyumeKayit> gelenBuyume = new ArrayList<>();
        List<Hatirlatici> gelenHatirlatici = new ArrayList<>();
        List<IlacKayit> gelenIlac = new ArrayList<>();
        try {
            gelenUyku = KayitDeposu.metniCozUyku(metin);
            gelenBuyume = KayitDeposu.metniCozBuyume(metin);
            gelenHatirlatici = KayitDeposu.metniCozHatirlatici(metin);
            gelenIlac = KayitDeposu.metniCozIlac(metin);
        } catch (RuntimeException yoksay) {
            // bozuk bölüm içe aktarımı durdurmaz
        }

        int toplam = gelenKayit.size() + gelenUyku.size()
                + gelenBuyume.size() + gelenHatirlatici.size() + gelenIlac.size();
        if (toplam == 0) {
            try {
                KayitDeposu.metniCoz(metin);
                bilgiGoster("Dosyada kayıt bulunamadı.");
            } catch (RuntimeException e) {
                hataGoster("Dosya okunamadı: " + e.getMessage());
            }
            return;
        }

        StringBuilder ozet = new StringBuilder();
        if (!gelenKayit.isEmpty()) {
            ozet.append(gelenKayit.size()).append(" beslenme/bez\n");
        }
        if (!gelenUyku.isEmpty()) {
            ozet.append(gelenUyku.size()).append(" uyku\n");
        }
        if (!gelenBuyume.isEmpty()) {
            ozet.append(gelenBuyume.size()).append(" büyüme ölçümü\n");
        }
        if (!gelenHatirlatici.isEmpty()) {
            ozet.append(gelenHatirlatici.size()).append(" hatırlatıcı\n");
        }
        if (!gelenIlac.isEmpty()) {
            ozet.append(gelenIlac.size()).append(" vitamin/ilaç\n");
        }

        Object[] secenekler = {"Mevcuda ekle", "Hepsini değiştir", "İptal"};
        int secim = JOptionPane.showOptionDialog(this,
                "Dosyada bulunanlar:\n\n" + ozet + "\nNasıl içe aktarılsın?",
                "İçe aktar", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, secenekler, secenekler[0]);
        if (secim == 2 || secim == JOptionPane.CLOSED_OPTION) {
            return;
        }
        boolean degistir = secim == 1;

        if (!gelenKayit.isEmpty()) {
            if (degistir) {
                depo.getKayitlar().clear();
            }
            depo.getKayitlar().addAll(gelenKayit);
            depo.kaydet();
        }
        if (!gelenUyku.isEmpty()) {
            if (degistir) {
                depo.getUykular().clear();
            }
            depo.getUykular().addAll(gelenUyku);
            depo.kaydetUyku();
        }
        if (!gelenBuyume.isEmpty()) {
            if (degistir) {
                depo.getBuyumeler().clear();
            }
            depo.getBuyumeler().addAll(gelenBuyume);
            depo.kaydetBuyume();
        }
        if (!gelenHatirlatici.isEmpty()) {
            if (degistir) {
                depo.getHatirlaticilar().clear();
            }
            depo.getHatirlaticilar().addAll(gelenHatirlatici);
            depo.kaydetHatirlatici();
        }
        if (!gelenIlac.isEmpty()) {
            if (degistir) {
                depo.getIlaclar().clear();
            }
            depo.getIlaclar().addAll(gelenIlac);
            depo.kaydetIlac();
        }
        depo.sirala();
        depo.kaydet();
        tumunuYenile();
        bilgiGoster(toplam + " kayıt içe aktarıldı.");
    }

    private File dosyaSec(String baslik, String varsayilanAd, String aciklama, String uzanti) {
        JFileChooser secici = new JFileChooser();
        secici.setDialogTitle(baslik);
        secici.setSelectedFile(new File(varsayilanAd));
        secici.setFileFilter(new FileNameExtensionFilter(aciklama, uzanti));
        if (secici.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File dosya = secici.getSelectedFile();
        if (!dosya.getName().toLowerCase(Tema.TR).endsWith("." + uzanti)) {
            dosya = new File(dosya.getParentFile(), dosya.getName() + "." + uzanti);
        }
        if (dosya.exists()) {
            int cevap = JOptionPane.showConfirmDialog(this,
                    "Dosya zaten var. Üzerine yazılsın mı?\n" + dosya.getName(),
                    "Üzerine yaz", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (cevap != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return dosya;
    }

    private void veriKlasorunuAc() {
        Path dizin = depo.getDosya().getParent();
        try {
            Files.createDirectories(dizin);
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dizin.toFile());
            } else {
                bilgiGoster("Veri klasörü:\n" + dizin);
            }
        } catch (IOException | RuntimeException e) {
            bilgiGoster("Veri klasörü:\n" + dizin);
        }
    }

    private void hakkinda() {
        JOptionPane.showMessageDialog(this,
                "Bebek Beslenme Takibi " + SURUM + "\n"
                        + Tema.TELIF + "\n\n"
                        + "Beslenme/bez kaydı: " + depo.sayi() + "\n"
                        + "Uyku kaydı: " + depo.getUykular().size() + "\n"
                        + "Büyüme ölçümü: " + depo.getBuyumeler().size() + "\n"
                        + "Vitamin/ilaç kaydı: " + depo.getIlaclar().size() + "\n"
                        + "Hatırlatıcı: " + depo.getHatirlaticilar().size() + "\n\n"
                        + "Veri klasörü:\n" + depo.getDosya().getParent() + "\n\n"
                        + "Java " + System.getProperty("java.version"),
                "Hakkında", JOptionPane.INFORMATION_MESSAGE);
    }

    private void bilgiGoster(String mesaj) {
        JOptionPane.showMessageDialog(this, mesaj, "Bilgi",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void hataGoster(String mesaj) {
        JOptionPane.showMessageDialog(this, mesaj == null ? "Bilinmeyen hata" : mesaj,
                "Hata", JOptionPane.ERROR_MESSAGE);
    }

    private void tarihiGuncelle() {
        tarihEtiketi.setText(Tema.uzunTarih(LocalDate.now()));
    }

    /** Gece yarısı geçilince "bugün" ölçümlerini tazeler. */
    private void gunuKontrolEt() {
        LocalDate simdi = LocalDate.now();
        if (!simdi.equals(gosterilenGun)) {
            gosterilenGun = simdi;
            tarihiGuncelle();
            ozetPanel.yenile();
            uykuPanel.yenile();
            istatistiklerPanel.yenile();
        }
    }

    private void kisayollariKur() {
        int ctrl = InputEvent.CTRL_DOWN_MASK;
        kisayol("yeniKayit", KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), e -> yeniKayit());
        kisayol("kaydet", KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrl), e -> {
            kaydetZamanlayici.stop();
            kaydetVeOzetiTazele();
        });
        kisayol("csv", KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrl), e -> csvDisaAktar());
        kisayol("ara", KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl), e -> {
            gorunumeGec(YanMenu.GORUNUM_KAYITLAR);
            kayitlarPanel.aramayaOdaklan();
        });
        kisayol("gorunum1", KeyStroke.getKeyStroke(KeyEvent.VK_1, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_OZET));
        kisayol("gorunum2", KeyStroke.getKeyStroke(KeyEvent.VK_2, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_KAYITLAR));
        kisayol("gorunum3", KeyStroke.getKeyStroke(KeyEvent.VK_3, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_UYKU));
        kisayol("gorunum4", KeyStroke.getKeyStroke(KeyEvent.VK_4, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_BUYUME));
        kisayol("gorunum5", KeyStroke.getKeyStroke(KeyEvent.VK_5, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_ILAC));
        kisayol("gorunum6", KeyStroke.getKeyStroke(KeyEvent.VK_6, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_HATIRLATICI));
        kisayol("gorunum7", KeyStroke.getKeyStroke(KeyEvent.VK_7, ctrl),
                e -> gorunumeGec(YanMenu.GORUNUM_ISTATISTIK));
    }

    private void kisayol(String ad, KeyStroke tus, java.awt.event.ActionListener eylem) {
        JComponent kok = (JComponent) getContentPane();
        kok.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tus, ad);
        kok.getActionMap().put(ad, new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                eylem.actionPerformed(e);
            }
        });
    }

    private void kapat() {
        kaydetZamanlayici.stop();
        saatZamanlayici.stop();
        if (!depo.kaydet()) {
            int cevap = JOptionPane.showConfirmDialog(this,
                    "Kayıtlar diske yazılamadı:\n" + depo.getSonHata()
                            + "\n\nYine de çıkılsın mı?",
                    "Kaydedilemedi", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (cevap != JOptionPane.YES_OPTION) {
                saatZamanlayici.start();
                return;
            }
        }
        if (tepsiSimgesi != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(tepsiSimgesi);
        }
        dispose();
        System.exit(0);
    }
}
