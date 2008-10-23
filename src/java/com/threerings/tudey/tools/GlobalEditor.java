//
// $Id$

package com.threerings.tudey.tools;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
        // compare the current state to the stored state
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        for (Iterator<IntEntry<GlobalEntry>> it = _globals.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<GlobalEntry> entry = it.next();
            int id = entry.getIntKey();
            GlobalEntry oglobal = entry.getValue();
            GlobalEntry nglobal = editable.getGlobal(id);
            if (nglobal == null) { // removed
                _editor.removeEntry(id);
                it.remove();

            } else if (!nglobal.equals(oglobal)) { // modified
                GlobalEntry cglobal = (GlobalEntry)nglobal.clone();
                _editor.updateEntry(cglobal);
                entry.setValue(cglobal);
            }
        }
        for (GlobalEntry nglobal : editable.globals) {
            if (nglobal.getId() == 0) { // added
                GlobalEntry cglobal = (GlobalEntry)nglobal.clone();
                _editor.addEntry(cglobal);
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
        editable.globals = _globals.values().toArray(new GlobalEntry[_globals.size()]);
        QuickSort.sort(editable.globals);
        _epanel.setObject((EditableGlobals)editable.clone());
    }

    /**
     * Allows us to edit the scene's globals.
     */
    protected static class EditableGlobals extends DeepObject
        implements Exportable
    {
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
}
