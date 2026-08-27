package com.akansel.bebektakip.ui.bilesen;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Kaydırma alanı içinde genişliği viewport'a uyan, yüksekliği ise içeriğe
 * göre belirlenen panel.
 */
public class KaydirilabilirPanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    public KaydirilabilirPanel(LayoutManager duzen) {
        super(duzen);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle gorunur, int yon, int taraf) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle gorunur, int yon, int taraf) {
        return Math.max(48, gorunur.height - 40);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
