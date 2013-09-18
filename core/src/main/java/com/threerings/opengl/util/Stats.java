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

package com.threerings.opengl.util;

import java.awt.Font;

import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.gui.text.CharacterTextFactory;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Displays rendering statistics.
 */
public class Stats extends SimpleOverlay
{
    /**
     * Creates a new stats display.
     */
    public Stats (GlContext ctx)
    {
        super(ctx);

        // create the label (empty for now)
        _textFactory = CharacterTextFactory.getInstance(
            new Font("Dialog", Font.PLAIN, 12), true, 0f);
        _stats = _textFactory.createText("", Color4f.WHITE, 0, 0, Color4f.BLACK, true);
    }

    @Override
    public void composite ()
    {
        Compositor compositor = _ctx.getCompositor();
        if (compositor.getSubrenderDepth() > 0) {
            return;
        }
        compositor.addEnqueueable(this);
        _frameCount++;

        // update the stats if the required interval has passed
        long now = System.currentTimeMillis(), interval = now - _lastUpdate;
        if (interval >= REPORT_INTERVAL) {
            int fps = (int)((_frameCount * 1000) / interval);
            Renderer renderer = _ctx.getRenderer();
            _stats = _textFactory.createText(
                fps + " fps (" + "b: " + renderer.getBatchCount() + "; " + "p: " +
                renderer.getPrimitiveCount() + "; tc: " + renderer.getTextureChangeCount() +
                ") [bo: " + renderer.getBufferObjectCount() + "/" +
                renderer.getBufferObjectBytes()/1024 + "k, tx: " + renderer.getTextureCount() +
                "/" + renderer.getTextureBytes()/1024 + "k]",
                Color4f.WHITE, 0, 0, Color4f.BLACK, true);
            _lastUpdate = now;
            _frameCount = 0;
        }
    }

    @Override
    protected void draw ()
    {
        _stats.render(_ctx.getRenderer(), 16, getY(), 1f);
    }

    /**
     * Returns the y coordinate at which to render the stats.
     */
    protected int getY ()
    {
        return 16;
    }

    /** Used to create text objects. */
    protected CharacterTextFactory _textFactory;

    /** The stats display text. */
    protected Text _stats;

    /** The time at which we last updated the stats. */
    protected long _lastUpdate = System.currentTimeMillis();

    /** The number of frames rendered since the last update. */
    protected int _frameCount;

    /** The interval at which we update the stats. */
    protected static final long REPORT_INTERVAL = 1000L;
}
