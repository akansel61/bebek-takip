package com.akansel.bebektakip.ui;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/** Kaydetme penceresi ve kaydedilen dosyayı açma; dışa aktaran ekranların ortak işi. */
final class DosyaSecici {

    private DosyaSecici() {
    }

    /**
     * Kaydetme penceresini açar. Uzantı yazılmadıysa eklenir, dosya varsa
     * üzerine yazmak için onay istenir. Vazgeçilirse null döner.
     */
    static File kaydetmeYeri(Component sahip, String baslik, String varsayilanAd,
                             String aciklama, String uzanti) {
        JFileChooser secici = new JFileChooser();
        secici.setDialogTitle(baslik);
        secici.setSelectedFile(new File(varsayilanAd));
        secici.setFileFilter(new FileNameExtensionFilter(aciklama, uzanti));
        if (secici.showSaveDialog(sahip) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File dosya = secici.getSelectedFile();
        if (!dosya.getName().toLowerCase(Tema.TR).endsWith("." + uzanti)) {
            dosya = new File(dosya.getParentFile(), dosya.getName() + "." + uzanti);
        }
        if (dosya.exists()) {
            int cevap = JOptionPane.showConfirmDialog(sahip,
                    "Dosya zaten var. Üzerine yazılsın mı?\n" + dosya.getName(),
                    "Üzerine yaz", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (cevap != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return dosya;
    }

    /** "Kaydedildi" mesajı; kullanıcı isterse dosya varsayılan programla açılır. */
    static void kaydedildi(Component sahip, File dosya) {
        boolean acilabilir = Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
        if (!acilabilir) {
            JOptionPane.showMessageDialog(sahip, "Kaydedildi:\n" + dosya.getAbsolutePath(),
                    "Bilgi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object[] secenekler = {"Dosyayı aç", "Tamam"};
        int secim = JOptionPane.showOptionDialog(sahip,
                "Kaydedildi:\n" + dosya.getAbsolutePath(), "Bilgi",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                secenekler, secenekler[0]);
        if (secim != 0) {
            return;
        }
        try {
            Desktop.getDesktop().open(dosya);
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(sahip,
                    "Dosya açılamadı: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
}
