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

import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import com.samskivert.swing.GroupLayout;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.swing.PrefsTree;
import com.threerings.swing.PrefsTreeNode;
import com.threerings.util.DeepUtil;
import com.threerings.util.ToolUtil;

import com.threerings.tudey.data.TudeySceneModel.Entry;

import static com.threerings.tudey.Log.log;

/**
 * The palette tool.
 */
public class Palette extends BaseMover
    implements TreeSelectionListener, ActionListener
{
    /**
     * Creates the palette tool.
     */
    public Palette (SceneEditor editor)
    {
        super(editor);

        // create and add the tree
        add(new JScrollPane(_tree = new PrefsTree(_prefs.node("palette"))));
        _tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _tree.addTreeSelectionListener(this);

        // and the button panel
        JPanel bpanel = new JPanel();
        add(bpanel, GroupLayout.FIXED);
        bpanel.add(ToolUtil.createButton(this, _msgs, "new_folder"));
        bpanel.add(_delete = ToolUtil.createButton(this, _msgs, "delete"));
        _delete.setEnabled(false);

        // and the export panel
        JPanel epanel = new JPanel();
        add(epanel, GroupLayout.FIXED);
        epanel.add(ToolUtil.createButton(this, _msgs, "import_short"));
        epanel.add(ToolUtil.createButton(this, _msgs, "export_short"));

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("palette_export_dir", null));
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
     * Adds a new entry to the palette.
     */
    public void add (Entry... entries)
    {
        _tree.insertNewNode(_msgs.get("m.new_entry"), DeepUtil.copy(entries));
    }

    // documentation inherited from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent event)
    {
        PrefsTreeNode node = _tree.getSelectedNode();
        if (node != null && node.getValue() != null) {
            move((Entry[])node.getValue());
        } else {
            clear();
        }
        _delete.setEnabled(node != null);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("new_folder")) {
            _tree.insertNewNode(_msgs.get("m.new_folder"), null);
        } else if (action.equals("delete")) {
            _tree.removeSelectedNode();
        } else if (action.equals("import_short")) {
            importPalette();
        } else { // action.equals("export_short")
            exportPalette();
        }
    }

    /**
     * Attempts to import a palette.
     */
    protected void importPalette ()
    {
        if (_chooser.showOpenDialog(_editor.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = _chooser.getSelectedFile();
            try {
                XMLImporter in = new XMLImporter(new FileInputStream(file));
                merge(_tree.getRootNode(), (PrefsTreeNode)in.readObject());
                in.close();
            } catch (Exception e) { // IOException, ClassCastException
                log.warning("Failed to import palette.", "file", file, e);
            }
        }
        _prefs.put("palette_export_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to export the palette.
     */
    protected void exportPalette ()
    {
        if (_chooser.showSaveDialog(_editor.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = _chooser.getSelectedFile();
            try {
                XMLExporter out = new XMLExporter(new FileOutputStream(file));
                out.writeObject(_tree.getRootNode());
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export palette.", "file", file, e);
            }
        }
        _prefs.put("palette_export_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Merges in the contents of the supplied node.
     */
    protected void merge (PrefsTreeNode onode, PrefsTreeNode nnode)
    {
        PrefsTreeNode[] nchildren = new PrefsTreeNode[nnode.getChildCount()];
        for (int ii = 0; ii < nchildren.length; ii++) {
            nchildren[ii] = (PrefsTreeNode)nnode.getChildAt(ii);
        }
        for (PrefsTreeNode nchild : nchildren) {
            PrefsTreeNode ochild = onode.getChild((String)nchild.getUserObject());
            if (ochild == null) {
                _tree.insertNodeInto(nchild, onode);
            } else if (!(ochild.getAllowsChildren() && nchild.getAllowsChildren())) {
                _tree.removeNodeFromParent(ochild);
                _tree.insertNodeInto(nchild, onode);
            } else {
                merge(ochild, nchild);
            }
        }
    }

    @Override
    public void calculateElevation (int minElevation, int maxElevation)
    {
        _elevation = _editor.getGrid().getElevation();
    }

    /** The palette tree. */
    protected PrefsTree _tree;

    /** The delete button. */
    protected JButton _delete;

    /** The file chooser for importing and exporting entry files. */
    protected JFileChooser _chooser;

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(Palette.class);
}
