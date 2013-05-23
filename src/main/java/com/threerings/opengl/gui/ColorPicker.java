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

import java.util.Collection;
import java.util.Comparator;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import com.samskivert.util.ListUtil;
import com.samskivert.util.QuickSort;

import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.ColorPository.ColorRecord;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

/**
 * Allows the selection of a single color from a set of swatches.
 */
public class ColorPicker extends Component
    implements Selectable<ColorRecord>
{
    /**
     * Creates a new color picker.
     *
     * @param colorClass the color class from which we select.
     */
    public ColorPicker (GlContext ctx, String colorClass)
    {
        super(ctx);
        setColorClass(colorClass);
    }

    /**
     * Creates an uninitialized color picker.
     */
    public ColorPicker (GlContext ctx)
    {
        super(ctx);
    }

    /**
     * Sets the color class from which we select.
     */
    public void setColorClass (String colorClass)
    {
        setColorClass(colorClass, false);
    }

    /**
     * Gets the color class from this color picker.
     */
    public String getColorClass ()
    {
        return _colorClass;
    }

    /**
     * Sets the color class from which we select.
     *
     * @param starters if true, only allow the starter colors.
     */
    public void setColorClass (String colorClass, boolean starters)
    {
        _colorClass = colorClass;

        // retrieve and sort the colors
        ClassRecord crec = _ctx.getColorPository().getClassRecord(colorClass);
        if (crec == null) {
            _colors = new ColorRecord[0];
        } else {
            Collection<ColorRecord> colors = crec.colors.values();
            if (starters) {
                colors = Collections2.filter(colors, new Predicate<ColorRecord>() {
                    public boolean apply (ColorRecord color) {
                        return color.starter;
                    }
                });
            }
            _colors = colors.toArray(new ColorRecord[colors.size()]);
            QuickSort.sort(_colors, new Comparator<ColorRecord>() {
                public int compare (ColorRecord c1, ColorRecord c2) {
                    return c1.colorId - c2.colorId;
                }
            });
        }

        // retrieve the colorized root colors
        _colorizedRoots = new Color4f[_colors.length];
        for (int ii = 0; ii < _colors.length; ii++) {
            _colorizedRoots[ii] = new Color4f(_colors[ii].getColorization().getColorizedRoot());
        }
    }

    // from Selectable<ColorRecord>
    public ColorRecord getSelected ()
    {
        return _colors[_sidx];
    }

    // from Selectable<ColorRecord>
    public void setSelected (ColorRecord color)
    {
        setSelectedIndex(ListUtil.indexOf(_colors, color));
    }

    // from Selectable<ColorRecord>
    public void setSelectedIndex (int sidx)
    {
        _sidx = Math.min(Math.max(sidx, 0), _colors.length - 1);
    }

    // from Selectable<ColorRecord>
    public int getSelectedIndex ()
    {
        return _sidx;
    }

    @Override
    public boolean dispatchEvent (Event event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            int oidx = _sidx;
            float swatchWidth = (float)getWidth() / _colors.length;
            int midx = (int)((float)(mev.getX() - getAbsoluteX()) / swatchWidth);
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == MouseEvent.BUTTON1) {
                    setSelectedIndex(midx);
                }
                break;

            case MouseEvent.MOUSE_DRAGGED:
                setSelectedIndex(midx);
                break;

            default:
                return super.dispatchEvent(event);
            }
            if (_sidx != oidx) {
                emitEvent(new ActionEvent(this, mev.getWhen(), mev.getModifiers(),
                    SELECT, getSelected()));
            }
            return true;
        }

        return super.dispatchEvent(event);
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/ColorPicker";
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(_colors.length, 1);
    }

    @Override
    protected void renderComponent (Renderer renderer)
    {
        renderer.setTextureState(null);
        float swatchWidth = (float)getWidth() / _colors.length;
        int height = getHeight();

        // render the swatches
        Insets insets = getInsets();
        float x = insets.left, y = insets.bottom;
        GL11.glBegin(GL11.GL_QUADS);
        for (int ii = 0; ii < _colors.length; ii++) {
            Color4f color = _colorizedRoots[ii];
            GL11.glColor4f(color.r * _alpha, color.g * _alpha, color.b * _alpha, color.a * _alpha);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + swatchWidth, y);
            GL11.glVertex2f(x + swatchWidth, y + height);
            GL11.glVertex2f(x, y + height);
            x += swatchWidth;
        }
        GL11.glEnd();

        // outline the selected swatch
        x = insets.left + (_sidx * swatchWidth);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glColor4f(_alpha, _alpha, _alpha, _alpha);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + swatchWidth, y);
        GL11.glVertex2f(x + swatchWidth, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glLineWidth(1f);
        renderer.invalidateColorState();
    }

    /** The colors available for selection. */
    protected ColorRecord[] _colors;

    /** The colorized roots corresponding to each record. */
    protected Color4f[] _colorizedRoots;

    /** The color class. */
    protected String _colorClass;

    /** The currently selected index. */
    protected int _sidx;
}
