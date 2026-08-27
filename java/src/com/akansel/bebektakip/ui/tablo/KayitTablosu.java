package com.akansel.bebektakip.ui.tablo;

import com.akansel.bebektakip.model.Kayit;
import com.akansel.bebektakip.ui.Tema;
import com.akansel.bebektakip.ui.bilesen.TakvimSecici;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** Satır vurgusu, tıklamayla işaretleme ve satır silme davranışını taşıyan tablo. */
public class KayitTablosu extends JTable implements Hucreler.UzerindeBilgisi {

    private static final long serialVersionUID = 1L;

    /** Sabit sütun genişlikleri; "Not" sütunu kalan alanı doldurur. */
    private static final int[] GENISLIKLER = {100, 66, 62, 62, 78, 78, 62, 100, 0, 42};
    private static final int NOT_EN_AZ = 140;

    private final KayitTabloModeli modeli;
    private int uzerindekiSatir = -1;
    private int uzerindekiSutun = -1;
    private Consumer<Kayit> silmeIstegi;

    public KayitTablosu(KayitTabloModeli modeli, boolean kisaTarih) {
        super(modeli);
        this.modeli = modeli;

        setRowHeight(42);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setBackground(Tema.YUZEY);
        setSelectionBackground(Tema.SECILI);
        setSelectionForeground(Tema.METIN);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        setRowSelectionAllowed(modeli.isDuzenlenebilir());
        setColumnSelectionAllowed(false);
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        setSurrendersFocusOnKeystroke(true);
        setFocusable(modeli.isDuzenlenebilir());
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JTableHeader baslik = getTableHeader();
        baslik.setDefaultRenderer(new Hucreler.BaslikCizici());
        baslik.setPreferredSize(new Dimension(0, 40));
        baslik.setReorderingAllowed(false);
        baslik.setResizingAllowed(false);
        baslik.setBackground(Tema.YUZEY);

        ciziciKur(kisaTarih);
        genislikKur();

        fareKur();
        if (modeli.isDuzenlenebilir()) {
            klavyeKur();
        }
    }

    public void setSilmeIstegi(Consumer<Kayit> silmeIstegi) {
        this.silmeIstegi = silmeIstegi;
    }

    private void ciziciKur(boolean kisaTarih) {
        boolean duzenlenebilir = modeli.isDuzenlenebilir();
        sutun(KayitTabloModeli.SUTUN_TARIH)
                .setCellRenderer(new Hucreler.TarihCizici(kisaTarih, duzenlenebilir));
        sutun(KayitTabloModeli.SUTUN_SAAT).setCellRenderer(new Hucreler.SaatCizici());
        for (int s = KayitTabloModeli.SUTUN_CIS; s <= KayitTabloModeli.SUTUN_MAMA; s++) {
            sutun(s).setCellRenderer(new Hucreler.OnayCizici(duzenlenebilir));
        }
        sutun(KayitTabloModeli.SUTUN_MAMA_NOTU).setCellRenderer(
                new Hucreler.MetinCizici(duzenlenebilir ? "ml/gr" : "—", false));
        sutun(KayitTabloModeli.SUTUN_NOT).setCellRenderer(
                new Hucreler.MetinCizici(duzenlenebilir ? "Not…" : "—", false));
        sutun(KayitTabloModeli.SUTUN_SIL).setCellRenderer(new Hucreler.SilCizici());

        if (duzenlenebilir) {
            sutun(KayitTabloModeli.SUTUN_SAAT).setCellEditor(new Hucreler.MetinDuzenleyici(true));
            sutun(KayitTabloModeli.SUTUN_MAMA_NOTU)
                    .setCellEditor(new Hucreler.MetinDuzenleyici(false));
            sutun(KayitTabloModeli.SUTUN_NOT).setCellEditor(new Hucreler.MetinDuzenleyici(false));
        }
    }

    private TableColumn sutun(int indeks) {
        return getColumnModel().getColumn(indeks);
    }

    private void genislikKur() {
        for (int i = 0; i < GENISLIKLER.length; i++) {
            TableColumn c = sutun(i);
            if (i == KayitTabloModeli.SUTUN_NOT) {
                c.setMinWidth(NOT_EN_AZ);
                c.setPreferredWidth(240);
                c.setMaxWidth(Integer.MAX_VALUE);
            } else {
                c.setMinWidth(GENISLIKLER[i]);
                c.setPreferredWidth(GENISLIKLER[i]);
                c.setMaxWidth(GENISLIKLER[i]);
            }
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

    private void fareKur() {
        final boolean duzenlenebilir = modeli.isDuzenlenebilir();
        MouseAdapter a = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int satir = rowAtPoint(e.getPoint());
                int sutun = columnAtPoint(e.getPoint());
                vurguGuncelle(satir, sutun);
                boolean elle = satir >= 0 && sutun >= 0
                        && (sutun == KayitTabloModeli.SUTUN_SIL
                        || (duzenlenebilir && (KayitTabloModeli.onaySutunu(sutun)
                        || sutun == KayitTabloModeli.SUTUN_TARIH)));
                setCursor(Cursor.getPredefinedCursor(
                        elle ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                vurguGuncelle(-1, -1);
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int satir = rowAtPoint(e.getPoint());
                int sutun = columnAtPoint(e.getPoint());
                if (satir < 0 || sutun < 0) {
                    return;
                }
                if (sutun == KayitTabloModeli.SUTUN_SIL) {
                    duzenlemeyiBitir();
                    silmeyiIste(satir);
                } else if (!duzenlenebilir) {
                    return;
                } else if (KayitTabloModeli.onaySutunu(sutun)) {
                    duzenlemeyiBitir();
                    setRowSelectionInterval(satir, satir);
                    modeli.tersineCevir(satir, sutun);
                } else if (sutun == KayitTabloModeli.SUTUN_TARIH) {
                    duzenlemeyiBitir();
                    setRowSelectionInterval(satir, satir);
                    takvimiAc(satir);
                }
            }
        };
        addMouseListener(a);
        addMouseMotionListener(a);
    }

    private void duzenlemeyiBitir() {
        if (isEditing() && getCellEditor() != null) {
            getCellEditor().stopCellEditing();
        }
    }

    private void takvimiAc(int satir) {
        Kayit k = modeli.satirdaki(satir);
        if (k == null) {
            return;
        }
        Rectangle r = getCellRect(satir, KayitTabloModeli.SUTUN_TARIH, true);
        TakvimSecici.goster(this, r.x, r.y + r.height, k.getTarih(), tarih -> {
            modeli.setValueAt(tarih, satir, KayitTabloModeli.SUTUN_TARIH);
        });
    }

    private void silmeyiIste(int satir) {
        Kayit k = modeli.satirdaki(satir);
        if (k != null && silmeIstegi != null) {
            silmeIstegi.accept(k);
        }
    }

    private void klavyeKur() {
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "isaretiDegistir");
        getActionMap().put("isaretiDegistir", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int satir = getSelectedRow();
                int sutun = getSelectedColumn();
                if (satir >= 0 && KayitTabloModeli.onaySutunu(sutun)) {
                    modeli.tersineCevir(satir, sutun);
                }
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "satiriSil");
        getActionMap().put("satiriSil", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int satir = getSelectedRow();
                if (satir >= 0) {
                    silmeyiIste(satir);
                }
            }
        });
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
