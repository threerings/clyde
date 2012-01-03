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

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Point;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * A multiline text-editing widget.
 */
public class TextEditor extends EditableTextComponent
{
    /**
     * Constructor.
     */
    public TextEditor (GlContext ctx)
    {
        this(ctx, "");
    }

    /**
     * Constructor.
     */
    public TextEditor (GlContext ctx, String text)
    {
        super(ctx, text, 0);
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/TextEditor";
    }

    @Override
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        Insets insets = getInsets();
        int lineHeight = getTextFactory().getHeight();

        // draw the selection background if there is a selection
        if (showCursor() && (_cursp != _selp)) {
            Background bkg = getSelectionBackground();
            if (bkg != null) {
                // TODO Pre-calculate a List of rectangles when the selection is set?
                int startP = Math.min(_cursp, _selp);
                int endP = Math.max(_cursp, _selp);
                Point startLoc = (startP == _cursp) ? _curs : _sel;
                Point endLoc = (startP == _cursp) ? _sel : _curs;
                if (startLoc.y == endLoc.y) {
                    bkg.render(renderer,
                        insets.left + startLoc.x, insets.bottom + startLoc.y,
                        endLoc.x - startLoc.x + 1, lineHeight, _alpha);

                } else {
                    int idx = ((_height - insets.getVertical() - startLoc.y) / lineHeight) - 1;
                    bkg.render(renderer,
                        insets.left + startLoc.x, insets.bottom + startLoc.y,
                        _glyphs[idx].getSize().width - startLoc.x + 1, lineHeight, _alpha);
                    for (int y = startLoc.y - lineHeight; y > endLoc.y; y -= lineHeight) {
                        bkg.render(renderer,
                            insets.left, insets.bottom + y,
                            _glyphs[++idx].getSize().width + 1, lineHeight, _alpha);
                    }
                    bkg.render(renderer,
                        insets.left, insets.bottom + endLoc.y,
                        endLoc.x + 1, lineHeight, _alpha);
                }
            }
        }

        // draw the glyphs
        if (_glyphs != null) {
            Rectangle oscissor = intersectScissor(
                renderer, _srect,
                getAbsoluteX() + insets.left,
                getAbsoluteY() + insets.bottom,
                _width - insets.getHorizontal(),
                _height - insets.getVertical());
            try {
                int x = insets.left;
                int y = _height - insets.top - lineHeight;
                for (Text text : _glyphs) {
                    text.render(renderer, x, y, _alpha);
                    y -= lineHeight;
                }
            } finally {
                renderer.setScissor(oscissor);
            }
        }

        // draw the cursor
        if (showCursor() && (_cursp == _selp)) {
            renderCursor(renderer, insets.left + _curs.x, insets.bottom + _curs.y, lineHeight);
        }
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Insets insets = getInsets();
        int lineHeight = getTextFactory().getHeight();
        int width = (_prefWidth == -1) ? insets.getHorizontal() : _prefWidth;
        return new Dimension(width, (_lines * lineHeight) + insets.getVertical());
    }

    @Override
    protected boolean processCommand (int cmd)
    {
        switch (cmd) {
        default:
            return super.processCommand(cmd);

        case CURSOR_UP:
        case CURSOR_DOWN:
        case START_OF_LINE:
        case END_OF_LINE:
            int lineHeight = getTextFactory().getHeight();
            int x = _curs.x;
            int y = _curs.y + (lineHeight/2);
            switch (cmd) {
            case CURSOR_UP:
                y += lineHeight;
                break;

            case CURSOR_DOWN:
                y -= lineHeight;
                break;

            case START_OF_LINE:
                x = 0;
                break;

            case END_OF_LINE:
                x = Integer.MAX_VALUE;
                break;
            }
            setCursorPos(getPosition(x, y));
            break;
        }

        return true;
    }

    @Override
    protected void recreateGlyphs ()
    {
        super.recreateGlyphs();

        // our preferred size may have changed
        int lines = (_glyphs == null) ? 1 : _glyphs.length;
        if (_lines != lines) {
            _lines = lines;
            invalidate();
        }
    }

    @Override
    protected boolean hasGlyphs ()
    {
        return (_glyphs != null);
    }

    @Override
    protected void createGlyphs ()
    {
        _glyphs = getTextFactory().wrapText(
            getDisplayText(), getColor(), UIConstants.PLAIN, UIConstants.DEFAULT_SIZE, null,
            _width - getInsets().getHorizontal());
    }

    @Override
    protected void clearGlyphs ()
    {
        _glyphs = null;
    }

    @Override
    protected int getPosition (int mouseX, int mouseY)
    {
        int y = _height - getInsets().getVertical();
        if (mouseY > y) {
            return 0;
        }
        int lineHeight = getTextFactory().getHeight();
        int pos = 0;
        String docText = _text.getText();
        for (Text text : _glyphs) {
            y -= lineHeight;
            if (mouseY > y) {
                return pos + text.getHitPos(mouseX, mouseY - y);

            } else {
                pos += text.getLength();
                if (pos < docText.length() && Character.isWhitespace(docText.charAt(pos))) {
                    pos++;
                }
            }
        }
        return Math.min(pos, _text.getText().length());
    }

    @Override
    protected void selectionWasSet ()
    {
        calculatePosition(_cursp, _curs);
        if (_selp == _cursp) {
            _sel.set(_curs); // no need to recalculate

        } else {
            calculatePosition(_selp, _sel);
        }

        // scroll so that the cursor is visible (plus some)
        if (showCursor()) {
            Insets insets = getInsets();
            int lineHeight = getTextFactory().getHeight();
            scrollRectToVisible(
                // _curs needs to be offset by insets, but we pad it with our insets so we
                // subtract them back out again, giving the plain value...
                // (_curs.x + insets.bottom) - insets.bottom
                _curs.x, _curs.y,
                insets.getHorizontal() + 1, lineHeight + insets.getVertical() + 1);
        }
    }

    /**
     * Calculate the inset coordinates of the specified position, populating the specified point.
     */
    protected void calculatePosition (int pos, Point loc)
    {
        int lineHeight = getTextFactory().getHeight();
        int y = _height - getInsets().getVertical() - lineHeight;
        if (_glyphs != null) {
            String docText = _text.getText();
            int docPos = 0;
            for (Text text : _glyphs) {
                int len = text.getLength();
                if (pos <= len) {
                    loc.set(text.getCursorPos(pos), y);
                    return;
                }
                docPos += len;
                pos -= len;
                if (docPos < docText.length() && Character.isWhitespace(docText.charAt(docPos))) {
                    docPos++;
                    pos--;
                }
                y -= lineHeight;
            }
        }
        loc.set(0, y);
    }

    /** Our lines of text. */
    protected Text[] _glyphs;

    /** The number of lines of glyphs we have, or 1. */
    protected int _lines = 1;

    /** A mutable Point containing the inset coordiantes of the cursor. */
    protected Point _curs = new Point();

    /** A mutable Point containing the inset coordinates of the end of selection. */
    protected Point _sel = new Point();
}
