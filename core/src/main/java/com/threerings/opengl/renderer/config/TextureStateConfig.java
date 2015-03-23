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

import java.lang.ref.SoftReference;

import java.util.List;

import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.util.GlContext;

/**
 * Configurable texture state.
 */
public class TextureStateConfig extends DeepObject
    implements Exportable
{
    /** If true, do not use a shared instance. */
    @Editable
    public boolean uniqueInstance;

    /** The texture unit configurations. */
    @Editable
    public TextureUnitConfig[] units = new TextureUnitConfig[0];

    @Deprecated
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
    }

    /**
     * Determines whether this state is supported by the hardware.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        if (units.length > ctx.getRenderer().getMaxTextureImageUnits()) {
            return false;
        }
        for (TextureUnitConfig unit : units) {
            if (!unit.isSupported(ctx, fallback)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Populates the relevant portion of the supplied descriptor.
     */
    public void populateDescriptor (PassDescriptor desc)
    {
        desc.texCoordSets = new int[units.length];
        for (int ii = 0; ii < units.length; ii++) {
            TextureUnitConfig unit = units[ii];
            desc.texCoordSets[ii] = unit.coordSet;
            desc.normals |=
                unit.coordGenS != null && unit.coordGenS.usesNormals() ||
                unit.coordGenT != null && unit.coordGenT.usesNormals() ||
                unit.coordGenR != null && unit.coordGenR.usesNormals();
        }
    }

    /**
     * Returns the corresponding texture state.
     */
    public TextureState getState (
        GlContext ctx, Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
    {
        if (units.length == 0) {
            return TextureState.DISABLED;
        }
        if (uniqueInstance) {
            return createInstance(ctx, scope, adders, updaters);
        }
        TextureState instance = (_instance == null) ? null : _instance.get();
        if (instance == null) {
            // if the instance adds any adders/updaters, it must be unique;
            // otherwise we can cache it
            int esize = adders.size(), usize = updaters.size();
            instance = createInstance(ctx, scope, adders, updaters);
            if (adders.size() == esize && updaters.size() == usize) {
                _instance = new SoftReference<TextureState>(instance);
            }
        }
        return instance;
    }

    /**
     * Invalidates the config's cached data.
     */
    public void invalidate ()
    {
        _instance = null;
    }

    /**
     * Creates a material state instance corresponding to this config.
     */
    protected TextureState createInstance (
        GlContext ctx, Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
    {
        TextureUnit[] sunits = new TextureUnit[units.length];
        TextureState state = new TextureState(sunits);
        for (int ii = 0; ii < units.length; ii++) {
            sunits[ii] = units[ii].createUnit(ctx, state, scope, adders, updaters);
        }
        return state;
    }

    /** Cached state instance. */
    @DeepOmit
    protected transient SoftReference<TextureState> _instance;
}
