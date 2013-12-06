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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap.IntEntry;
import com.samskivert.util.QuickSort;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.editors.ArrayListEditor;
import com.threerings.export.Exportable;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.util.DeepObject;
import com.threerings.util.ToolUtil;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.GlobalEntry;

import static com.threerings.tudey.Log.log;

/**
 * The global editor tool.
 */
public class GlobalEditor extends EditorTool
    implements ChangeListener, ActionListener, TudeySceneModel.LayerObserver
{
    /**
     * Creates the global editor tool.
     */
    public GlobalEditor (SceneEditor editor, Layers layers)
    {
        super(editor);

        // create and add the editor panel
        add(_epanel = new EditorPanel(editor));
        _epanel.addChangeListener(this);

        // and the export panel
        JPanel xpanel = new JPanel();
        add(xpanel, GroupLayout.FIXED);
        xpanel.add(ToolUtil.createButton(this, _msgs, "import_short"));
        xpanel.add(ToolUtil.createButton(this, _msgs, "export_short"));

        layers.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                updateShownGlobals();
            }
        });

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("global_export_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".xml");
            }
            public String getDescription () {
                return _msgs.get("m.xml_files");
            }
        });
    }

    /**
     * Requests to start editing the specified entry.
     */
    public void edit (GlobalEntry entry)
    {
        // make the entry visible
        int idx = ((EditableGlobals)_epanel.getObject()).getIndex(entry.getId());
        if (idx != -1) {
            ((ArrayListEditor)_epanel.getPropertyEditor("globals")).makeVisible(idx);
        }
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _editor.incrementEditId();

        // compare the current state to the stored state
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        Predicate<Entry> layerPredicate = _editor.getLayerPredicate();
        for (Iterator<IntEntry<GlobalEntry>> it = _globals.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<GlobalEntry> entry = it.next();
            int id = entry.getIntKey();
            GlobalEntry oglobal = entry.getValue();
            if (!layerPredicate.apply(oglobal)) {
                continue;
            }
            GlobalEntry nglobal = editable.getGlobal(id);
            if (nglobal == null) { // removed
                _ignoreRemove = true;
                try {
                    _editor.removeEntries(id);
                } finally {
                    _ignoreRemove = false;
                }
                it.remove();

            } else if (!nglobal.equals(oglobal)) { // modified
                GlobalEntry cglobal = (GlobalEntry)nglobal.clone();
                _ignoreUpdate = true;
                try {
                    _editor.updateEntries(cglobal);
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
                    _editor.addEntries(cglobal);
                } finally {
                    _ignoreAdd = false;
                }
                int id = cglobal.getId();
                _globals.put(id, cglobal);
                nglobal.setId(id);
            }
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("import_short")) {
            importGlobals();
        } else { // action.equals("export_short")
            exportGlobals();
        }
    }

    @Override
    public boolean allowsMouseCamera ()
    {
        return true;
    }

    @Override
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

        // update which ones we show based on the selected layer
        updateShownGlobals();
    }

    @Override
    public void entryAdded (Entry entry)
    {
        if (_ignoreAdd || !(entry instanceof GlobalEntry)) {
            return;
        }
        GlobalEntry gentry = (GlobalEntry)entry;
        int id = gentry.getId();
        _globals.put(id, gentry);

        // if it's on the current layer, add it
        if (_editor.getLayerPredicate().apply(entry)) {
            EditableGlobals editable = (EditableGlobals)_epanel.getObject();
            editable.globals = ArrayUtil.append(editable.globals, (GlobalEntry)entry.clone());
            QuickSort.sort(editable.globals);
            _epanel.update();
        }
    }

    @Override
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
        if (idx != -1) {
            entry.copy(editable.globals[idx]);
            _epanel.update();
        } // else: probably a different layer
    }

    @Override
    public void entryRemoved (Entry oentry)
    {
        if (_ignoreRemove || !(oentry instanceof GlobalEntry)) {
            return;
        }
        int id = ((GlobalEntry)oentry).getId();
        _globals.remove(id);
        EditableGlobals editable = (EditableGlobals)_epanel.getObject();
        int idx = editable.getIndex(id);
        if (idx != -1) {
            editable.globals = ArrayUtil.splice(editable.globals, idx, 1);
            _epanel.update();
        } // else: probably a different layer
    }

    // from TudeySceneModel.LayerObserver
    public void entryLayerWasSet (Object key, int layer)
    {
        if (_ignoreUpdate || _ignoreAdd || _ignoreRemove) {
            return;
        }

        if (_globals.containsKey(key)) {
            updateShownGlobals();
        }
    }

    /**
     * Update the globals being shown on our editor panel (layer-specific).
     */
    protected void updateShownGlobals ()
    {
        // create the (cloned) editable list
        EditableGlobals editable = new EditableGlobals();
        editable.globals = Iterables.toArray(
            Iterables.filter(_globals.values(), _editor.getLayerPredicate()), GlobalEntry.class);
        QuickSort.sort(editable.globals);
        _epanel.setObject(editable.clone());
    }

    /**
     * Attempts to import a set of globals.
     */
    protected void importGlobals ()
    {
        if (_chooser.showOpenDialog(_editor.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = _chooser.getSelectedFile();
            try {
                XMLImporter in = new XMLImporter(new FileInputStream(file));
                GlobalEntry[] entries = (GlobalEntry[])in.readObject();
                _editor.incrementEditId();
                _editor.addEntries(entries);
                in.close();
            } catch (Exception e) { // IOException, ClassCastException
                log.warning("Failed to import globals.", "file", file, e);
            }
        }
        _prefs.put("global_export_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to export the set of globals.
     */
    protected void exportGlobals ()
    {
        if (_chooser.showSaveDialog(_editor.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = _chooser.getSelectedFile();
            try {
                XMLExporter out = new XMLExporter(new FileOutputStream(file));
                out.writeObject(Iterables.toArray(
                    Iterables.filter(_globals.values(), _editor.getVisiblePredicate()),
                    GlobalEntry.class));
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export globals.", "file", file, e);
            }
        }
        _prefs.put("global_export_dir", _chooser.getCurrentDirectory().toString());
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

    /** The file chooser for importing and exporting global files. */
    protected JFileChooser _chooser;

    /** The current set of all globals. */
    protected HashIntMap<GlobalEntry> _globals = new HashIntMap<GlobalEntry>();

    /** Notes that we should ignore an add/update/remove because we're the one effecting it. */
    protected boolean _ignoreAdd, _ignoreUpdate, _ignoreRemove;

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(GlobalEditor.class);
}
