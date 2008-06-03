//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.samskivert.util.QuickSort;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.editor.swing.EditorPanel;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

import static com.threerings.ClydeLog.*;

/**
 * Allows editing the configuration database.  Can either be invoked standalone or from within
 * another application.
 */
public class ConfigEditor
    implements ActionListener, ItemListener, ChangeListener, ClipboardOwner
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
        file.addSeparator();
        file.add(createMenuItem("save_group", KeyEvent.VK_S, KeyEvent.VK_S));
        file.add(createMenuItem("revert_group", KeyEvent.VK_R, KeyEvent.VK_R));
        file.addSeparator();
        file.add(createMenuItem("import_group", KeyEvent.VK_I, KeyEvent.VK_I));
        file.add(createMenuItem("export_group", KeyEvent.VK_E, KeyEvent.VK_E));
        file.addSeparator();
        file.add(createMenuItem("import_configs", KeyEvent.VK_C, -1));
        file.add(_exportConfigs = createMenuItem("export_configs", KeyEvent.VK_X, -1));
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

        JMenu gmenu = createMenu("groups", KeyEvent.VK_G);
        menubar.add(gmenu);
        gmenu.add(createMenuItem("save_all", KeyEvent.VK_S, KeyEvent.VK_A));
        gmenu.add(createMenuItem("revert_all", KeyEvent.VK_R, KeyEvent.VK_T));

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
        _epanel.addChangeListener(this);

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
        } else if (action.equals("save_group")) {
            gstate.group.save();
        } else if (action.equals("revert_group")) {
            gstate.group.revert();
        } else if (action.equals("import_group")) {
            importGroup();
        } else if (action.equals("export_group")) {
            exportGroup();
        } else if (action.equals("import_configs")) {
            importConfigs();
        } else if (action.equals("export_configs")) {
            exportConfigs();
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
        } else if (action.equals("save_all")) {
            _cfgmgr.saveAll();
        } else if (action.equals("revert_all")) {
            _cfgmgr.revertAll();
        }
    }

    // documentation inherited from interface ItemListener
    public void itemStateChanged (ItemEvent event)
    {
        ((GroupState)_gbox.getSelectedItem()).activate();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        ((GroupState)_gbox.getSelectedItem()).configChanged();
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
     * Brings up the import group dialog.
     */
    protected void importGroup ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            ((GroupState)_gbox.getSelectedItem()).group.load(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the export group dialog.
     */
    protected void exportGroup ()
    {
        if (_chooser.showSaveDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            ((GroupState)_gbox.getSelectedItem()).group.save(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the import config dialog.
     */
    protected void importConfigs ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            ((GroupState)_gbox.getSelectedItem()).group.load(_chooser.getSelectedFile(), true);
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the export config dialog.
     */
    protected void exportConfigs ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            ((GroupState)_gbox.getSelectedItem()).exportNode(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Contains the state of a single group.
     */
    protected class GroupState
        implements TreeSelectionListener, Comparable<GroupState>
    {
        /** The actual group reference. */
        public ConfigGroup<ManagedConfig> group;

        public GroupState (ConfigGroup group)
        {
            @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig> mgroup =
                (ConfigGroup<ManagedConfig>)group;
            this.group = mgroup;
            _label = getLabel(group.getName());
        }

        /**
         * Activates this group.
         */
        public void activate ()
        {
            if (_tree == null) {
                _tree = new ConfigTree(group, true) {
                    public void selectedConfigUpdated () {
                        _epanel.refresh();
                    }
                };
                _tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                _tree.addTreeSelectionListener(this);

                // remove the mappings for cut/copy/paste since we handle those ourself
                InputMap imap = _tree.getInputMap();
                imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK), "noop");
                imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK), "noop");
                imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK), "noop");
            }
            _pane.setViewportView(_tree);
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
            Clipboard clipboard = _tree.getToolkit().getSystemClipboard();
            clipboard.setContents(_tree.createClipboardTransferable(), ConfigEditor.this);
            _clipgroup = this;
            _paste.setEnabled(true);
        }

        /**
         * Pastes the node in the clipboard.
         */
        public void pasteNode ()
        {
            Clipboard clipboard = _tree.getToolkit().getSystemClipboard();
            _tree.getTransferHandler().importData(_tree, clipboard.getContents(this));
        }

        /**
         * Deletes the currently selected node.
         */
        public void deleteNode ()
        {
            ConfigTreeNode node = _tree.getSelectedNode();
            ConfigTreeNode parent = (ConfigTreeNode)node.getParent();
            int index = parent.getIndex(node);
            ((DefaultTreeModel)_tree.getModel()).removeNodeFromParent(node);
            int ccount = parent.getChildCount();
            node = (ccount > 0) ?
                (ConfigTreeNode)parent.getChildAt(Math.min(index, ccount - 1)) : parent;
            if (node != _tree.getModel().getRoot()) {
                _tree.setSelectionPath(new TreePath(node.getPath()));
            }
        }

        /**
         * Exports the configurations under the currently selected node to a file.
         */
        public void exportNode (File file)
        {
            ArrayList<ManagedConfig> configs = new ArrayList<ManagedConfig>();
            _tree.getSelectedNode().getConfigs(configs);
            group.save(configs, file);
        }

        /**
         * Notes that the state of the currently selected configuration has changed.
         */
        public void configChanged ()
        {
            _tree.selectedConfigChanged();
        }

        // documentation inherited from interface TreeSelectionListener
        public void valueChanged (TreeSelectionEvent event)
        {
            updateSelection();
        }

        // documentation inherited from interface Comparable
        public int compareTo (GroupState other)
        {
            return _label.compareTo(other._label);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return _label;
        }

        /**
         * Updates the state of the UI based on the selection.
         */
        protected void updateSelection ()
        {
            // find the selected node
            ConfigTreeNode node = _tree.getSelectedNode();

            // update the editor panel
            _epanel.setObject(node == null ? null : node.getConfig());

            // enable or disable the menu items
            boolean enable = (node != null);
            _exportConfigs.setEnabled(enable);
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
            ConfigTreeNode snode = _tree.getSelectedNode();
            ConfigTreeNode parent = (ConfigTreeNode)(snode == null ?
                _tree.getModel().getRoot() : snode.getParent());

            // create a node with a unique name and start editing it
            String name = parent.findNameForChild(
                _msgs.get(config == null ? "m.new_folder" : "m.new_config"));
            ConfigTreeNode child = new ConfigTreeNode(name, config);
            ((DefaultTreeModel)_tree.getModel()).insertNodeInto(
                child, parent, parent.getInsertionIndex(child));
            _tree.startEditingAtPath(new TreePath(child.getPath()));
        }

        /** The (possibly translated) group label. */
        protected String _label;

        /** The configuration tree. */
        protected ConfigTree _tree;
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

    /** The configuration export menu item. */
    protected JMenuItem _exportConfigs;

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
}
