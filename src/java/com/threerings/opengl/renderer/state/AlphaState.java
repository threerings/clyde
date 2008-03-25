//
// $Id$

package com.threerings.opengl.renderer.state;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the alpha testing and blending state.
 */
public class AlphaState extends RenderState
{
    /** An alpha state for completely opaque objects. */
    public static final AlphaState OPAQUE =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_ONE, GL11.GL_ZERO);

    /** An alpha state for masked opaque objects. */
    public static final AlphaState MASKED =
        new AlphaState(GL11.GL_EQUAL, 1f, GL11.GL_ONE, GL11.GL_ZERO);

    /** An alpha state for translucent objects. */
    public static final AlphaState TRANSLUCENT =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    /** An alpha state for additive objects. */
    public static final AlphaState ADDITIVE =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_ONE, GL11.GL_ONE);

    /** An alpha state for objects with premultiplied alpha. */
    public static final AlphaState PREMULTIPLIED =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

    /**
     * Returns an alpha state that only passes fragments with alpha values greater than or equal to
     * the supplied value, then blends them with the destination.  All requests for instances with
     * the same value will return the same object.
     */
    public static AlphaState getTestInstance (float reference)
    {
        if (reference == 0f) {
            return PREMULTIPLIED;
        } else if (reference == 1f) {
            return MASKED;
        }
        AlphaState instance = _testInstances.get(reference);
        if (instance == null) {
            _testInstances.put(reference, instance = new AlphaState(
                GL11.GL_GEQUAL, reference, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA));
        }
        return instance;
    }

    /**
     * Creates a new alpha state.
     */
    public AlphaState (
        int alphaTestFunc, float alphaTestRef, int srcBlendFactor, int destBlendFactor)
    {
        _alphaTestFunc = alphaTestFunc;
        _alphaTestRef = alphaTestRef;
        _srcBlendFactor = srcBlendFactor;
        _destBlendFactor = destBlendFactor;
    }

    /**
     * Returns the alpha test function.
     */
    public int getAlphaTestFunc ()
    {
        return _alphaTestFunc;
    }

    /**
     * Returns the alpha test reference value.
     */
    public float getAlphaTestRef ()
    {
        return _alphaTestRef;
    }

    /**
     * Returns the source blend factor.
     */
    public int getSrcBlendFactor ()
    {
        return _srcBlendFactor;
    }

    /**
     * Returns the destination blend factor.
     */
    public int getDestBlendFactor ()
    {
        return _destBlendFactor;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return ALPHA_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setAlphaState(_alphaTestFunc, _alphaTestRef, _srcBlendFactor, _destBlendFactor);
    }

    /** The alpha test function. */
    protected int _alphaTestFunc;

    /** The reference value for alpha testing. */
    protected float _alphaTestRef;

    /** The source blend factor. */
    protected int _srcBlendFactor;

    /** The destination blend factor. */
    protected int _destBlendFactor;

    /** Shared instances mapped by test threshold. */
    protected static HashMap<Float, AlphaState> _testInstances = new HashMap<Float, AlphaState>();
}
