//
// $Id$

package com.threerings.tudey.tools;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * Represents an edit to the scene's entries.
 */
public class EntryEdit extends AbstractUndoableEdit
{
    /**
     * Creates and applies a new edit.
     */
    public EntryEdit (TudeySceneModel scene, int id, Entry[] add, Entry[] update, Object[] remove)
    {
        _scene = scene;
        _id = id;

        // add the new entries and put them in the map
        for (Entry entry : add) {
            if (_scene.addEntry(entry)) {
                _added.put(entry.getKey(), null);
            }
        }

        // update the modified entries and store old state
        for (Entry entry : update) {
            Entry oentry = _scene.updateEntry(entry);
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

    @Override // documentation inherited
    public boolean addEdit (UndoableEdit edit)
    {
        // make sure it's an entry edit with the same id as this one
        if (!(edit instanceof EntryEdit)) {
            return false;
        }
        EntryEdit oedit = (EntryEdit)edit;
        if (oedit._id != _id) {
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

        return true;
    }

    @Override // documentation inherited
    public void undo ()
        throws CannotUndoException
    {
        super.undo();
        swap(_removed, _added);
    }

    @Override // documentation inherited
    public void redo ()
        throws CannotRedoException
    {
        super.redo();
        swap(_added, _removed);
    }

    /**
     * Performs the actual undo or redo operation.
     *
     * @param add the map containing the entries to add.
     * @param remove the map containing the entries to remove.
     */
    protected void swap (HashMap<Object, Entry> removed, HashMap<Object, Entry> added)
    {
        // add back the entries we removed (retaining their ids)
        for (Map.Entry<Object, Entry> entry : removed.entrySet()) {
            _scene.addEntry(entry.getValue(), false);
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
    }

    /** The affected scene. */
    protected TudeySceneModel _scene;

    /** The edit id, which determines which edits we can merge. */
    protected int _id;

    /** The entries added in this edit. */
    protected HashMap<Object, Entry> _added = new HashMap<Object, Entry>();

    /** The entries updated in this edit. */
    protected HashMap<Object, Entry> _updated = new HashMap<Object, Entry>();

    /** The entries removed in this edit. */
    protected HashMap<Object, Entry> _removed = new HashMap<Object, Entry>();
}
