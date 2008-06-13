//
// $Id$

package com.threerings.opengl.renderer.state;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the polygon state.
 */
public class PolygonState extends RenderState
{
    /** The default state. */
    public static final PolygonState DEFAULT =
        new PolygonState(GL11.GL_FILL, GL11.GL_FILL, 0f, 0f);

    /** Simple wireframe state. */
    public static final PolygonState WIREFRAME =
        new PolygonState(GL11.GL_LINE, GL11.GL_LINE, 0f, 0f);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static PolygonState getInstance (
        int frontPolygonMode, int backPolygonMode, float polygonOffsetFactor,
        float polygonOffsetUnits)
    {
        return getInstance(new PolygonState(
            frontPolygonMode, backPolygonMode, polygonOffsetFactor, polygonOffsetUnits));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static PolygonState getInstance (PolygonState state)
    {
        if (state.equals(DEFAULT)) {
            return DEFAULT;
        } else if (state.equals(WIREFRAME)) {
            return WIREFRAME;
        } else {
            return state;
        }
    }

    /**
     * Creates a new polygon state.
     */
    public PolygonState (
        int frontPolygonMode, int backPolygonMode, float polygonOffsetFactor,
        float polygonOffsetUnits)
    {
        _frontPolygonMode = frontPolygonMode;
        _backPolygonMode = backPolygonMode;
        _polygonOffsetFactor = polygonOffsetFactor;
        _polygonOffsetUnits = polygonOffsetUnits;
    }

    /**
     * Returns the front-facing polygon mode.
     */
    public int getFrontPolygonMode ()
    {
        return _frontPolygonMode;
    }

    /**
     * Returns the back-facing polygon mode.
     */
    public int getBackPolygonMode ()
    {
        return _backPolygonMode;
    }

    /**
     * Returns the proportional polygon offset.
     */
    public float getPolygonOffsetFactor ()
    {
        return _polygonOffsetFactor;
    }

    /**
     * Returns the constant polygon offset.
     */
    public float getPolygonOffsetUnits ()
    {
        return _polygonOffsetUnits;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return POLYGON_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setPolygonState(
            _frontPolygonMode, _backPolygonMode, _polygonOffsetFactor, _polygonOffsetUnits);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        PolygonState ostate;
        return other instanceof PolygonState &&
            _frontPolygonMode == (ostate = (PolygonState)other)._frontPolygonMode &&
            _backPolygonMode == ostate._backPolygonMode &&
            _polygonOffsetFactor == ostate._polygonOffsetFactor &&
            _polygonOffsetUnits == ostate._polygonOffsetUnits;
    }

    /** The front polygon mode. */
    protected int _frontPolygonMode;

    /** The back polygon mode. */
    protected int _backPolygonMode;

    /** The proportional polygon offset. */
    protected float _polygonOffsetFactor;

    /** The constant polygon offset. */
    protected float _polygonOffsetUnits;
}

