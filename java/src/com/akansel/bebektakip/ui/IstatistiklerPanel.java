package com.akansel.bebektakip.ui;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.model.UykuKayit;
import com.akansel.bebektakip.ui.bilesen.CubukGrafik;
import com.akansel.bebektakip.ui.bilesen.Kart;
import com.akansel.bebektakip.ui.bilesen.KartIzgara;
import com.akansel.bebektakip.ui.bilesen.KaydirilabilirPanel;
import com.akansel.bebektakip.ui.bilesen.OlcumListesi;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Haftalık özet ve genel toplamlar. */
public class IstatistiklerPanel extends KaydirilabilirPanel {

    private static final long serialVersionUID = 1L;

    private static final int HAFTA_GUNU = 7;

    /** Günlük mama listesinde gösterilecek en fazla gün sayısı. */
    private static final int GUNLUK_MAMA_SINIRI = 30;

    private final KayitDeposu depo;
    private final CubukGrafik haftalik = new CubukGrafik();
    private final CubukGrafik haftalikMama = new CubukGrafik().cubukRengi(Tema.TURUNCU);
    private final CubukGrafik haftalikUyku = new CubukGrafik().cubukRengi(Tema.MOR);
    private final OlcumListesi genel = new OlcumListesi();
    private final OlcumListesi gunlukMama = new OlcumListesi();

    public IstatistiklerPanel(KayitDeposu depo) {
        super(new BorderLayout());
        this.depo = depo;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        haftalik.setBosMesaj("Son 7 günde kayıt yok");
        haftalikMama.setBosMesaj("Son 7 günde mama kaydı yok");
        haftalikUyku.setBosMesaj("Son 7 günde uyku kaydı yok");

        // Sol sütunda üç grafik alt alta, sağ sütunda genel toplamlar.
        JPanel altGrafikler = new JPanel(new BorderLayout(0, 16));
        altGrafikler.setOpaque(false);
        altGrafikler.add(kartYap("Haftalık Mama", "Son 7 gündeki toplam mama (ml/gr)",
                haftalikMama), BorderLayout.NORTH);
        altGrafikler.add(usteYasla(kartYap("Haftalık Uyku", "Son 7 gündeki toplam uyku",
                haftalikUyku)), BorderLayout.CENTER);

        JPanel sol = new JPanel(new BorderLayout(0, 16));
        sol.setOpaque(false);
        sol.add(kartYap("Haftalık Özet", "Son 7 gündeki kayıt sayısı", haftalik),
                BorderLayout.NORTH);
        sol.add(altGrafikler, BorderLayout.CENTER);

        KartIzgara izgara = new KartIzgara(320, 16);
        izgara.add(sol);
        izgara.add(usteYasla(kartYap("Genel İstatistikler", null, genel)));
        izgara.add(usteYasla(kartYap("Günlük Mama",
                "Gün gün toplam mama (ml/gr, son " + GUNLUK_MAMA_SINIRI + " gün)",
                gunlukMama)));
        add(izgara, BorderLayout.NORTH);
    }

    /** Kartı bulunduğu alanın üstüne yaslar, dikeyde gerilmesini önler. */
    private JPanel usteYasla(JComponent icerik) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(icerik, BorderLayout.NORTH);
        return p;
    }

    private Kart kartYap(String baslik, String altBaslik, JComponent icerik) {
        Kart kart = new Kart(new BorderLayout(0, 14));
        kart.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel ust = new JPanel(new BorderLayout(0, 2));
        ust.setOpaque(false);
        JLabel etiket = new JLabel(baslik);
        etiket.setFont(Tema.fontKalin(15));
        etiket.setForeground(Tema.METIN);
        ust.add(etiket, BorderLayout.NORTH);
        if (altBaslik != null) {
            JLabel alt = new JLabel(altBaslik);
            alt.setFont(Tema.font(12));
            alt.setForeground(Tema.METIN_SOLUK);
            ust.add(alt, BorderLayout.CENTER);
        }
        kart.add(ust, BorderLayout.NORTH);
        kart.add(icerik, BorderLayout.CENTER);
        return kart;
    }

    /** Tüm istatistikleri yeniden hesaplar. */
    public void yenile() {
        List<Kayit> tumu = depo.getKayitlar();
        LocalDate bugun = LocalDate.now();

        List<CubukGrafik.Satir> gunler = new ArrayList<>();
        List<CubukGrafik.Satir> mamalar = new ArrayList<>();
        List<CubukGrafik.Satir> uykular = new ArrayList<>();
        boolean mamaVar = false;
        boolean uykuVar = false;
        for (int i = HAFTA_GUNU - 1; i >= 0; i--) {
            LocalDate gun = bugun.minusDays(i);
            List<Kayit> gunun = depo.gununKayitlari(gun);
            double mama = KayitDeposu.mamaToplami(gunun);
            long uyku = depo.gununUykuSuresi(gun);
            mamaVar |= mama > 0;
            uykuVar |= uyku > 0;
            boolean bugunMu = i == 0;
            String etiket = bugunMu ? "Bugün" : Tema.gunAdi(gun);
            gunler.add(new CubukGrafik.Satir(etiket, gunun.size(),
                    gunun.size() + " kayıt", bugunMu));
            mamalar.add(new CubukGrafik.Satir(etiket, mama,
                    Tema.sayi(mama), bugunMu));
            uykular.add(new CubukGrafik.Satir(etiket, uyku,
                    uyku > 0 ? Tema.sure(uyku) : "0", bugunMu));
        }
        haftalik.setSatirlar(gunler);
        haftalikMama.setSatirlar(mamaVar ? mamalar : new ArrayList<>());
        haftalikUyku.setSatirlar(uykuVar ? uykular : new ArrayList<>());

        Set<LocalDate> gunKumesi = new HashSet<>();
        for (Kayit k : tumu) {
            gunKumesi.add(k.getTarih());
        }
        int gunSayisi = gunKumesi.size();
        double toplamMama = KayitDeposu.mamaToplami(tumu);

        long toplamUyku = 0;
        for (UykuKayit u : depo.getUykular()) {
            toplamUyku += u.sureDakika();
        }

        List<OlcumListesi.Olcum> olcumler = new ArrayList<>();
        olcumler.add(new OlcumListesi.Olcum("Toplam kayıt",
                String.valueOf(tumu.size()), true));
        olcumler.add(new OlcumListesi.Olcum("Toplam çiş",
                String.valueOf(KayitDeposu.say(tumu, Kayit::isCis))));
        olcumler.add(new OlcumListesi.Olcum("Toplam kaka",
                String.valueOf(KayitDeposu.say(tumu, Kayit::isKaka))));
        olcumler.add(new OlcumListesi.Olcum("Toplam mama",
                String.valueOf(KayitDeposu.say(tumu, Kayit::isMama))));
        olcumler.add(new OlcumListesi.Olcum("Toplam emzirme",
                String.valueOf(KayitDeposu.say(tumu, Kayit::isEmzirme))));
        olcumler.add(new OlcumListesi.Olcum("Toplam mama miktarı",
                Tema.sayi(toplamMama) + " ml/gr"));
        olcumler.add(new OlcumListesi.Olcum("Uyku kaydı",
                String.valueOf(depo.getUykular().size())));
        olcumler.add(new OlcumListesi.Olcum("Toplam uyku",
                toplamUyku > 0 ? Tema.sure(toplamUyku) : "0"));
        olcumler.add(new OlcumListesi.Olcum("Büyüme ölçümü",
                String.valueOf(depo.getBuyumeler().size())));
        olcumler.add(new OlcumListesi.Olcum("Vitamin / ilaç kaydı",
                String.valueOf(depo.getIlaclar().size())));
        olcumler.add(new OlcumListesi.Olcum("Bekleyen hatırlatıcı",
                String.valueOf(depo.bekleyenHatirlatici())));
        olcumler.add(new OlcumListesi.Olcum("Kayıtlı gün sayısı",
                String.valueOf(gunSayisi)));
        olcumler.add(new OlcumListesi.Olcum("Günlük ortalama kayıt",
                gunSayisi == 0 ? "0" : Tema.sayi(Math.round(tumu.size() * 10.0 / gunSayisi) / 10.0)));
        genel.setOlcumler(olcumler);

        // gün gün toplam mama; en yeni gün en üstte
        Map<LocalDate, Double> gunlukToplam = new TreeMap<>(Comparator.reverseOrder());
        for (Kayit k : tumu) {
            double miktar = k.mamaMiktari();
            if (miktar > 0) {
                gunlukToplam.merge(k.getTarih(), miktar, Double::sum);
            }
        }
        List<OlcumListesi.Olcum> mamaGunleri = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> e : gunlukToplam.entrySet()) {
            if (mamaGunleri.size() >= GUNLUK_MAMA_SINIRI) {
                break;
            }
            boolean bugunMu = e.getKey().equals(bugun);
            String etiket = bugunMu
                    ? "Bugün · " + Tema.tamTarih(e.getKey())
                    : Tema.gunAdi(e.getKey()) + " · " + Tema.tamTarih(e.getKey());
            mamaGunleri.add(new OlcumListesi.Olcum(etiket,
                    Tema.sayi(e.getValue()), bugunMu));
        }
        if (mamaGunleri.isEmpty()) {
            mamaGunleri.add(new OlcumListesi.Olcum("Henüz mama kaydı yok", "—"));
        }
        gunlukMama.setOlcumler(mamaGunleri);

        revalidate();
        repaint();
    }
}
