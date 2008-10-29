//
// $Id$

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
        add(new PrefsTreeNode(_msgs.get("m.new_entry"), DeepUtil.copy(entries)));
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
            add(new PrefsTreeNode(_msgs.get("m.new_folder"), null));
        } else { // action.equals("delete")
            PrefsTreeNode node = _tree.getSelectedNode();
            PrefsTreeNode parent = (PrefsTreeNode)node.getParent();
            int idx = parent.getIndex(node);
            _tree.removeNodeFromParent(node);
            PrefsTreeNode select;
            int ccount = parent.getChildCount();
            if (ccount > 0) {
                select = (PrefsTreeNode)parent.getChildAt(Math.min(idx, ccount - 1));
            } else if (!parent.isRoot()) {
                select = parent;
            } else {
                return;
            }
            _tree.setSelectionPath(new TreePath(select.getPath()));
        }
    }

    /**
     * Adds a new node under the root.
     */
    protected void add (PrefsTreeNode node)
    {
        _tree.insertNodeInto(node, _tree.getRootNode());
        _tree.startEditingAtPath(new TreePath(node.getPath()));
    }

    /** The palette tree. */
    protected PrefsTree _tree;

    /** The delete button. */
    protected JButton _delete;

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(Palette.class);
}
