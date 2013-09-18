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
import com.threerings.util.AbstractIdentityHashSet;

import com.threerings.opengl.renderer.Color4f;

/**
 * A set of viewer effects.
 */
public class ViewerEffectSet extends AbstractIdentityHashSet<ViewerEffect>
{
    /**
     * Returns the background color for this effect set.
     *
     * @param bounds the bounds used to resolve conflicts.
     */
    public Color4f getBackgroundColor (Box bounds)
    {
        Color4f closestColor = null;
        float cdist = Float.MAX_VALUE;
        for (ViewerEffect effect : this) {
            Color4f color = effect.getBackgroundColor();
            if (color != null) {
                float distance = effect.getBounds().getExtentDistance(bounds);
                if (closestColor == null || distance < cdist) {
                    closestColor = color;
                    cdist = distance;
                }
            }
        }
        return closestColor;
    }
}
