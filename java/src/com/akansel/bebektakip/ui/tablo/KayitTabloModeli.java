package com.akansel.bebektakip.ui.tablo;

import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Kayıt tablosunun veri modeli; arama filtresini de bu yönetir. */
public class KayitTabloModeli extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int SUTUN_TARIH = 0;
    public static final int SUTUN_SAAT = 1;
    public static final int SUTUN_CIS = 2;
    public static final int SUTUN_KAKA = 3;
    public static final int SUTUN_SAG = 4;
    public static final int SUTUN_SOL = 5;
    public static final int SUTUN_MAMA = 6;
    public static final int SUTUN_MAMA_NOTU = 7;
    public static final int SUTUN_NOT = 8;
    public static final int SUTUN_SIL = 9;
    public static final int SUTUN_SAYISI = 10;

    private static final String[] BASLIKLAR_UZUN = {
            "Tarih", "Saat", "Çiş", "Kaka", "Sağ Meme", "Sol Meme", "Mama",
            "Mama Notu", "Not", ""
    };
    private static final String[] BASLIKLAR_KISA = {
            "Tarih", "Saat", "Çiş", "Kaka", "Sağ", "Sol", "Mama",
            "Mama Notu", "Not", ""
    };

    private final List<Kayit> kaynak;
    private final List<Kayit> gorunen = new ArrayList<>();
    private final boolean duzenlenebilir;
    private final boolean kisaBaslik;

    private String filtre = "";
    private int enFazlaSatir = Integer.MAX_VALUE;
    private Runnable degisiklikDinleyici;

    public KayitTabloModeli(List<Kayit> kaynak, boolean duzenlenebilir, boolean kisaBaslik) {
        this.kaynak = kaynak;
        this.duzenlenebilir = duzenlenebilir;
        this.kisaBaslik = kisaBaslik;
        filtreUygula();
    }

    public void setDegisiklikDinleyici(Runnable r) {
        this.degisiklikDinleyici = r;
    }

    private void degisti() {
        if (degisiklikDinleyici != null) {
            degisiklikDinleyici.run();
        }
    }

    public void setFiltre(String filtre) {
        this.filtre = filtre == null ? "" : filtre.trim();
        filtreUygula();
        fireTableDataChanged();
    }

    public String getFiltre() {
        return filtre;
    }

    public void setEnFazlaSatir(int enFazlaSatir) {
        this.enFazlaSatir = enFazlaSatir;
        filtreUygula();
        fireTableDataChanged();
    }

    /** Kaynak listeden görünen listeyi yeniden oluşturur. */
    public void filtreUygula() {
        gorunen.clear();
        String f = filtre.toLowerCase(Tema.TR);
        for (Kayit k : kaynak) {
            if (f.isEmpty() || eslesiyor(k, f)) {
                gorunen.add(k);
                if (gorunen.size() >= enFazlaSatir) {
                    break;
                }
            }
        }
    }

    private boolean eslesiyor(Kayit k, String f) {
        if (k.getNot().toLowerCase(Tema.TR).contains(f)) {
            return true;
        }
        if (k.getMamaNotu().toLowerCase(Tema.TR).contains(f)) {
            return true;
        }
        if (k.tarihMetni().contains(f) || Tema.tamTarih(k.getTarih()).contains(f)) {
            return true;
        }
        if (Tema.kisaTarih(k.getTarih()).toLowerCase(Tema.TR).contains(f)) {
            return true;
        }
        return k.saatMetni().contains(f);
    }

    /** Kaynak liste dışarıdan değiştiğinde çağrılır. */
    public void yenile() {
        filtreUygula();
        fireTableDataChanged();
    }

    public Kayit satirdaki(int satir) {
        if (satir < 0 || satir >= gorunen.size()) {
            return null;
        }
        return gorunen.get(satir);
    }

    public boolean isKaynakBos() {
        return kaynak.isEmpty();
    }

    @Override
    public int getRowCount() {
        return gorunen.size();
    }

    @Override
    public int getColumnCount() {
        return SUTUN_SAYISI;
    }

    @Override
    public String getColumnName(int sutun) {
        return (kisaBaslik ? BASLIKLAR_KISA : BASLIKLAR_UZUN)[sutun];
    }

    @Override
    public Class<?> getColumnClass(int sutun) {
        switch (sutun) {
            case SUTUN_TARIH:
                return LocalDate.class;
            case SUTUN_SAAT:
                return LocalTime.class;
            case SUTUN_CIS:
            case SUTUN_KAKA:
            case SUTUN_SAG:
            case SUTUN_SOL:
            case SUTUN_MAMA:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int satir, int sutun) {
        if (!duzenlenebilir) {
            return false;
        }
        return sutun == SUTUN_SAAT || sutun == SUTUN_MAMA_NOTU || sutun == SUTUN_NOT;
    }

    public boolean isDuzenlenebilir() {
        return duzenlenebilir;
    }

    @Override
    public Object getValueAt(int satir, int sutun) {
        Kayit k = satirdaki(satir);
        if (k == null) {
            return null;
        }
        switch (sutun) {
            case SUTUN_TARIH:
                return k.getTarih();
            case SUTUN_SAAT:
                return k.getSaat();
            case SUTUN_CIS:
                return k.isCis();
            case SUTUN_KAKA:
                return k.isKaka();
            case SUTUN_SAG:
                return k.isSagMeme();
            case SUTUN_SOL:
                return k.isSolMeme();
            case SUTUN_MAMA:
                return k.isMama();
            case SUTUN_MAMA_NOTU:
                return k.getMamaNotu();
            case SUTUN_NOT:
                return k.getNot();
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object deger, int satir, int sutun) {
        Kayit k = satirdaki(satir);
        if (k == null) {
            return;
        }
        switch (sutun) {
            case SUTUN_TARIH: {
                LocalDate t = deger instanceof LocalDate
                        ? (LocalDate) deger
                        : Kayit.tarihCoz(String.valueOf(deger));
                if (t == null) {
                    return;
                }
                k.setTarih(t);
                break;
            }
            case SUTUN_SAAT: {
                LocalTime s = deger instanceof LocalTime
                        ? (LocalTime) deger
                        : Kayit.saatCoz(String.valueOf(deger));
                if (s == null) {
                    return;
                }
                k.setSaat(s);
                break;
            }
            case SUTUN_CIS:
                k.setCis(Boolean.TRUE.equals(deger));
                break;
            case SUTUN_KAKA:
                k.setKaka(Boolean.TRUE.equals(deger));
                break;
            case SUTUN_SAG:
                k.setSagMeme(Boolean.TRUE.equals(deger));
                break;
            case SUTUN_SOL:
                k.setSolMeme(Boolean.TRUE.equals(deger));
                break;
            case SUTUN_MAMA:
                k.setMama(Boolean.TRUE.equals(deger));
                break;
            case SUTUN_MAMA_NOTU:
                k.setMamaNotu(deger == null ? "" : String.valueOf(deger));
                break;
            case SUTUN_NOT:
                k.setNot(deger == null ? "" : String.valueOf(deger));
                break;
            default:
                return;
        }
        fireTableRowsUpdated(satir, satir);
        degisti();
    }

    /** Onay kutusu sütunlarını tersine çevirir. */
    public void tersineCevir(int satir, int sutun) {
        Object v = getValueAt(satir, sutun);
        if (v instanceof Boolean) {
            setValueAt(!((Boolean) v), satir, sutun);
        }
    }

    public static boolean onaySutunu(int sutun) {
        return sutun >= SUTUN_CIS && sutun <= SUTUN_MAMA;
    }
}
