//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.samskivert.util.QuickSort;

import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.util.EditorContext;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;
import com.threerings.config.swing.ConfigTree;
import com.threerings.config.swing.ConfigTreeNode;

import static com.threerings.ClydeLog.*;

/**
 * Allows editing the configuration database.  Can either be invoked standalone or from within
 * another application.
 */
public class ConfigEditor extends BaseConfigEditor
    implements ClipboardOwner
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, "config/");
        ColorPository colorpos = ColorPository.loadColorPository(rsrcmgr);
        new ConfigEditor(msgmgr, cfgmgr, colorpos).setVisible(true);
    }

    /**
     * Creates a new config editor.
     */
    public ConfigEditor (MessageManager msgmgr, ConfigManager cfgmgr, ColorPository colorpos)
    {
        this(msgmgr, cfgmgr, colorpos, null, null);
    }

    /**
     * Creates a new config editor.
     */
    public ConfigEditor (
        MessageManager msgmgr, ConfigManager cfgmgr, ColorPository colorpos,
        Class clazz, String name)
    {
        super(msgmgr, cfgmgr, colorpos, "config");
        setSize(850, 600);
        SwingUtil.centerWindow(this);

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);

        JMenu nmenu = createMenu("new", KeyEvent.VK_N);
        file.add(nmenu);
        nmenu.add(createMenuItem("window", KeyEvent.VK_W, KeyEvent.VK_N));
        nmenu.addSeparator();
        Action nconfig = createAction("config", KeyEvent.VK_C, KeyEvent.VK_F);
        nmenu.add(new JMenuItem(nconfig));
        Action nfolder = createAction("folder", KeyEvent.VK_F, KeyEvent.VK_D);
        nmenu.add(new JMenuItem(nfolder));
        file.addSeparator();
        file.add(_save = createMenuItem("save_group", KeyEvent.VK_S, KeyEvent.VK_S));
        file.add(_revert = createMenuItem("revert_group", KeyEvent.VK_R, KeyEvent.VK_R));
        file.addSeparator();
        file.add(createMenuItem("import_group", KeyEvent.VK_I, KeyEvent.VK_I));
        file.add(createMenuItem("export_group", KeyEvent.VK_E, KeyEvent.VK_E));
        file.addSeparator();
        file.add(createMenuItem("import_configs", KeyEvent.VK_M, -1));
        file.add(_exportConfigs = createMenuItem("export_configs", KeyEvent.VK_X, -1));
        file.addSeparator();
        file.add(createMenuItem("close", KeyEvent.VK_C, KeyEvent.VK_W));
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(new JMenuItem(_cut = createAction("cut", KeyEvent.VK_T, KeyEvent.VK_X)));
        edit.add(new JMenuItem(_copy = createAction("copy", KeyEvent.VK_C, KeyEvent.VK_C)));
        edit.add(new JMenuItem(_paste = createAction("paste", KeyEvent.VK_P, KeyEvent.VK_V)));
        edit.add(new JMenuItem(
            _delete = createAction("delete", KeyEvent.VK_D, KeyEvent.VK_DELETE, 0)));
        edit.addSeparator();
        edit.add(createMenuItem("resources", KeyEvent.VK_R, KeyEvent.VK_U));
        edit.add(createMenuItem("preferences", KeyEvent.VK_F, KeyEvent.VK_F));

        JMenu gmenu = createMenu("groups", KeyEvent.VK_G);
        menubar.add(gmenu);
        gmenu.add(_saveAll = createMenuItem("save_all", KeyEvent.VK_S, KeyEvent.VK_A));
        gmenu.add(_revertAll = createMenuItem("revert_all", KeyEvent.VK_R, KeyEvent.VK_T));

        // create the pop-up menu
        _popup = new JPopupMenu();
        nmenu = createMenu("new", KeyEvent.VK_N);
        _popup.add(nmenu);
        nmenu.add(new JMenuItem(nconfig));
        nmenu.add(new JMenuItem(nfolder));
        _popup.addSeparator();
        _popup.add(new JMenuItem(_cut));
        _popup.add(new JMenuItem(_copy));
        _popup.add(new JMenuItem(_paste));
        _popup.add(new JMenuItem(_delete));

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

        // create the tabbed pane
        add(_tabs = new JTabbedPane(), BorderLayout.WEST);
        _tabs.setPreferredSize(new Dimension(250, 1));
        _tabs.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        // create the tabs for each configuration manager
        for (; cfgmgr != null; cfgmgr = cfgmgr.getParent()) {
            _tabs.add(new ManagerPanel(cfgmgr), getLabel(cfgmgr.getType()), 0);
        }

        // activate the last tab
        final ManagerPanel panel = (ManagerPanel)_tabs.getComponentAt(_tabs.getTabCount() - 1);
        _tabs.setSelectedComponent(panel);
        panel.activate();

        // add a listener for tab change
        _tabs.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                _panel.deactivate();
                (_panel = (ManagerPanel)_tabs.getSelectedComponent()).activate();
            }
            protected ManagerPanel _panel = panel;
        });

        // open the initial config, if one was specified
        if (clazz != null) {
            select(clazz, name);
        }
    }

    // documentation inherited from interface ClipboardOwner
    public void lostOwnership (Clipboard clipboard, Transferable contents)
    {
        _paste.setEnabled(false);
        _clipclass = null;
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        ManagerPanel panel = (ManagerPanel)_tabs.getSelectedComponent();
        ManagerPanel.GroupItem item = (ManagerPanel.GroupItem)panel.gbox.getSelectedItem();
        if (action.equals("window")) {
            showFrame(new ConfigEditor(_msgmgr, _cfgmgr, _colorpos));
        } else if (action.equals("config")) {
            item.newConfig();
        } else if (action.equals("folder")) {
            item.newFolder();
        } else if (action.equals("save_group")) {
            item.group.save();
        } else if (action.equals("revert_group")) {
            item.group.revert();
        } else if (action.equals("import_group")) {
            item.importGroup();
        } else if (action.equals("export_group")) {
            item.exportGroup();
        } else if (action.equals("import_configs")) {
            item.importConfigs();
        } else if (action.equals("export_configs")) {
            item.exportConfigs();
        } else if (action.equals("cut")) {
            item.cutNode();
        } else if (action.equals("copy")) {
            item.copyNode();
        } else if (action.equals("paste")) {
            item.pasteNode();
        } else if (action.equals("delete")) {
            item.deleteNode();
        } else if (action.equals("resources")) {
            showFrame(new ResourceEditor(_msgmgr, _cfgmgr, _colorpos));
        } else if (action.equals("save_all")) {
            panel.cfgmgr.saveAll();
        } else if (action.equals("revert_all")) {
            panel.cfgmgr.revertAll();
        } else {
            super.actionPerformed(event);
        }
    }

    @Override // documentation inherited
    public void removeNotify ()
    {
        super.removeNotify();
        for (int ii = 0, nn = _tabs.getComponentCount(); ii < nn; ii++) {
            ((ManagerPanel)_tabs.getComponentAt(ii)).dispose();
        }
    }

    /**
     * Selects a configuration.
     */
    protected void select (Class clazz, String name)
    {
        for (int ii = _tabs.getComponentCount() - 1; ii >= 0; ii--) {
            ManagerPanel panel = (ManagerPanel)_tabs.getComponentAt(ii);
            if (panel.select(clazz, name)) {
                return;
            }
        }
    }

    /**
     * The panel for a single manager.
     */
    protected class ManagerPanel extends JPanel
        implements EditorContext, ItemListener, ChangeListener
    {
        /**
         * Contains the state of a single group.
         */
        public class GroupItem
            implements TreeSelectionListener
        {
            /** The actual group reference. */
            public ConfigGroup<ManagedConfig> group;

            public GroupItem (ConfigGroup group)
            {
                @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig> mgroup =
                    (ConfigGroup<ManagedConfig>)group;
                this.group = mgroup;
                _label = getLabel(group.getConfigClass(), group.getName());
            }

            /**
             * Activates this group.
             */
            public void activate ()
            {
                if (_tree == null) {
                    _tree = new ConfigTree(group, true) {
                        public void selectedConfigUpdated () {
                            _epanel.update();
                        }
                    };
                    _tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    _tree.addTreeSelectionListener(this);
                    _tree.setComponentPopupMenu(_popup);

                    // remove the mappings for cut/copy/paste since we handle those ourself
                    InputMap imap = _tree.getInputMap();
                    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK), "noop");
                    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK), "noop");
                    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK), "noop");
                }
                _pane.setViewportView(_tree);
                _paste.setEnabled(_clipclass == group.getConfigClass());
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
             * Brings up the import group dialog.
             */
            public void importGroup ()
            {
                if (_chooser.showOpenDialog(ConfigEditor.this) == JFileChooser.APPROVE_OPTION) {
                    group.load(_chooser.getSelectedFile());
                }
                _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
            }

            /**
             * Brings up the export group dialog.
             */
            public void exportGroup ()
            {
                if (_chooser.showSaveDialog(ConfigEditor.this) == JFileChooser.APPROVE_OPTION) {
                    group.save(_chooser.getSelectedFile());
                }
                _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
            }

            /**
             * Brings up the import config dialog.
             */
            public void importConfigs ()
            {
                if (_chooser.showOpenDialog(ConfigEditor.this) == JFileChooser.APPROVE_OPTION) {
                    group.load(_chooser.getSelectedFile(), true);
                }
                _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
            }

            /**
             * Brings up the export config dialog.
             */
            public void exportConfigs ()
            {
                if (_chooser.showOpenDialog(ConfigEditor.this) == JFileChooser.APPROVE_OPTION) {
                    ArrayList<ManagedConfig> configs = new ArrayList<ManagedConfig>();
                    _tree.getSelectedNode().getConfigs(configs);
                    group.save(configs, _chooser.getSelectedFile());
                }
                _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
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
                _clipclass = group.getConfigClass();
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
             * Notes that the state of the currently selected configuration has changed.
             */
            public void configChanged ()
            {
                _tree.selectedConfigChanged();
            }

            /**
             * Attempts to select the specified config within this group.
             */
            public boolean select (String name)
            {
                if (group.getConfig(name) == null) {
                    return false;
                }
                _tabs.setSelectedComponent(ManagerPanel.this);
                gbox.setSelectedItem(this);
                _tree.setSelectedNode(name);
                return true;
            }

            /**
             * Disposes of the resources held by this item.
             */
            public void dispose ()
            {
                if (_tree != null) {
                    _tree.dispose();
                    _tree = null;
                }
            }

            // documentation inherited from interface TreeSelectionListener
            public void valueChanged (TreeSelectionEvent event)
            {
                updateSelection();
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

        /** The configuration manager. */
        public ConfigManager cfgmgr;

        /** Determines the selected group. */
        public JComboBox gbox;

        public ManagerPanel (ConfigManager cfgmgr)
        {
            super(new VGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            this.cfgmgr = cfgmgr;

            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // create the group panel
            JPanel gpanel = GroupLayout.makeHStretchBox(5);
            add(gpanel, GroupLayout.FIXED);
            gpanel.add(new JLabel(_msgs.get("m.group")), GroupLayout.FIXED);

            // initialize the list of groups
            Collection<ConfigGroup> groups = cfgmgr.getGroups();
            GroupItem[] items = new GroupItem[groups.size()];
            int idx = 0;
            for (ConfigGroup group : groups) {
                items[idx++] = new GroupItem(group);
            }
            QuickSort.sort(items, new Comparator<GroupItem>() {
                public int compare (GroupItem g1, GroupItem g2) {
                    return g1.toString().compareTo(g2.toString());
                }
            });
            gpanel.add(gbox = new JComboBox(items));
            gbox.addItemListener(this);

            // add the pane that will contain the group tree
            add(_pane = new JScrollPane());

            // create the editor panel
            _epanel = new EditorPanel(this, EditorPanel.CategoryMode.TABS, null);
            _epanel.addChangeListener(this);
        }

        /**
         * Called when the panel is shown.
         */
        public void activate ()
        {
            // add the editor panel
            ConfigEditor.this.add(_epanel, BorderLayout.CENTER);
            ConfigEditor.this.repaint();

            // activate the selected item
            ((GroupItem)gbox.getSelectedItem()).activate();

            // can only save/revert configurations with a config path
            boolean enable = (cfgmgr.getConfigPath() != null);
            _save.setEnabled(enable);
            _revert.setEnabled(enable);
            _saveAll.setEnabled(enable);
            _revertAll.setEnabled(enable);
        }

        /**
         * Called when the panel is hidden.
         */
        public void deactivate ()
        {
            // remove the editor panel
            ConfigEditor.this.remove(_epanel);
        }

        /**
         * Attempts to select the specified config.
         */
        public boolean select (Class clazz, String name)
        {
            for (int ii = 0, nn = gbox.getItemCount(); ii < nn; ii++) {
                GroupItem item = (GroupItem)gbox.getItemAt(ii);
                if (item.group.getConfigClass() == clazz) {
                    return item.select(name);
                }
            }
            return false;
        }

        /**
         * Disposes of the resources held by this manager.
         */
        public void dispose ()
        {
            for (int ii = 0, nn = gbox.getItemCount(); ii < nn; ii++) {
                ((GroupItem)gbox.getItemAt(ii)).dispose();
            }
        }

        // documentation inherited from interface EditorContext
        public ResourceManager getResourceManager ()
        {
            return _rsrcmgr;
        }

        // documentation inherited from interface EditorContext
        public MessageManager getMessageManager ()
        {
            return _msgmgr;
        }

        // documentation inherited from interface EditorContext
        public ConfigManager getConfigManager ()
        {
            return cfgmgr;
        }

        // documentation inherited from interface EditorContext
        public ColorPository getColorPository ()
        {
            return _colorpos;
        }

        // documentation inherited from interface ItemListener
        public void itemStateChanged (ItemEvent event)
        {
            ((GroupItem)gbox.getSelectedItem()).activate();
        }

        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            ((GroupItem)gbox.getSelectedItem()).configChanged();
        }

        /** The scroll pane that holds the group trees. */
        protected JScrollPane _pane;

        /** The object editor panel. */
        protected EditorPanel _epanel;
    }

    /** The config tree pop-up menu. */
    protected JPopupMenu _popup;

    /** The save and revert menu items. */
    protected JMenuItem _save, _revert, _saveAll, _revertAll;

    /** The configuration export menu item. */
    protected JMenuItem _exportConfigs;

    /** The edit menu actions. */
    protected Action _cut, _copy, _paste, _delete;

    /** The file chooser for opening and saving config files. */
    protected JFileChooser _chooser;

    /** The tabs for each manager. */
    protected JTabbedPane _tabs;

    /** The class of the clipboard selection. */
    protected Class _clipclass;
}
