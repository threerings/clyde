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

import com.threerings.math.Box;

import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.text.CharacterTextFactory;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.scene.SimpleSceneElement;

/**
 * A text billboard that may be embedded within a scene.
 */
public class TextBillboard extends SimpleSceneElement
    implements UIConstants
{
    /**
     * Creates a new text object using a character text factory.
     */
    public TextBillboard (
        GlContext ctx, Font font, boolean antialias, String text, Color4f color)
    {
        this(ctx, null);
        setText(font, antialias, text, color);
    }

    /**
     * Creates a new text object using a character text factory.
     */
    public TextBillboard (
        GlContext ctx, Font font, boolean antialias, String text,
        Color4f color, int effect, int effectSize, Color4f effectColor)
    {
        this(ctx, null);
        setText(font, antialias, text, color, effect, effectSize, effectColor);
    }

    /**
     * Creates a new text object.
     */
    public TextBillboard (GlContext ctx, Text text)
    {
        super(ctx, RenderQueue.TRANSPARENT);
        setText(text);
    }

    /**
     * Sets the text using a character text factory.
     */
    public void setText (Font font, boolean antialias, String text, Color4f color)
    {
        setText(font, antialias, text, color, NORMAL, 0, Color4f.BLACK);
    }

    /**
     * Sets the text using a character text factory.
     */
    public void setText (
        Font font, boolean antialias, String text, Color4f color,
        int effect, int effectSize, Color4f effectColor)
    {
        setText(CharacterTextFactory.getInstance(font, antialias, 0f).createText(
            text, color, effect, effectSize, effectColor, true));
    }

    /**
     * Sets the text to render.
     */
    public void setText (Text text)
    {
        _text = text;
        Dimension size = _text.getSize();
        float extent = Math.max(size.width/2, size.height);
        _localBounds.getMinimumExtent().set(-extent, -extent, -extent);
        _localBounds.getMaximumExtent().set(+extent, +extent, +extent);
        updateBounds();
    }

    /**
     * Returns the text being rendered.
     */
    public Text getText ()
    {
        return _text;
    }

    /**
     * Sets the alpha value with which to render the text.
     */
    public void setAlpha (float alpha)
    {
        _alpha = alpha;
    }

    /**
     * Returns the alpha value with which the text is being rendered.
     */
    public float getAlpha ()
    {
        return _alpha;
    }

    @Override
    public void enqueue ()
    {
        Compositor compositor = _ctx.getCompositor();
        if (compositor.getSubrenderDepth() > 0) {
            return; // don't show up in reflections, etc.
        }

        // rotate to face the camera
        _transform.getRotation().set(compositor.getCamera().getWorldTransform().getRotation());

        super.enqueue();
    }

    @Override
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
        states[RenderState.DEPTH_STATE] = DepthState.TEST;
        return states;
    }

    @Override
    protected Box getLocalBounds ()
    {
        return _localBounds;
    }

    @Override
    protected void draw ()
    {
        if (_text != null) {
            _text.render(_ctx.getRenderer(), _text.getSize().width/2, 0, _alpha);
        }
    }

    /** The text to render. */
    protected Text _text;

    /** The bounds of the text. */
    protected Box _localBounds = new Box();

    /** The alpha value with which to render the text. */
    protected float _alpha = 1f;
}
