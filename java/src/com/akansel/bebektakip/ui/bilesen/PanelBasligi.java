package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/** Kart üst şeridi: solda başlık, sağda isteğe bağlı bir bileşen. */
public class PanelBasligi extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel baslikEtiketi;

    public PanelBasligi(String baslik, JComponent sagBilesen) {
        super(new BorderLayout(12, 0));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.CIZGI),
                BorderFactory.createEmptyBorder(0, 20, 0, 16)));

        baslikEtiketi = new JLabel(baslik);
        baslikEtiketi.setFont(Tema.fontKalin(15));
        baslikEtiketi.setForeground(Tema.METIN);
        add(baslikEtiketi, BorderLayout.WEST);

        if (sagBilesen != null) {
            // GridBag kullanılıyor: bileşen tercih ettiği boyutta kalır,
            // BorderLayout.EAST gibi dikeyde gerilmez.
            JPanel sarmal = new JPanel(new GridBagLayout());
            sarmal.setOpaque(false);
            GridBagConstraints kural = new GridBagConstraints();
            kural.anchor = GridBagConstraints.EAST;
            kural.weightx = 1;
            sarmal.add(sagBilesen, kural);
            add(sarmal, BorderLayout.CENTER);
        }
    }

    public void setBaslik(String baslik) {
        baslikEtiketi.setText(baslik);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(56, d.height));
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
