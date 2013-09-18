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

package com.threerings.tudey.tools;

import org.lwjgl.input.Keyboard;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.util.PseudoKeys;

import com.threerings.tudey.client.TudeySceneController;
import com.threerings.tudey.util.TudeyContext;

/**
 * Scene controller for tools.
 */
public class ToolSceneController extends TudeySceneController
{
    @Override
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_tsview = new ToolSceneView((TudeyContext)ctx, this));
    }

    @Override
    protected void bindKeys ()
    {
        super.bindKeys();

        // go back to editor mode if escape is pressed
        addKeyObserver(Keyboard.KEY_ESCAPE, new PseudoKeys.Adapter() {
            public void keyPressed (long when, int key, float amount) {
                _ctx.getLocationDirector().leavePlace();
            }
        });
    }

    @Override
    protected int getMouseCameraModifiers ()
    {
        return KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK;
    }
}
