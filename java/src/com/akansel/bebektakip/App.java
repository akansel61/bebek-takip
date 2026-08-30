package com.akansel.bebektakip;

import com.akansel.bebektakip.depo.KayitDeposu;
import com.akansel.bebektakip.ui.AnaPencere;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;

/** Uygulamanın giriş noktası. */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        ortamAyarla();

        SwingUtilities.invokeLater(() -> {
            gorunumAyarla();
            try {
                KayitDeposu depo = new KayitDeposu();
                depo.yukle();
                new AnaPencere(depo).setVisible(true);
            } catch (RuntimeException e) {
                hatayiGoster(e);
            }
        });
    }

    /** Swing başlamadan önce ayarlanması gereken sistem özellikleri. */
    private static void ortamAyarla() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> hatayiGoster(e));
    }

    /** İşletim sisteminin görünümünü alır, yazı tipini kendi tipimizle değiştirir. */
    private static void gorunumAyarla() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception yoksay) {
            // olmadı, varsayılan görünümle devam
        }
        Font varsayilan = Tema.font(13);
        Enumeration<Object> anahtarlar = UIManager.getDefaults().keys();
        while (anahtarlar.hasMoreElements()) {
            Object anahtar = anahtarlar.nextElement();
            Object deger = UIManager.get(anahtar);
            if (deger instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(anahtar, new javax.swing.plaf.FontUIResource(varsayilan));
            }
        }
        UIManager.put("OptionPane.messageFont", varsayilan);
        UIManager.put("OptionPane.buttonFont", varsayilan);
        UIManager.put("ToolTip.font", Tema.font(12));
    }

    private static void hatayiGoster(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        System.err.println(sw);
        String kisa = sw.toString();
        if (kisa.length() > 1500) {
            kisa = kisa.substring(0, 1500) + "\n…";
        }
        try {
            JOptionPane.showMessageDialog(null,
                    "Beklenmeyen bir hata oluştu:\n\n" + kisa,
                    "Hata", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException yoksay) {
            // grafik ortam yoksa konsola yazmakla yetin
        }
    }
}
