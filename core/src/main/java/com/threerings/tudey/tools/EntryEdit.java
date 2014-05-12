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

import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.Paint;
import com.threerings.tudey.util.Coord;

/**
 * Represents an edit to the scene's entries (and paint).
 */
public class EntryEdit extends AbstractUndoableEdit
{
    /**
     * Creates and applies a new entry edit.
     */
    public EntryEdit (
        TudeySceneModel scene, int id, int layer, Entry[] add, Entry[] update, Object[] remove)
    {
        _scene = scene;
        _id = id;
        _layer = layer;

        // add the new entries and put them in the map
        for (Entry entry : add) {
            if (_scene.addEntry(entry, layer)) {
                _added.put(entry.getKey(), null);
            }
        }

        // update the modified entries and store old state
        for (Entry entry : update) {
            Entry oentry = _scene.updateEntry(entry);
            //_scene.setLayer(entry.getKey(), layer); // this shouldn't be necessary: TODO
            if (oentry != null) {
                _updated.put(entry.getKey(), oentry);
            }
        }

        // remove the requested entries and store old state
        for (Object key : remove) {
            Entry oentry = _scene.removeEntry(key);
            if (oentry != null) {
                _removed.put(key, oentry);
            }
        }
    }

    /**
     * Creates and applies a new paint edit.
     */
    public EntryEdit (TudeySceneModel scene, int id, int layer, Rectangle region, Paint paint)
    {
        _scene = scene;
        _id = id;
        _layer = layer;

        // set the paint entry and store the old state
        for (int yy = region.y, yymax = yy + region.height; yy < yymax; yy++) {
            for (int xx = region.x, xxmax = xx + region.width; xx < xxmax; xx++) {
                Paint opaint = _scene.setPaint(xx, yy, paint);
                if (!Objects.equal(opaint, paint)) {
                    _paint.put(new Coord(xx, yy), opaint);
                }
            }
        }
    }

    @Override
    public boolean addEdit (UndoableEdit edit)
    {
        // make sure it's an entry edit with the same id as this one
        if (!(edit instanceof EntryEdit)) {
            return false;
        }
        EntryEdit oedit = (EntryEdit)edit;
        if ((oedit._id != _id) || (oedit._layer != _layer)) {
            return false;
        }

        // merge the added entries
        for (Object key : oedit._added.keySet()) {
            Entry oentry = _removed.remove(key);
            if (oentry == null) {
                _added.put(key, null);
            } else {
                _updated.put(key, oentry);
            }
        }

        // merge the updated entries
        for (Entry entry : oedit._updated.values()) {
            Object key = entry.getKey();
            if (!(_added.containsKey(key) || _updated.containsKey(key))) {
                _updated.put(key, entry);
            }
        }

        // merge the removed entries
        for (Entry entry : oedit._removed.values()) {
            Object key = entry.getKey();
            if (_added.containsKey(key)) {
                _added.remove(key);
            } else {
                Entry oentry = _updated.remove(key);
                _removed.put(key, oentry == null ? entry : oentry);
            }
        }

        // merge the paint
        for (Map.Entry<Coord, Paint> entry : oedit._paint.entrySet()) {
            Coord coord = entry.getKey();
            if (!_paint.containsKey(coord)) {
                _paint.put(coord, entry.getValue());
            }
        }

        return true;
    }

    @Override
    public void undo ()
        throws CannotUndoException
    {
        super.undo();
        swap(_removed, _added);
    }

    @Override
    public void redo ()
        throws CannotRedoException
    {
        super.redo();
        swap(_added, _removed);
    }

    /**
     * Performs the actual undo or redo operation.
     *
     * @param removed the map containing the entries to add.
     * @param added the map containing the entries to remove.
     */
    protected void swap (Map<Object, Entry> removed, Map<Object, Entry> added)
    {
        // add back the entries we removed (retaining their ids)
        for (Map.Entry<Object, Entry> entry : removed.entrySet()) {
            _scene.addEntry(entry.getValue(), _layer, false);
            entry.setValue(null);
        }

        // swap out the entries we updated
        for (Map.Entry<Object, Entry> entry : _updated.entrySet()) {
            entry.setValue(_scene.updateEntry(entry.getValue()));
        }

        // remove the entries we added
        for (Map.Entry<Object, Entry> entry : added.entrySet()) {
            entry.setValue(_scene.removeEntry(entry.getKey()));
        }

        // swap the paint
        for (Map.Entry<Coord, Paint> entry : _paint.entrySet()) {
            Coord coord = entry.getKey();
            entry.setValue(_scene.setPaint(coord.x, coord.y, entry.getValue()));
        }
    }

    /** The affected scene. */
    protected TudeySceneModel _scene;

    /** The edit id, which determines which edits we can merge. */
    protected int _id;

    /** The layer to which this edit applies. */
    protected int _layer;

    /** The entries added in this edit. */
    protected Map<Object, Entry> _added = Maps.newHashMap();

    /** The entries updated in this edit. */
    protected Map<Object, Entry> _updated = Maps.newHashMap();

    /** The entries removed in this edit. */
    protected Map<Object, Entry> _removed = Maps.newHashMap();

    /** The paint set or cleared in this edit. */
    protected Map<Coord, Paint> _paint = Maps.newHashMap();
}
