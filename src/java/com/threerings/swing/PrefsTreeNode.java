//
// $Id$

package com.threerings.swing;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.threerings.export.util.ExportUtil;

import static com.threerings.ClydeLog.*;

/**
 * A node in a {@link PrefsTree}.  Nodes are either internal nodes with <code>null</code> values
 * (representing {@link Preferences} nodes) or leaf nodes with non-null values (representing
 * properties set in the parent {@link Preferences} node).
 */
public class PrefsTreeNode extends DefaultMutableTreeNode
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
     * Returns a reference to the decoded value of this node.
     */
    public Object getValue ()
    {
        return _value;
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
            getPreferenceNode(prefs);
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
        return (name.length() == 0) ? EMPTY_NAME : name.replace("/", SLASH_REPLACEMENT);
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
