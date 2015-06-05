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

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.annotation.Nullable;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.samskivert.util.ObserverList;
import com.samskivert.util.QuickSort;

import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.swing.PrintStreamDialog;
import com.threerings.util.MessageManager;
import com.threerings.util.ResourceUtil;
import com.threerings.util.ToolUtil;

import com.threerings.editor.Editable;
import com.threerings.editor.util.Validator;
import com.threerings.editor.swing.BaseEditorPanel;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.TreeEditorPanel;
import com.threerings.editor.util.EditorContext;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigGroupListener;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.util.PasteHelper;
import com.threerings.config.swing.ConfigTree;
import com.threerings.config.swing.ConfigTreeFilterPanel;
import com.threerings.config.swing.ConfigTreeNode;

import com.threerings.opengl.renderer.Color4f;

import static com.threerings.ClydeLog.log;

/**
 * Allows editing the configuration database.  Can either be invoked standalone or from within
 * another application.
 */
public class ConfigEditor extends BaseConfigEditor
    implements ClipboardOwner
{
    /**
     * Create a ConfigEditor.
     */
    public static ConfigEditor create (EditorContext ctx)
    {
        return create(ctx, null, null);
    }

    /**
     * Create a ConfigEditor and edit the specified config.
     */
    public static ConfigEditor create (EditorContext ctx, Class<?> clazz, String name)
    {
        ConfigEditor editor = (_editorCreator != null)
            ? _editorCreator.apply(ctx)
            : new ConfigEditor(
                    ctx.getMessageManager(), ctx.getConfigManager(), ctx.getColorPository());
        if (clazz != null) {
            editor.select(clazz, name);
        }
        return editor;
    }

    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
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
        Class<?> clazz, String name)
    {
        super(msgmgr, cfgmgr, colorpos, "editor.config");

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);

        JMenu nmenu = createMenu("new", KeyEvent.VK_N);
        file.add(nmenu);
        nmenu.add(createMenuItem("window", KeyEvent.VK_W, KeyEvent.VK_N));
        nmenu.addSeparator();
        Action nconfig = createAction("config", KeyEvent.VK_C, KeyEvent.VK_O);
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

        final JMenu edit = createMenu("edit", KeyEvent.VK_E);
        edit.addMenuListener(new MenuListener() {
            public void menuSelected (MenuEvent event) {
                // hackery to allow cut/copy/paste/delete to act on editor tree
                TreeEditorPanel panel = (TreeEditorPanel)SwingUtilities.getAncestorOfClass(
                    TreeEditorPanel.class, getFocusOwner());
                if (panel != null) {
                    edit.getItem(0).setAction(panel.getCutAction());
                    edit.getItem(1).setAction(panel.getCopyAction());
                    edit.getItem(2).setAction(panel.getPasteAction());
                    edit.getItem(3).setAction(panel.getDeleteAction());
                } else {
                    restoreActions();
                }
            }
            public void menuDeselected (MenuEvent event) {
                // restore after a delay so as not to interfere with selected item
                EventQueue.invokeLater(new Runnable() {
                    public void run () {
                        restoreActions();
                    }
                });
            }
            public void menuCanceled (MenuEvent event) {
                // no-op
            }
            protected void restoreActions () {
                edit.getItem(0).setAction(_cut);
                edit.getItem(1).setAction(_copy);
                edit.getItem(2).setAction(_paste);
                edit.getItem(3).setAction(_delete);
            }
        });
        menubar.add(edit);
        edit.add(new JMenuItem(_cut = createAction("cut", KeyEvent.VK_T, KeyEvent.VK_X)));
        edit.add(new JMenuItem(_copy = createAction("copy", KeyEvent.VK_C, KeyEvent.VK_C)));
        edit.add(new JMenuItem(_paste = createAction("paste", KeyEvent.VK_P, KeyEvent.VK_V)));
        edit.add(new JMenuItem(
            _delete = createAction("delete", KeyEvent.VK_D, KeyEvent.VK_DELETE, 0)));
        addFindMenu(edit);
        edit.addSeparator();
        edit.add(new JMenuItem(_findUses = createAction("find_uses", 0, -1)));
        edit.addSeparator();
        edit.add(createMenuItem("validate_refs", KeyEvent.VK_V, -1));
        addEditMenuItems(edit);
        edit.addSeparator();
        edit.add(createMenuItem("resources", KeyEvent.VK_R, KeyEvent.VK_U));
        edit.add(createMenuItem("preferences", KeyEvent.VK_F, -1));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_treeMode = ToolUtil.createCheckBoxMenuItem(
            this, _msgs, "tree_mode", KeyEvent.VK_T, -1));

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
        _popup.add(new JMenuItem(_findUses));
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

        // create the split pane
        add(_split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true), BorderLayout.CENTER);

        // create the tabbed pane
        _split.setLeftComponent(_tabs = new JTabbedPane());
        _tabs.setPreferredSize(new Dimension(250, 1));
        _tabs.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        // create the tabs for each configuration manager
        for (; cfgmgr != null; cfgmgr = cfgmgr.getParent()) {
            _tabs.add(new ManagerPanel(cfgmgr), getLabel(cfgmgr.getType()), 0);
        }

        // activate the first tab
        ManagerPanel panel = (ManagerPanel)_tabs.getComponentAt(0);
        _tabs.setSelectedComponent(panel);
        panel.activate();

        // add a listener for tab change
        _tabs.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                ((ManagerPanel)_tabs.getSelectedComponent()).activate();
            }
        });

        // set sensible default bounds
        setSize(850, 600);
        SwingUtil.centerWindow(this);

        // restore our prefs (may override bounds)
        restorePrefs();

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

    @Override
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        ManagerPanel panel = (ManagerPanel)_tabs.getSelectedComponent();
        ManagerPanel.GroupItem item = (ManagerPanel.GroupItem)panel.gbox.getSelectedItem();
        if (action.equals("window")) {
            showFrame(ConfigEditor.create(this));
        } else if (action.equals("config")) {
            item.newConfig();
        } else if (action.equals("folder")) {
            item.newFolder();
        } else if (action.equals("save_group")) {
            item.group.save();
            DirtyGroupManager.setDirty(item.group, false);
        } else if (action.equals("revert_group")) {
            if (!DirtyGroupManager.isDirty(item.group) || showCantUndo()) {
                item.group.revert();
                DirtyGroupManager.setDirty(item.group, false);
            }
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
        } else if (action.equals("find_uses")) {
            findUses(panel.getSelected(), item.group.getConfigClass(),
                    (null != panel.getEditorPanel().getObject())); // on folders, do a prefix match
        } else if (action.equals("validate_refs")) {
            validateReferences();
        } else if (action.equals("resources")) {
            showFrame(new ResourceEditor(_msgmgr, _cfgmgr, _colorpos));
        } else if (action.equals("tree_mode")) {
            boolean enabled = _treeMode.isSelected();
            for (int ii = _tabs.getComponentCount() - 1; ii >= 0; ii--) {
                ((ManagerPanel)_tabs.getComponentAt(ii)).setTreeModeEnabled(enabled);
            }
        } else if (action.equals("save_all")) {
            panel.cfgmgr.saveAll();
            DirtyGroupManager.setDirty(panel.cfgmgr, false);
        } else if (action.equals("revert_all")) {
            if (!DirtyGroupManager.isDirty(panel.cfgmgr) || showCantUndo()) {
                panel.cfgmgr.revertAll();
                DirtyGroupManager.setDirty(panel.cfgmgr, false);
            }
        } else if (action.equals("quit")) {
            if (!DirtyGroupManager.anyDirty() || showUnsavedChanges()) {
                super.actionPerformed(event);
            }

        } else {
            super.actionPerformed(event);
        }
    }

    @Override
    public void dispose ()
    {
        if (DirtyGroupManager.getRegisteredEditorCount() == 1) {
            // if we're the last editor...
            boolean dirty = false;
            for (int ii = 0, nn = _tabs.getComponentCount(); ii < nn; ii++) {
                dirty |= DirtyGroupManager.isDirty(((ManagerPanel)_tabs.getComponentAt(ii)).cfgmgr);
            }
            if (dirty && !showUnsavedChanges()) {
                return;
            }
        }
        super.dispose();
    }

    @Override
    public void addNotify ()
    {
        super.addNotify();
        DirtyGroupManager.registerEditor(this);
    }

    @Override
    public void removeNotify ()
    {
        DirtyGroupManager.unregisterEditor(this);
        super.removeNotify();
        for (int ii = 0, nn = _tabs.getComponentCount(); ii < nn; ii++) {
            ((ManagerPanel)_tabs.getComponentAt(ii)).dispose();
        }
    }

    /**
     * Check to see if any of our groups are now dirty.
     */
    protected void recheckDirty ()
    {
        // refresh the 'gbox' in each manager, the GroupItem will toString() differently if dirty
        for (int ii = 0, nn = _tabs.getComponentCount(); ii < nn; ii++) {
            SwingUtil.refresh(((ManagerPanel)_tabs.getComponentAt(ii)).gbox);
        }
    }

    /**
     * Selects a configuration.
     */
    protected void select (Class<?> clazz, String name)
    {
        for (int ii = _tabs.getComponentCount() - 1; ii >= 0; ii--) {
            ManagerPanel panel = (ManagerPanel)_tabs.getComponentAt(ii);
            if (panel.select(clazz, name)) {
                return;
            }
        }
    }

    /**
     * Shows a confirm dialog.
     */
    protected boolean showCantUndo ()
    {
        return showConfirm("m.cant_undo", "t.cant_undo");
    }

    /**
     * Shows a confirm dialog.
     */
    protected boolean showUnsavedChanges ()
    {
        return showConfirm("m.unsaved_changes", "t.unsaved_changes");
    }

    /**
     * Shows a confirm dialog.
     */
    protected boolean showConfirm (String msg, String title)
    {
        String[] options = {
            UIManager.getString("OptionPane.okButtonText"),
            UIManager.getString("OptionPane.cancelButtonText")
        };
        return 0 == JOptionPane.showOptionDialog(
                this, _msgs.get(msg), _msgs.get(title),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                options, options[1]); // default to cancel
    }

    /**
     * Finds the uses of the specified config.
     */
    protected void findUses (
            final String cfgNameOrPrefix, Class<? extends ManagedConfig> clazz,
            final boolean exact)
    {
        if (cfgNameOrPrefix == null) {
            return;
        }
        new ConfigSearcher(this, cfgNameOrPrefix,
                ConfigSearcher.Presence.getReporter(clazz, new Predicate<ConfigReference<?>>() {
                    public boolean apply (ConfigReference<?> ref) {
                        return exact
                            ? ref.getName().equals(cfgNameOrPrefix)
                            : ref.getName().startsWith(cfgNameOrPrefix);
                    }
                }),
                getSearcherDomains());
    }

    /**
     * Return the domains over which we'll search for config uses.
     */
    protected Iterable<ConfigSearcher.Domain> getSearcherDomains ()
    {
        return ImmutableList.<ConfigSearcher.Domain>of(new ConfigSearcher.ConfigDomain(this));
    }

    /**
     * Validates the references.
     */
    protected void validateReferences ()
    {
        PrintStreamDialog dialog = new PrintStreamDialog(
            this, _msgs.get("m.validate_refs"), _msgs.get("b.ok"));
        PrintStream stream = dialog.getPrintStream();
        try {
            validateReferences(createValidator(stream));
        } catch (Exception e) {
            stream.println("Exception while validating!!");
            e.printStackTrace(stream);
        }
        dialog.maybeShow();
    }

    /**
     * Break out the validation separately from the creation of the validator so that it
     * can be overridden.
     */
    protected void validateReferences (Validator validator)
    {
        _cfgmgr.validateReferences(validator);
    }

    /**
     * Create a validator for validating config references.
     */
    protected Validator createValidator (PrintStream out)
    {
        return new Validator(out);
    }

    /**
     * Create a paste helper for copy/paste or move operations.
     */
    protected PasteHelper createPasteHelper (ConfigGroup<?> group)
    {
        // return the default do-nothing paste helper
        return new PasteHelper();
    }

    /**
     * Used to add addition items to the edit menu.
     */
    protected void addEditMenuItems (JMenu edit)
    {
    }

    @Override
    protected BaseEditorPanel getFindEditorPanel ()
    {
        return ((ManagerPanel)_tabs.getSelectedComponent()).getEditorPanel();
    }

    @Override
    protected ConfigEditorPrefs createEditablePrefs (Preferences prefs)
    {
        return new ConfigEditorPrefs(_prefs);
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

            public GroupItem (ConfigGroup<?> group)
            {
                @SuppressWarnings("unchecked")
                ConfigGroup<ManagedConfig> mgroup = (ConfigGroup<ManagedConfig>)group;
                this.group = mgroup;
                this.group.addListener(new ConfigGroupListener() {
                        public void configAdded (ConfigEvent<ManagedConfig> evt) {
                            DirtyGroupManager.setDirty(GroupItem.this.group, true);
                        }
                        public void configRemoved (ConfigEvent<ManagedConfig> evt) {
                            DirtyGroupManager.setDirty(GroupItem.this.group, true);
                        }
                    });
                _label = getLabel(group.getConfigClass(), group.getName());
            }

            /**
             * Activates this group.
             */
            public void activate ()
            {
                if (_tree == null) {
                    _tree = new ConfigTree(group, true) {
                        @Override public void selectedConfigUpdated () {
                            super.selectedConfigUpdated();
                            _epanel.update();
                        }
                        @Override protected PasteHelper createPasteHelper (ConfigGroup<?> group)
                        {
                            return ConfigEditor.this.createPasteHelper(group);
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
                _filterPanel.setTree(_tree);
                _paste.setEnabled(_clipclass == group.getConfigClass());
                updateSelection();
            }

            /**
             * Creates a new configuration and prepares it for editing.
             */
            public void newConfig ()
            {
                Class<?> clazz = group.getRawConfigClasses().get(0);
                try {
                    ManagedConfig cfg = (ManagedConfig)clazz.newInstance();
                    if (cfg instanceof DerivedConfig) {
                        ((DerivedConfig)cfg).cclass = group.getConfigClass();
                    }
                    newNode(cfg);
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
                PasteHelper paster = createPasteHelper(group);
                Clipboard clipboard = _tree.getToolkit().getSystemClipboard();
                if (_tree.getTransferHandler().importData(_tree, clipboard.getContents(this))) {
                    paster.didPaste();
                }
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
                DirtyGroupManager.setDirty(group, true);
            }

            /**
             * Notes that the state of the currently selected configuration has changed.
             */
            public void configChanged ()
            {
                DirtyGroupManager.setDirty(group, true);
                _tree.selectedConfigChanged();
            }

            /**
             * Attempts to select the specified config within this group.
             */
            public boolean select (String name)
            {
                if (group.getRawConfig(name) == null) {
                    return false;
                }
                _tabs.setSelectedComponent(ManagerPanel.this);
                gbox.setSelectedItem(this);
                _tree.setSelectedNode(name);
                return true;
            }

            /**
             * Get the selected node, which may be a "folder" name: a partial config name.
             */
            public String getSelected ()
            {
                ConfigTreeNode node = _tree.getSelectedNode();
                return (node == null)
                    ? null
                    : node.getName();
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

            @Override
            public String toString ()
            {
                return _label + (DirtyGroupManager.isDirty(group) ? " *" : "");
            }

            /**
             * Updates the state of the UI based on the selection.
             */
            protected void updateSelection ()
            {
                // find the selected node
                ConfigTreeNode node = _tree.getSelectedNode();

                // update the editor panel
                _epanel.removeChangeListener(ManagerPanel.this);
                try {
                    _epanel.setObject(node == null ? null : node.getConfig());
                } finally {
                    _epanel.addChangeListener(ManagerPanel.this);
                }

                // enable or disable the menu items
                boolean enable = (node != null);
                _exportConfigs.setEnabled(enable);
                _cut.setEnabled(enable);
                _copy.setEnabled(enable);
                _delete.setEnabled(enable);
                _findUses.setEnabled(enable);
            }

            /**
             * Creates a new node for the supplied configuration (or a folder node, if the
             * configuration is <code>null</code>).
             */
            protected void newNode (ManagedConfig config)
            {
                // presently we must clear the filter
                _filterPanel.clearFilter();

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
                DirtyGroupManager.setDirty(group, true);
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
            Collection<ConfigGroup<?>> groups = cfgmgr.getGroups();
            GroupItem[] items = new GroupItem[groups.size()];
            int idx = 0;
            for (ConfigGroup<?> group : groups) {
                items[idx++] = new GroupItem(group);
            }
            QuickSort.sort(items, new Comparator<GroupItem>() {
                public int compare (GroupItem g1, GroupItem g2) {
                    return String.CASE_INSENSITIVE_ORDER.compare(g1.toString(), g2.toString());
                }
            });
            gpanel.add(gbox = new JComboBox(items));
            gbox.addItemListener(this);

            // add the filtering panel
            add(_filterPanel = new ConfigTreeFilterPanel(_msgmgr), VGroupLayout.FIXED);

            // add the pane that will contain the group tree
            add(_pane = new JScrollPane());

            // create the editor panel
            _epanel = new EditorPanel(this, EditorPanel.CategoryMode.TABS, null);
            _epanel.addChangeListener(this);
        }

        @Override
        public void setBackground (Color c)
        {
            super.setBackground(c);
            if (_pane != null) {
                _pane.setBackground(c);
                _epanel.setBackground(c);
            }
        }

        /**
         * Called when the panel is shown.
         */
        public void activate ()
        {
            // add the editor panel
            _split.setRightComponent(_epanel);
            SwingUtil.refresh(_epanel);

            // activate the selected item
            GroupItem group = (GroupItem)gbox.getSelectedItem();
            if (group != null) {
                group.activate();
            }

            // can only save/revert configurations with a config path
            boolean enable = (cfgmgr.getConfigPath() != null);
            _save.setEnabled(enable);
            _revert.setEnabled(enable);
            _saveAll.setEnabled(enable);
            _revertAll.setEnabled(enable);
        }

        /**
         * Attempts to select the specified config.
         */
        public boolean select (Class<?> clazz, String name)
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
         * Get the selected node, which may be a "folder" name: a partial config name.
         */
        public String getSelected ()
        {
            return ((GroupItem)gbox.getSelectedItem()).getSelected();
        }

        /**
         * Enables or disables tree view mode.
         */
        public void setTreeModeEnabled (boolean enabled)
        {
            BaseEditorPanel opanel = _epanel;
            _epanel = enabled ? new TreeEditorPanel(this) :
                new EditorPanel(this, EditorPanel.CategoryMode.TABS);
            _epanel.addChangeListener(this);
            _epanel.setObject(opanel.getObject());
            if (_split.getRightComponent() == opanel) {
                _split.setRightComponent(_epanel);
                SwingUtil.refresh(_epanel);
            }
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

        /**
         * Returns the editor panel.
         */
        public BaseEditorPanel getEditorPanel ()
        {
            return _epanel;
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

        /** Holds a configuration filtering panel. */
        protected ConfigTreeFilterPanel _filterPanel;

        /** The scroll pane that holds the group trees. */
        protected JScrollPane _pane;

        /** The object editor panel. */
        protected BaseEditorPanel _epanel;
    }

    /**
     * Restore and bind prefs.
     */
    protected void restorePrefs ()
    {
        final String p = getConfigKey();

        // restore/bind window bounds
        _eprefs.bindWindowBounds(p, this);

        // restore/bind the location of the divider
        _eprefs.bindDividerLocation(p + "div", _split);

        // restore/bind the selected group
        String cat = _prefs.get(p + "group", null);
        for (int tab = _tabs.getComponentCount() - 1; tab >= 0; tab--) {
            final JComboBox gbox = ((ManagerPanel)_tabs.getComponentAt(tab)).gbox;
            if (cat != null) {
                for (int ii = 0, nn = gbox.getItemCount(); ii < nn; ii++) {
                    if (cat.equals(String.valueOf(gbox.getItemAt(ii)))) {
                        gbox.setSelectedIndex(ii);
                        break;
                    }
                }
            }
            gbox.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    _prefs.put(p + "group", String.valueOf(gbox.getSelectedItem()));
                }
            });
        }

        // restore color
        setBackground(((ConfigEditorPrefs)_eprefs).getBackgroundColor());
    }

    /**
     * Set the background color for this editor.
     */
    protected void setBackground (Color4f color)
    {
        final Color c = color.getColor();
        setBackground(c);
        getContentPane().setBackground(c);
        for (int ii = 0, nn = _tabs.getComponentCount(); ii < nn; ii++) {
            ((ManagerPanel)_tabs.getComponentAt(ii)).setBackground(c);
        }
//        SwingUtil.applyToHierarchy(this, new SwingUtil.ComponentOp() {
//                public void apply (Component comp) {
//                    comp.setBackground(c);
//                }
//            });
    }

    /**
     * Get our prefs key prefix.
     */
    protected String getConfigKey ()
    {
        return "ConfigEditor." + ResourceUtil.getPrefsPrefix();
    }

    /**
     * Our prefs.
     */
    protected class ConfigEditorPrefs extends ToolUtil.EditablePrefs
    {
        public ConfigEditorPrefs (Preferences prefs)
        {
            super(prefs);
        }

        @Editable(weight=3)
        public void setBackgroundColor (Color4f color)
        {
            putPref(getConfigKey() + "background_color", color);
            ConfigEditor.this.setBackground(color);
        }

        @Editable // see setter
        public Color4f getBackgroundColor ()
        {
            return getPref(getConfigKey() + "background_color", Color4f.GRAY);
        }
    }

    /**
     * Tracks which config groups have unsaved changes within them.
     */
    protected static class DirtyGroupManager
    {
        /**
         * Add an editor to be notified of dirty groups.
         */
        public static void registerEditor (ConfigEditor editor)
        {
            _editors.add(editor);
        }

        /**
         * Remove a editor.
         */
        public static void unregisterEditor (ConfigEditor editor)
        {
            _editors.remove(editor);
        }

        /**
         * How may editors are registered?
         */
        public static int getRegisteredEditorCount ()
        {
            return _editors.size();
        }

        /**
         * Set all groups within the specified manager as dirty.
         */
        public static void setDirty (ConfigManager cfgmgr, boolean dirty)
        {
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                setDirty(group, dirty);
            }
        }

        /**
         * Set the specified group as dirty.
         */
        public static void setDirty (ConfigGroup<?> group, boolean dirty)
        {
            Boolean oldVal = _dirty.put(group, dirty);
            boolean oldDirty = Boolean.TRUE.equals(oldVal);
            if (dirty != oldDirty) {
                _editors.apply(new ObserverList.ObserverOp<ConfigEditor>() {
                        public boolean apply (ConfigEditor editor) {
                            editor.recheckDirty();
                            return true;
                        }
                    });
            }
        }

        /**
         * Is any group within the specified configmanager dirty?
         */
        public static boolean isDirty (ConfigManager cfgmgr)
        {
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                if (isDirty(group)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Is the specified group dirty?
         */
        public static boolean isDirty (ConfigGroup<?> group)
        {
            return Boolean.TRUE.equals(_dirty.get(group));
        }

        /**
         * Are any of the values dirty?
         */
        public static boolean anyDirty ()
        {
            for (Boolean b : _dirty.values()) {
                if (Boolean.TRUE.equals(b)) {
                    return true;
                }
            }
            return false;
        }

        /** A weak mapping of group to dirtyness. */
        protected static Map<ConfigGroup<?>, Boolean> _dirty = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .weakKeys()
                .<ConfigGroup<?>, Boolean>build().asMap();

        /** The editors currently registered to hear about dirty groups. */
        protected static ObserverList<ConfigEditor> _editors = ObserverList.newFastUnsafe();
    }

    /** The config tree pop-up menu. */
    protected JPopupMenu _popup;

    /** The save and revert menu items. */
    protected JMenuItem _save, _revert, _saveAll, _revertAll;

    /** The configuration export menu item. */
    protected JMenuItem _exportConfigs;

    /** The edit menu actions. */
    protected Action _cut, _copy, _paste, _delete, _findUses;

    /** The tree mode toggle. */
    protected JCheckBoxMenuItem _treeMode;

    /** The file chooser for opening and saving config files. */
    protected JFileChooser _chooser;

    /** The split pane containing the tabs and the editor panel. */
    protected JSplitPane _split;

    /** The tabs for each manager. */
    protected JTabbedPane _tabs;

    /** The class of the clipboard selection. */
    protected Class<?> _clipclass;

    /** A Function for creating new instances of the config editor via our public static method.
     * This function will be assigned by subclasses that wish to ensure that that subclass
     * is always used. */
    protected static Function<? super EditorContext, ? extends ConfigEditor> _editorCreator;
}
