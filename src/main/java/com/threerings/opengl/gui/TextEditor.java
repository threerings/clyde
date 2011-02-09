//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.text.Text;
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

        // TODO
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        // TODO
        return null;
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

        // TODO
    }

    @Override
    protected void clearGlyphs ()
    {
        _glyphs = null;
    }

    @Override
    protected int getPosition (int mouseX, int mouseY)
    {
        return 0;
    }

    @Override
    protected void setSelection (int cursorPos, int selectPos)
    {
        // TODO
    }

    /** Our lines of text. */
    protected Text[] _glyphs;
}
