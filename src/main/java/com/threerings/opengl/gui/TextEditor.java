//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Dimension;

/**
 * A multiline text-editing widget.
 */
public class TextEditor extends EditableTextComponent
{
    public TextEditor (GlContext ctx)
    {
        this(ctx, "");
    }

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
        int x = insets.left;
        int y = _height - insets.top - lineHeight;

        if (_showCursor && (_cursp != _selp)) {
            // TODO: background
        }

        if (_glyphs != null) {
            // TODO: scissoring
            for (Text text : _glyphs) {
                text.render(renderer, x, y, _alpha);
                y -= lineHeight;
            }
        }

        if (_showCursor && (_cursp == _selp)) {
            renderCursor(renderer, insets.left + _cursx, insets.bottom + _cursy, lineHeight);
        }
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Insets insets = getInsets();
        int height = getTextFactory().getHeight();
        int width = (_prefWidth == -1) ? insets.getHorizontal() : _prefWidth;
        return (_glyphs == null)
            ? new Dimension(width, height)
            : new Dimension(width, (height * _glyphs.length) + insets.getVertical());
    }

    @Override
    protected boolean hasGlyphs ()
    {
        return (_glyphs != null);
    }

    @Override
    protected void recreateGlyphs ()
    {
        clearGlyphs();

        // if we have no text, clear our internal markers
        if (_text.getLength() == 0) {
            _cursp = _selp = 0;
            _cursx = _cursy = _selx = _sely = 0;
            return;
        }

        Insets insets = getInsets();
        _glyphs = getTextFactory().wrapText(
            getDisplayText(), getColor(), UIConstants.PLAIN, UIConstants.DEFAULT_SIZE, null,
            _width - insets.getHorizontal());
        setSelection(_cursp, _selp);
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
        for (Text text : _glyphs) {
            y -= lineHeight;
            if (mouseY > y) {
                return pos + text.getHitPos(mouseX, mouseY - y);
            } else {
                pos += text.getLength();
            }
        }
        return pos;
    }

    @Override
    protected void selectionWasSet ()
    {
        if (_glyphs == null) {
            _cursx = _cursy = _selx = _sely = 0;

        } else {
            int pos = _cursp;
            int lineHeight = getTextFactory().getHeight();
            int y = _height - getInsets().getVertical() - lineHeight;
            boolean set = false;
            for (Text text : _glyphs) {
                int len = text.getLength();
                if (pos < len) {
                    _cursx = text.getCursorPos(pos);
                    _cursy = y;
                    set = true;
                    break;
                }
                pos -= len;
                y -= lineHeight;
            }
            if (!set) {
                _cursx = 0;
                _cursy = y;
            }
        }
    }

    /** Our lines of text. */
    protected Text[] _glyphs;

    protected int _cursx, _cursy, _selx, _sely;
}
