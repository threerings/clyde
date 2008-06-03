//
// $Id$

package com.threerings.config.tools;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.util.ListUtil;
import com.samskivert.util.Tuple;

import com.threerings.export.SerializableWrapper;
import com.threerings.util.ToolUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigListener;
import com.threerings.config.ManagedConfig;

import static com.threerings.ClydeLog.*;

/**
 * Displays a tree of configurations.
 */
public class ConfigTree extends JTree
    implements ConfigListener<ManagedConfig>
{
    /**
     * Creates a new config tree to display the configurations in the specified group.
     */
    public ConfigTree (ConfigGroup group)
    {
        this(group, false);
    }

    /**
     * Creates a new config tree to display the configurations in the specified group.
     *
     * @param editable if true, the tree will allow editing the configurations.
     */
    public ConfigTree (ConfigGroup group, boolean editable)
    {
        @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig> mgroup =
            (ConfigGroup<ManagedConfig>)group;
        _group = mgroup;
        setModel(new DefaultTreeModel(new ConfigTreeNode(null, null), true) {
            public void valueForPathChanged (TreePath path, Object newValue) {
                super.valueForPathChanged(path, newValue);

                // save selection
                TreePath selection = getSelectionPath();

                // remove and reinsert in sorted order
                ConfigTreeNode node = (ConfigTreeNode)path.getLastPathComponent();
                ConfigTreeNode parent = (ConfigTreeNode)node.getParent();
                removeNodeFromParent(node);
                insertNodeInto(node, parent, parent.getInsertionIndex(node));

                // re-expand paths, reapply the selection
                node.expandPaths(ConfigTree.this);
                setSelectionPath(selection);
            }
            public void insertNodeInto (MutableTreeNode child, MutableTreeNode parent, int index) {
                super.insertNodeInto(child, parent, index);
                if (!enterChangeBlock()) {
                    return;
                }
                try {
                    ((ConfigTreeNode)child).addConfigs(_group);
                } finally {
                    leaveChangeBlock();
                }
            }
            public void removeNodeFromParent (MutableTreeNode node) {
                super.removeNodeFromParent(node);
                if (!enterChangeBlock()) {
                    return;
                }
                try {
                    ((ConfigTreeNode)node).removeConfigs(_group);
                } finally {
                    leaveChangeBlock();
                }
            }
        });

        // start with some basics
        setRootVisible(false);
        setEditable(editable);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // the expansion listener simply notes expansion in the node state
        addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded (TreeExpansionEvent event) {
                ((ConfigTreeNode)event.getPath().getLastPathComponent()).setExpanded(true);
            }
            public void treeCollapsed (TreeExpansionEvent event) {
                ((ConfigTreeNode)event.getPath().getLastPathComponent()).setExpanded(false);
            }
        });

        // the transfer handler handles dragging and dropping (both within the tree and
        // between applications)
        setDragEnabled(true);
        setTransferHandler(new TransferHandler() {
            public int getSourceActions (JComponent comp) {
                return COPY_OR_MOVE;
            }
            public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                return isEditable() &&
                    ListUtil.contains(flavors, ToolUtil.SERIALIZED_WRAPPED_FLAVOR);
            }
            public boolean importData (JComponent comp, Transferable t) {
                boolean local = t.isDataFlavorSupported(LOCAL_NODE_TRANSFER_FLAVOR);
                DataFlavor flavor = local ?
                    LOCAL_NODE_TRANSFER_FLAVOR : ToolUtil.SERIALIZED_WRAPPED_FLAVOR;
                Object data;
                try {
                    data = t.getTransferData(local ?
                        LOCAL_NODE_TRANSFER_FLAVOR : ToolUtil.SERIALIZED_WRAPPED_FLAVOR);
                } catch (Exception e) {
                    log.warning("Failure importing data.", e);
                    return false;
                }
                ConfigTreeNode node, onode = null;
                if (local) {
                    NodeTransfer transfer = (NodeTransfer)data;
                    node = transfer.cnode;
                    onode = transfer.onode;
                } else {
                    data = ((SerializableWrapper)data).getObject();
                    if (!(data instanceof ConfigTreeNode)) {
                        return false; // some other kind of wrapped transfer
                    }
                    node = (ConfigTreeNode)data;
                }
                if (!node.verifyConfigClass(_group.getConfigClass())) {
                    return false; // some other kind of config
                }
                ConfigTreeNode snode = getSelectedNode();
                ConfigTreeNode parent = (ConfigTreeNode)getModel().getRoot();
                if (snode != null && snode.getParent() != null) {
                    parent = snode.getAllowsChildren() ?
                        snode : (ConfigTreeNode)snode.getParent();
                }
                if (onode == parent || (onode != null && onode.getParent() == parent)) {
                    return false; // can't move to self or to the same folder
                }
                // have to clone it in case we are going to paste it multiple times
                node = (ConfigTreeNode)node.clone();

                // find a unique name
                String name = (String)node.getUserObject();
                node.setUserObject(parent.findNameForChild(name));

                // insert, re-expand, select
                ((DefaultTreeModel)getModel()).insertNodeInto(
                    node, parent, parent.getInsertionIndex(node));
                node.expandPaths(ConfigTree.this);
                setSelectionPath(new TreePath(node.getPath()));
                return true;
            }
            protected Transferable createTransferable (JComponent c) {
                ConfigTreeNode node = getSelectedNode();
                return (node == null) ? null : new NodeTransfer(node, false);
            }
            protected void exportDone (JComponent source, Transferable data, int action) {
                if (action == MOVE) {
                    ConfigTreeNode onode = ((NodeTransfer)data).onode;
                    ((DefaultTreeModel)getModel()).removeNodeFromParent(onode);
                }
            }
        });

        // build the tree model
        ConfigTreeNode root = (ConfigTreeNode)getModel().getRoot();
        for (ManagedConfig config : _group.getConfigs()) {
            root.insertConfig(config, config.getName());
        }
        ((DefaultTreeModel)getModel()).reload();

        // start listening for updates
        _group.addListener(this);
    }

    /**
     * Releases the resources held by this tree.  This should be called when the tree is no longer
     * needed.
     */
    public void dispose ()
    {
        // stop listening for updates
        _group.removeListener(this);
    }

    /**
     * Creates a {@link Transferable} containing the selected node for the clipboard.
     */
    public Transferable createClipboardTransferable ()
    {
        ConfigTreeNode node = getSelectedNode();
        return (node == null) ? null : new NodeTransfer(node, true);
    }

    /**
     * Returns the selected node, or <code>null</code> for none.
     */
    public ConfigTreeNode getSelectedNode ()
    {
        TreePath path = getSelectionPath();
        return (path == null) ? null : (ConfigTreeNode)path.getLastPathComponent();
    }

    /**
     * Notes that the selected node's configuration has changed.
     */
    public void selectedConfigChanged ()
    {
        if (!enterChangeBlock()) {
            return;
        }
        try {
            _group.updateConfig(getSelectedNode().getConfig());
        } finally {
            leaveChangeBlock();
        }
    }

    // documentation inherited from interface ConfigListener
    public void configAdded (ConfigEvent<ManagedConfig> event)
    {
        if (!enterChangeBlock()) {
            return;
        }
        try {
            ManagedConfig config = event.getConfig();
            ConfigTreeNode root = (ConfigTreeNode)getModel().getRoot();
            Tuple<ConfigTreeNode, ConfigTreeNode> point =
                root.getInsertionPoint(config, config.getName());
            ((DefaultTreeModel)getModel()).insertNodeInto(
                point.right, point.left, point.left.getInsertionIndex(point.right));

        } finally {
            leaveChangeBlock();
        }
    }

    // documentation inherited from interface ConfigListener
    public void configRemoved (ConfigEvent<ManagedConfig> event)
    {
        if (!enterChangeBlock()) {
            return;
        }
        try {
            String name = event.getConfig().getName();
            ConfigTreeNode node = ((ConfigTreeNode)getModel().getRoot()).getNode(name);
            if (node != null) {
                ((DefaultTreeModel)getModel()).removeNodeFromParent(node);
            } else {
                log.warning("Missing config node [name=" + name + "].");
            }
        } finally {
            leaveChangeBlock();
        }
    }

    // documentation inherited from interface ConfigListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        ManagedConfig config = event.getConfig();
        ConfigTreeNode node = getSelectedNode();
        if (node == null || node.getConfig() != config || !enterChangeBlock()) {
            return;
        }
        try {
            selectedConfigUpdated();
        } finally {
            leaveChangeBlock();
        }
    }

    /**
     * Called when the selected configuration has been modified by a source <em>other</em> than
     * {@link #selectedConfigChanged}.
     */
    protected void selectedConfigUpdated ()
    {
        // nothing by default
    }

    /**
     * Attempts to enter the change block.
     *
     * @return true if we have entered, false if we are already within a change block.
     */
    protected boolean enterChangeBlock ()
    {
        return _changing ? false : (_changing = true);
    }

    /**
     * Leaves the change block.
     */
    protected void leaveChangeBlock ()
    {
        _changing = false;
    }

    /**
     * Contains a node for transfer.
     */
    protected static class NodeTransfer
        implements Transferable
    {
        /** The original node (to delete when the transfer completes). */
        public ConfigTreeNode onode;

        /** The cloned node. */
        public ConfigTreeNode cnode;

        public NodeTransfer (ConfigTreeNode onode, boolean clipboard)
        {
            this.onode = clipboard ? null : onode;
            cnode = (ConfigTreeNode)onode.clone();
        }

        // documentation inherited from interface Transferable
        public DataFlavor[] getTransferDataFlavors ()
        {
            return NODE_TRANSFER_FLAVORS;
        }

        // documentation inherited from interface Transferable
        public boolean isDataFlavorSupported (DataFlavor flavor)
        {
            return ListUtil.contains(NODE_TRANSFER_FLAVORS, flavor);
        }

        // documentation inherited from interface Transferable
        public Object getTransferData (DataFlavor flavor)
        {
            return flavor.equals(LOCAL_NODE_TRANSFER_FLAVOR) ?
                this : new SerializableWrapper(cnode);
        }
    }

    /** The configuration group. */
    protected ConfigGroup<ManagedConfig> _group;

    /** Indicates that we should ignore any changes, because we're the one effecting them. */
    protected boolean _changing;

    /** A data flavor that provides access to the actual transfer object. */
    protected static final DataFlavor LOCAL_NODE_TRANSFER_FLAVOR;
    static {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType +
                ";class=" + NodeTransfer.class.getName());
        } catch (ClassNotFoundException e) {
             // won't happen
        }
        LOCAL_NODE_TRANSFER_FLAVOR = flavor;
    }

    /** The flavors available for node transfer. */
    protected static final DataFlavor[] NODE_TRANSFER_FLAVORS =
        { LOCAL_NODE_TRANSFER_FLAVOR, ToolUtil.SERIALIZED_WRAPPED_FLAVOR };
}
