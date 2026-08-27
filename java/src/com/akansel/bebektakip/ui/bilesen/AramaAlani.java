package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Ikonlar;
import com.akansel.bebektakip.ui.Tema;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/** Büyüteç ikonlu, yer tutuculu arama kutusu. */
public class AramaAlani extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField alan = new JTextField();
    private final String yerTutucu;
    private boolean odakli;

    public AramaAlani(String yerTutucu, int genislik, Consumer<String> degisince) {
        super(new BorderLayout());
        this.yerTutucu = yerTutucu;
        setOpaque(false);
        setPreferredSize(new Dimension(genislik, 32));

        alan.setOpaque(false);
        alan.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 10));
        alan.setFont(Tema.font(13));
        alan.setForeground(Tema.METIN);
        alan.setCaretColor(Tema.METIN);
        add(alan, BorderLayout.CENTER);

        alan.getDocument().addDocumentListener(new DocumentListener() {
            private void bildir() {
                degisince.accept(alan.getText());
                repaint();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                bildir();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                bildir();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                bildir();
            }
        });
        alan.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                odakli = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                odakli = false;
                repaint();
            }
        });
    }

    public String getMetin() {
        return alan.getText();
    }

    public void temizle() {
        alan.setText("");
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            RoundRectangle2D sekil = new RoundRectangle2D.Double(0.5, 0.5,
                    getWidth() - 1.0, getHeight() - 1.0, 8, 8);
            g2.setColor(Tema.YUZEY);
            g2.fill(sekil);
            g2.setColor(odakli ? Tema.METIN : Tema.CIZGI);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(sekil);

            Ikonlar.ara(g2, 9, (getHeight() - 14) / 2.0, 14,
                    odakli ? Tema.METIN : Tema.METIN_SOLUK);

            if (alan.getText().isEmpty() && !odakli) {
                g2.setFont(Tema.font(13));
                g2.setColor(Tema.METIN_SOLUK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(yerTutucu, 30,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
