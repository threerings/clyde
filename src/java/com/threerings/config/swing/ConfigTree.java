//
// $Id$

package com.threerings.config.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.util.ListUtil;
import com.samskivert.util.Tuple;

import com.threerings.export.util.SerializableWrapper;
import com.threerings.util.ChangeBlock;
import com.threerings.util.ToolUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigGroupListener;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.ConfigEvent;
import com.threerings.config.ManagedConfig;

import static com.threerings.ClydeLog.*;

/**
 * Displays a tree of configurations.
 */
public class ConfigTree extends JTree
    implements ConfigGroupListener<ManagedConfig>, ConfigUpdateListener<ManagedConfig>
{
    /**
     * Creates a new config tree to display the configurations in the specified groups.
     */
    public ConfigTree (ConfigGroup... groups)
    {
        this(groups, false);
    }

    /**
     * Creates a new config tree to display the configurations in the specified group.
     *
     * @param editable if true, the tree will allow editing the configurations.
     */
    public ConfigTree (ConfigGroup group, boolean editable)
    {
        this(new ConfigGroup[] { group }, editable);
    }

    /**
     * Releases the resources held by this tree.  This should be called when the tree is no longer
     * needed.
     */
    public void dispose ()
    {
        // stop listening for updates
        for (ConfigGroup<ManagedConfig> group : _groups) {
            group.removeListener(this);
        }
        if (_lconfig != null) {
            _lconfig.removeListener(this);
            _lconfig = null;
        }
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
     * Selects a node by name (if it exists).
     */
    public void setSelectedNode (String name)
    {
        if (name == null) {
            clearSelection();
            return;
        }
        ConfigTreeNode node = ((ConfigTreeNode)getModel().getRoot()).getNode(name);
        if (node != null) {
            setSelectionPath(new TreePath(node.getPath()));
        }
    }

    /**
     * Notes that the selected node's configuration has changed.
     */
    public void selectedConfigChanged ()
    {
        if (!_block.enter()) {
            return;
        }
        try {
            getSelectedNode().getConfig().wasUpdated();
        } finally {
            _block.leave();
        }
    }

    // documentation inherited from interface ConfigGroupListener
    public void configAdded (ConfigEvent<ManagedConfig> event)
    {
        if (!_block.enter()) {
            return;
        }
        try {
            ManagedConfig config = event.getConfig();
            ConfigTreeNode root = (ConfigTreeNode)getModel().getRoot();
            Tuple<ConfigTreeNode, ConfigTreeNode> point =
                root.getInsertionPoint(config, config.getName());
            if (point.right.getParent() != null) {
                point.right.incrementCount();
            } else {
                ((DefaultTreeModel)getModel()).insertNodeInto(
                    point.right, point.left, point.left.getInsertionIndex(point.right));
            }
        } finally {
            _block.leave();
        }
    }

    // documentation inherited from interface ConfigGroupListener
    public void configRemoved (ConfigEvent<ManagedConfig> event)
    {
        if (!_block.enter()) {
            return;
        }
        try {
            String name = event.getConfig().getName();
            ConfigTreeNode node = ((ConfigTreeNode)getModel().getRoot()).getNode(name);
            if (node != null) {
                if (node.decrementCount() == 0) {
                    ((DefaultTreeModel)getModel()).removeNodeFromParent(node);
                }
            } else {
                log.warning("Missing config node [name=" + name + "].");
            }
        } finally {
            _block.leave();
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        if (!_block.enter()) {
            return;
        }
        try {
            selectedConfigUpdated();
        } finally {
            _block.leave();
        }
    }

    /**
     * Creates a new config tree to display the configurations in the specified group.
     *
     * @param editable if true, the tree will allow editing the configurations (only allowed for
     * trees depicting a single group).
     */
    protected ConfigTree (ConfigGroup[] groups, boolean editable)
    {
        @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig>[] mgroups =
            (ConfigGroup<ManagedConfig>[])groups;
        _groups = mgroups;
        setModel(new DefaultTreeModel(new ConfigTreeNode(null, null), true) {
            public void valueForPathChanged (TreePath path, Object newValue) {
                // save selection
                TreePath selection = getSelectionPath();

                // remove and reinsert with a unique name in sorted order
                ConfigTreeNode node = (ConfigTreeNode)path.getLastPathComponent();
                ConfigTreeNode parent = (ConfigTreeNode)node.getParent();
                removeNodeFromParent(node);
                node.setUserObject(parent.findNameForChild((String)newValue));
                insertNodeInto(node, parent, parent.getInsertionIndex(node));

                // re-expand paths, reapply the selection
                node.expandPaths(ConfigTree.this);
                setSelectionPath(selection);
            }
            public void insertNodeInto (MutableTreeNode child, MutableTreeNode parent, int index) {
                super.insertNodeInto(child, parent, index);
                if (!_block.enter()) {
                    return;
                }
                try {
                    ((ConfigTreeNode)child).addConfigs(_groups[0]);
                } finally {
                    _block.leave();
                }
            }
            public void removeNodeFromParent (MutableTreeNode node) {
                super.removeNodeFromParent(node);
                if (!_block.enter()) {
                    return;
                }
                try {
                    ((ConfigTreeNode)node).removeConfigs(_groups[0]);
                } finally {
                    _block.leave();
                }
            }
        });

        // start with some basics
        setRootVisible(false);
        setEditable(editable);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // the selection listener adds us as a listener for selected configs
        addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged (TreeSelectionEvent event) {
                ConfigTreeNode node = getSelectedNode();
                ManagedConfig config = (node == null) ? null : node.getConfig();
                if (_lconfig != null) {
                    _lconfig.removeListener(ConfigTree.this);
                }
                if ((_lconfig = config) != null) {
                    _lconfig.addListener(ConfigTree.this);
                }
            }
        });

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
                return MOVE;
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
                if (!node.verifyConfigClass(_groups[0].getConfigClass())) {
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

                // if we're moving within the tree, remove the original node here so that we
                // can reuse our identifiers
                if (onode != null && onode.getRoot() == parent.getRoot()) {
                    ((DefaultTreeModel)getModel()).removeNodeFromParent(onode);
                }

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
                    if (onode.getParent() != null) {
                        ((DefaultTreeModel)getModel()).removeNodeFromParent(onode);
                    }
                }
            }
        });

        // build the tree model and listen for updates
        ConfigTreeNode root = (ConfigTreeNode)getModel().getRoot();
        for (ConfigGroup<ManagedConfig> group : _groups) {
            for (ManagedConfig config : group.getConfigs()) {
                root.insertConfig(config, config.getName());
            }
            group.addListener(this);
        }
        ((DefaultTreeModel)getModel()).reload();

        // expand the paths up to a point
        root.expandPaths(this, 2);
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

    /** The configuration groups. */
    protected ConfigGroup<ManagedConfig>[] _groups;

    /** Indicates that we should ignore any changes, because we're the one effecting them. */
    protected ChangeBlock _block = new ChangeBlock();

    /** The configuration that we're listening to, if any. */
    protected ManagedConfig _lconfig;

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
