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

import com.threerings.math.Box;

import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.config.FontConfig;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.text.TextFactory;
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
   * Creates a new text billboard using the default font.
   */
  public TextBillboard (
    GlContext ctx, int fontSize, String text, Color4f color)
  {
    this(ctx, null);
    setText(fontSize, text, color);
  }

  /**
   * Creates a new text billboard using the default font.
   */
  public TextBillboard (
    GlContext ctx, int fontSize, String text,
    Color4f color, int effect, int effectSize, Color4f effectColor)
  {
    this(ctx, null);
    setText(fontSize, text, color, effect, effectSize, effectColor);
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
   * Sets the text using the default font.
   */
  public void setText (int fontSize, String text, Color4f color)
  {
    setText(fontSize, text, color, NORMAL, 0, Color4f.BLACK);
  }

  /**
   * Sets the text using the default font.
   */
  public void setText (
    int fontSize, String text, Color4f color,
    int effect, int effectSize, Color4f effectColor)
  {
    TextFactory factory = FontConfig.NULL.getTextFactory(
      _ctx, FontConfig.Style.PLAIN, fontSize);
    setText(factory.createText(text, color, effect, effectSize, effectColor, true));
  }

  /**
   * Sets the text to render.
   */
  public void setText (Text text)
  {
    _text = text;
    if (text != null) {
      Dimension size = text.getSize();
      _bounds.getMinimumExtent().set(-size.width / 2f, -size.height / 2f, 0f);
      _bounds.getMaximumExtent().set(+size.width / 2f, +size.height / 2f, 0f);
    }
    updateBounds();
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
    return _bounds;
  }

  @Override
  protected void draw ()
  {
    if (_text != null) {
      Dimension size = _text.getSize();
      _text.render(_ctx.getRenderer(), -size.width / 2, -size.height / 2, 1f);
    }
  }

  /** The text to render. */
  protected Text _text;

  /** The bounds of the text. */
  protected Box _bounds = new Box();
}
