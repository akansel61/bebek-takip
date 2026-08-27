package com.akansel.bebektakip.ui.bilesen;

import com.akansel.bebektakip.ui.Tema;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/** Beyaz zeminli, ince kenarlıklı, köşesi yuvarlatılmış panel. */
public class Kart extends JPanel {

    private static final long serialVersionUID = 1L;

    private int yaricap = 12;
    private Color zemin = Tema.YUZEY;
    private Color kenarlik = Tema.CIZGI;

    public Kart() {
        setOpaque(false);
    }

    public Kart(LayoutManager duzen) {
        super(duzen);
        setOpaque(false);
    }

    public Kart yaricap(int yaricap) {
        this.yaricap = yaricap;
        return this;
    }

    public Kart zemin(Color zemin) {
        this.zemin = zemin;
        return this;
    }

    public Kart kenarlik(Color kenarlik) {
        this.kenarlik = kenarlik;
        return this;
    }

    private RoundRectangle2D sekil() {
        return new RoundRectangle2D.Double(0.5, 0.5,
                getWidth() - 1.0, getHeight() - 1.0, yaricap, yaricap);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            if (zemin != null) {
                g2.setColor(zemin);
                g2.fill(sekil());
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /** Alt bileşenler yuvarlatılmış köşenin dışına taşmasın. */
    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                    yaricap, yaricap));
            super.paintChildren(g2);
        } finally {
            g2.dispose();
        }
    }

    /** Kenarlık en üste çizilir, böylece alt bileşenler üstünü örtmez. */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (kenarlik == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema.kaliteAyarla(g2);
            g2.setColor(kenarlik);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(sekil());
        } finally {
            g2.dispose();
        }
    }
}
