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

import java.util.List;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.util.GlContext;

/**
 * Configurable light state.
 */
@EditorTypes({ LightStateConfig.Disabled.class, LightStateConfig.Enabled.class })
public abstract class LightStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Explicitly disables lighting.
     */
    public static class Disabled extends LightStateConfig
    {
        @Override
        public LightState getState (GlContext ctx, Scope scope, List<Updater> updaters)
        {
            return LightState.DISABLED;
        }
    }

    /**
     * Enables lighting.
     */
    public static class Enabled extends LightStateConfig
    {
        /** The global ambient light intensity. */
        @Editable
        public Color4f globalAmbient = new Color4f(0.2f, 0.2f, 0.2f, 1f);

        /** The individual light configurations. */
        @Editable
        public LightConfig[] lights = new LightConfig[0];

        @Override
        public LightState getState (GlContext ctx, Scope scope, List<Updater> updaters)
        {
            Light[] slights = new Light[lights.length];
            for (int ii = 0; ii < lights.length; ii++) {
                slights[ii] = lights[ii].createLight(ctx, scope, false, updaters);
            }
            return new LightState(slights, globalAmbient);
        }
    }

    /**
     * Returns the corresponding light state.
     */
    public abstract LightState getState (GlContext ctx, Scope scope, List<Updater> updaters);
}
