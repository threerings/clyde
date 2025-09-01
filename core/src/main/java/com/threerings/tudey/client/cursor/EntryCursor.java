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

package com.threerings.tudey.client.cursor;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an entry.
 */
public abstract class EntryCursor extends Cursor
{
  /**
   * Creates a new entry cursor.
   */
  public EntryCursor (TudeyContext ctx, TudeySceneView view)
  {
    super(ctx, view);
  }

  /**
   * Returns a reference to the most recently set entry state.
   */
  public abstract Entry getEntry ();

  /**
   * Returns a reference to the shape of the entry (or <code>null</code> for none).
   */
  public abstract Shape getShape ();

  /**
   * Updates the cursor with new entry state.
   */
  public abstract void update (Entry entry);

  /** The color to use for footprints. */
  protected static final Color4f FOOTPRINT_COLOR = Color4f.CYAN;
}
