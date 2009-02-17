//
// $Id$

package com.threerings.opengl.gui;

import java.awt.Color;

import java.util.Comparator;

import org.lwjgl.opengl.GL11;

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
{
    /** The action fired when the color selection changes. */
    public static final String SELECT = "select";

    /**
     * Creates a new color picker.
     *
     * @param colorClass the color class from which we select.
     * @param swatchWidth the width of the swatches.
     * @param swatchHeight the height of the swatches.
     */
    public ColorPicker (GlContext ctx, String colorClass, int swatchWidth, int swatchHeight)
    {
        super(ctx);
        setColorClass(colorClass);
        _swatchWidth = swatchWidth;
        _swatchHeight = swatchHeight;
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
     * Sets the color class from which we select.
     *
     * @param starters if true, only allow the starter colors.
     */
    public void setColorClass (String colorClass, boolean starters)
    {
        // retrieve and sort the colors
        ClassRecord crec = _ctx.getColorPository().getClassRecord(colorClass);
        if (crec == null) {
            _colors = new ColorRecord[0];
        } else {
            _colors = crec.colors.values().toArray(new ColorRecord[0]);
            QuickSort.sort(_colors, new Comparator<ColorRecord>() {
                public int compare (ColorRecord c1, ColorRecord c2) {
                    return c1.colorId - c2.colorId;
                }
            });
        }
        _starters = starters;

        // retrieve the colorized root colors
        _colorizedRoots = new Color4f[_colors.length];
        for (int ii = 0; ii < _colors.length; ii++) {
            _colorizedRoots[ii] = new Color4f(_colors[ii].getColorization().getColorizedRoot());
        }
    }

    /**
     * Sets the dimensions of the swatches.
     */
    public void setSwatchSize (int swatchWidth, int swatchHeight)
    {
        _swatchWidth = swatchWidth;
        _swatchHeight = swatchHeight;
    }

    /**
     * Sets the selected color record.
     */
    public void setSelectedColor (ColorRecord color)
    {
        setSelectedIndex(ListUtil.indexOf(_colors, color));
    }

    /**
     * Returns a reference to the selected color record.
     */
    public ColorRecord getSelectedColor ()
    {
        return _colors[_sidx];
    }

    /**
     * Sets the index of the selected color.
     */
    public void setSelectedIndex (int sidx)
    {
        _sidx = Math.min(Math.max(sidx, 0), _colors.length - 1);
    }

    /**
     * Returns the index of the selected color.
     */
    public int getSelectedIndex ()
    {
        return _sidx;
    }

    @Override // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            int oidx = _sidx;
            int midx = (mev.getX() - getAbsoluteX()) / _swatchWidth;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
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
                emitEvent(new ActionEvent(this, mev.getWhen(), mev.getModifiers(), SELECT));
            }
            return true;
        }

        return super.dispatchEvent(event);
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/ColorPicker";
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(_swatchWidth * _colors.length, _swatchHeight);
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        renderer.setTextureState(null);

        // render the swatches
        Insets insets = getInsets();
        int x = insets.left, y = insets.bottom;
        GL11.glBegin(GL11.GL_QUADS);
        for (int ii = 0; ii < _colors.length; ii++) {
            Color4f color = _colorizedRoots[ii];
            if (_starters && !_colors[ii].starter) {
                GL11.glColor4f(0.25f * _alpha, 0.25f * _alpha, 0.25f * _alpha, _alpha);
            } else {
                GL11.glColor4f(
                    color.r * _alpha, color.g * _alpha, color.b * _alpha, color.a * _alpha);
            }
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + _swatchWidth, y);
            GL11.glVertex2f(x + _swatchWidth, y + _swatchHeight);
            GL11.glVertex2f(x, y + _swatchHeight);
            x += _swatchWidth;
        }
        GL11.glEnd();

        // outline the selected swatch
        x = insets.left + (_sidx * _swatchWidth);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glColor4f(_alpha, _alpha, _alpha, _alpha);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + _swatchWidth, y);
        GL11.glVertex2f(x + _swatchWidth, y + _swatchHeight);
        GL11.glVertex2f(x, y + _swatchHeight);
        GL11.glEnd();
        GL11.glLineWidth(1f);
        renderer.invalidateColorState();
    }

    /** The colors available for selection. */
    protected ColorRecord[] _colors;

    /** Whether or not we're limited to starting colors. */
    protected boolean _starters;

    /** The colorized roots corresponding to each record. */
    protected Color4f[] _colorizedRoots;

    /** The dimensions of the swatches. */
    protected int _swatchWidth, _swatchHeight;

    /** The currently selected index. */
    protected int _sidx;
}
