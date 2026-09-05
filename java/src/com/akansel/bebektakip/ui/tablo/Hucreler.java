package com.akansel.bebektakip.ui.tablo;

import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.ui.Ikonlar;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalTime;

/** Kayıt tablosunun hücre çizicileri ve düzenleyicileri. */
public final class Hucreler {

    private Hucreler() {
    }

    public static final int SOL_BOSLUK = 14;

    /** Farenin hangi satırın üstünde olduğunu çizicilere bildirir. */
    public interface UzerindeBilgisi {
        int uzerindekiSatir();

        int uzerindekiSutun();
    }

    /** Satırın ait olduğu günün bir ton koyu çizilip çizilmeyeceğini bildirir. */
    public interface GunTonu {
        boolean gunKoyu(int satir);
    }

    /** Ortak zemin ve alt çizgi çizimini yapan taban sınıf. */
    abstract static class Temel extends JComponent implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        protected Object deger;
        protected boolean uzerinde;
        protected boolean hucreUzerinde;
        protected boolean secili;
        protected boolean sonSatir;
        protected boolean koyuGun;

        @Override
        public Component getTableCellRendererComponent(JTable tablo, Object deger,
                                                       boolean secili, boolean odakli,
                                                       int satir, int sutun) {
            this.deger = deger;
            this.secili = secili;
            this.sonSatir = satir == tablo.getRowCount() - 1;
            if (tablo instanceof UzerindeBilgisi) {
                UzerindeBilgisi u = (UzerindeBilgisi) tablo;
                this.uzerinde = u.uzerindekiSatir() == satir;
                this.hucreUzerinde = this.uzerinde && u.uzerindekiSutun() == sutun;
            } else {
                this.uzerinde = false;
                this.hucreUzerinde = false;
            }
            this.koyuGun = tablo instanceof GunTonu && ((GunTonu) tablo).gunKoyu(satir);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Tema.kaliteAyarla(g2);
                Color taban = koyuGun ? Tema.SATIR_KOYU : Tema.YUZEY;
                Color zemin = secili ? Tema.SECILI
                        : (uzerinde ? Tema.karistir(taban, Tema.SECILI, 0.5) : taban);
                g2.setColor(zemin);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (!sonSatir) {
                    g2.setColor(Tema.CIZGI_ACIK);
                    g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                }
                icerikCiz(g2);
            } finally {
                g2.dispose();
            }
        }

        protected abstract void icerikCiz(Graphics2D g2);

        // Çizici olarak kullanıldığından bu çağrılar gereksiz, boş bırakıldı.
        @Override
        public void invalidate() {
        }

        @Override
        public void revalidate() {
        }

        @Override
        public void repaint() {
        }

        @Override
        public void repaint(long tm, int x, int y, int w, int h) {
        }

        @Override
        protected void firePropertyChange(String ad, Object eski, Object yeni) {
        }
    }

    /** Düz metin hücresi; değer boşsa yer tutucu gösterir. */
    public static class MetinCizici extends Temel {

        private static final long serialVersionUID = 1L;

        private final String yerTutucu;
        private final boolean soluk;

        public MetinCizici(String yerTutucu, boolean soluk) {
            this.yerTutucu = yerTutucu == null ? "" : yerTutucu;
            this.soluk = soluk;
        }

        @Override
        protected void icerikCiz(Graphics2D g2) {
            String metin = deger == null ? "" : String.valueOf(deger);
            boolean bos = metin.trim().isEmpty();
            if (bos) {
                metin = yerTutucu;
            }
            if (metin.isEmpty()) {
                return;
            }
            g2.setFont(Tema.font(13));
            g2.setColor(bos || soluk ? Tema.METIN_SOLUK : Tema.METIN);
            FontMetrics fm = g2.getFontMetrics();
            String kisa = Tema.kisalt(g2, metin, getWidth() - SOL_BOSLUK * 2);
            g2.drawString(kisa, SOL_BOSLUK,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    /** Tarih hücresi; düzenlenebilir tabloda satır üstüne gelince takvim ikonu çıkar. */
    public static class TarihCizici extends Temel {

        private static final long serialVersionUID = 1L;

        private final boolean kisaBicim;
        private final boolean duzenlenebilir;

        public TarihCizici(boolean kisaBicim, boolean duzenlenebilir) {
            this.kisaBicim = kisaBicim;
            this.duzenlenebilir = duzenlenebilir;
        }

        @Override
        protected void icerikCiz(Graphics2D g2) {
            if (!(deger instanceof LocalDate)) {
                return;
            }
            LocalDate t = (LocalDate) deger;
            String metin = kisaBicim ? Tema.kisaTarih(t) : Tema.tamTarih(t);
            g2.setFont(Tema.font(13));
            g2.setColor(Tema.METIN);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(metin, SOL_BOSLUK,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            if (duzenlenebilir && uzerinde) {
                Ikonlar.takvim(g2, getWidth() - 24, (getHeight() - 15) / 2.0, 15,
                        Tema.METIN_SOLUK);
            }
        }
    }

    /** Saat hücresi. */
    public static class SaatCizici extends Temel {

        private static final long serialVersionUID = 1L;

        @Override
        protected void icerikCiz(Graphics2D g2) {
            String metin;
            if (deger instanceof LocalTime) {
                metin = Tema.saat((LocalTime) deger);
            } else {
                metin = deger == null ? "" : String.valueOf(deger);
            }
            g2.setFont(Tema.font(13));
            g2.setColor(Tema.METIN);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(metin, SOL_BOSLUK,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    /** İşaretliyse yeşil onay kutusu, değilse boş çizgi. */
    public static class OnayCizici extends Temel {

        private static final long serialVersionUID = 1L;

        private final boolean tiklanabilir;

        public OnayCizici(boolean tiklanabilir) {
            this.tiklanabilir = tiklanabilir;
        }

        @Override
        protected void icerikCiz(Graphics2D g2) {
            boolean isaretli = Boolean.TRUE.equals(deger);
            int kutu = 22;
            double x = (getWidth() - kutu) / 2.0;
            double y = (getHeight() - kutu) / 2.0;
            RoundRectangle2D sekil = new RoundRectangle2D.Double(x, y, kutu, kutu, 6, 6);
            if (isaretli) {
                g2.setColor(Tema.YESIL);
                g2.fill(sekil);
                Ikonlar.onay(g2, x + 4, y + 4, kutu - 8, Color.WHITE);
            } else {
                g2.setColor(tiklanabilir && hucreUzerinde ? Tema.CIZGI : Tema.ARKA);
                g2.fill(sekil);
                Ikonlar.tire(g2, x + 3, y + 3, kutu - 6, Tema.PASIF_KUTU);
            }
        }
    }

    /** Satır üstüne gelince beliren çöp kutusu. */
    public static class SilCizici extends Temel {

        private static final long serialVersionUID = 1L;

        @Override
        protected void icerikCiz(Graphics2D g2) {
            if (!uzerinde) {
                return;
            }
            Ikonlar.cop(g2, (getWidth() - 16) / 2.0, (getHeight() - 16) / 2.0, 16,
                    hucreUzerinde ? Tema.KIRMIZI : Tema.karistir(Tema.KIRMIZI, Color.WHITE, 0.35));
        }
    }

    /** Tablo başlığı. */
    public static class BaslikCizici extends JComponent implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private String metin = "";
        private boolean ortala;

        @Override
        public Component getTableCellRendererComponent(JTable tablo, Object deger,
                                                       boolean secili, boolean odakli,
                                                       int satir, int sutun) {
            this.metin = deger == null ? "" : String.valueOf(deger);
            this.ortala = KayitTabloModeli.onaySutunu(sutun);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Tema.kaliteAyarla(g2);
                g2.setColor(Tema.YUZEY);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Tema.CIZGI);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                if (metin.isEmpty()) {
                    return;
                }
                g2.setFont(Tema.font(12));
                g2.setColor(Tema.METIN_SOLUK);
                FontMetrics fm = g2.getFontMetrics();
                int x = ortala
                        ? (getWidth() - fm.stringWidth(metin)) / 2
                        : SOL_BOSLUK;
                g2.drawString(metin, x, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }

    /** Hücre içi metin düzenleyici. */
    public static class MetinDuzenleyici extends DefaultCellEditor {

        private static final long serialVersionUID = 1L;

        private final JTextField alan;
        private final boolean saatAlani;

        public MetinDuzenleyici(boolean saatAlani) {
            super(new JTextField());
            this.saatAlani = saatAlani;
            this.alan = (JTextField) getComponent();
            alan.setFont(Tema.font(13));
            alan.setForeground(Tema.METIN);
            alan.setBackground(Tema.YUZEY);
            alan.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, Tema.METIN),
                    BorderFactory.createEmptyBorder(0, SOL_BOSLUK - 1, 0, 6)));
            alan.setCaretColor(Tema.METIN);
            setClickCountToStart(2);
        }

        @Override
        public Component getTableCellEditorComponent(JTable tablo, Object deger,
                                                     boolean secili, int satir, int sutun) {
            Component c = super.getTableCellEditorComponent(tablo, deger, secili, satir, sutun);
            SwingUtilities.invokeLater(alan::selectAll);
            return c;
        }

        @Override
        public boolean stopCellEditing() {
            if (saatAlani) {
                String metin = alan.getText();
                if (metin != null && !metin.trim().isEmpty()
                        && Kayit.saatCoz(metin) == null) {
                    alan.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 1, 1, 1, Tema.KIRMIZI),
                            BorderFactory.createEmptyBorder(0, SOL_BOSLUK - 1, 0, 6)));
                    alan.requestFocusInWindow();
                    return false;
                }
                alan.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, Tema.METIN),
                        BorderFactory.createEmptyBorder(0, SOL_BOSLUK - 1, 0, 6)));
            }
            return super.stopCellEditing();
        }
    }
}
