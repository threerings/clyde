//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.*;

/**
 * Configurations for client-side actions.
 */
@EditorTypes({
    ClientActionConfig.ControllerAction.class,
    ClientActionConfig.Compound.class })
public abstract class ClientActionConfig extends DeepObject
    implements Exportable
{
    /**
     * Fires an action on the scene controller.
     */
    public static class ControllerAction extends ClientActionConfig
    {
        /** The name of the command to fire. */
        @Editable
        public String command = "";

        @Override // documentation inherited
        public void execute (TudeyContext ctx, TudeySceneView view, Sprite source)
        {
            if (!view.getController().handleAction(source, command, null)) {
                log.warning("Controller didn't handle action.", "command", command);
            }
        }
    }

    /**
     * Executes multiple actions simultaneously.
     */
    public static class Compound extends ClientActionConfig
    {
        /** The actions to execute. */
        @Editable
        public ClientActionConfig[] actions = new ClientActionConfig[0];

        @Override // documentation inherited
        public void execute (TudeyContext ctx, TudeySceneView view, Sprite source)
        {
            for (ClientActionConfig action : actions) {
                action.execute(ctx, view, source);
            }
        }
    }

    /**
     * Executes the action.
     */
    public abstract void execute (TudeyContext ctx, TudeySceneView view, Sprite source);
}
