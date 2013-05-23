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

import org.lwjgl.input.Keyboard;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.text.EditCommands;
import com.threerings.opengl.gui.text.KeyMap;
import com.threerings.opengl.gui.text.LengthLimitedDocument;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Displays and allows for the editing of a single line of text.
 */
public class TextField extends EditableTextComponent
{
    /**
     * Creates a blank text field.
     */
    public TextField (GlContext ctx)
    {
        this(ctx, "");
    }

    /**
     * Creates a blank text field with maximum input length.  The maximum input
     * length is controlled by a {@link LengthLimitedDocument}, changing the
     * document will remove the length control.
     */
    public TextField (GlContext ctx, int maxLength)
    {
        this(ctx, "", maxLength);
    }

    /**
     * Creates a text field with the specified starting text.
     */
    public TextField (GlContext ctx, String text)
    {
        this(ctx, text, 0);
    }

    /**
     * Creates a text field with the specified starting text and max length.
     * The maximum input length is controlled by a {@link
     * LengthLimitedDocument}, changing the document will remove the length
     * control.
     */
    public TextField (GlContext ctx, String text, int maxLength)
    {
        super(ctx, text, maxLength);
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/TextField";
    }

    @Override
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        Insets insets = getInsets();
        int lineHeight = getTextFactory().getHeight();

        // render the selection background if appropriate
        if (showCursor() && _cursx != _selx) {
            Background background = getSelectionBackground();
            if (background != null) {
                int cx = _cursx - _txoff;
                int sx = Math.min(Math.max(_selx - _txoff, 0),
                    _width - insets.getHorizontal() - 1);
                int x1 = Math.min(cx, sx), x2 = Math.max(cx, sx);
                background.render(
                    renderer, insets.left + x1, insets.bottom, x2 - x1 + 1, lineHeight, _alpha);
            }
        }

        // render our text
        if (_glyphs != null) {
            // clip the text to our visible text region
            Rectangle oscissor = intersectScissor(
                renderer, _srect,
                getAbsoluteX() + insets.left,
                getAbsoluteY() + insets.bottom,
                _width - insets.getHorizontal(),
                _height - insets.getVertical());
            try {
                _glyphs.render(renderer, insets.left - _txoff, insets.bottom, _alpha);

            } finally {
                renderer.setScissor(oscissor);
            }
        }

        // render the cursor if we have focus
        if (showCursor() && _cursx == _selx) {
            renderCursor(renderer, insets.left - _txoff + _cursx, insets.bottom, lineHeight);
        }
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension d = (_glyphs == null) ?
            new Dimension(0, getTextFactory().getHeight()) :
            new Dimension(_glyphs.getSize());
        if (_prefWidth != -1) {
            d.width = _prefWidth;
        }
        return d;
    }

    @Override
    protected boolean processCommand (int cmd)
    {
        switch (cmd) {
        default:
            return super.processCommand(cmd);

        case START_OF_LINE:
            setCursorPos(0);
            break;

        case END_OF_LINE:
            setCursorPos(_text.getLength());
            break;
        }

        return true;
    }

    @Override
    protected String validatePaste (String pasted)
    {
        // only paste up to the first newline
        int idx = pasted.indexOf('\n');
        return (idx == -1) ? pasted : pasted.substring(0, idx);
    }

    @Override
    protected boolean hasGlyphs ()
    {
        return (_glyphs != null);
    }

    @Override
    protected void createGlyphs ()
    {
        _glyphs = getTextFactory().createText(
            getDisplayText(), getColor(), UIConstants.PLAIN, UIConstants.DEFAULT_SIZE, null, true);
    }

    @Override
    protected void clearGlyphs ()
    {
        _glyphs = null;
    }

    @Override
    protected int getPosition (int mouseX, int mouseY)
    {
        return _glyphs.getHitPos(mouseX + _txoff, mouseY);
    }

    @Override
    protected void selectionWasSet ()
    {
        // compute the new screen positions
        if (_glyphs == null) {
            _cursx = _selx = 0;

        } else {
            _cursx = _glyphs.getCursorPos(_cursp);
            _selx = (_cursp == _selp) ? _cursx : _glyphs.getCursorPos(_selp);
        }

        // scroll our text left or right as necessary
        if (_cursx < _txoff) {
            _txoff = _cursx;

        } else if (_width > 0) { // make sure we're laid out
            int avail = getWidth() - getInsets().getHorizontal();
            if (_cursx > _txoff + avail) {
                _txoff = _cursx - avail;

            } else if (_glyphs != null && _glyphs.getSize().width - _txoff < avail) {
                _txoff = Math.max(0, _cursx - avail);
            }
        }
    }

    /** Our text glyphs. */
    protected Text _glyphs;

    protected int _cursx, _selx, _txoff;

    { // initializer
        // map return/enter to ACTION
        _keymap.addMapping(KeyMap.ANY_MODIFIER, Keyboard.KEY_RETURN, EditCommands.ACTION);
        _keymap.addMapping(KeyMap.ANY_MODIFIER, Keyboard.KEY_NUMPADENTER, EditCommands.ACTION);
    }
}
