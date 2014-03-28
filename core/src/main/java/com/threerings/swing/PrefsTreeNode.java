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

import java.io.IOException;

import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.export.util.ExportUtil;

import static com.threerings.ClydeLog.log;

/**
 * A node in a {@link PrefsTree}.  Nodes are either internal nodes with <code>null</code> values
 * (representing {@link Preferences} nodes) or leaf nodes with non-null values (representing
 * properties set in the parent {@link Preferences} node).
 */
public class PrefsTreeNode extends DefaultMutableTreeNode
    implements Exportable
{
    /**
     * Creates a new preferences node from the supplied preferences.
     */
    public PrefsTreeNode (Preferences prefs)
    {
        super(decode(prefs.name()), true);

        // create the value nodes, then the child nodes
        try {
            for (String key : prefs.keys()) {
                if (key.equals(EXPANDED)) {
                    continue;
                }
                byte[] bytes = prefs.getByteArray(key, null);
                Object object = (bytes == null) ? null : ExportUtil.fromBytes(bytes);
                if (object != null) {
                    insertSorted(new PrefsTreeNode(decode(key), object));
                }
            }
            for (String name : prefs.childrenNames()) {
                insertSorted(new PrefsTreeNode(prefs.node(name)));
            }
        } catch (BackingStoreException e) {
            log.warning("Error reading preferences.", "prefs", prefs, e);
        }

        // initialize the expanded flag
        _expanded = prefs.getBoolean(EXPANDED, true);
    }

    /**
     * Creates a new preferences node.
     */
    public PrefsTreeNode (String name, Object value)
    {
        super(name, value == null);
        _value = value;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public PrefsTreeNode ()
    {
    }

    /**
     * Returns a reference to the decoded value of this node.
     */
    public Object getValue ()
    {
        return _value;
    }

    /**
     * Returns the child with the supplied name, if it exists.
     */
    public PrefsTreeNode getChild (String name)
    {
        for (int ii = 0, nn = getChildCount(); ii < nn; ii++) {
            PrefsTreeNode child = (PrefsTreeNode)getChildAt(ii);
            if (child.getUserObject().equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Sets whether or not this node is expanded in the tree.
     */
    public void setExpanded (JTree tree, Preferences prefs, boolean expanded)
    {
        getPreferenceNode(prefs).putBoolean(EXPANDED, _expanded = expanded);
        if (_expanded && children != null) {
            for (Object child : children) {
                ((PrefsTreeNode)child).expandPaths(tree);
            }
        }
    }

    /**
     * Finds a unique name for a new child node derived from the supplied base.
     */
    public String findNameForChild (String base)
    {
        String name = base;
        for (int ii = 2; haveChildNamed(name); ii++) {
            name = base + " (" + ii + ")";
        }
        return name;
    }

    /**
     * Adds the contents of this node to the supplied preferences.
     */
    public void addToPreferences (Preferences prefs)
    {
        if (getAllowsChildren()) {
            // simply attempting to retrieve the node will create it
            getPreferenceNode(prefs).putBoolean(EXPANDED, _expanded);
            for (int ii = 0, nn = getChildCount(); ii < nn; ii++) {
                ((PrefsTreeNode)getChildAt(ii)).addToPreferences(prefs);
            }
        } else {
            byte[] bytes = ExportUtil.toBytes(_value);
            if (bytes != null) {
                PrefsTreeNode parent = (PrefsTreeNode)getParent();
                parent.getPreferenceNode(prefs).putByteArray(getName(), bytes);
            }
        }
    }

    /**
     * Removes the contents of this node from the supplied preferences.
     */
    public void removeFromPreferences (Preferences prefs)
    {
        if (getAllowsChildren()) {
            Preferences pnode = getPreferenceNode(prefs);
            try {
                pnode.removeNode();
            } catch (BackingStoreException e) {
                log.warning("Error removing preferences.", "prefs", pnode, e);
            }
        } else {
            PrefsTreeNode parent = (PrefsTreeNode)getParent();
            parent.getPreferenceNode(prefs).remove(getName());
        }
    }

    /**
     * Returns the index at which the specified child should be inserted.
     */
    public int getInsertionIndex (PrefsTreeNode child)
    {
        if (children == null) {
            return 0;
        }
        String name = (String)child.getUserObject();
        boolean folder = child.getAllowsChildren();
        for (int ii = 0, nn = children.size(); ii < nn; ii++) {
            PrefsTreeNode ochild = (PrefsTreeNode)children.get(ii);
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
                    ((PrefsTreeNode)child).expandPaths(tree);
                }
            }
        }
    }

    /**
     * Custom field write method.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.defaultWriteFields();
        out.write("parent", (PrefsTreeNode)parent, null, PrefsTreeNode.class);
        out.write("children", children, null, Vector.class);
        out.write("userObject", (String)userObject, (String)null);
    }

    /**
     * Custom field read method.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        setAllowsChildren(_value == null);
        parent = in.read("parent", null, PrefsTreeNode.class);
        children = in.read("children", null, Vector.class);
        userObject = in.read("userObject", (String)null);
    }

    /**
     * Determines whether the node has a child with the given name.
     */
    protected boolean haveChildNamed (String name)
    {
        // compare the names as encoded
        String ename = encode(name);
        for (int ii = 0, nn = getChildCount(); ii < nn; ii++) {
            if (((PrefsTreeNode)getChildAt(ii)).getName().equals(ename)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the preference node corresponding to this node.
     */
    protected Preferences getPreferenceNode (Preferences prefs)
    {
        return prefs.node(getNodeName());
    }

    /**
     * Returns the name of the node relative to the preferences.
     */
    protected String getNodeName ()
    {
        PrefsTreeNode parent = (PrefsTreeNode)getParent();
        if (parent == null) {
            return "";
        } else if (parent.isRoot()) {
            return getName();
        } else {
            return parent.getNodeName() + "/" + getName();
        }
    }

    /**
     * Returns the (encoded) name of this node.
     */
    protected String getName ()
    {
        return encode((String)userObject);
    }

    /**
     * Inserts the specified node in sorted order.
     */
    protected void insertSorted (PrefsTreeNode child)
    {
        insert(child, getInsertionIndex(child));
    }

    /**
     * Encodes the supplied name, making it safe for use as a {@link Preferences} name.
     */
    protected static String encode (String name)
    {
        return name.isEmpty() ? EMPTY_NAME : name.replace("/", SLASH_REPLACEMENT);
    }

    /**
     * Decodes the supplied name encoded with {@link #encode}.
     */
    protected static String decode (String name)
    {
        return name.equals(EMPTY_NAME) ? "" : name.replace(SLASH_REPLACEMENT, "/");
    }

    /** The value of this node, if it is a leaf. */
    protected Object _value;

    /** Whether or not this node is expanded in the tree. */
    protected boolean _expanded;

    /** The name of the expanded property. */
    protected static final String EXPANDED = "%EXPANDED%";

    /** Our replacement for the empty name. */
    protected static final String EMPTY_NAME = "%EMPTY%";

    /** Replacement for slashes, which are not allowed in names. */
    protected static final String SLASH_REPLACEMENT = "%SLASH%";
}
