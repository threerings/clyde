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

package com.threerings.opengl.gui.icon;

import com.threerings.opengl.renderer.Renderer;

/**
 * An icon which is combination of multiple icons.
 */
public class MergedIcon extends Icon
{
  /**
   * Create a merged icon.
   */
  public MergedIcon (Icon...icons)
  {
    _icons = icons;
    for (Icon icon : _icons) {
      if (icon.getWidth() > _maxWidth) {
        _maxWidth = icon.getWidth();
      }
      if (icon.getHeight() > _maxHeight) {
        _maxHeight = icon.getHeight();
      }
    }
  }

  @Override
  public int getWidth ()
  {
    return _maxWidth;
  }

  @Override
  public int getHeight ()
  {
    return _maxHeight;
  }

  @Override
  public void render (Renderer renderer, int x, int y, float alpha)
  {
    for (Icon icon : _icons) {
      icon.render(renderer, x, y, alpha);
    }
  }

  /** The max width of the icons. */
  protected int _maxWidth;

  /** The max height of the icons. */
  protected int _maxHeight;

  /** The merged icons. */
  protected Icon[] _icons;
}
