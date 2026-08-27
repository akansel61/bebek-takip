package com.akansel.bebektakip.arac;

import com.akansel.bebektakip.ui.UygulamaIkonu;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * jpackage'in Windows'ta istediği .ico dosyasını üretir.
 *
 * Küçük boyutlar klasik BMP (DIB), 256x256 ise PNG olarak gömülür; Windows
 * her sürümde bu ikisini sorunsuz okur.
 *
 * Kullanım: java -cp build com.akansel.bebektakip.arac.IkonUret cikti.ico
 */
public final class IkonUret {

    private IkonUret() {
    }

    private static final int[] BOYUTLAR = {16, 20, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws IOException {
        Path hedef = Paths.get(args.length > 0 ? args[0] : "bebek-takip.ico");
        Path ustDizin = hedef.toAbsolutePath().getParent();
        if (ustDizin != null) {
            Files.createDirectories(ustDizin);
        }
        Files.write(hedef, icoUret());
        System.out.println("Simge yazıldı: " + hedef.toAbsolutePath());
    }

    public static byte[] icoUret() throws IOException {
        List<byte[]> govdeler = new ArrayList<>();
        for (int boyut : BOYUTLAR) {
            BufferedImage img = UygulamaIkonu.olustur(boyut);
            govdeler.add(boyut >= 256 ? pngGovde(img) : bmpGovde(img));
        }

        int basliklar = 6 + 16 * BOYUTLAR.length;
        int toplam = basliklar;
        for (byte[] b : govdeler) {
            toplam += b.length;
        }

        ByteBuffer bb = ByteBuffer.allocate(toplam).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) 0);                       // ayrilmis
        bb.putShort((short) 1);                       // tur: ikon
        bb.putShort((short) BOYUTLAR.length);         // resim sayisi

        int konum = basliklar;
        for (int i = 0; i < BOYUTLAR.length; i++) {
            int boyut = BOYUTLAR[i];
            byte[] govde = govdeler.get(i);
            bb.put((byte) (boyut >= 256 ? 0 : boyut)); // genislik
            bb.put((byte) (boyut >= 256 ? 0 : boyut)); // yukseklik
            bb.put((byte) 0);                          // renk sayisi
            bb.put((byte) 0);                          // ayrilmis
            bb.putShort((short) 1);                    // duzlem
            bb.putShort((short) 32);                   // bit derinligi
            bb.putInt(govde.length);
            bb.putInt(konum);
            konum += govde.length;
        }
        for (byte[] govde : govdeler) {
            bb.put(govde);
        }
        return bb.array();
    }

    private static byte[] pngGovde(BufferedImage img) throws IOException {
        ByteArrayOutputStream cikti = new ByteArrayOutputStream();
        ImageIO.write(img, "png", cikti);
        return cikti.toByteArray();
    }

    /** 32 bit BGRA + AND maskesi içeren klasik ikon gövdesi. */
    private static byte[] bmpGovde(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int maskeSatiri = ((w + 31) / 32) * 4;
        int uzunluk = 40 + w * h * 4 + maskeSatiri * h;

        ByteBuffer bb = ByteBuffer.allocate(uzunluk).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(40);          // biSize
        bb.putInt(w);           // biWidth
        bb.putInt(h * 2);       // biHeight (XOR + AND)
        bb.putShort((short) 1); // biPlanes
        bb.putShort((short) 32);// biBitCount
        bb.putInt(0);           // biCompression
        bb.putInt(w * h * 4 + maskeSatiri * h);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);

        // XOR verisi alttan üste ve BGRA sirasiyla yazilir
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                bb.put((byte) (argb & 0xFF));         // mavi
                bb.put((byte) ((argb >> 8) & 0xFF));  // yesil
                bb.put((byte) ((argb >> 16) & 0xFF)); // kirmizi
                bb.put((byte) ((argb >> 24) & 0xFF)); // alfa
            }
        }
        // alfa kanalı kullanıldığından AND maskesi tamamen sıfır kalabilir
        for (int i = 0; i < maskeSatiri * h; i++) {
            bb.put((byte) 0);
        }
        return bb.array();
    }
}
