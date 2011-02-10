//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Point;

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
            renderCursor(renderer, insets.left + _curs.x, insets.bottom + _curs.y, lineHeight);
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
    protected boolean processCommand (int cmd)
    {
        switch (cmd) {
        default:
            return super.processCommand(cmd);

        case CURSOR_UP:
            setCursorPos(getPosition(_curs.x, _curs.y + getTextFactory().getHeight()));
            break;

        case CURSOR_DOWN:
            setCursorPos(getPosition(_curs.x, _curs.y - getTextFactory().getHeight()));
            break;
        }

        return true;
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
        for (Text text : _glyphs) {
            y -= lineHeight;
            if (mouseY > y) {
                return pos + text.getHitPos(mouseX, mouseY - y);

            } else {
                pos += text.getLength() + 1;
            }
        }
        return pos;
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
    }

    /**
     * Calculate the inset coordinates of the specified position, populating the specified point.
     */
    protected void calculatePosition (int pos, Point loc)
    {
        int lineHeight = getTextFactory().getHeight();
        int y = _height - getInsets().getVertical() - lineHeight;
        if (_glyphs != null) {
            for (Text text : _glyphs) {
                int len = text.getLength();
                if (pos <= len) {
                    loc.set(text.getCursorPos(pos), y);
                    return;
                }
                pos -= len + 1;
                y -= lineHeight;
            }
        }
        loc.set(0, y);
    }

    /** Our lines of text. */
    protected Text[] _glyphs;

    /** A mutable Point containing the inset coordiantes of the cursor. */
    protected Point _curs = new Point();

    /** A mutable Point containing the inset coordinates of the end of selection. */
    protected Point _sel = new Point();
}
