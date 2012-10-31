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

package com.threerings.opengl.scene;

import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.SimpleTransformable;

/**
 * Extends {@link SimpleTransformable} and provides a basic implementation of the
 * {@link SceneElement} interface.
 */
public abstract class SimpleSceneElement extends SimpleTransformable
    implements SceneElement
{
    /**
     * Creates a new scene element.
     */
    public SimpleSceneElement (GlContext ctx)
    {
        this(ctx, RenderQueue.OPAQUE);
    }

    /**
     * Creates a new simple scene element.
     *
     * @param queue the name of the queue into which we place the batch.
     */
    public SimpleSceneElement (GlContext ctx, String queue)
    {
        this(ctx, queue, 0);
    }

    /**
     * Creates a new simple scene element.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     */
    public SimpleSceneElement (GlContext ctx, String queue, int priority)
    {
        this(ctx, queue, priority, false, 0);
    }

    /**
     * Creates a new simple scene element.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleSceneElement (
        GlContext ctx, String queue, int priority, boolean modifiesColorState, int primitiveCount)
    {
        super(ctx, queue, priority, modifiesColorState, primitiveCount);
    }

    /**
     * Sets the element's tick policy.
     */
    public void setTickPolicy (TickPolicy policy)
    {
        if (_tickPolicy == policy) {
            return;
        }
        if (_scene != null) {
            _scene.tickPolicyWillChange(this);
        }
        _tickPolicy = policy;
        if (_scene != null) {
            _scene.tickPolicyDidChange(this);
        }
    }

    /**
     * Sets the element's user object reference.
     */
    public void setUserObject (Object object)
    {
        _userObject = object;
    }

    /**
     * Sets the transform to the specified value and promotes it to {@link Transform3D#UNIFORM},
     * then updates the bounds of the element.
     */
    public void setTransform (Transform3D transform)
    {
        _transform.set(transform);
        _transform.promote(Transform3D.UNIFORM);
        updateBounds();
    }

    /**
     * Updates the bounds of the element.  The default implementation transforms the bounds
     * returned by {@link #getLocalBounds}.
     */
    public void updateBounds ()
    {
        // and the world bounds
        computeBounds(_nbounds);
        if (!_bounds.equals(_nbounds)) {
            boundsWillChange();
            _bounds.set(_nbounds);
            boundsDidChange();
        }
    }

    // documentation inherited from interface SceneElement
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    // documentation inherited from interface SceneElement
    public Object getUserObject ()
    {
        return _userObject;
    }

    // documentation inherited from interface SceneElement
    public void wasAdded (Scene scene)
    {
        _scene = scene;
    }

    // documentation inherited from interface SceneElement
    public void willBeRemoved ()
    {
        _scene = null;
    }

    // documentation inherited from interface SceneElement
    public void setInfluences (SceneInfluenceSet influences)
    {
        _influences.clear();
        _influences.addAll(influences);
    }

    // documentation inherited from interface SceneElement
    public boolean isInfluenceable ()
    {
        return true;
    }

    // documentation inherited from interface SceneObject
    public Box getBounds ()
    {
        return _bounds;
    }

    // documentation inherited from interface SceneObject
    public boolean updateLastVisit (int visit)
    {
        if (_lastVisit == visit) {
            return false;
        }
        _lastVisit = visit;
        return true;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        updateBounds();
    }

    // documentation inherited from interface Intersectable
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        return false;
    }

    /**
     * Computes the bounds of the element and places them in the provided result object.  The
     * default implementation simply transforms the bounds returned by {@link #getLocalBounds}.
     */
    protected void computeBounds (Box result)
    {
        getLocalBounds().transform(_transform, result);
    }

    /**
     * Returns the local bounds of the element.  Default implementation returns {@link Box#ZERO};
     * override to return actual local bounds.
     */
    protected Box getLocalBounds ()
    {
        return Box.ZERO;
    }

    /**
     * Notes that the bounds are about to change.
     */
    protected void boundsWillChange ()
    {
        if (_scene != null) {
            _scene.boundsWillChange(this);
        }
    }

    /**
     * Notes that the bounds have changed.
     */
    protected void boundsDidChange ()
    {
        if (_scene != null) {
            _scene.boundsDidChange(this);
        }
    }

    /** The bounds of the element. */
    protected Box _bounds = new Box();

    /** Holds the new bounds of the element when updating. */
    protected Box _nbounds = new Box();

    /** The element's tick policy. */
    protected TickPolicy _tickPolicy = TickPolicy.NEVER;

    /** The element's user object. */
    protected Object _userObject;

    /** The scene to which this element has been added. */
    protected Scene _scene;

    /** The influences affecting the element. */
    protected SceneInfluenceSet _influences = new SceneInfluenceSet();

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
