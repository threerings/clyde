//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ListUtil;

import com.threerings.export.util.ExportUtil;
import com.threerings.swing.PrefsTree;
import com.threerings.swing.PrefsTreeNode;
import com.threerings.util.DeepUtil;
import com.threerings.util.ToolUtil;

import com.threerings.tudey.data.TudeySceneModel.Entry;

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
        } else { // action.equals("delete")
            _tree.removeSelectedNode();
        }
    }

    /** The palette tree. */
    protected PrefsTree _tree;

    /** The delete button. */
    protected JButton _delete;

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(Palette.class);
}
