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

import com.threerings.expr.Scoped;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.util.TudeyContext;

/**
 * Scene view for tools.
 */
public class ToolSceneView extends TudeySceneView
{
    /**
     * Creates a new scene view.
     */
    public ToolSceneView (TudeyContext ctx, ToolSceneController ctrl)
    {
        super(ctx, ctrl);
    }

    /** Whether or not markers are visible. */
    @Scoped
    protected boolean _markersVisible;

    /** Whether or not lighting is enabled. */
    @Scoped
    protected boolean _lightingEnabled = true;

    /** Whether or not fog is enabled. */
    @Scoped
    protected boolean _fogEnabled = true;

    /** Whether or not sound is enabled. */
    @Scoped
    protected boolean _soundEnabled = true;

    /** Whether or not camera transitions are enabled. */
    @Scoped
    protected boolean _cameraEnabled = true;
}
