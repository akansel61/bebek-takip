package com.akansel.bebektakip.rapor;

import com.akansel.bebektakip.model.AtesKayit;
import com.akansel.bebektakip.model.IlacKayit;
import com.akansel.bebektakip.model.Kayit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Raporu Excel (.xlsx) dosyası olarak yazar.
 *
 * .xlsx aslında içinde XML dosyaları olan bir zip arşivi; Java'nın kendi
 * zip desteğiyle, ek kütüphane olmadan üretiliyor. Tarih ve saatler
 * Excel'in kendi sayı biçiminde yazılır, böylece sıralama, filtre ve
 * formüller doğrudan çalışır. Özet sayfasındaki toplamlar formüldür.
 */
public final class ExcelYazici {

    /** Excel'in tarih sayacının sıfır günü (1900 artık yıl hatası dahil). */
    private static final LocalDate EXCEL_SIFIR = LocalDate.of(1899, 12, 30);

    // styles.xml içindeki cellXfs sırası
    private static final int S_BASLIK = 1;
    private static final int S_ALT_BASLIK = 2;
    private static final int S_TABLO_BASLIGI = 3;
    private static final int S_TARIH = 4;
    private static final int S_SAAT = 5;
    private static final int S_DERECE = 6;
    private static final int S_KIRMIZI = 7;
    private static final int S_TURUNCU = 8;
    private static final int S_YESIL = 9;
    private static final int S_KALIN = 10;
    private static final int S_SARMALI = 11;
    private static final int S_KALIN_DERECE = 12;
    private static final int S_DEGER = 13;
    private static final int S_TABLO_BASLIGI_SAG = 14;

    private ExcelYazici() {
    }

    public static void yaz(Path hedef, RaporVerisi veri) throws IOException {
        try (OutputStream o = Files.newOutputStream(hedef)) {
            yaz(o, veri);
        }
    }

    public static byte[] uret(RaporVerisi veri) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        yaz(o, veri);
        return o.toByteArray();
    }

    private static void yaz(OutputStream cikti, RaporVerisi veri) throws IOException {
        List<CalismaSayfasi> sayfalar = new ArrayList<>();
        sayfalar.add(ozetSayfasi(veri));
        if (veri.bolumVar(RaporVerisi.Bolum.MAMA)) {
            sayfalar.add(mamaSayfasi(veri));
        }
        if (veri.bolumVar(RaporVerisi.Bolum.VITAMIN)) {
            sayfalar.add(ilacSayfasi(veri, true));
        }
        if (veri.bolumVar(RaporVerisi.Bolum.ILAC)) {
            sayfalar.add(ilacSayfasi(veri, false));
        }
        if (veri.bolumVar(RaporVerisi.Bolum.ATES)) {
            sayfalar.add(atesSayfasi(veri));
        }

        try (ZipOutputStream zip = new ZipOutputStream(cikti, StandardCharsets.UTF_8)) {
            girdi(zip, "[Content_Types].xml", icerikTurleri(sayfalar.size()));
            girdi(zip, "_rels/.rels", baglantilar(List.of(
                    BAG_BELGE + "/officeDocument", "xl/workbook.xml",
                    BAG_PAKET + "/metadata/core-properties", "docProps/core.xml",
                    BAG_BELGE + "/extended-properties", "docProps/app.xml")));
            girdi(zip, "docProps/core.xml", cekirdek(veri));
            girdi(zip, "docProps/app.xml", XML_BASI + "<Properties xmlns=\"" + OOXML
                    + "officeDocument/2006/extended-properties\">"
                    + "<Application>Bebek Takip</Application></Properties>");
            girdi(zip, "xl/workbook.xml", calismaKitabi(sayfalar));
            girdi(zip, "xl/_rels/workbook.xml.rels", kitapBaglantilari(sayfalar.size()));
            girdi(zip, "xl/styles.xml", stiller());
            for (int i = 0; i < sayfalar.size(); i++) {
                girdi(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sayfalar.get(i).xml());
            }
        }
    }

    private static void girdi(ZipOutputStream zip, String ad, String icerik) throws IOException {
        zip.putNextEntry(new ZipEntry(ad));
        zip.write(icerik.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ------------------------------------------------------------ sayfalar

    private static CalismaSayfasi ozetSayfasi(RaporVerisi veri) {
        CalismaSayfasi s = new CalismaSayfasi("Özet");
        Donem d = veri.getDonem();
        s.sutunGenislikleri(16, 14, 12, 12, 11, 11, 13, 14);
        s.satir().metin("Bebek Takip – " + d.raporAdi(), S_BASLIK);
        s.satir().metin(d.aralikMetni(), S_ALT_BASLIK);
        s.satir().metin("Hazırlanma: " + Bicim.tarihSaat(veri.getHazirlanma()), S_ALT_BASLIK);
        s.bosSatir();

        boolean mama = veri.bolumVar(RaporVerisi.Bolum.MAMA);
        boolean vitamin = veri.bolumVar(RaporVerisi.Bolum.VITAMIN);
        boolean ilac = veri.bolumVar(RaporVerisi.Bolum.ILAC);
        boolean ates = veri.bolumVar(RaporVerisi.Bolum.ATES);

        // Değerler C sütununda; B boş kalınca uzun etiket onun üstüne taşabiliyor.
        if (mama) {
            ozetSatiri(s, "Toplam mama (ml)").sayi(veri.toplamMama(), S_DEGER);
            ozetSatiri(s, "Mama öğün sayısı").sayi(veri.getMamalar().size(), S_DEGER);
            ozetSatiri(s, "Günlük ortalama (ml)")
                    .sayi(Math.round(veri.gunlukOrtalamaMama() * 10) / 10.0, S_DEGER);
        }
        if (vitamin) {
            ozetSatiri(s, "Vitamin verilme sayısı").sayi(veri.getVitaminler().size(), S_DEGER);
        }
        if (ilac) {
            ozetSatiri(s, "İlaç verilme sayısı").sayi(veri.getIlaclar().size(), S_DEGER);
        }
        if (ates) {
            AtesKayit en = veri.enYuksekAtes();
            ozetSatiri(s, "Ateş ölçüm sayısı").sayi(veri.getAtesler().size(), S_DEGER);
            Satir r = ozetSatiri(s, "En yüksek ateş (°C)");
            if (en != null) {
                r.sayi(en.getDerece(), S_KALIN_DERECE).metin(Bicim.tarihSaat(en.zaman()), 0);
            } else {
                r.metin("—", S_DEGER);
            }
            ozetSatiri(s, "Ateşli ölçüm (38 °C ve üstü)").sayi(veri.atesliOlcumSayisi(), S_DEGER);
        }
        if (!mama && !vitamin && !ilac && !ates) {
            s.satir().metin("Rapora eklenecek bölüm seçilmedi.", 0);
            return s;
        }
        s.bosSatir();

        Satir baslik = s.satir();
        baslik.metin("Tarih", S_TABLO_BASLIGI).metin("Gün", S_TABLO_BASLIGI);
        int sutun = 2;
        if (mama) {
            baslik.metin("Mama öğün", S_TABLO_BASLIGI_SAG).metin("Mama (ml)", S_TABLO_BASLIGI_SAG);
            sutun += 2;
        }
        if (vitamin) {
            baslik.metin("Vitamin", S_TABLO_BASLIGI_SAG);
            sutun++;
        }
        if (ilac) {
            baslik.metin("İlaç", S_TABLO_BASLIGI_SAG);
            sutun++;
        }
        if (ates) {
            baslik.metin("Ateş ölçümü", S_TABLO_BASLIGI_SAG)
                    .metin("En yüksek (°C)", S_TABLO_BASLIGI_SAG);
            sutun += 2;
        }
        int ilkVeri = s.satirSayisi() + 1;
        for (RaporVerisi.GunOzeti g : veri.getGunler()) {
            Satir r = s.satir().tarih(g.tarih).metin(Bicim.gunAdi(g.tarih), 0);
            if (mama) {
                r.sayi(g.mamaOgun, 0).sayi(g.mamaMiktar, 0);
            }
            if (vitamin) {
                r.sayi(g.vitamin, 0);
            }
            if (ilac) {
                r.sayi(g.ilac, 0);
            }
            if (ates) {
                r.sayi(g.atesOlcum, 0);
                if (g.enYuksekAtes > 0) {
                    r.sayi(g.enYuksekAtes, dereceStili(g.enYuksekAtes));
                } else {
                    r.bos();
                }
            }
        }
        int sonVeri = s.satirSayisi();
        if (sonVeri >= ilkVeri) {
            Satir toplam = s.satir().metin("Toplam", S_KALIN).metin("", S_KALIN);
            for (int c = 2; c < sutun; c++) {
                String harf = sutunHarfi(c);
                boolean enYuksek = ates && c == sutun - 1;
                String formul = (enYuksek ? "MAX(" : "SUM(") + harf + ilkVeri + ":" + harf
                        + sonVeri + ")";
                toplam.formul(formul, enYuksek ? S_KALIN_DERECE : S_KALIN);
            }
            s.filtre(baslik.no, 0, sonVeri, sutun - 1);
        }
        return s;
    }

    private static Satir ozetSatiri(CalismaSayfasi s, String etiket) {
        return s.satir().metin(etiket, S_KALIN).bos();
    }

    private static CalismaSayfasi mamaSayfasi(RaporVerisi veri) {
        CalismaSayfasi s = new CalismaSayfasi("Mama");
        s.sutunGenislikleri(13, 8, 12, 24, 40);
        Satir b = s.satir().metin("Tarih", S_TABLO_BASLIGI).metin("Saat", S_TABLO_BASLIGI)
                .metin("Miktar (ml)", S_TABLO_BASLIGI_SAG).metin("Mama notu", S_TABLO_BASLIGI)
                .metin("Not", S_TABLO_BASLIGI);
        s.dondur(b.no);
        for (Kayit k : veri.getMamalar()) {
            Satir r = s.satir().tarih(k.getTarih()).saat(k.getSaat());
            if (k.mamaMiktari() > 0) {
                r.sayi(k.mamaMiktari(), 0);
            } else {
                r.bos();
            }
            r.metin(k.getMamaNotu(), 0).metin(k.getNot(), S_SARMALI);
        }
        s.filtre(b.no, 0, s.satirSayisi(), 4);
        return s;
    }

    private static CalismaSayfasi ilacSayfasi(RaporVerisi veri, boolean vitaminMi) {
        CalismaSayfasi s = new CalismaSayfasi(vitaminMi ? "Vitamin" : "İlaç");
        s.sutunGenislikleri(13, 8, 26, 16, 40);
        Satir b = s.satir().metin("Tarih", S_TABLO_BASLIGI).metin("Saat", S_TABLO_BASLIGI)
                .metin(vitaminMi ? "Vitamin" : "İlaç", S_TABLO_BASLIGI)
                .metin("Doz", S_TABLO_BASLIGI).metin("Not", S_TABLO_BASLIGI);
        s.dondur(b.no);
        for (IlacKayit i : vitaminMi ? veri.getVitaminler() : veri.getIlaclar()) {
            s.satir().tarih(i.getTarih()).saat(i.getSaat()).metin(i.getAd(), 0)
                    .metin(i.getDoz(), 0).metin(i.getNot(), S_SARMALI);
        }
        s.filtre(b.no, 0, s.satirSayisi(), 4);
        return s;
    }

    private static CalismaSayfasi atesSayfasi(RaporVerisi veri) {
        CalismaSayfasi s = new CalismaSayfasi("Ateş");
        s.sutunGenislikleri(13, 8, 12, 15, 13, 36, 32);
        Satir b = s.satir().metin("Tarih", S_TABLO_BASLIGI).metin("Saat", S_TABLO_BASLIGI)
                .metin("Derece (°C)", S_TABLO_BASLIGI_SAG).metin("Durum", S_TABLO_BASLIGI)
                .metin("Ölçüm yeri", S_TABLO_BASLIGI).metin("Belirtiler", S_TABLO_BASLIGI)
                .metin("Not", S_TABLO_BASLIGI);
        s.dondur(b.no);
        for (AtesKayit a : veri.getAtesler()) {
            s.satir().tarih(a.getTarih()).saat(a.getSaat()).sayi(a.getDerece(), S_DERECE)
                    .metin(a.seviye().getAd(), durumStili(a.seviye()))
                    .metin(a.getOlcumYeri(), 0).metin(a.belirtiMetni(), S_SARMALI)
                    .metin(a.getNot(), S_SARMALI);
        }
        s.filtre(b.no, 0, s.satirSayisi(), 6);
        return s;
    }

    private static int durumStili(AtesKayit.Seviye seviye) {
        switch (seviye) {
            case YUKSEK:
            case ATES:
                return S_KIRMIZI;
            case HAFIF:
                return S_TURUNCU;
            case NORMAL:
                return S_YESIL;
            default:
                return 0;
        }
    }

    private static int dereceStili(double derece) {
        return AtesKayit.seviye(derece).atesli() ? S_KALIN_DERECE : S_DERECE;
    }

    // ------------------------------------------------------------ XML

    private static final String XML_BASI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    private static final String OOXML = "http://schemas.openxmlformats.org/";
    private static final String NS_TABLO = OOXML + "spreadsheetml/2006/main";
    private static final String BAG_PAKET = OOXML + "package/2006/relationships";
    private static final String BAG_BELGE = OOXML + "officeDocument/2006/relationships";
    private static final String TUR_PAKET = "application/vnd.openxmlformats-package.";
    private static final String TUR_OFIS = "application/vnd.openxmlformats-officedocument.";

    private static String stiller() {
        StringBuilder sb = new StringBuilder(XML_BASI);
        sb.append("<styleSheet xmlns=\"").append(NS_TABLO).append("\">");
        sb.append("<numFmts count=\"3\">")
                .append("<numFmt numFmtId=\"164\" formatCode=\"dd\\.mm\\.yyyy\"/>")
                .append("<numFmt numFmtId=\"165\" formatCode=\"hh:mm\"/>")
                .append("<numFmt numFmtId=\"166\" formatCode=\"0.0\"/>")
                .append("</numFmts>");
        // 0 düz, 1 kalın, 2 büyük başlık, 3 soluk, 4 kırmızı kalın
        sb.append("<fonts count=\"5\">")
                .append(yaziTipi(false, 11, null))
                .append(yaziTipi(true, 11, null))
                .append(yaziTipi(true, 15, null))
                .append(yaziTipi(false, 11, "FF6E6E73"))
                .append(yaziTipi(true, 11, "FFD70015"))
                .append("</fonts>");
        // İlk iki dolgu Excel'in kendi ayırdıkları; olmazsa dosya onarılmak istenir.
        sb.append("<fills count=\"6\">")
                .append("<fill><patternFill patternType=\"none\"/></fill>")
                .append("<fill><patternFill patternType=\"gray125\"/></fill>")
                .append(dolgu("FFF0F0F3"))
                .append(dolgu("FFFFE5E3"))
                .append(dolgu("FFFFF0D9"))
                .append(dolgu("FFE3F6E8"))
                .append("</fills>");
        sb.append("<borders count=\"2\">")
                .append("<border><left/><right/><top/><bottom/><diagonal/></border>")
                .append("<border><left/><right/><top/><bottom style=\"thin\">")
                .append("<color rgb=\"FFC7C7CC\"/></bottom><diagonal/></border>")
                .append("</borders>");
        sb.append("<cellStyleXfs count=\"1\">")
                .append("<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>")
                .append("</cellStyleXfs>");

        // Sıra, sınıfın başındaki S_ sabitleriyle birebir aynı olmalı.
        String[] bicimler = {
                hucreBicimi(0, 0, 0, 0, null),
                hucreBicimi(0, 2, 0, 0, null),
                hucreBicimi(0, 3, 0, 0, null),
                hucreBicimi(0, 1, 2, 1, null),
                hucreBicimi(164, 0, 0, 0, "horizontal=\"left\""),
                hucreBicimi(165, 0, 0, 0, "horizontal=\"left\""),
                hucreBicimi(166, 0, 0, 0, null),
                hucreBicimi(0, 1, 3, 0, null),
                hucreBicimi(0, 0, 4, 0, null),
                hucreBicimi(0, 0, 5, 0, null),
                hucreBicimi(0, 1, 0, 0, null),
                hucreBicimi(0, 0, 0, 0, "vertical=\"top\" wrapText=\"1\""),
                hucreBicimi(166, 4, 0, 0, null),
                hucreBicimi(0, 0, 0, 0, "horizontal=\"right\""),
                hucreBicimi(0, 1, 2, 1, "horizontal=\"right\""),
        };
        sb.append("<cellXfs count=\"").append(bicimler.length).append("\">");
        for (String b : bicimler) {
            sb.append(b);
        }
        sb.append("</cellXfs>")
                .append("<cellStyles count=\"1\">")
                .append("<cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/>")
                .append("</cellStyles></styleSheet>");
        return sb.toString();
    }

    private static String yaziTipi(boolean kalin, int boyut, String renk) {
        return "<font>" + (kalin ? "<b/>" : "") + "<sz val=\"" + boyut + "\"/>"
                + (renk == null ? "" : "<color rgb=\"" + renk + "\"/>")
                + "<name val=\"Calibri\"/><family val=\"2\"/></font>";
    }

    private static String dolgu(String renk) {
        return "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"" + renk
                + "\"/><bgColor indexed=\"64\"/></patternFill></fill>";
    }

    private static String hucreBicimi(int sayiBicimi, int yazi, int dolgu, int kenar,
                                      String hizalama) {
        StringBuilder sb = new StringBuilder("<xf numFmtId=\"").append(sayiBicimi)
                .append("\" fontId=\"").append(yazi)
                .append("\" fillId=\"").append(dolgu)
                .append("\" borderId=\"").append(kenar).append("\" xfId=\"0\"");
        if (sayiBicimi > 0) {
            sb.append(" applyNumberFormat=\"1\"");
        }
        if (yazi > 0) {
            sb.append(" applyFont=\"1\"");
        }
        if (dolgu > 0) {
            sb.append(" applyFill=\"1\"");
        }
        if (kenar > 0) {
            sb.append(" applyBorder=\"1\"");
        }
        if (hizalama == null) {
            return sb.append("/>").toString();
        }
        return sb.append(" applyAlignment=\"1\"><alignment ").append(hizalama)
                .append("/></xf>").toString();
    }

    private static String icerikTurleri(int sayfaSayisi) {
        StringBuilder sb = new StringBuilder(XML_BASI);
        sb.append("<Types xmlns=\"").append(OOXML).append("package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"")
                .append(TUR_PAKET).append("relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        parca(sb, "/xl/workbook.xml", TUR_OFIS + "spreadsheetml.sheet.main+xml");
        parca(sb, "/xl/styles.xml", TUR_OFIS + "spreadsheetml.styles+xml");
        parca(sb, "/docProps/core.xml", TUR_PAKET + "core-properties+xml");
        parca(sb, "/docProps/app.xml", TUR_OFIS + "extended-properties+xml");
        for (int i = 1; i <= sayfaSayisi; i++) {
            parca(sb, "/xl/worksheets/sheet" + i + ".xml",
                    TUR_OFIS + "spreadsheetml.worksheet+xml");
        }
        return sb.append("</Types>").toString();
    }

    private static void parca(StringBuilder sb, String ad, String tur) {
        sb.append("<Override PartName=\"").append(ad).append("\" ContentType=\"").append(tur)
                .append("\"/>");
    }

    /** Tür/hedef çiftlerinden sırayla rId1, rId2… numaralı bağlantı listesi. */
    private static String baglantilar(List<String> turVeHedef) {
        StringBuilder sb = new StringBuilder(XML_BASI);
        sb.append("<Relationships xmlns=\"").append(BAG_PAKET).append("\">");
        for (int i = 0; i + 1 < turVeHedef.size(); i += 2) {
            sb.append("<Relationship Id=\"rId").append(i / 2 + 1)
                    .append("\" Type=\"").append(turVeHedef.get(i))
                    .append("\" Target=\"").append(turVeHedef.get(i + 1)).append("\"/>");
        }
        return sb.append("</Relationships>").toString();
    }

    private static String cekirdek(RaporVerisi veri) {
        String zaman = veri.getHazirlanma().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String baslik = "Bebek Takip – " + veri.getDonem().raporAdi() + " ("
                + veri.getDonem().aralikMetni() + ")";
        return XML_BASI
                + "<cp:coreProperties xmlns:cp=\"" + OOXML
                + "package/2006/metadata/core-properties\""
                + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
                + " xmlns:dcterms=\"http://purl.org/dc/terms/\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:title>" + kacir(baslik) + "</dc:title>"
                + "<dc:creator>Bebek Takip</dc:creator>"
                + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + zaman + "Z</dcterms:created>"
                + "</cp:coreProperties>";
    }

    private static String calismaKitabi(List<CalismaSayfasi> sayfalar) {
        StringBuilder sb = new StringBuilder(XML_BASI);
        sb.append("<workbook xmlns=\"").append(NS_TABLO).append("\" xmlns:r=\"")
                .append(BAG_BELGE).append("\">")
                .append("<bookViews><workbookView/></bookViews><sheets>");
        for (int i = 0; i < sayfalar.size(); i++) {
            sb.append("<sheet name=\"").append(kacir(sayfalar.get(i).ad)).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        sb.append("</sheets>");
        // Excel filtreyi bu gizli adla izler; eksik olursa dosyayı açarken ekler.
        StringBuilder adlar = new StringBuilder();
        for (int i = 0; i < sayfalar.size(); i++) {
            String alan = sayfalar.get(i).filtreAlani;
            if (alan != null) {
                adlar.append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"")
                        .append(i).append("\" hidden=\"1\">'")
                        .append(kacir(sayfalar.get(i).ad)).append("'!")
                        .append(mutlak(alan)).append("</definedName>");
            }
        }
        if (adlar.length() > 0) {
            sb.append("<definedNames>").append(adlar).append("</definedNames>");
        }
        sb.append("<calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/></workbook>");
        return sb.toString();
    }

    private static String kitapBaglantilari(int sayfaSayisi) {
        List<String> liste = new ArrayList<>();
        for (int i = 1; i <= sayfaSayisi; i++) {
            liste.add(BAG_BELGE + "/worksheet");
            liste.add("worksheets/sheet" + i + ".xml");
        }
        liste.add(BAG_BELGE + "/styles");
        liste.add("styles.xml");
        return baglantilar(liste);
    }

    /** A1 → $A$1; "A3:E9" → "$A$3:$E$9". */
    private static String mutlak(String alan) {
        StringBuilder sb = new StringBuilder();
        for (String parca : alan.split(":")) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            int i = 0;
            while (i < parca.length() && Character.isLetter(parca.charAt(i))) {
                i++;
            }
            sb.append('$').append(parca, 0, i).append('$').append(parca.substring(i));
        }
        return sb.toString();
    }

    static String sutunHarfi(int sutun) {
        StringBuilder sb = new StringBuilder();
        int n = sutun + 1;
        while (n > 0) {
            int kalan = (n - 1) % 26;
            sb.insert(0, (char) ('A' + kalan));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    /** XML'de yasak karakterleri atar, özel karakterleri kaçırır. */
    static String kacir(String metin) {
        StringBuilder sb = new StringBuilder(metin.length() + 8);
        for (int i = 0; i < metin.length(); i++) {
            char c = metin.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String sayiMetni(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-9 && Math.abs(d) < 1e15) {
            return String.valueOf((long) Math.rint(d));
        }
        return String.format(Locale.ROOT, "%.10f", d).replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    /** Tek çalışma sayfası; satırlar sırayla eklenir. */
    private static final class CalismaSayfasi {
        final String ad;
        private final List<Satir> satirlar = new ArrayList<>();
        private double[] genislikler = new double[0];
        private int dondurulenSatir;
        String filtreAlani;

        CalismaSayfasi(String ad) {
            this.ad = ad;
        }

        void sutunGenislikleri(double... g) {
            genislikler = g;
        }

        Satir satir() {
            Satir s = new Satir(satirlar.size() + 1);
            satirlar.add(s);
            return s;
        }

        void bosSatir() {
            satir();
        }

        int satirSayisi() {
            return satirlar.size();
        }

        /** Verilen satıra kadar olan kısım kaydırınca sabit kalır. */
        void dondur(int satirNo) {
            dondurulenSatir = satirNo;
        }

        void filtre(int ustSatir, int ilkSutun, int sonSatir, int sonSutun) {
            filtreAlani = sutunHarfi(ilkSutun) + ustSatir + ":" + sutunHarfi(sonSutun)
                    + Math.max(ustSatir, sonSatir);
        }

        String xml() {
            StringBuilder sb = new StringBuilder(XML_BASI);
            sb.append("<worksheet xmlns=\"").append(NS_TABLO).append("\" xmlns:r=\"")
                    .append(BAG_BELGE).append("\">");
            sb.append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
            sb.append("<sheetViews><sheetView workbookViewId=\"0\"");
            if (dondurulenSatir > 0) {
                sb.append("><pane ySplit=\"").append(dondurulenSatir).append("\" topLeftCell=\"A")
                        .append(dondurulenSatir + 1)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                        .append("<selection pane=\"bottomLeft\" activeCell=\"A")
                        .append(dondurulenSatir + 1).append("\" sqref=\"A")
                        .append(dondurulenSatir + 1).append("\"/></sheetView>");
            } else {
                sb.append("/>");
            }
            sb.append("</sheetViews><sheetFormatPr defaultRowHeight=\"15\"/>");
            if (genislikler.length > 0) {
                sb.append("<cols>");
                for (int i = 0; i < genislikler.length; i++) {
                    sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                            .append("\" width=\"").append(sayiMetni(genislikler[i]))
                            .append("\" customWidth=\"1\"/>");
                }
                sb.append("</cols>");
            }
            sb.append("<sheetData>");
            for (Satir s : satirlar) {
                s.xml(sb);
            }
            sb.append("</sheetData>");
            if (filtreAlani != null) {
                sb.append("<autoFilter ref=\"").append(filtreAlani).append("\"/>");
            }
            sb.append("<pageMargins left=\"0.6\" right=\"0.6\" top=\"0.7\" bottom=\"0.7\"")
                    .append(" header=\"0.3\" footer=\"0.3\"/>");
            // A4, sütunlar tek sayfa genişliğine sığdırılır
            sb.append("<pageSetup paperSize=\"9\" orientation=\"portrait\"")
                    .append(" fitToWidth=\"1\" fitToHeight=\"0\"/>");
            sb.append("</worksheet>");
            return sb.toString();
        }
    }

    /** Tek satır; hücreler soldan sağa eklenir. */
    private static final class Satir {
        final int no;
        private final StringBuilder hucreler = new StringBuilder();
        private int sutun;

        Satir(int no) {
            this.no = no;
        }

        private String adres() {
            return sutunHarfi(sutun++) + no;
        }

        Satir metin(String metin, int stil) {
            String adres = adres();
            String t = metin == null ? "" : metin;
            hucreler.append("<c r=\"").append(adres).append('"');
            if (stil > 0) {
                hucreler.append(" s=\"").append(stil).append('"');
            }
            if (t.isEmpty()) {
                hucreler.append("/>");
            } else {
                hucreler.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(kacir(t)).append("</t></is></c>");
            }
            return this;
        }

        Satir sayi(double d, int stil) {
            hucreler.append("<c r=\"").append(adres()).append('"');
            if (stil > 0) {
                hucreler.append(" s=\"").append(stil).append('"');
            }
            hucreler.append("><v>").append(sayiMetni(d)).append("</v></c>");
            return this;
        }

        Satir tarih(LocalDate d) {
            return sayi(ChronoUnit.DAYS.between(EXCEL_SIFIR, d), S_TARIH);
        }

        Satir saat(LocalTime t) {
            return sayi(t.toSecondOfDay() / 86400.0, S_SAAT);
        }

        Satir formul(String formul, int stil) {
            hucreler.append("<c r=\"").append(adres()).append('"');
            if (stil > 0) {
                hucreler.append(" s=\"").append(stil).append('"');
            }
            hucreler.append("><f>").append(kacir(formul)).append("</f></c>");
            return this;
        }

        Satir bos() {
            sutun++;
            return this;
        }

        void xml(StringBuilder sb) {
            sb.append("<row r=\"").append(no).append('"');
            if (hucreler.length() == 0) {
                sb.append("/>");
                return;
            }
            sb.append('>').append(hucreler).append("</row>");
        }
    }
}
