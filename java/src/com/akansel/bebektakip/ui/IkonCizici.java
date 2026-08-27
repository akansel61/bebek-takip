package com.akansel.bebektakip.ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** Ikonlar sınıfındaki çizim yöntemlerine referans verebilmek için. */
@FunctionalInterface
public interface IkonCizici {
    void ciz(Graphics2D g, double x, double y, double boyut, Color renk);
}
