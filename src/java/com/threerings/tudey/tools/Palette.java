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
        _tree = new JTree(new PaletteTreeNode(null, null), true);
        _tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _tree.setRootVisible(false);
        _tree.setEditable(true);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);
        _tree.setDragEnabled(true);
        final DataFlavor nflavor = ToolUtil.createLocalFlavor(PaletteTreeNode.class);
        _tree.setTransferHandler(new TransferHandler() {
            public int getSourceActions (JComponent comp) {
                return MOVE;
            }
            public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                return ListUtil.containsRef(flavors, nflavor);
            }
            public boolean importData (JComponent comp, Transferable t) {
                try {
                    DefaultTreeModel model = (DefaultTreeModel)_tree.getModel();
                    PaletteTreeNode node = (PaletteTreeNode)t.getTransferData(nflavor);
                    PaletteTreeNode selected = getSelectedNode();
                    PaletteTreeNode parent;
                    if (selected == node) {
                        return false;
                    } else if (selected == null) {
                        parent = (PaletteTreeNode)model.getRoot();
                    } else if (selected.getAllowsChildren()) {
                        parent = selected;
                    } else {
                        parent = (PaletteTreeNode)selected.getParent();
                    }
                    if (node.getParent() == parent) {
                        return false;
                    }
                    model.removeNodeFromParent(node);
                    model.insertNodeInto(node, parent, parent.getInsertionIndex(node));
                    return true;

                } catch (Exception e) {
                    return false;
                }
            }
            protected Transferable createTransferable (JComponent c) {
                final PaletteTreeNode node = getSelectedNode();
                if (node == null) {
                    return null;
                }
                return new Transferable() {
                    public Object getTransferData (DataFlavor flavor) {
                        return node;
                    }
                    public DataFlavor[] getTransferDataFlavors () {
                        return new DataFlavor[] { nflavor };
                    }
                    public boolean isDataFlavorSupported (DataFlavor flavor) {
                        return flavor == nflavor;
                    }
                };
            }
            protected void exportDone (JComponent source, Transferable data, int action) {
            }
        });
        add(new JScrollPane(_tree));

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
        add(new PaletteTreeNode(_msgs.get("m.new_entry"), DeepUtil.copy(entries)));
    }

    // documentation inherited from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent event)
    {
        PaletteTreeNode node = getSelectedNode();
        if (node != null && node.entries != null) {
            move(node.entries);
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
            add(new PaletteTreeNode(_msgs.get("m.new_folder"), null));
        } else { // action.equals("delete")
            ((DefaultTreeModel)_tree.getModel()).removeNodeFromParent(getSelectedNode());
        }
    }

    /**
     * Adds a new node under the root.
     */
    protected void add (PaletteTreeNode node)
    {
        DefaultTreeModel model = (DefaultTreeModel)_tree.getModel();
        PaletteTreeNode root = (PaletteTreeNode)model.getRoot();
        model.insertNodeInto(node, root, root.getInsertionIndex(node));
        _tree.startEditingAtPath(new TreePath(node.getPath()));
    }

    /**
     * Returns the selected node, or <code>null</code> for none.
     */
    protected PaletteTreeNode getSelectedNode ()
    {
        TreePath path = _tree.getSelectionPath();
        return (path == null) ? null : (PaletteTreeNode)path.getLastPathComponent();
    }

    /**
     * A node in the palette tree.
     */
    protected static class PaletteTreeNode extends DefaultMutableTreeNode
    {
        /** The entries in this node (or <code>null</code> if this is a folder). */
        public Entry[] entries;

        public PaletteTreeNode (String name, Entry[] entries)
        {
            super(name, entries == null);
            this.entries = entries;
        }

        /**
         * Returns the index at which the specified child should be inserted.
         */
        public int getInsertionIndex (PaletteTreeNode child)
        {
            if (children == null) {
                return 0;
            }
            String name = (String)child.getUserObject();
            boolean folder = child.getAllowsChildren();
            for (int ii = 0, nn = children.size(); ii < nn; ii++) {
                PaletteTreeNode ochild = (PaletteTreeNode)children.get(ii);
                String oname = (String)ochild.getUserObject();
                boolean ofolder = ochild.getAllowsChildren();
                if ((folder == ofolder) ? (name.compareTo(oname) <= 0) : !folder) {
                    return ii;
                }
            }
            return children.size();
        }
    }

    /** The palette tree. */
    protected JTree _tree;

    /** The delete button. */
    protected JButton _delete;

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(Palette.class);
}
