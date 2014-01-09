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

package com.threerings.opengl.gui.config;

import java.awt.Font;

import java.util.HashMap;
import java.util.HashSet;

import com.samskivert.util.IntTuple;
import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.text.CharacterTextFactory;
import com.threerings.opengl.gui.text.TextFactory;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.gui.Log.log;

/**
 * Describes a font.
 */
public class FontConfig extends ManagedConfig
{
    /** An object to use when the config cannot be resolved. */
    public static final FontConfig NULL = new FontConfig();

    /** The available font styles. */
    public enum Style
    {
        /** The plain font style. */
        PLAIN(Font.PLAIN),

        /** The bold font style. */
        BOLD(Font.BOLD),

        /** The italic font style. */
        ITALIC(Font.ITALIC),

        /** The bold/italic font style. */
        BOLD_ITALIC(Font.BOLD | Font.ITALIC);

        /**
         * Returns the AWT flags corresponding to the style.
         */
        public int getFlags ()
        {
            return _flags;
        }

        Style (int flags)
        {
            _flags = flags;
        }

        /** The AWT flags corresponding to the style. */
        protected final int _flags;
    }

    @EditorTypes({ FontConfig.Default.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Returns the text factory for this font with the specified style and point size.
         */
        public abstract TextFactory getTextFactory (GlContext ctx, int style, int size);

        /**
         * Invalidate stored cache.
         */
        public abstract void invalidate();

        /**
         * Add resources to monitor.
         */
        public abstract void getUpdateResources (HashSet<String> paths);

        /**
         * Adjusts the line spacing for the font.
         */
        public abstract int adjustSpacing (int spacing);
    }

    public static class Default extends Implementation
    {
        /** The font file. */
        @Editable(editor="resource", nullable=true)
        @FileConstraints(
            description="m.font_files_desc",
            extensions={".ttf", ".ttc"},
            directory="font_dir")
        public String file;

        /** Whether or not the font should be antialiased. */
        @Editable
        public boolean antialias = true;

        /** A base style for the font. */
        @Editable
        public Style baseStyle = Style.PLAIN;

        /** A descent modifier as percentage of height. */
        @Editable(min=-1, step=0.01, hgroup="m")
        public float descentModifier = 0f;

        /** A height modifier. */
        @Editable(hgroup="m")
        public int heightModifier = 0;

        /** A size modifier. */
        @Editable(hgroup="m")
        public int sizeModifier = 0;

        /** If the font allows negative spacing. */
        @Editable(hgroup="m")
        public boolean allowNegativeSpacing = true;

        @Override
        public TextFactory getTextFactory (GlContext ctx, int style, int size)
        {
            return CharacterTextFactory.getInstance(
                    getFont(ctx, style, size), antialias, descentModifier, heightModifier);
        }

        @Override
        public void invalidate()
        {
            _fonts.clear();
        }

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override
        public int adjustSpacing (int spacing)
        {
            return allowNegativeSpacing ? spacing : Math.max(0, spacing);
        }

        /**
         * Returns the cached font with the specified style and size.
         */
        protected Font getFont (GlContext ctx, int style, int size)
        {
            IntTuple key = new IntTuple(style, size);
            Font font = _fonts.get(key);
            if (font == null) {
                _fonts.put(key, font = createFont(ctx, style, size));
            }
            return font;
        }

        /**
         * Creates the font with the specified style and size.
         */
        protected Font createFont (GlContext ctx, int style, int size)
        {
            if (style != Font.PLAIN || size != 1) {
                style |= baseStyle.getFlags();
                return getFont(ctx, Font.PLAIN, 1).deriveFont(style, size + sizeModifier);
            }
            if (file != null) {
                try {
                    return Font.createFont(
                        Font.TRUETYPE_FONT, ctx.getResourceManager().getResource(file));
                } catch (Exception e) { // FontFormatException, IOException
                    log.warning("Failed to load font file.", "file", file, e);
                }
            }
            return new Font("Dialog", Font.PLAIN, 1);
        }

        /** Cached font instances. */
        @DeepOmit
        protected transient HashMap<IntTuple, Font> _fonts = new HashMap<IntTuple, Font>();
    }

    /** The actual font implementation. */
    @Editable
    public Implementation implementation = new Default();

    /**
     * Returns the text factory for this font with the specified style and point size.
     */
    public TextFactory getTextFactory (GlContext ctx, Style style, int size)
    {
        return getTextFactory(ctx, style.getFlags(), size);
    }

    /**
     * Returns the text factory for this font with the specified style and point size.
     */
    public TextFactory getTextFactory (GlContext ctx, int style, int size)
    {
        return implementation.getTextFactory(ctx, style, size);
    }

    /**
     * Return a valid line spacing for the font.
     */
    public int adjustSpacing (int spacing)
    {
        return implementation.adjustSpacing(spacing);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }
}
