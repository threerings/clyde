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
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.FogState;

/**
 * Configurable fog state.
 */
@EditorTypes({
    FogStateConfig.Disabled.class, FogStateConfig.Linear.class,
    FogStateConfig.Exponential.class })
public abstract class FogStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Explicitly disables the fog.
     */
    public static class Disabled extends FogStateConfig
    {
        @Override
        public FogState getState ()
        {
            return FogState.DISABLED;
        }
    }

    /**
     * Superclass of the enabled states.
     */
    public static abstract class Enabled extends FogStateConfig
    {
        /** The fog color. */
        @Editable(hgroup="p")
        public Color4f color = new Color4f(0f, 0f, 0f, 0f);
    }

    /**
     * Linear fog.
     */
    public static class Linear extends Enabled
    {
        /** The fog start distance. */
        @Editable(min=0, step=0.1, hgroup="p")
        public float start;

        /** The fog end distance. */
        @Editable(min=0, step=0.1, hgroup="p")
        public float end = 1f;

        public Linear (Enabled other)
        {
            color.set(other.color);
        }

        public Linear ()
        {
        }

        @Override
        public FogState getState ()
        {
            return new FogState(GL11.GL_LINEAR, start, end, color);
        }
    }

    /**
     * Exponential fog.
     */
    public static class Exponential extends Enabled
    {
        /** The fog density. */
        @Editable(min=0, step=0.001, hgroup="p")
        public float density = 1f;

        /** Whether or not to square the exponential function. */
        @Editable(hgroup="p")
        public boolean squared;

        public Exponential (Enabled other)
        {
            color.set(other.color);
        }

        public Exponential ()
        {
        }

        @Override
        public FogState getState ()
        {
            return new FogState(squared ? GL11.GL_EXP2 : GL11.GL_EXP, density, color);
        }
    }

    /**
     * Returns the corresponding fog state.
     */
    public abstract FogState getState ();
}
