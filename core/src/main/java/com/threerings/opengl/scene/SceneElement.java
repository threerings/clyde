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

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Tickable;

/**
 * Interface for elements that can be embedded into scenes.
 */
public interface SceneElement extends SceneObject, Tickable, Intersectable, Compositable
{
    /** Determines when the {@link #tick} method must be called. */
    public enum TickPolicy { DEFAULT, NEVER, WHEN_VISIBLE, ALWAYS };

    /**
     * Returns the policy that determines when the {@link #tick} method must be called.
     */
    public TickPolicy getTickPolicy ();

    /**
     * Returns this element's user object reference.
     */
    public Object getUserObject ();

    /**
     * Notes that the element was added to the specified scene.
     */
    public void wasAdded (Scene scene);

    /**
     * Notes that the element will be removed from the scene.
     */
    public void willBeRemoved ();

    /**
     * Sets the influences affecting this element.
     */
    public void setInfluences (SceneInfluenceSet influences);

    /**
     * Returns true in this element is influenceable.
     */
    public boolean isInfluenceable ();
}
