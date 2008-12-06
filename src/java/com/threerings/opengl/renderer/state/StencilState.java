//
// $Id$

package com.threerings.opengl.renderer.state;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.threerings.util.ArrayKey;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the stencil state.
 */
public class StencilState extends RenderState
{
    /** Disables stencil testing/writing. */
    public static final StencilState DISABLED = getInstance(
        GL11.GL_ALWAYS, 0, 0x7FFFFFFF, GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP, 0x7FFFFFFF);

    /**
     * Returns a stencil state instance with the supplied parameters.  All requests for
     * instances with the same parameters will return the same object.
     */
    public static StencilState getInstance (
        int stencilTestFunc, int stencilTestRef, int stencilTestMask,
        int stencilFailOp, int stencilDepthFailOp, int stencilPassOp,
        int stencilWriteMask)
    {
        if (_instances == null) {
            _instances = new HashMap<ArrayKey, StencilState>();
        }
        ArrayKey key = new ArrayKey(
            stencilTestFunc, stencilTestRef, stencilTestMask,
            stencilFailOp, stencilDepthFailOp, stencilPassOp,
            stencilWriteMask);
        StencilState instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key, instance = new StencilState(
                stencilTestFunc, stencilTestRef, stencilTestMask,
                stencilFailOp, stencilDepthFailOp, stencilPassOp,
                stencilWriteMask));
        }
        return instance;
    }

    /**
     * Creates a new stencil state with the supplied parameters.
     */
    public StencilState (
        int stencilTestFunc, int stencilTestRef, int stencilTestMask,
        int stencilFailOp, int stencilDepthFailOp, int stencilPassOp,
        int stencilWriteMask)
    {
        _stencilTestFunc = stencilTestFunc;
        _stencilTestRef = stencilTestRef;
        _stencilTestMask = stencilTestMask;
        _stencilFailOp = stencilFailOp;
        _stencilDepthFailOp = stencilDepthFailOp;
        _stencilPassOp = stencilPassOp;
        _stencilWriteMask = stencilWriteMask;
    }

    /**
     * Returns the stencil test function.
     */
    public int getStencilTestFunc ()
    {
        return _stencilTestFunc;
    }

    /**
     * Returns the stencil test reference value.
     */
    public int getStencilTestRef ()
    {
        return _stencilTestRef;
    }

    /**
     * Returns the stencil test mask.
     */
    public int getStencilTestMask ()
    {
        return _stencilTestMask;
    }

    /**
     * Returns the action to take when the fragment fails the stencil test.
     */
    public int getStencilFailOp ()
    {
        return _stencilFailOp;
    }

    /**
     * Returns the action to take when the fragment passes the stencil test, but fails the depth
     * test.
     */
    public int getStencilDepthFailOp ()
    {
        return _stencilDepthFailOp;
    }

    /**
     * Returns the action to take when the fragment passes both the stencil test and the depth
     * test.
     */
    public int getStencilPassOp ()
    {
        return _stencilPassOp;
    }

    /**
     * Returns the stencil write mask.
     */
    public int getStencilWriteMask ()
    {
        return _stencilWriteMask;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return STENCIL_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setStencilState(
            _stencilTestFunc, _stencilTestRef, _stencilTestMask,
            _stencilFailOp, _stencilDepthFailOp, _stencilPassOp,
            _stencilWriteMask);
    }

    /** The stencil test function. */
    protected int _stencilTestFunc;

    /** The reference value for stencil testing. */
    protected int _stencilTestRef;

    /** The mask applied to both the reference value and the stencil value when testing. */
    protected int _stencilTestMask;

    /** The operation to take when the incoming value fails the stencil test. */
    protected int _stencilFailOp;

    /** The operation to take when the incoming value passes the stencil test but fails the
     * depth test. */
    protected int _stencilDepthFailOp;

    /** The operation to take when the incoming value passes both the stencil test and the
     * depth test. */
    protected int _stencilPassOp;

    /** The stencil write mask. */
    protected int _stencilWriteMask;

    /** Shared instances. */
    protected static HashMap<ArrayKey, StencilState> _instances;
}
