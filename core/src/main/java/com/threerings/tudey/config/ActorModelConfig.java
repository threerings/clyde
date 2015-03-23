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

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;

import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

import com.threerings.opengl.model.config.ModelConfig;

/**
 * A model config wrapper for using an actor's model config.
 */
public abstract class ActorModelConfig extends ModelConfig
{
    /**
     * An actor model config wrapper.
     */
    public static class Wrapper extends BaseWrapper
    {
        /** The actor reference. */
        @Editable(nullable=true)
        public ConfigReference<ActorConfig> actor;

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(ActorConfig.class, actor).preload(ctx);
        }

        @Override
        protected ModelConfig getModelConfig (ConfigManager cfgmgr)
        {
            ActorConfig config = cfgmgr.getConfig(ActorConfig.class, actor);
            if (config != null) {
                ActorConfig.Original original = config.getOriginal(cfgmgr);
                return cfgmgr.getConfig(ModelConfig.class, original.sprite.model);
            }
            return null;
        }
    }
}
