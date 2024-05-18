package com.github.mikephil.charting.data;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class MultiFiledData {
    public final int startIndex;
    public final int endIndex;
    public final int range;
    public final String name;
    public final int nameOffsite;
    public Drawable fillDrawable;
    public int fillColor;
    public int fillAlpha;



    public MultiFiledData(int startIndex, int endIndex, String name, int nameOffsite) {
        this.fillColor = Color.rgb(140, 234, 255);
        this.fillAlpha = 255;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.range = endIndex - startIndex;
        this.name = name;
        this.nameOffsite = nameOffsite;
    }

    public MultiFiledData(int startIndex, int endIndex, Drawable drawable, String name, int nameOffsite) {
        this(startIndex, endIndex, name, nameOffsite);
        this.fillDrawable = drawable;
    }

    public MultiFiledData(int startIndex, int endIndex, int fillColor) {
        this(startIndex, endIndex, "", 0);
        this.fillColor = fillColor;
    }

    public MultiFiledData(int startIndex, int endIndex, int fillColor, int fillAlpha, String name, int nameOffsite) {
        this(startIndex, endIndex, name, nameOffsite);
        this.fillColor = fillColor;
        this.fillAlpha = fillAlpha;
    }

    public MultiFiledData(int startIndex, int endIndex, int fillColor, String name, int nameOffsite) {
        this(startIndex, endIndex, name, nameOffsite);
        this.fillColor = fillColor;
    }
}