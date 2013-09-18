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

package com.threerings.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.util.ListUtil;

import com.threerings.util.ToolUtil;

/**
 * Provides a mechanism for {@link Preferences}-backed persistent storage of a tree of exportable
 * objects.  Each internal node in the tree corresponds to a {@link Preferences} node underneath
 * the root passed to the constructor, and each leaf node corresponds to a property set in its
 * parent's {@link Preferences} node (with the value of the property being equal to the exported
 * bytes of the node's object).  The tree displays the nodes in sorted order, and allows editing
 * the nodes' names and dragging and dropping nodes within the tree (adjusting names as necessary
 * to ensure uniqueness).  To keep the contents of the tree synchronized with the preferences,
 * only use the {@link #removeNodeFromParent} and {@link #insertNodeInto} methods to modify the
 * tree.  Currently, the tree will not reflect external changes made to the preferences.
 */
public class PrefsTree extends JTree
{
    /**
     * Creates a new preferences tree.
     */
    public PrefsTree (Preferences prefs)
    {
        setModel(new DefaultTreeModel(new PrefsTreeNode(_prefs = prefs), true) {
            public void valueForPathChanged (TreePath path, Object newValue) {
                // save selection
                TreePath selection = getSelectionPath();

                // remove and reinsert with a unique name in sorted order
                PrefsTreeNode node = (PrefsTreeNode)path.getLastPathComponent();
                PrefsTreeNode parent = (PrefsTreeNode)node.getParent();
                PrefsTree.this.removeNodeFromParent(node);
                node.setUserObject(newValue);
                PrefsTree.this.insertNodeInto(node, parent);

                // re-expand paths, reapply the selection
                node.expandPaths(PrefsTree.this);
                setSelectionPath(selection);
            }
        });
        setRootVisible(false);
        setEditable(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // the expansion listener notes expansion in the node state and preferences
        addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded (TreeExpansionEvent event) {
                ((PrefsTreeNode)event.getPath().getLastPathComponent()).setExpanded(
                    PrefsTree.this, _prefs, true);
            }
            public void treeCollapsed (TreeExpansionEvent event) {
                ((PrefsTreeNode)event.getPath().getLastPathComponent()).setExpanded(
                    PrefsTree.this, _prefs, false);
            }
        });

        // the transfer handler handles dragging and dropping within the tree
        setDragEnabled(true);
        final DataFlavor nflavor = ToolUtil.createLocalFlavor(PrefsTreeNode.class);
        setTransferHandler(new TransferHandler() {
            public int getSourceActions (JComponent comp) {
                return MOVE;
            }
            public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                return ListUtil.containsRef(flavors, nflavor);
            }
            public boolean importData (JComponent comp, Transferable t) {
                try {
                    PrefsTreeNode node = (PrefsTreeNode)t.getTransferData(nflavor);
                    PrefsTreeNode selected = getSelectedNode();
                    PrefsTreeNode parent;
                    if (selected == node) {
                        return false;
                    } else if (selected == null) {
                        parent = (PrefsTreeNode)getModel().getRoot();
                    } else if (selected.getAllowsChildren()) {
                        parent = selected;
                    } else {
                        parent = (PrefsTreeNode)selected.getParent();
                    }
                    if (node.getParent() == parent) {
                        return false;
                    }
                    removeNodeFromParent(node);
                    insertNodeInto(node, parent);
                    node.expandPaths(PrefsTree.this);
                    setSelectionPath(new TreePath(node.getPath()));
                    return true;

                } catch (Exception e) {
                    return false;
                }
            }
            protected Transferable createTransferable (JComponent c) {
                final PrefsTreeNode node = getSelectedNode();
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

        // add a listener for the delete key
        addKeyListener(new KeyAdapter() {
            public void keyPressed (KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedNode();
                    event.consume();
                }
            }
        });

        // expand the paths as set
        getRootNode().expandPaths(this);
    }

    /**
     * Returns a reference to the root node.
     */
    public PrefsTreeNode getRootNode ()
    {
        return (PrefsTreeNode)getModel().getRoot();
    }

    /**
     * Returns a reference to the selected node, if any.
     */
    public PrefsTreeNode getSelectedNode ()
    {
        TreePath path = getSelectionPath();
        return (path == null) ? null : (PrefsTreeNode)path.getLastPathComponent();
    }

    /**
     * Inserts a new node and starts editing it.
     */
    public void insertNewNode (String name, Object value)
    {
        PrefsTreeNode node = new PrefsTreeNode(name, value);
        insertNodeInto(node, getRootNode());
        startEditingAtPath(new TreePath(node.getPath()));
    }

    /**
     * Removes the selected node and adjusts the selection sensibly.  Does nothing if no node is
     * selected.
     */
    public void removeSelectedNode ()
    {
        PrefsTreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }
        PrefsTreeNode parent = (PrefsTreeNode)node.getParent();
        int idx = parent.getIndex(node);
        removeNodeFromParent(node);
        PrefsTreeNode select;
        int ccount = parent.getChildCount();
        if (ccount > 0) {
            select = (PrefsTreeNode)parent.getChildAt(Math.min(idx, ccount - 1));
        } else if (!parent.isRoot()) {
            select = parent;
        } else {
            return;
        }
        setSelectionPath(new TreePath(select.getPath()));
    }

    /**
     * Removes the specified node from its parent and updates the underlying preferences
     * accordingly.
     */
    public void removeNodeFromParent (PrefsTreeNode node)
    {
        // remove from preferences, then from tree
        node.removeFromPreferences(_prefs);
        ((DefaultTreeModel)getModel()).removeNodeFromParent(node);
    }

    /**
     * Inserts the specified node and updates the underlying preferences accordingly.
     */
    public void insertNodeInto (PrefsTreeNode node, PrefsTreeNode parent)
    {
        // uniquify the name
        String base = (String)node.getUserObject();
        node.setUserObject(parent.findNameForChild(base));

        // add to tree, then to preferences
        ((DefaultTreeModel)getModel()).insertNodeInto(
            node, parent, parent.getInsertionIndex(node));
        node.addToPreferences(_prefs);
    }

    /** The underlying preferences. */
    protected Preferences _prefs;
}
