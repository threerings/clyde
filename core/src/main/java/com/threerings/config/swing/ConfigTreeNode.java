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

package com.threerings.config.swing;

import java.io.IOException;

import java.util.List;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.samskivert.util.Tuple;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

import com.threerings.config.ConfigGroup;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;

/**
 * A node in the config tree.
 */
public class ConfigTreeNode extends DefaultMutableTreeNode
    implements Exportable
{
    /**
     * Creates a new node with the specified partial name.
     *
     * @param config the configuration for this node, or <code>null</code> if this is a
     * folder node.
     */
    public ConfigTreeNode (String partialName, ManagedConfig config)
    {
        super(partialName, config == null);
        _config = config;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigTreeNode ()
    {
    }

    /**
     * Returns the full name of this node.
     */
    public String getName ()
    {
        String partialName = encode((String)userObject);
        String parentName = (parent == null) ? null : ((ConfigTreeNode)parent).getName();
        return (parentName == null) ? partialName : (parentName + "/" + partialName);
    }

    /**
     * Returns the configuration contained in this node, or <code>null</code> for none.
     */
    public ManagedConfig getConfig ()
    {
        return _config;
    }

    /**
     * Increments the count for this node.
     */
    public void incrementCount ()
    {
        _count++;
    }

    /**
     * Decrements the count for this node.
     */
    public int decrementCount ()
    {
        return --_count;
    }

    /**
     * Sets whether or not this node is expanded in the tree.
     */
    public void setExpanded (boolean expanded)
    {
        _expanded = expanded;
    }

    /**
     * Finds the insertion point (existing parent and new child) for the specified configuration.
     * The new child will contain the necessary intermediate nodes.
     */
    public Tuple<ConfigTreeNode, ConfigTreeNode> getInsertionPoint (
        ManagedConfig config, String name)
    {
        int idx = name.indexOf('/');
        if (idx == -1) {
            // last path component; insert in this node
            String partial = decode(name);
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            if (child == null) {
                child = new ConfigTreeNode(partial, config);
            }
            return new Tuple<ConfigTreeNode, ConfigTreeNode>(this, child);

        } else {
            // find (or create) next component in path, pass on the config
            String partial = decode(name.substring(0, idx));
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            if (child == null) {
                child = new ConfigTreeNode(partial, null);
                child.insertConfig(config, name.substring(idx + 1));
                return new Tuple<ConfigTreeNode, ConfigTreeNode>(this, child);
            }
            return child.getInsertionPoint(config, name.substring(idx + 1));
        }
    }

    /**
     * Inserts the given configuration under this node, creating any required intermediate
     * nodes.
     */
    public void insertConfig (ManagedConfig config, String name)
    {
        int idx = name.indexOf('/');
        if (idx == -1) {
            // last path component; insert in this node
            String partial = decode(name);
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            if (child == null) {
                child = new ConfigTreeNode(partial, config);
                insert(child, getInsertionIndex(child));
            } else {
                child.incrementCount();
            }

        } else {
            // find (or create) next component in path, pass on the config
            String partial = decode(name.substring(0, idx));
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            if (child == null) {
                child = new ConfigTreeNode(partial, null);
                insert(child, getInsertionIndex(child));
            }
            child.insertConfig(config, name.substring(idx + 1));
        }
    }

    /**
     * Adds all configurations under this node to the supplied group.
     */
    public void addConfigs (ConfigGroup<ManagedConfig> group)
    {
        if (_config != null) {
            _config.setName(getName());
            group.addConfig(_config);

        } else if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).addConfigs(group);
            }
        }
    }

    /**
     * Removes all configurations under this node from the supplied group.
     */
    public void removeConfigs (ConfigGroup<ManagedConfig> group)
    {
        if (_config != null) {
            group.removeConfig(_config);

        } else if (children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).removeConfigs(group);
            }
        }
    }

    /**
     * Puts all of the configurations under this node into the supplied list.
     */
    public void getConfigs (List<ManagedConfig> configs)
    {
        getConfigs(configs, false);
    }

    /**
     * Puts all of the configurations under this node into the supplied list.
     */
    public void getConfigs (List<ManagedConfig> configs, boolean immediate)
    {
        if (_config != null) {
            configs.add(_config);

        } else if (children != null) {
            for (Object child : children) {
                if (immediate) {
                    ManagedConfig config = ((ConfigTreeNode)child).getConfig();
                    if (config != null) {
                        configs.add(config);
                    }
                } else {
                    ((ConfigTreeNode)child).getConfigs(configs);
                }
            }
        }
    }

    /**
     * Finds and returns the node with the specified name (or <code>null</code> if it can't be
     * found).
     */
    public ConfigTreeNode getNode (String name)
    {
        int idx = name.indexOf('/');
        if (idx == -1) {
            // last path component; look for it in this node
            return (_childrenByName == null) ? null : _childrenByName.get(decode(name));

        } else {
            // find (or create) next component in path, and look for it there
            String partial = decode(name.substring(0, idx));
            ConfigTreeNode child = (_childrenByName == null) ? null : _childrenByName.get(partial);
            return (child == null) ? null : child.getNode(name.substring(idx + 1));
        }
    }

    /**
     * Verifies that if this node contains any actual configurations, they're instances of
     * the supplied class.
     */
    public boolean verifyConfigClass (Class<?> clazz)
    {
        if (_config != null) {
            return clazz.isInstance(_config) || (_config instanceof DerivedConfig);

        } else if (children != null) {
            for (Object child : children) {
                if (!((ConfigTreeNode)child).verifyConfigClass(clazz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Finds an unused name for a child of this node from the specified base.
     */
    public String findNameForChild (String base)
    {
        if (_childrenByName == null || !_childrenByName.containsKey(base)) {
            return base;
        }
        for (int ii = 2;; ii++) {
            String name = base + " (" + ii + ")";
            if (!_childrenByName.containsKey(name)) {
                return name;
            }
        }
    }

    /**
     * Returns the index at which the specified node should be inserted to maintain the sort
     * order.
     */
    public int getInsertionIndex (ConfigTreeNode child)
    {
        if (children == null) {
            return 0;
        }
        String name = (String)child.getUserObject();
        boolean folder = child.getAllowsChildren();
        for (int ii = 0, nn = children.size(); ii < nn; ii++) {
            ConfigTreeNode ochild = (ConfigTreeNode)children.get(ii);
            String oname = (String)ochild.getUserObject();
            boolean ofolder = ochild.getAllowsChildren();
            if ((folder == ofolder) ? (name.compareTo(oname) <= 0) : !folder) {
                return ii;
            }
        }
        return children.size();
    }

    /**
     * Expands paths according to the {@link #_expanded} field.
     */
    public void expandPaths (JTree tree)
    {
        if (_expanded) {
            tree.expandPath(new TreePath(getPath()));
            if (children != null) {
                for (Object child : children) {
                    ((ConfigTreeNode)child).expandPaths(tree);
                }
            }
        }
    }

    /**
     * Expands all paths up to the specified depth.
     */
    public void expandPaths (JTree tree, int depth)
    {
        if (!getAllowsChildren()) {
            return;
        }
        tree.expandPath(new TreePath(getPath()));
        if (depth-- > 0 && children != null) {
            for (Object child : children) {
                ((ConfigTreeNode)child).expandPaths(tree, depth);
            }
        }
    }

    /**
     * Writes the exportable fields of the object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.defaultWriteFields();
        out.write("name", (String)userObject, (String)null);
        out.write("parent", parent, null, MutableTreeNode.class);
        out.write("children", children, null, Vector.class);
    }

    /**
     * Reads the exportable fields of the object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        userObject = in.read("name", (String)null);
        parent = in.read("parent", null, MutableTreeNode.class);
        children = in.read("children", null, Vector.class);
        if (children != null) {
            _childrenByName = new HashMap<String, ConfigTreeNode>(children.size());
            for (Object child : children) {
                ConfigTreeNode node = (ConfigTreeNode)child;
                _childrenByName.put((String)node.getUserObject(), node);
            }
        }
        allowsChildren = (_config == null);
    }

    @Override
    public void insert (MutableTreeNode child, int index)
    {
        super.insert(child, index);
        if (_childrenByName == null) {
            _childrenByName = new HashMap<String, ConfigTreeNode>(1);
        }
        ConfigTreeNode node = (ConfigTreeNode)child;
        _childrenByName.put((String)node.getUserObject(), node);
    }

    @Override
    public void remove (int index)
    {
        ConfigTreeNode child = (ConfigTreeNode)children.get(index);
        _childrenByName.remove(child.getUserObject());
        if (_childrenByName.isEmpty()) {
            _childrenByName = null;
        }
        super.remove(index);
    }

    @Override
    public ConfigTreeNode clone ()
    {
        ConfigTreeNode cnode = (ConfigTreeNode)super.clone();
        cnode.parent = null;
        if (_config != null) {
            cnode._config = (ManagedConfig)_config.clone();

        } else if (children != null) {
            cnode.children = new Vector<ConfigTreeNode>();
            for (int ii = 0, nn = children.size(); ii < nn; ii++) {
                ConfigTreeNode child = (ConfigTreeNode)children.get(ii);
                cnode.insert(child.clone(), ii);
            }
        }
        return cnode;
    }

    /**
     * Encodes a partial name for use in a path name.
     */
    protected static String encode (String name)
    {
        return (name == null) ? null : name.replace("/", SLASH_REPLACEMENT);
    }

    /**
     * Decodes a name encoded with {@link #encode}.
     */
    protected static String decode (String name)
    {
        return (name == null) ? null : name.replace(SLASH_REPLACEMENT, "/");
    }

    /** The configuration contained in this node, if any. */
    protected ManagedConfig _config;

    /** The number of copies of this node. */
    protected int _count = 1;

    /** Whether or not this node is expanded in the tree. */
    protected boolean _expanded;

    /** The children of this node, mapped by (partial) name. */
    protected transient HashMap<String, ConfigTreeNode> _childrenByName;

    /** Because we use slashes as name delimiters, any slashes in the name must be replaced. */
    protected static final String SLASH_REPLACEMENT = "%SLASH%";
}
