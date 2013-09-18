//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
