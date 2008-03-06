//
// $Id$

package com.threerings.opengl.renderer.state;

import java.util.HashMap;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlUtil;

/**
 * Contains the color mask state.
 */
public class ColorMaskState extends RenderState
{
    /** All colors enabled. */
    public static final ColorMaskState ALL = getInstance(true, true, true, true);

    /** All colors disabled. */
    public static final ColorMaskState NONE = getInstance(false, false, false, false);

    /**
     * Returns a color mask state instance with the supplied parameters.  All requests for
     * instances with the same parameters will return the same object.
     */
    public static ColorMaskState getInstance (
        boolean red, boolean green, boolean blue, boolean alpha)
    {
        if (_instances == null) {
            _instances = new HashMap<Object, ColorMaskState>();
        }
        Object key = GlUtil.createKey(red, green, blue, alpha);
        ColorMaskState instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key, instance = new ColorMaskState(red, green, blue, alpha));
        }
        return instance;
    }

    /**
     * Creates a new color mask state with the supplied parameters.
     */
    public ColorMaskState (boolean red, boolean green, boolean blue, boolean alpha)
    {
        _red = red;
        _green = green;
        _blue = blue;
        _alpha = alpha;
    }

    /**
     * Checks whether the mask allows writing red values.
     */
    public boolean getRed ()
    {
        return _red;
    }

    /**
     * Checks whether the mask allows writing green values.
     */
    public boolean getGreen ()
    {
        return _green;
    }

    /**
     * Checks whether the mask allows writing blue values.
     */
    public boolean getBlue ()
    {
        return _blue;
    }

    /**
     * Checks whether the mask allows writing alpha values.
     */
    public boolean getAlpha ()
    {
        return _alpha;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return COLOR_MASK_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setColorMaskState(_red, _green, _blue, _alpha);
    }

    /** Whether each component is enabled for writing. */
    protected boolean _red, _green, _blue, _alpha;

    /** Shared instances. */
    protected static HashMap<Object, ColorMaskState> _instances;
}
