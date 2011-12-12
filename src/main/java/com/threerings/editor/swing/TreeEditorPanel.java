//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

package com.threerings.editor.swing;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.io.File;

import java.lang.reflect.Array;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.export.util.SerializableWrapper;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform2D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepUtil;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.*;

/**
 * Allows editing properties of an object in tree mode.
 */
public class TreeEditorPanel extends BaseEditorPanel
{
    /**
     * Creates an empty editor panel.
     */
    public TreeEditorPanel (EditorContext ctx, Property[] ancestors, boolean omitColumns)
    {
        super(ctx, ancestors, omitColumns);

        _tree = new JTree(new Object[0]);
        add(isEmbedded() ? _tree : new JScrollPane(_tree));

        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.setDragEnabled(true);
        _tree.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                return ListUtil.contains(flavors, ToolUtil.SERIALIZED_WRAPPED_FLAVOR);
            }
            @Override public boolean importData (JComponent comp, Transferable t) {
                if (!canImport(comp, t.getTransferDataFlavors())) {
                    return false; // this isn't checked automatically for paste
                }
                boolean local = t.isDataFlavorSupported(LOCAL_NODE_TRANSFER_FLAVOR);
                Object data;
                try {
                    data = t.getTransferData(local ?
                        LOCAL_NODE_TRANSFER_FLAVOR : ToolUtil.SERIALIZED_WRAPPED_FLAVOR);
                } catch (Exception e) {
                    log.warning("Failure importing data.", e);
                    return false;
                }
                DefaultMutableTreeNode node = null;
                Object value;
                if (local) {
                    NodeTransfer transfer = (NodeTransfer)data;
                    node = transfer.node;
                    value = transfer.value;
                } else {
                    value = ((SerializableWrapper)data).getObject();
                }
                // no copying nodes onto themselves
                DefaultMutableTreeNode snode = getSelectedNode();
                if (node == snode) {
                    return false;
                }

                // have to clone the value in case we are going to paste it multiple times
                value = DeepUtil.copy(value);

                // perhaps copy the value onto a compatible property
                NodeObject snobj = (NodeObject)snode.getUserObject();
                DefaultMutableTreeNode pnode = (DefaultMutableTreeNode)snode.getParent();
                NodeObject pnobj = (NodeObject)pnode.getUserObject();
                if (snobj.property != null && snobj.property.isLegalValue(value)) {
                    Object object = pnobj.value;
                    if (snobj.comp instanceof String) {
                        object = ((ConfigReference)object).getArguments();
                    }
                    snobj.property.set(object, value);
                    populateNode(snode, getLabel(snobj.property), value,
                        snobj.property.getSubtypes(), snobj.property, snobj.comp);
                    ((DefaultTreeModel)_tree.getModel()).reload(snode);
                    fireStateChanged();
                    return true;
                }

                // or into a compatible array/list
                NodeObject dnobj;
                DefaultMutableTreeNode dnode;
                int idx;
                if (snobj.comp instanceof Integer && pnobj.property != null &&
                        pnobj.property.isLegalComponentValue(value)) {
                    dnobj = pnobj;
                    dnode = pnode;
                    idx = (Integer)snobj.comp;

                } else if (snobj.property != null && snobj.property.getComponentType() != null &&
                        snobj.property.isLegalComponentValue(value)) {
                    dnobj = snobj;
                    dnode = snode;
                    idx = Integer.MAX_VALUE;

                } else {
                    return false;
                }
                // find out if we're moving within the list (and from where)
                int oidx = -1;
                pnode = (DefaultMutableTreeNode)node.getParent();
                if (pnode != null) {
                    pnobj = (NodeObject)pnode.getUserObject();
                    if (pnobj.value == dnobj.value) {
                        NodeObject nobj = (NodeObject)node.getUserObject();
                        if (nobj.comp instanceof Integer) {
                            oidx = (Integer)nobj.comp;
                        }
                    }
                }

                // add/move element in list/array
                if (dnobj.value instanceof List) {
                    @SuppressWarnings("unchecked") List<Object> list =
                        (List<Object>)dnobj.value;
                    idx = Math.min(idx, list.size());
                    if (oidx != -1) {
                        if (idx == oidx) {
                            return false;
                        }
                        list.remove(oidx);
                    }
                    list.add(idx, value);

                } else {
                    int len = Array.getLength(dnobj.value);
                    idx = Math.min(idx, len);
                    if (oidx != -1) {
                        if (idx == oidx) {
                            return false;
                        }
                        // store value, shift elements up/down, replace
                        Object tmp = Array.get(dnobj.value, oidx);
                        if (oidx < idx) {
                            System.arraycopy(dnobj.value, oidx + 1, dnobj.value, oidx, idx - oidx);
                        } else {
                            System.arraycopy(dnobj.value, idx, dnobj.value, idx + 1, oidx - idx);
                        }
                        Array.set(dnobj.value, idx, value);

                    } else {
                        Object narray = Array.newInstance(
                            dnobj.value.getClass().getComponentType(), len + 1);
                        System.arraycopy(dnobj.value, 0, narray, 0, idx);
                        Array.set(narray, idx, value);
                        System.arraycopy(dnobj.value, idx, narray, idx + 1, len - idx);
                        Object pdnobj = ((DefaultMutableTreeNode)dnode.getParent()).getUserObject();
                        dnobj.property.set(((NodeObject)pdnobj).value, dnobj.value = narray);
                    }
                }
                populateNode(dnode, getLabel(dnobj.property), dnobj.value,
                    dnobj.property.getSubtypes(), dnobj.property, dnobj.comp);
                ((DefaultTreeModel)_tree.getModel()).reload(dnode);
                fireStateChanged();
                return true;
            }
            @Override public int getSourceActions (JComponent comp) {
                return COPY_OR_MOVE;
            }
            @Override protected Transferable createTransferable (JComponent comp) {
                DefaultMutableTreeNode node = getSelectedNode();
                return (node == null) ? null : new NodeTransfer(node, false);
            }
            @Override protected void exportDone (
                    JComponent source, Transferable data, int action) {
                if (action != MOVE) {
                    return;
                }
                // remove element from list/array (if applicable and not already removed)
                DefaultMutableTreeNode node = ((NodeTransfer)data).node;
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
                NodeObject nodeobj = (NodeObject)node.getUserObject();
                if (parent == null || !(nodeobj.comp instanceof Integer)) {
                    return;
                }
                NodeObject pnobj = (NodeObject)parent.getUserObject();
                int idx = (Integer)nodeobj.comp;
                if (pnobj.value instanceof List) {
                    ((List)pnobj.value).remove(idx);
                } else {
                    int len = Array.getLength(pnobj.value);
                    Object narray = Array.newInstance(
                        pnobj.value.getClass().getComponentType(), len - 1);
                    System.arraycopy(pnobj.value, 0, narray, 0, idx);
                    System.arraycopy(pnobj.value, idx + 1, narray, idx, len - 1 - idx);
                    Object gpnobj = ((DefaultMutableTreeNode)parent.getParent()).getUserObject();
                    pnobj.property.set(((NodeObject)gpnobj).value, pnobj.value = narray);
                }
                populateNode(parent, getLabel(pnobj.property), pnobj.value,
                    pnobj.property.getSubtypes(), pnobj.property, pnobj.comp);
                ((DefaultTreeModel)_tree.getModel()).reload(parent);
                fireStateChanged();
            }
        });
    }

    @Override // documentation inherited
    public void setObject (Object object)
    {
        // make sure it's not the same object
        if (object == _object) {
            return;
        }
        super.setObject(object);
        update();
    }

    @Override // documentation inherited
    public void update ()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
            new NodeObject(null, _object, null, null));
        addPropertyNodes(root, _object);
        _tree.setModel(new DefaultTreeModel(root, true));
    }

    @Override // documentation inherited
    protected String getMousePath (Point pt)
    {
        pt = _tree.getMousePosition();
        TreePath path = (pt == null) ? null : _tree.getPathForLocation(pt.x, pt.y);
        if (path == null) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        for (int ii = 1, nn = path.getPathCount(); ii < nn; ii++) {
            NodeObject obj = (NodeObject)((DefaultMutableTreeNode)
                path.getPathComponent(ii)).getUserObject();
            if (obj.comp instanceof Integer) {
                buf.append('[').append(obj.comp).append(']');
            } else if (obj.comp instanceof String) {
                buf.append("[\"").append(((String)obj.comp).replace("\"", "\\\"")).append("\"]");
            } else { // obj.comp == null
                buf.append('.').append(obj.property.getName());
            }
        }
        return buf.toString();
    }

    /**
     * Creates a {@link Transferable} containing the selected node for the clipboard.
     */
    protected Transferable createClipboardTransferable ()
    {
        DefaultMutableTreeNode node = getSelectedNode();
        return (node == null) ? null : new NodeTransfer(node, true);
    }

    /**
     * Returns the selected node, or <code>null</code> for none.
     */
    protected DefaultMutableTreeNode getSelectedNode ()
    {
        TreePath path = _tree.getSelectionPath();
        return (path == null) ? null : (DefaultMutableTreeNode)path.getLastPathComponent();
    }

    /**
     * Adds child nodes for the specified object's properties to the specified parent node.
     */
    protected void addPropertyNodes (DefaultMutableTreeNode parent, Object object)
    {
        Property[] properties = Introspector.getProperties(object);
        if (properties.length == 0) {
            return;
        }
        parent.setAllowsChildren(true);
        for (Property property : properties) {
            addNode(parent, getLabel(property), property.get(object),
                property.getSubtypes(), property, null);
        }
    }

    /**
     * Adds a child node for the specified labeled value.
     */
    protected void addNode (
        DefaultMutableTreeNode parent, String label, Object value,
        Class<?>[] subtypes, Property property, Object comp)
    {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        populateNode(child, label, value, subtypes, property, comp);
        parent.add(child);
    }

    /**
     * (Re)populates the specified node.
     */
    protected void populateNode (
        DefaultMutableTreeNode node, String label, Object value,
        Class<?>[] subtypes, Property property, Object comp)
    {
        if (value == null || value instanceof Boolean || value instanceof Number ||
                value instanceof Color4f || value instanceof File ||
                value instanceof Quaternion || value instanceof String ||
                value instanceof Transform2D || value instanceof Transform3D ||
                value instanceof Vector2f || value instanceof Vector3f ||
                value instanceof Enum) {
            Object dval = value;
            if (value == null) {
                dval = _msgs.get("m.null_value");

            } else if (value instanceof String) {
                dval = "\"" + value + "\"";

            } else if (value instanceof Enum) {
                Enum<?> eval = (Enum)value;
                dval = getLabel(eval, _msgmgr.getBundle(
                    Introspector.getMessageBundle(eval.getDeclaringClass())));
            }
            node.setUserObject(new NodeObject(label + ": " + dval, value, property, comp));
            node.setAllowsChildren(false);

        } else if (value instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference)value;
            String name = ref.getName();
            node.setUserObject(new NodeObject(label + ": " + name, value, property, comp));
            node.setAllowsChildren(false);
            if (property != null) {
                @SuppressWarnings("unchecked") Class<ManagedConfig> clazz =
                    (Class<ManagedConfig>)property.getArgumentType(ConfigReference.class);
                ManagedConfig config = _ctx.getConfigManager().getConfig(clazz, name);
                if (config instanceof ParameterizedConfig) {
                    ParameterizedConfig pconfig = (ParameterizedConfig)config;
                    if (pconfig.parameters.length > 0) {
                        for (Parameter param : pconfig.parameters) {
                            Property aprop = param.getArgumentProperty(pconfig);
                            if (aprop != null) {
                                node.setAllowsChildren(true);
                                addNode(node, param.name, aprop.get(ref.getArguments()),
                                    aprop.getSubtypes(), aprop, param.name);
                            }
                        }
                    }
                }
            }
        } else if (value instanceof List || value.getClass().isArray()) {
            node.setUserObject(new NodeObject(label, value, property, comp));
            node.removeAllChildren();
            node.setAllowsChildren(true);
            Class<?>[] componentSubtypes = (property != null) ?
                property.getComponentSubtypes() : new Class[0];
            if (value instanceof List) {
                List<?> list = (List)value;
                for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                    addNode(node, String.valueOf(ii), list.get(ii), componentSubtypes, null, ii);
                }
            } else {
                for (int ii = 0, nn = Array.getLength(value); ii < nn; ii++) {
                    addNode(node, String.valueOf(ii), Array.get(value, ii),
                        componentSubtypes, null, ii);
                }
            }
        } else {
            if (subtypes.length > 1) {
                label = label + ": " + getLabel(value.getClass());
            }
            node.setUserObject(new NodeObject(label, value, property, comp));
            node.setAllowsChildren(false);
            addPropertyNodes(node, value);
        }
    }

    /**
     * A user object for a tree node.
     */
    protected static class NodeObject
    {
        /** The object's string representation. */
        public final String label;

        /** The object's value. */
        public Object value;

        /** The node property. */
        public final Property property;

        /** The array index or the parameter name, if applicable. */
        public final Object comp;

        /**
         * Creates a new node object.
         */
        public NodeObject (String label, Object value, Property property, Object comp)
        {
            this.label = label;
            this.value = value;
            this.property = property;
            this.comp = comp;
        }

        @Override // documentation inherited
        public String toString ()
        {
            return label;
        }
    }

    /**
     * Contains a node for transfer.
     */
    protected static class NodeTransfer
        implements Transferable
    {
        /** The node being transferred. */
        public DefaultMutableTreeNode node;

        /** The contained value. */
        public Object value;

        public NodeTransfer (DefaultMutableTreeNode node, boolean clipboard)
        {
            this.node = clipboard ? null : node;
            value = DeepUtil.copy(((NodeObject)node.getUserObject()).value);
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
                this : new SerializableWrapper(value);
        }
    }

    /** The tree component. */
    protected JTree _tree;

    /** A data flavor that provides access to the actual transfer object. */
    protected static final DataFlavor LOCAL_NODE_TRANSFER_FLAVOR =
        ToolUtil.createLocalFlavor(NodeTransfer.class);

    /** The flavors available for node transfer. */
    protected static final DataFlavor[] NODE_TRANSFER_FLAVORS =
        { LOCAL_NODE_TRANSFER_FLAVOR, ToolUtil.SERIALIZED_WRAPPED_FLAVOR };
}
