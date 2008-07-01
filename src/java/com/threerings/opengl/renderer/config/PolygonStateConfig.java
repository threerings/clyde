//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.PolygonState;

/**
 * Configurable polygon state.
 */
public class PolygonStateConfig extends DeepObject
    implements Exportable
{
    /** Polygon mode constants. */
    public enum Mode
    {
        POINT(GL11.GL_POINT),
        LINE(GL11.GL_LINE),
        FILL(GL11.GL_FILL);

        public int getConstant ()
        {
            return _constant;
        }

        Mode (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** The front polygon mode. */
    @Editable(hgroup="m")
    public Mode frontMode = Mode.FILL;

    /** The back polygon mode. */
    @Editable(hgroup="m")
    public Mode backMode = Mode.FILL;

    /** The proportional polygon offset. */
    @Editable(step=0.01, hgroup="o")
    public float offsetFactor;

    /** The constant polygon offset. */
    @Editable(hgroup="o")
    public float offsetUnits;

    /** If true, do not use a shared instance. */
    @Editable
    public boolean uniqueInstance;

    /**
     * Returns the corresponding polygon state.
     */
    public PolygonState getState ()
    {
        return uniqueInstance ?
            new PolygonState(
                frontMode.getConstant(), backMode.getConstant(), offsetFactor, offsetUnits) :
            PolygonState.getInstance(
                frontMode.getConstant(), backMode.getConstant(), offsetFactor, offsetUnits);
    }
}
