package com.akansel.bebektakip.rapor;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;

/**
 * Sayfa listesini PDF dosyasına yazar.
 *
 * Türkçe harflerin her bilgisayarda doğru görünmesi için yazı tipi PDF'in
 * içine gömülür: yalnızca kullanılan glifler alınır (CIDFontType2,
 * Identity-H). ToUnicode tablosu sayesinde PDF'teki metin seçilip
 * kopyalanabilir ve aranabilir.
 */
public final class PdfYazici {

    private static final DateTimeFormatter PDF_TARIHI =
            DateTimeFormatter.ofPattern("'D:'yyyyMMddHHmmss");

    private PdfYazici() {
    }

    /** Tek bir gömülü yazı tipinin bu belgede kullanılan glifleri. */
    private static final class Kullanim {
        final TrueTypeYazi tt;
        final String kaynakAdi;
        final Map<Integer, Integer> glifKarakter = new TreeMap<>();

        Kullanim(TrueTypeYazi tt, String kaynakAdi) {
            this.tt = tt;
            this.kaynakAdi = kaynakAdi;
        }

        /** Metni glif numaralarının onaltılık dizisine çevirir. */
        String kodla(String metin) {
            StringBuilder sb = new StringBuilder(metin.length() * 4 + 2);
            sb.append('<');
            for (int i = 0; i < metin.length(); ) {
                int kod = metin.codePointAt(i);
                i += Character.charCount(kod);
                int g = tt.glif(kod);
                glifKarakter.putIfAbsent(g, kod);
                sb.append(String.format("%04X", g));
            }
            return sb.append('>').toString();
        }
    }

    public static void yaz(Path hedef, List<Sayfa> sayfalar, RaporYazisi yazi, String baslik)
            throws IOException {
        Files.write(hedef, uret(sayfalar, yazi, baslik));
    }

    public static byte[] uret(List<Sayfa> sayfalar, RaporYazisi yazi, String baslik)
            throws IOException {
        if (!yazi.pdfYazilabilir()) {
            throw new IOException("PDF için gömülebilir yazı tipi bulunamadı");
        }
        Kullanim duz = new Kullanim(yazi.dosya(false), "F1");
        Kullanim kalin = new Kullanim(yazi.dosya(true), "F2");

        // Yazı tiplerinden önce sayfalar kodlanır: hangi gliflerin
        // gömüleceği ancak bütün metinler görüldükten sonra belli olur.
        List<byte[]> icerikler = new ArrayList<>();
        for (Sayfa s : sayfalar) {
            icerikler.add(icerikUret(s, duz, kalin).getBytes(StandardCharsets.ISO_8859_1));
        }

        Nesneler n = new Nesneler();
        int katalog = n.ayir();
        int sayfaAgaci = n.ayir();
        int bilgi = n.ayir();
        int duzNo = yaziTipiYaz(n, duz, false);
        int kalinNo = yaziTipiYaz(n, kalin, true);

        StringBuilder cocuklar = new StringBuilder();
        String kaynaklar = "<< /Font << /F1 " + duzNo + " 0 R /F2 " + kalinNo
                + " 0 R >> /ProcSet [/PDF /Text] >>";
        for (byte[] icerik : icerikler) {
            int sayfaNo = n.ayir();
            int icerikNo = n.ayir();
            n.akis(icerikNo, "", icerik, true);
            n.koy(sayfaNo, "<< /Type /Page /Parent " + sayfaAgaci + " 0 R /MediaBox [0 0 "
                    + s(Sayfa.GENISLIK) + " " + s(Sayfa.YUKSEKLIK) + "] /Resources "
                    + kaynaklar + " /Contents " + icerikNo + " 0 R >>");
            cocuklar.append(sayfaNo).append(" 0 R ");
        }
        n.koy(sayfaAgaci, "<< /Type /Pages /Kids [" + cocuklar.toString().trim()
                + "] /Count " + sayfalar.size() + " >>");
        n.koy(katalog, "<< /Type /Catalog /Pages " + sayfaAgaci + " 0 R >>");

        OffsetDateTime simdi = OffsetDateTime.now();
        String saatDilimi = simdi.getOffset().getTotalSeconds() == 0 ? "Z"
                : simdi.getOffset().getId().replace(":", "'") + "'";
        n.koy(bilgi, "<< /Title " + metin16(baslik) + " /Creator " + metin16("Bebek Takip")
                + " /Producer " + metin16("Bebek Takip")
                + " /CreationDate (" + simdi.format(PDF_TARIHI) + saatDilimi + ") >>");
        return n.bitir(katalog, bilgi, baslik);
    }

    /** Sayfanın çizim komutlarını PDF içerik akışına çevirir. */
    private static String icerikUret(Sayfa sayfa, Kullanim duz, Kullanim kalin) {
        StringBuilder sb = new StringBuilder();
        double h = Sayfa.YUKSEKLIK;
        for (Sayfa.Oge o : sayfa.getOgeler()) {
            if (o instanceof Sayfa.Kutu) {
                Sayfa.Kutu k = (Sayfa.Kutu) o;
                sb.append(renk(k.renk)).append(" rg\n");
                if (k.yaricap > 0) {
                    yuvarlakKutu(sb, k.x, h - k.y - k.yukseklik, k.genislik, k.yukseklik,
                            Math.min(k.yaricap, Math.min(k.genislik, k.yukseklik) / 2));
                } else {
                    sb.append(s(k.x)).append(' ').append(s(h - k.y - k.yukseklik)).append(' ')
                            .append(s(k.genislik)).append(' ').append(s(k.yukseklik))
                            .append(" re f\n");
                }
            } else if (o instanceof Sayfa.Cizgi) {
                Sayfa.Cizgi c = (Sayfa.Cizgi) o;
                sb.append(renk(c.renk)).append(" RG ").append(s(c.kalinlik)).append(" w ")
                        .append(s(c.x1)).append(' ').append(s(h - c.y1)).append(" m ")
                        .append(s(c.x2)).append(' ').append(s(h - c.y2)).append(" l S\n");
            } else if (o instanceof Sayfa.Yazi) {
                Sayfa.Yazi y = (Sayfa.Yazi) o;
                Kullanim k = y.kalin ? kalin : duz;
                sb.append(renk(y.renk)).append(" rg BT /").append(k.kaynakAdi).append(' ')
                        .append(s(y.boyut)).append(" Tf ").append(s(y.x)).append(' ')
                        .append(s(h - y.y)).append(" Td ").append(k.kodla(y.metin))
                        .append(" Tj ET\n");
            }
        }
        return sb.toString();
    }

    /** Köşeleri çeyrek daire yaklaşımlı Bézier eğrileriyle çizilen dolu kutu. */
    private static void yuvarlakKutu(StringBuilder sb, double x, double y, double g, double y2,
                                     double r) {
        double k = r * 0.5523;
        sb.append(s(x + r)).append(' ').append(s(y)).append(" m\n");
        sb.append(s(x + g - r)).append(' ').append(s(y)).append(" l\n");
        sb.append(s(x + g - r + k)).append(' ').append(s(y)).append(' ')
                .append(s(x + g)).append(' ').append(s(y + r - k)).append(' ')
                .append(s(x + g)).append(' ').append(s(y + r)).append(" c\n");
        sb.append(s(x + g)).append(' ').append(s(y + y2 - r)).append(" l\n");
        sb.append(s(x + g)).append(' ').append(s(y + y2 - r + k)).append(' ')
                .append(s(x + g - r + k)).append(' ').append(s(y + y2)).append(' ')
                .append(s(x + g - r)).append(' ').append(s(y + y2)).append(" c\n");
        sb.append(s(x + r)).append(' ').append(s(y + y2)).append(" l\n");
        sb.append(s(x + r - k)).append(' ').append(s(y + y2)).append(' ')
                .append(s(x)).append(' ').append(s(y + y2 - r + k)).append(' ')
                .append(s(x)).append(' ').append(s(y + y2 - r)).append(" c\n");
        sb.append(s(x)).append(' ').append(s(y + r)).append(" l\n");
        sb.append(s(x)).append(' ').append(s(y + r - k)).append(' ')
                .append(s(x + r - k)).append(' ').append(s(y)).append(' ')
                .append(s(x + r)).append(' ').append(s(y)).append(" c f\n");
    }

    /** Type0 yazı tipini ve bağlı nesnelerini yazar; Type0 nesnesinin numarasını döner. */
    private static int yaziTipiYaz(Nesneler n, Kullanim k, boolean kalinMi) throws IOException {
        TrueTypeYazi tt = k.tt;
        int tip0 = n.ayir();
        int cid = n.ayir();
        int tanim = n.ayir();
        int dosya = n.ayir();
        int unicode = n.ayir();

        String ad = altKumeEki(k) + "+" + tt.ad;
        byte[] gomulu = tt.altKume(k.glifKarakter.keySet());

        n.akis(dosya, "/Length1 " + gomulu.length, gomulu, true);
        n.koy(tanim, "<< /Type /FontDescriptor /FontName /" + ad
                + " /Flags 32 /FontBBox [" + s(tt.olcekle(tt.xMin)) + " " + s(tt.olcekle(tt.yMin))
                + " " + s(tt.olcekle(tt.xMax)) + " " + s(tt.olcekle(tt.yMax)) + "]"
                + " /ItalicAngle " + s(tt.egiklik)
                + " /Ascent " + s(tt.olcekle(tt.yukselen))
                + " /Descent " + s(tt.olcekle(tt.alcalan))
                + " /CapHeight " + s(tt.olcekle(tt.buyukHarf))
                + " /StemV " + (kalinMi ? 140 : 80)
                + " /FontFile2 " + dosya + " 0 R >>");

        StringBuilder genislikler = new StringBuilder("[");
        for (int g : k.glifKarakter.keySet()) {
            genislikler.append(g).append(" [").append(tt.binde(g)).append("] ");
        }
        genislikler.append(']');

        n.koy(cid, "<< /Type /Font /Subtype /CIDFontType2 /BaseFont /" + ad
                + " /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >>"
                + " /FontDescriptor " + tanim + " 0 R /DW 1000 /W " + genislikler
                + " /CIDToGIDMap /Identity >>");
        n.akis(unicode, "", unicodeTablosu(k).getBytes(StandardCharsets.ISO_8859_1), true);
        n.koy(tip0, "<< /Type /Font /Subtype /Type0 /BaseFont /" + ad
                + " /Encoding /Identity-H /DescendantFonts [" + cid + " 0 R] /ToUnicode "
                + unicode + " 0 R >>");
        return tip0;
    }

    /** Alt küme yazı tiplerinin adına konan altı harflik ek (ör. "QWERTY+"). */
    private static String altKumeEki(Kullanim k) {
        String ozet = kimlik(k.kaynakAdi + k.glifKarakter.keySet());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int bayt = Integer.parseInt(ozet.substring(i * 2, i * 2 + 2), 16);
            sb.append((char) ('A' + bayt % 26));
        }
        return sb.toString();
    }

    private static String unicodeTablosu(Kullanim k) {
        StringBuilder sb = new StringBuilder();
        sb.append("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n")
                .append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS)")
                .append(" /Supplement 0 >> def\n")
                .append("/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n")
                .append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
        List<Map.Entry<Integer, Integer>> liste = new ArrayList<>(k.glifKarakter.entrySet());
        for (int i = 0; i < liste.size(); i += 100) {
            int son = Math.min(liste.size(), i + 100);
            sb.append(son - i).append(" beginbfchar\n");
            for (int j = i; j < son; j++) {
                sb.append(String.format("<%04X> <", liste.get(j).getKey()));
                for (char c : Character.toChars(liste.get(j).getValue())) {
                    sb.append(String.format("%04X", (int) c));
                }
                sb.append(">\n");
            }
            sb.append("endbfchar\n");
        }
        sb.append("endcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n");
        return sb.toString();
    }

    /** PDF metni: UTF-16BE, başında bayt sırası işaretiyle, onaltılık. */
    private static String metin16(String metin) {
        StringBuilder sb = new StringBuilder("<FEFF");
        for (char c : metin.toCharArray()) {
            sb.append(String.format("%04X", (int) c));
        }
        return sb.append('>').toString();
    }

    private static String renk(Color c) {
        return s(c.getRed() / 255.0) + " " + s(c.getGreen() / 255.0) + " " + s(c.getBlue() / 255.0);
    }

    /** Sayıyı en fazla üç ondalıkla, sondaki sıfırlar atılarak yazar. */
    static String s(double d) {
        if (Math.abs(d) < 0.0005) {
            return "0";
        }
        String t = String.format(Locale.ROOT, "%.3f", d);
        t = t.replaceAll("0+$", "");
        return t.endsWith(".") ? t.substring(0, t.length() - 1) : t;
    }

    /** Nesne numaralarını dağıtır, gövdeleri toplar, sonunda xref tablosunu kurar. */
    private static final class Nesneler {
        private final List<byte[]> govdeler = new ArrayList<>();

        int ayir() {
            govdeler.add(null);
            return govdeler.size();
        }

        void koy(int no, String govde) {
            govdeler.set(no - 1, govde.getBytes(StandardCharsets.ISO_8859_1));
        }

        void akis(int no, String ekSozluk, byte[] veri, boolean sikistir) {
            byte[] yazilacak = sikistir ? sikistir(veri) : veri;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            String sozluk = "<< /Length " + yazilacak.length
                    + (sikistir ? " /Filter /FlateDecode" : "")
                    + (ekSozluk.isEmpty() ? "" : " " + ekSozluk) + " >>\nstream\n";
            b.writeBytes(sozluk.getBytes(StandardCharsets.ISO_8859_1));
            b.writeBytes(yazilacak);
            b.writeBytes("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            govdeler.set(no - 1, b.toByteArray());
        }

        byte[] bitir(int katalog, int bilgi, String baslik) throws IOException {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.writeBytes("%PDF-1.4\n%".getBytes(StandardCharsets.ISO_8859_1));
            o.write(new byte[]{(byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});
            long[] konumlar = new long[govdeler.size()];
            for (int i = 0; i < govdeler.size(); i++) {
                byte[] g = govdeler.get(i);
                if (g == null) {
                    throw new IOException("Boş PDF nesnesi: " + (i + 1));
                }
                konumlar[i] = o.size();
                o.writeBytes(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                o.writeBytes(g);
                o.writeBytes("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }
            long xref = o.size();
            StringBuilder sb = new StringBuilder();
            sb.append("xref\n0 ").append(govdeler.size() + 1).append('\n');
            sb.append("0000000000 65535 f \n");
            for (long k : konumlar) {
                sb.append(String.format("%010d 00000 n \n", k));
            }
            String kimlik = kimlik(baslik + System.nanoTime());
            sb.append("trailer\n<< /Size ").append(govdeler.size() + 1)
                    .append(" /Root ").append(katalog).append(" 0 R /Info ").append(bilgi)
                    .append(" 0 R /ID [<").append(kimlik).append("> <").append(kimlik)
                    .append(">] >>\nstartxref\n").append(xref).append("\n%%EOF\n");
            o.writeBytes(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
            return o.toByteArray();
        }
    }

    private static byte[] sikistir(byte[] veri) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(veri);
        d.finish();
        ByteArrayOutputStream o = new ByteArrayOutputStream(veri.length / 2 + 64);
        byte[] tampon = new byte[8192];
        while (!d.finished()) {
            int n = d.deflate(tampon);
            o.write(tampon, 0, n);
        }
        d.end();
        return o.toByteArray();
    }

    private static String kimlik(String tohum) {
        try {
            byte[] ozet = MessageDigest.getInstance("MD5")
                    .digest(tohum.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : ozet) {
                sb.append(String.format("%02X", b & 0xFF));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "00000000000000000000000000000000";
        }
    }
}
