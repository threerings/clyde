//
// $Id$

package com.threerings.tudey.tools;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap.IntEntry;
import com.samskivert.util.QuickSort;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.editors.ArrayListEditor;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.GlobalEntry;

import static com.threerings.tudey.Log.*;

/**
 * The global editor tool.
 */
public class GlobalEditor extends EditorTool
    implements ChangeListener
{
    /**
     * Creates the global editor tool.
     */
    public GlobalEditor (SceneEditor editor)
    {
        super(editor);

        // create and add the editor panel
        add(_epanel = new EditorPanel(editor));
        _epanel.addChangeListener(this);
    }

    /**
     * Requests to start editing the specified entry.
     */
    public void edit (GlobalEntry entry)
    {
        // make the entry visible
        int idx = ((EditableGlobals)_epanel.getObject()).getIndex(entry.getId());
        ((ArrayListEditor)_epanel.getPropertyEditor("globals")).makeVisible(idx);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _editor.incrementEditId();

        // compare the current state to the stored state
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        _scene.setName(editable.name);
        for (Iterator<IntEntry<GlobalEntry>> it = _globals.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<GlobalEntry> entry = it.next();
            int id = entry.getIntKey();
            GlobalEntry oglobal = entry.getValue();
            GlobalEntry nglobal = editable.getGlobal(id);
            if (nglobal == null) { // removed
                _ignoreRemove = true;
                try {
                    _editor.removeEntry(id);
                } finally {
                    _ignoreRemove = false;
                }
                it.remove();

            } else if (!nglobal.equals(oglobal)) { // modified
                GlobalEntry cglobal = (GlobalEntry)nglobal.clone();
                _ignoreUpdate = true;
                try {
                    _editor.updateEntry(cglobal);
                } finally {
                    _ignoreUpdate = false;
                }
                entry.setValue(cglobal);
            }
        }
        for (GlobalEntry nglobal : editable.globals) {
            if (nglobal.getId() == 0) { // added
                GlobalEntry cglobal = (GlobalEntry)nglobal.clone();
                _ignoreAdd = true;
                try {
                    _editor.addEntry(cglobal);
                } finally {
                    _ignoreAdd = false;
                }
                int id = cglobal.getId();
                _globals.put(id, cglobal);
                nglobal.setId(id);
            }
        }
    }

    @Override // documentation inherited
    public boolean allowsMouseCamera ()
    {
        return true;
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);

        // extract the scene's globals
        _globals.clear();
        for (Entry entry : scene.getEntries()) {
            if (entry instanceof GlobalEntry) {
                GlobalEntry global = (GlobalEntry)entry;
                _globals.put(global.getId(), global);
            }
        }

        // create the (cloned) editable list
        EditableGlobals editable = new EditableGlobals();
        editable.name = scene.name;
        editable.globals = _globals.values().toArray(new GlobalEntry[_globals.size()]);
        QuickSort.sort(editable.globals);
        _epanel.setObject((EditableGlobals)editable.clone());
    }

    @Override // documentation inherited
    public void entryAdded (Entry entry)
    {
        if (_ignoreAdd || !(entry instanceof GlobalEntry)) {
            return;
        }
        GlobalEntry gentry = (GlobalEntry)entry;
        int id = gentry.getId();
        _globals.put(id, gentry);
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        editable.globals = ArrayUtil.append(editable.globals, (GlobalEntry)entry.clone());
        QuickSort.sort(editable.globals);
        _epanel.update();
    }

    @Override // documentation inherited
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        if (_ignoreUpdate || !(oentry instanceof GlobalEntry)) {
            return;
        }
        GlobalEntry entry = (GlobalEntry)nentry;
        int id = entry.getId();
        _globals.put(id, entry);
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        int idx = editable.getIndex(id);
        if (idx == -1) {
            log.warning("Missing global entry to update.", "editable", editable, "entry", entry);
        } else {
            entry.copy(editable.globals[idx]);
        }
        _epanel.update();
    }

    @Override // documentation inherited
    public void entryRemoved (Entry oentry)
    {
        if (_ignoreRemove || !(oentry instanceof GlobalEntry)) {
            return;
        }
        int id = ((GlobalEntry)oentry).getId();
        _globals.remove(id);
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        int idx = editable.getIndex(id);
        if (idx == -1) {
            log.warning("Missing global entry to remove.", "editable", editable, "id", id);
        } else {
            editable.globals = ArrayUtil.splice(editable.globals, idx, 1);
        }
        _epanel.update();
    }

    /**
     * Allows us to edit the scene's globals.
     */
    protected static class EditableGlobals extends DeepObject
        implements Exportable
    {
        /** The name of the scene. */
        @Editable
        public String name = "";

        /** The array of globals to edit. */
        @Editable
        public GlobalEntry[] globals;

        /**
         * Returns the global with the supplied identifier, or <code>null</code> if it couldn't be
         * found.
         */
        public GlobalEntry getGlobal (int id)
        {
            int idx = getIndex(id);
            return (idx == -1) ? null : globals[idx];
        }

        /**
         * Returns the index of the global with the supplied identifier, or <code>-1</code> if it
         * couldn't be found.
         */
        public int getIndex (int id)
        {
            for (int ii = 0; ii < globals.length; ii++) {
                if (globals[ii].getId() == id) {
                    return ii;
                }
            }
            return -1;
        }
    }

    /** The panel that we use to edit the scene's globals. */
    protected EditorPanel _epanel;

    /** The current set of globals. */
    protected HashIntMap<GlobalEntry> _globals = new HashIntMap<GlobalEntry>();

    /** Notes that we should ignore an add/update/remove because we're the one effecting it. */
    protected boolean _ignoreAdd, _ignoreUpdate, _ignoreRemove;
}
