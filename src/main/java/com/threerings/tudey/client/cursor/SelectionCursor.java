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

import java.util.ArrayList;
import java.util.HashMap;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.shape.Compound;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.TudeyContext;

/**
 * A cursor for a selection.
 */
public class SelectionCursor extends Cursor
{
    /**
     * Creates the cursor.
     */
    public SelectionCursor (TudeyContext ctx, TudeySceneView view)
    {
        super(ctx, view);
    }

    /**
     * Returns a reference to the shape of the cursor.
     */
    public Compound getShape ()
    {
        return _shape;
    }

    /**
     * Updates the cursor with new entry state.
     */
    public void update (Entry[] entries)
    {
        // find and create or update the corresponding cursors
        for (Entry entry : entries) {
            Object key = entry.getKey();
            EntryCursor cursor = _cursorsByKey.get(key);
            if (cursor == null) {
                _cursors.add(cursor = entry.createCursor(_ctx, (TudeySceneView)_parentScope));
                _cursorsByKey.put(key, cursor);
            } else {
                cursor.update(entry);
            }
        }
        // remove any cursors for which there is no longer an entry
        if (_cursors.size() > entries.length) {
            for (int ii = _cursors.size() - 1; ii >= 0; ii--) {
                Object key = _cursors.get(ii).getEntry().getKey();
                if (!containsKey(entries, key)) {
                    _cursors.remove(ii);
                    _cursorsByKey.remove(key);
                }
            }
        }
        // update the compound shape
        for (int ii = 0, nn = _cursors.size(); ii < nn; ii++) {
            Shape shape = _cursors.get(ii).getShape();
            if (shape != null) {
                _shapes.add(shape);
            }
        }
        int scount = _shapes.size();
        if (_shape.getShapeCount() != scount) {
            _shape = new Compound(scount);
        }
        _shapes.toArray(_shape.getShapes());
        _shape.updateBounds();
        _shapes.clear();
    }

    @Override
    public void tick (float elapsed)
    {
        for (int ii = 0, nn = _cursors.size(); ii < nn; ii++) {
            _cursors.get(ii).tick(elapsed);
        }
    }

    @Override
    public void composite ()
    {
        for (int ii = 0, nn = _cursors.size(); ii < nn; ii++) {
            _cursors.get(ii).composite();
        }
    }

    /**
     * Determines whether the supplied array of entries contains an entry with the specified key.
     */
    protected static boolean containsKey (Entry[] entries, Object key)
    {
        for (Entry entry : entries) {
            if (entry.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** The contained cursors. */
    protected ArrayList<EntryCursor> _cursors = new ArrayList<EntryCursor>();

    /** Maps entry keys to cursors. */
    protected HashMap<Object, EntryCursor> _cursorsByKey = new HashMap<Object, EntryCursor>();

    /** The shape of the cursor. */
    protected Compound _shape = new Compound();

    /** Holds the component shapes when updating. */
    protected ArrayList<Shape> _shapes = new ArrayList<Shape>();
}
