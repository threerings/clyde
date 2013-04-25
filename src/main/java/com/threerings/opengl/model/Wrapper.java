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

package com.threerings.opengl.model;

import java.util.List;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.scene.SceneElement.TickPolicy;

/**
 * A wrapper around a model implementation.
 */
public class Wrapper extends Model.Implementation
{
    public Wrapper (Scope parentScope, Model.Implementation impl)
    {
        super(parentScope);
        _impl = impl;
    }

    @Override
    public List<Model> getChildren ()
    {
        return _impl.getChildren();
    }

    @Override // documentation inherited
    public Transform3D getPointWorldTransform (String point)
    {
        return _impl.getPointWorldTransform(point);
    }


    @Override // documentation inherited
    public void attach (String point, Model model, boolean replace)
    {
        _impl.attach(point, model, replace);
    }

    @Override // documentation inherited
    public void detach (Model model)
    {
        _impl.detach(model);
    }

    @Override // documentation inherited
    public void detachAll (String point)
    {
        _impl.detachAll(point);
    }

    @Override // documentation inherited
    public List<Animation> getPlayingAnimations ()
    {
        return _impl.getPlayingAnimations();
    }

    @Override // documentation inherited
    public Animation getAnimation (String name)
    {
        return _impl.getAnimation(name);
    }

    @Override // documentation inherited
    public Animation[] getAnimations ()
    {
        return _impl.getAnimations();
    }

    @Override // documentation inherited
    public Animation createAnimation ()
    {
        return _impl.createAnimation();
    }

    @Override // documentation inherited
    public boolean hasCompleted ()
    {
        return _impl.hasCompleted();
    }

    @Override // documentation inherited
    public void setVisible (boolean visible)
    {
        _impl.setVisible(visible);
    }

    @Override
    public void visibilityWasSet ()
    {
        _impl.visibilityWasSet();
    }

    @Override // documentation inherited
    public void reset ()
    {
        _impl.reset();
    }

    @Override // documentation inherited
    public int getInfluenceFlags ()
    {
        return _impl.getInfluenceFlags();
    }

    @Override // documentation inherited
    public Box getBounds ()
    {
        return _impl.getBounds();
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _impl.updateBounds();
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        _impl.drawBounds();
    }

    @Override // documentation inherited
    public void setTickPolicy (TickPolicy policy)
    {
        _impl.setTickPolicy(policy);
    }

    @Override // documentation inherited
    public TickPolicy getTickPolicy ()
    {
        return _impl.getTickPolicy();
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        _impl.wasAdded();
    }

    @Override // documentation inherited
    public void willBeRemoved ()
    {
        _impl.willBeRemoved();
    }

    @Override // documentation inherited
    public boolean isImplementation (Model.Implementation impl)
    {
        return _impl == impl;
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        _impl.tick(elapsed);
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        return _impl.getIntersection(ray, result);
    }

    @Override // documentation inherited
    public void composite ()
    {
        _impl.composite();
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return _impl.getScopeName();
    }

    /** The implementation we're wrapping. */
    protected Model.Implementation _impl;
}
