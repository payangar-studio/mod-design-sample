package com.payangar.designsample.vendor.gui;

/**
 * The colours (ARGB) and measures every widget of the kit draws with. Nothing is a texture: the
 * widgets are fills, outlines, bevels and gradients in these colours, with the vanilla font.
 */
public final class Theme {
    /** The screen behind everything: opaque, so that a menu panorama never shows through. */
    public static final int BACKGROUND = 0xFF14161B;
    /** The header and footer bands. */
    public static final int BAND = 0xFF1B1E25;
    /** The lines between the bands and the content. */
    public static final int SEPARATOR = 0xFF2A2F39;

    /** A widget's face. */
    public static final int SURFACE = 0xFF252932;
    /** A widget's face under the mouse. */
    public static final int SURFACE_HOVER = 0xFF30353F;
    /** A disabled widget's face. */
    public static final int SURFACE_DISABLED = 0xFF1C1F25;
    /** A row of the list under the mouse. */
    public static final int ROW_HOVER = 0xFF1E2129;

    public static final int BORDER = 0xFF3A404C;
    public static final int BORDER_HOVER = 0xFF5B6680;
    public static final int BORDER_DISABLED = 0xFF2A2E36;

    /** The colour of what is on, selected or filled. */
    public static final int ACCENT = 0xFF3F7FE8;
    public static final int ACCENT_HOVER = 0xFF5B95F5;
    public static final int ACCENT_DISABLED = 0xFF2C3E5C;

    /** The knob of a toggle or a slider. */
    public static final int KNOB = 0xFFEDEFF4;
    public static final int KNOB_DISABLED = 0xFF6A707C;

    /** The light and dark edges of a bevel, blended over the face. */
    public static final int BEVEL_LIGHT = 0x26FFFFFF;
    public static final int BEVEL_DARK = 0x40000000;

    public static final int TEXT = 0xFFE6E8EE;
    public static final int TEXT_DIM = 0xFF9AA1AE;
    public static final int TEXT_DISABLED = 0xFF5D6370;
    public static final int HEADING = 0xFFFFFFFF;
    /** A reset button whose setting differs from its default. */
    public static final int RESET = 0xFFE8A33F;

    public static final int TOOLTIP = 0xF0101217;
    public static final int TOOLTIP_BORDER = 0xFF3F7FE8;

    /** The space around and between widgets, in GUI pixels. */
    public static final int PADDING = 6;
    /** The height of a row of the list and of most widgets. */
    public static final int ROW_HEIGHT = 20;
    /** The height of a widget inside a row. */
    public static final int WIDGET_HEIGHT = 16;
    /** The duration of the hover animation, in milliseconds. */
    public static final int HOVER_MILLIS = 120;
    /** How long the mouse rests on a row before its tooltip shows, in milliseconds. */
    public static final int TOOLTIP_DELAY_MILLIS = 500;

    private Theme() {}
}
