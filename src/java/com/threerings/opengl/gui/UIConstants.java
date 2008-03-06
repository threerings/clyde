//
// $Id$

package com.threerings.opengl.gui;

/**
 * Codes and constants shared by the UI components.
 */
public interface UIConstants
{
    /** An alignment constant. */
    public static final int LEFT = 0;

    /** An alignment constant. */
    public static final int RIGHT = 1;

    /** An alignment constant. */
    public static final int CENTER = 2;

    /** An alignment constant. */
    public static final int TOP = 0;

    /** An alignment constant. */
    public static final int BOTTOM = 1;

    /** An orientation constant. */
    public static final int HORIZONTAL = 0;

    /** An orientation constant. */
    public static final int VERTICAL = 1;

    /** A special orientation constant for labels. */
    public static final int OVERLAPPING = 2;

    /** A code for text with no effects.*/
    public static final int NORMAL = 0;

    /** A code for text with a single pixel outline.*/
    public static final int OUTLINE = 1;

    /** A code for text with a single pixel drop shadow.*/
    public static final int SHADOW = 2;

    /** A code for text with no effect and no styling.*/
    public static final int PLAIN = 3;

    /** A code for text with glow effect. */
    public static final int GLOW = 4;

    /** The default text effect size. */
    public static final int DEFAULT_SIZE = 1;

    /** The default text line spacing. */
    public static final int DEFAULT_SPACING = 0;
}
