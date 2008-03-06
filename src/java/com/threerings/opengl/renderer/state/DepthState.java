//
// $Id$

package com.threerings.opengl.renderer.state;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.Tuple;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the depth buffer testing/writing state.
 */
public class DepthState extends RenderState
{
    /** A depth state for testing, but not writing to, the depth buffer. */
    public static final DepthState TEST = getInstance(GL11.GL_LEQUAL, false);

    /** A depth state for writing to, but not testing, the depth buffer. */
    public static final DepthState WRITE = getInstance(GL11.GL_ALWAYS, true);

    /** A depth state for testing and writing to the depth buffer. */
    public static final DepthState TEST_WRITE = getInstance(GL11.GL_LEQUAL, true);

    /** A depth state for neither testing nor writing to the depth buffer. */
    public static final DepthState DISABLED = getInstance(GL11.GL_ALWAYS, false);

    /**
     * Returns a depth state instance with the supplied parameters.  All requests for instances
     * with the same parameters will return the same object.
     */
    public static DepthState getInstance (int depthTestFunc, boolean depthMask)
    {
        if (_instances == null) {
            _instances = new HashMap<Tuple<Integer, Boolean>, DepthState>();
        }
        Tuple<Integer, Boolean> key = new Tuple<Integer, Boolean>(depthTestFunc, depthMask);
        DepthState instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key, instance = new DepthState(depthTestFunc, depthMask));
        }
        return instance;
    }

    /**
     * Creates a new depth state with the supplied parameters.
     */
    public DepthState (int depthTestFunc, boolean depthMask)
    {
        _depthTestFunc = depthTestFunc;
        _depthMask = depthMask;
    }

    /**
     * Returns the depth test function.
     */
    public int getDepthTestFunc ()
    {
        return _depthTestFunc;
    }

    /**
     * Returns the depth mask value.
     */
    public boolean getDepthMask ()
    {
        return _depthMask;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return DEPTH_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setDepthState(_depthTestFunc, _depthMask);
    }

    /** The depth test function. */
    protected int _depthTestFunc;

    /** Whether or not depth-writing is enabled. */
    protected boolean _depthMask;

    /** Shared instances. */
    protected static HashMap<Tuple<Integer, Boolean>, DepthState> _instances;
}
