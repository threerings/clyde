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

        protected final int _constant;
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
