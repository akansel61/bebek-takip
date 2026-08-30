package com.akansel.bebektakip.ui.tablo;

import com.akansel.bebektakip.ui.Tema;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Uyku, büyüme ve hatırlatıcı ekranlarının ortak tablosu: kayıt tablosuyla
 * aynı görünümü taşır, fare hangi satırın üstündeyse çizicilere bildirir.
 * Sütunlara özel tıklama davranışları (takvim, silme, işaretleme) ekranın
 * kendisinde kurulur.
 */
public class SadeTablo extends JTable implements Hucreler.UzerindeBilgisi {

    private static final long serialVersionUID = 1L;

    private int uzerindekiSatir = -1;
    private int uzerindekiSutun = -1;

    public SadeTablo(TableModel model) {
        super(model);

        setRowHeight(42);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setBackground(Tema.YUZEY);
        setSelectionBackground(Tema.SECILI);
        setSelectionForeground(Tema.METIN);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSurrendersFocusOnKeystroke(true);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JTableHeader baslik = getTableHeader();
        baslik.setDefaultRenderer(new Hucreler.BaslikCizici());
        baslik.setPreferredSize(new Dimension(0, 40));
        baslik.setReorderingAllowed(false);
        baslik.setResizingAllowed(false);
        baslik.setBackground(Tema.YUZEY);

        MouseAdapter iz = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                vurguGuncelle(rowAtPoint(e.getPoint()), columnAtPoint(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                vurguGuncelle(-1, -1);
            }
        };
        addMouseListener(iz);
        addMouseMotionListener(iz);
    }

    /** Sütunu verilen genişliğe sabitler. */
    public void sabitSutun(int indeks, int genislik) {
        TableColumn c = getColumnModel().getColumn(indeks);
        c.setMinWidth(genislik);
        c.setPreferredWidth(genislik);
        c.setMaxWidth(genislik);
    }

    /** Sütun kalan alanı doldurur; en az verilen genişlikte kalır. */
    public void esnekSutun(int indeks, int enAz, int tercih) {
        TableColumn c = getColumnModel().getColumn(indeks);
        c.setMinWidth(enAz);
        c.setPreferredWidth(tercih);
        c.setMaxWidth(Integer.MAX_VALUE);
    }

    /** Açık bir hücre düzenleyicisi varsa kapatır. */
    public void duzenlemeyiBitir() {
        if (isEditing() && getCellEditor() != null) {
            getCellEditor().stopCellEditing();
        }
    }

    private void vurguGuncelle(int satir, int sutun) {
        if (satir == uzerindekiSatir && sutun == uzerindekiSutun) {
            return;
        }
        int eski = uzerindekiSatir;
        uzerindekiSatir = satir;
        uzerindekiSutun = sutun;
        if (eski >= 0) {
            satiriYenile(eski);
        }
        if (satir >= 0) {
            satiriYenile(satir);
        }
    }

    private void satiriYenile(int satir) {
        if (satir < 0 || satir >= getRowCount()) {
            return;
        }
        Rectangle r = getCellRect(satir, 0, true);
        repaint(0, r.y, getWidth(), r.height);
    }

    @Override
    public int uzerindekiSatir() {
        return uzerindekiSatir;
    }

    @Override
    public int uzerindekiSutun() {
        return uzerindekiSutun;
    }
}
