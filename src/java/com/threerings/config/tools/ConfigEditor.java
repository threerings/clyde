//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.samskivert.util.ListUtil;
import com.samskivert.util.QuickSort;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.export.SerializableWrapper;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigListener;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

import static com.threerings.ClydeLog.*;

/**
 * Allows editing the configuration database.  Can either be invoked standalone or from within
 * another application.
 */
public class ConfigEditor
    implements ActionListener, ItemListener, ClipboardOwner
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, "config/");
        new ConfigEditor(rsrcmgr, msgmgr, cfgmgr, true).start();
    }

    /**
     * Creates a new config editor.
     */
    public ConfigEditor (
        ResourceManager rsrcmgr, MessageManager msgmgr, ConfigManager cfgmgr, boolean standalone)
    {
        _rsrcmgr = rsrcmgr;
        _msgmgr = msgmgr;
        _cfgmgr = cfgmgr;
        _msgs = _msgmgr.getBundle("config");
        _standalone = standalone;

        _frame = new JFrame(_msgs.get("m.title"));
        _frame.setSize(800, 600);
        SwingUtil.centerWindow(_frame);

        // shutdown when the window is closed
        _frame.addWindowListener(new WindowAdapter() {
            public void windowClosing (WindowEvent event) {
                shutdown();
            }
        });

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);

        JMenu nmenu = createMenu("new", KeyEvent.VK_N);
        file.add(nmenu);
        nmenu.add(createMenuItem("config", KeyEvent.VK_C, KeyEvent.VK_N));
        nmenu.add(createMenuItem("folder", KeyEvent.VK_F, KeyEvent.VK_F));
        file.add(createMenuItem("open", KeyEvent.VK_O, KeyEvent.VK_O));
        file.addSeparator();
        file.add(createMenuItem("save", KeyEvent.VK_S, KeyEvent.VK_S));
        file.add(createMenuItem("save_as", KeyEvent.VK_A, KeyEvent.VK_A));
        file.add(_revert = createMenuItem("revert", KeyEvent.VK_R, KeyEvent.VK_R));
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(_cut = createMenuItem("cut", KeyEvent.VK_T, KeyEvent.VK_X));
        edit.add(_copy = createMenuItem("copy", KeyEvent.VK_C, KeyEvent.VK_C));
        edit.add(_paste = createMenuItem("paste", KeyEvent.VK_P, KeyEvent.VK_V));
        edit.add(_delete = createMenuItem("delete", KeyEvent.VK_D, KeyEvent.VK_DELETE, 0));

        // if running standalone, create and initialize the editable preferences to
        // allow changing the resource dir
        if (standalone) {
            edit.addSeparator();
            edit.add(createMenuItem("preferences", KeyEvent.VK_F, KeyEvent.VK_F));
            _eprefs = new ToolUtil.EditablePrefs(_prefs);
            _eprefs.init(_rsrcmgr);

            // initialize the configuration manager here, after we have set the resource dir
            _cfgmgr.init();
        }

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("config_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".xml");
            }
            public String getDescription () {
                return _msgs.get("m.xml_files");
            }
        });

        // create the chooser panel
        JPanel cpanel = GroupLayout.makeVStretchBox(5);
        _frame.add(cpanel, BorderLayout.WEST);
        cpanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        cpanel.setPreferredSize(new Dimension(250, 1));
        cpanel.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        // create the group panel
        JPanel gpanel = GroupLayout.makeHStretchBox(5);
        cpanel.add(gpanel, GroupLayout.FIXED);
        gpanel.add(new JLabel(_msgs.get("m.group")), GroupLayout.FIXED);

        // initialize the list of groups
        Collection<ConfigGroup> groups = _cfgmgr.getGroups();
        _gstates = new GroupState[groups.size()];
        int idx = 0;
        for (ConfigGroup group : groups) {
            _gstates[idx++] = new GroupState(group);
        }
        QuickSort.sort(_gstates);
        gpanel.add(_gbox = new JComboBox(_gstates));
        _gbox.addItemListener(this);

        cpanel.add(_pane = new JScrollPane());

        // create the editor panel
        _epanel = new EditorPanel(_msgs, EditorPanel.CategoryMode.TABS, null);
        _frame.add(_epanel, BorderLayout.CENTER);
        _epanel.setObject(new com.threerings.opengl.config.TextureConfig());

        // activate the first group
        _gstates[0].activate();
    }

    /**
     * Starts up the editor.
     */
    public void start ()
    {
        _frame.setVisible(true);
    }

    /**
     * Shuts down the editor.
     */
    public void shutdown ()
    {
        if (_standalone) {
            System.exit(0);
        } else {
            _frame.setVisible(false);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        GroupState gstate = (GroupState)_gbox.getSelectedItem();
        if (action.equals("config")) {
            gstate.newConfig();
        } else if (action.equals("folder")) {
            gstate.newFolder();
        } else if (action.equals("open")) {
            open();
        } else if (action.equals("save")) {

        } else if (action.equals("save_as")) {
            save();
        } else if (action.equals("revert")) {

        } else if (action.equals("quit")) {
            shutdown();
        } else if (action.equals("cut")) {
            gstate.cutNode();
        } else if (action.equals("copy")) {
            gstate.copyNode();
        } else if (action.equals("paste")) {
            gstate.pasteNode();
        } else if (action.equals("delete")) {
            gstate.deleteNode();
        } else if (action.equals("preferences")) {
            if (_pdialog == null) {
                _pdialog = EditorPanel.createDialog(_frame, _msgs, "t.preferences", _eprefs);
            }
            _pdialog.setVisible(true);
        }
    }

    // documentation inherited from interface ItemListener
    public void itemStateChanged (ItemEvent event)
    {
        ((GroupState)_gbox.getSelectedItem()).activate();
    }

    // documentation inherited from interface ClipboardOwner
    public void lostOwnership (Clipboard clipboard, Transferable contents)
    {
        _paste.setEnabled(false);
        _clipgroup = null;
    }

    /**
     * Creates a menu with the specified name and mnemonic.
     */
    protected JMenu createMenu (String name, int mnemonic)
    {
        return ToolUtil.createMenu(_msgs, name, mnemonic);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator.
     */
    protected JMenuItem createMenuItem (String action, int mnemonic, int accelerator)
    {
        return ToolUtil.createMenuItem(this, _msgs, action, mnemonic, accelerator);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    protected JMenuItem createMenuItem (
        String action, int mnemonic, int accelerator, int modifiers)
    {
        return ToolUtil.createMenuItem(this, _msgs, action, mnemonic, accelerator, modifiers);
    }

    /**
     * Creates a button with the specified action.
     */
    protected JButton createButton (String action)
    {
        return ToolUtil.createButton(this, _msgs, action);
    }

    /**
     * Creates a button with the specified action and translation key.
     */
    protected JButton createButton (String action, String key)
    {
        return ToolUtil.createButton(this, _msgs, action, key);
    }

    /**
     * Returns a translated label for the supplied one, if one exists; otherwise, simply returns
     * the untranslated name.
     */
    protected String getLabel (String name)
    {
        String key = "m." + name;
        return _msgs.exists(key) ? _msgs.get(key) : name;
    }

    /**
     * Brings up the open dialog.
     */
    protected void open ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            open(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to open the specified config file.
     */
    protected void open (File file)
    {

    }

    /**
     * Brings up the save dialog.
     */
    protected void save ()
    {
        if (_chooser.showSaveDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            save(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to save to the specified file.
     */
    protected void save (File file)
    {

    }

    /**
     * Contains the state of a single group.
     */
    protected class GroupState
        implements TreeSelectionListener, Comparable<GroupState>
    {
        /** The actual group reference. */
        public ConfigGroup group;

        /** The (possibly translated) group label. */
        public String label;

        /** The tree component. */
        public JTree tree;

        public GroupState (ConfigGroup group)
        {
            this.group = group;
            label = getLabel(group.getName());
        }

        /**
         * Activates this group.
         */
        public void activate ()
        {
            if (tree == null) {
                tree = new JTree(new ConfigTreeNode(null, null), true);
                tree.setRootVisible(false);
                tree.setEditable(true);

                // remove the mappings for cut/copy/paste since we handle those ourself
                InputMap imap = tree.getInputMap().getParent();
                imap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
                imap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
                imap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK));

                tree.setDragEnabled(true);
                tree.setTransferHandler(new TransferHandler() {
                    public int getSourceActions (JComponent comp) {
                        return COPY_OR_MOVE;
                    }
                    public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                        return ListUtil.contains(flavors, ToolUtil.SERIALIZED_WRAPPED_FLAVOR);
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
                        if (!node.verifyConfigClass(group.getConfigClass())) {
                            return false; // some other kind of config
                        }
                        ConfigTreeNode snode = getSelectedNode();
                        if (onode != null && onode == snode) {
                            return false; // can't drop onto the original
                        }
                        ConfigTreeNode parent = (ConfigTreeNode)tree.getModel().getRoot();
                        int index = parent.getChildCount();
                        if (snode != null && snode.getParent() != null) {
                            if (snode.getAllowsChildren()) {
                                parent = snode;
                                index = snode.getChildCount();
                            } else {
                                parent = (ConfigTreeNode)snode.getParent();
                                int oidx = (onode == null) ? -1 : parent.getIndex(onode);
                                int sidx = parent.getIndex(snode);
                                index = sidx + ((oidx >= 0 && sidx > oidx) ? 1 : 0);
                            }
                        }
                        // have to clone it in case we are going to paste it multiple times
                        node = (ConfigTreeNode)node.clone();
                        ((DefaultTreeModel)tree.getModel()).insertNodeInto(node, parent, index);
                        tree.setSelectionPath(new TreePath(node.getPath()));
                        return true;
                    }
                    protected Transferable createTransferable (JComponent c) {
                        ConfigTreeNode node = getSelectedNode();
                        return (node == null) ? null : new NodeTransfer(node, false);
                    }
                    protected void exportDone (JComponent source, Transferable data, int action) {
                        if (action == MOVE) {
                            ConfigTreeNode onode = ((NodeTransfer)data).onode;
                            ((DefaultTreeModel)tree.getModel()).removeNodeFromParent(onode);
                        }
                    }
                });
                tree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);
                tree.addTreeSelectionListener(this);
                tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
            _pane.setViewportView(tree);
            _paste.setEnabled(_clipgroup == this);
            updateSelection();
        }

        /**
         * Creates a new configuration and prepares it for editing.
         */
        public void newConfig ()
        {
            Class clazz = group.getConfigClass();
            try {
                newNode((ManagedConfig)clazz.newInstance());
            } catch (Exception e) {
                log.warning("Failed to instantiate config [class=" + clazz + "].", e);
            }
        }

        /**
         * Creates a new folder and prepares it for editing.
         */
        public void newFolder ()
        {
            newNode(null);
        }

        /**
         * Cuts the currently selected node.
         */
        public void cutNode ()
        {
            copyNode();
            deleteNode();
        }

        /**
         * Copies the currently selected node.
         */
        public void copyNode ()
        {
            Clipboard clipboard = tree.getToolkit().getSystemClipboard();
            clipboard.setContents(new NodeTransfer(getSelectedNode(), true), ConfigEditor.this);
            _clipgroup = this;
            _paste.setEnabled(true);
        }

        /**
         * Pastes the node in the clipboard.
         */
        public void pasteNode ()
        {
            Clipboard clipboard = tree.getToolkit().getSystemClipboard();
            tree.getTransferHandler().importData(tree, clipboard.getContents(this));
        }

        /**
         * Deletes the currently selected node.
         */
        public void deleteNode ()
        {
            ConfigTreeNode node = getSelectedNode();
            ConfigTreeNode parent = (ConfigTreeNode)node.getParent();
            int index = parent.getIndex(node);
            ((DefaultTreeModel)tree.getModel()).removeNodeFromParent(node);
            int ccount = parent.getChildCount();
            node = (ccount > 0) ?
                (ConfigTreeNode)parent.getChildAt(Math.min(index, ccount - 1)) : parent;
            if (node != tree.getModel().getRoot()) {
                tree.setSelectionPath(new TreePath(node.getPath()));
            }
        }

        // documentation inherited from interface TreeSelectionListener
        public void valueChanged (TreeSelectionEvent event)
        {
            updateSelection();
        }

        // documentation inherited from interface Comparable
        public int compareTo (GroupState other)
        {
            return label.compareTo(other.label);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return label;
        }

        /**
         * Updates the state of the UI based on the selection.
         */
        protected void updateSelection ()
        {
            // find the selected node
            ConfigTreeNode node = getSelectedNode();

            // update the editor panel
            _epanel.setObject(node == null ? null : node.config);

            // enable or disable the edit commands
            boolean enable = (node != null);
            _cut.setEnabled(enable);
            _copy.setEnabled(enable);
            _delete.setEnabled(enable);
        }

        /**
         * Creates a new node for the supplied configuration (or a folder node, if the
         * configuration is <code>null</code>).
         */
        protected void newNode (ManagedConfig config)
        {
            // find the parent under which we want to add the node
            ConfigTreeNode snode = getSelectedNode();
            ConfigTreeNode parent = (ConfigTreeNode)(snode == null ?
                tree.getModel().getRoot() : snode.getParent());

            // create a node with a unique name and start editing it
            String name = parent.findNameForChild(
                _msgs.get(config == null ? "m.new_folder" : "m.new_config"));
            ConfigTreeNode child = new ConfigTreeNode(name, config);
            ((DefaultTreeModel)tree.getModel()).insertNodeInto(
                child, parent, parent.getChildCount());
            tree.startEditingAtPath(new TreePath(child.getPath()));
        }

        /**
         * Returns the selected node, or <code>null</code> for none.
         */
        protected ConfigTreeNode getSelectedNode ()
        {
            TreePath path = tree.getSelectionPath();
            return (path == null) ? null : (ConfigTreeNode)path.getLastPathComponent();
        }
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

        @Override // documentation inherited
        public DataFlavor[] getTransferDataFlavors ()
        {
            return NODE_TRANSFER_FLAVORS;
        }

        @Override // documentation inherited
        public boolean isDataFlavorSupported (DataFlavor flavor)
        {
            return ListUtil.contains(NODE_TRANSFER_FLAVORS, flavor);
        }

        @Override // documentation inherited
        public Object getTransferData (DataFlavor flavor)
        {
            return (flavor == LOCAL_NODE_TRANSFER_FLAVOR) ?
                this : new SerializableWrapper(cnode);
        }
    }

    /**
     * A node in the config tree.
     */
    protected static class ConfigTreeNode extends DefaultMutableTreeNode
        implements Exportable
    {
        /** The configuration contained in this node, if any. */
        public ManagedConfig config;

        /**
         * Creates a new node.
         */
        public ConfigTreeNode (String partialName, ManagedConfig config)
        {
            super(partialName, config == null);
            this.config = config;
        }

        /**
         * No-arg constructor for deserialization.
         */
        public ConfigTreeNode ()
        {
        }

        /**
         * Verifies that if this node contains any actual configurations, they're instances of
         * the supplied class.
         */
        public boolean verifyConfigClass (Class clazz)
        {
            if (config != null) {
                return clazz.isInstance(config);
            }
            if (children != null) {
                for (Object child : children) {
                    if (!((ConfigTreeNode)child).verifyConfigClass(clazz)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Returns the full name of this node.
         */
        public String getName ()
        {
            String partialName = (String)userObject;
            String parentName = (parent == null) ? null : ((ConfigTreeNode)parent).getName();
            return (parentName == null) ? partialName : (parentName + "/" + partialName);
        }

        /**
         * Finds an unused name for a child of this node from the specified base.
         */
        public String findNameForChild (String base)
        {
            // get the set of all names
            HashSet<Object> names = new HashSet<Object>();
            if (children != null) {
                for (Object child : children) {
                    names.add(((ConfigTreeNode)child).getUserObject());
                }
            }

            // find one that hasn't been used
            if (!names.contains(base)) {
                return base;
            }
            for (int ii = 2;; ii++) {
                String name = base + " (" + ii + ")";
                if (!names.contains(name)) {
                    return name;
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
            allowsChildren = (config == null);
        }

        @Override // documentation inherited
        public Object clone ()
        {
            ConfigTreeNode cnode = (ConfigTreeNode)super.clone();
            if (cnode.config != null) {
                cnode.config = (ManagedConfig)config.clone();
            }
            cnode.parent = null;
            if (children != null) {
                cnode.children = new Vector();
                for (int ii = 0, nn = children.size(); ii < nn; ii++) {
                    ConfigTreeNode child = (ConfigTreeNode)children.get(ii);
                    cnode.insert((ConfigTreeNode)child.clone(), ii);
                }
            }
            return cnode;
        }
    }

    /** The resource manager. */
    protected ResourceManager _rsrcmgr;

    /** The message manager. */
    protected MessageManager _msgmgr;

    /** The config manager. */
    protected ConfigManager _cfgmgr;

    /** The config message bundle. */
    protected MessageBundle _msgs;

    /** Whether or not we're running as a standalone application. */
    protected boolean _standalone;

    /** The main frame. */
    protected JFrame _frame;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The edit menu items. */
    protected JMenuItem _cut, _copy, _paste, _delete;

    /** The file chooser for opening and saving config files. */
    protected JFileChooser _chooser;

    /** The editable preferences object. */
    protected ToolUtil.EditablePrefs _eprefs;

    /** The preferences dialog. */
    protected JDialog _pdialog;

    /** The configuration group states. */
    protected GroupState[] _gstates;

    /** The group that owns the clipboard selection, if any. */
    protected GroupState _clipgroup;

    /** Determines the selected group. */
    protected JComboBox _gbox;

    /** The scroll pane that holds the group trees. */
    protected JScrollPane _pane;

    /** The object editor panel. */
    protected EditorPanel _epanel;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ConfigEditor.class);

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
