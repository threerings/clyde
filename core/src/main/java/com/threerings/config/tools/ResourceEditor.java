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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import com.samskivert.swing.util.SwingUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.ChangeBlock;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.editor.swing.BaseEditorPanel;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.TreeEditorPanel;
import com.threerings.editor.tools.BatchValidateDialog;
import com.threerings.editor.util.Validator;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ParameterizedConfig;

import static com.threerings.ClydeLog.log;

/**
 * Allows editing single configurations stored as resources.
 */
public class ResourceEditor extends BaseConfigEditor
    implements ChangeListener, ConfigUpdateListener<ManagedConfig>
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
        ColorPository colorpos = ColorPository.loadColorPository(rsrcmgr);
        new ResourceEditor(
            msgmgr, cfgmgr, colorpos, args.length > 0 ? args[0] : null).setVisible(true);
    }

    /**
     * Creates a new resource editor.
     */
    public ResourceEditor (MessageManager msgmgr, ConfigManager cfgmgr, ColorPository colorpos)
    {
        this(msgmgr, cfgmgr, colorpos, null);
    }

    /**
     * Creates a new resource editor.
     */
    public ResourceEditor (
        MessageManager msgmgr, ConfigManager cfgmgr, ColorPository colorpos, String config)
    {
        super(msgmgr, cfgmgr, colorpos, "resource");
        setSize(550, 600);
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
        file.add(createMenuItem("open", KeyEvent.VK_O, KeyEvent.VK_O));
        file.addSeparator();
        file.add(_save = createMenuItem("save", KeyEvent.VK_S, KeyEvent.VK_S));
        _save.setEnabled(false);
        file.add(_saveAs = createMenuItem("save_as", KeyEvent.VK_A, KeyEvent.VK_A));
        _saveAs.setEnabled(false);
        file.add(_revert = createMenuItem("revert", KeyEvent.VK_R, KeyEvent.VK_R));
        _revert.setEnabled(false);
        file.addSeparator();
        file.add(createMenuItem("import", KeyEvent.VK_I, -1));
        file.add(_export = createMenuItem("export", KeyEvent.VK_E, -1));
        _export.setEnabled(false);
        file.addSeparator();
        file.add(createMenuItem("close", KeyEvent.VK_C, KeyEvent.VK_W));
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("update", KeyEvent.VK_U, KeyEvent.VK_U));
        addFindMenu(edit);
        edit.addSeparator();
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_treeMode = ToolUtil.createCheckBoxMenuItem(
            this, _msgs, "tree_mode", KeyEvent.VK_T, -1));

        JMenu tools = createMenu("tools", KeyEvent.VK_T);
        menubar.add(tools);
        tools.add(createMenuItem("batch_validate", KeyEvent.VK_B, -1));

        // add the new items now that we've initialized the config manager
        Set<Character> mnems = Sets.newHashSet();
        mnems.add('w');
        int idx = 0;
        for (final Class<?> clazz : cfgmgr.getResourceClasses()) {
            String label = getLabel(clazz, ConfigGroup.getName(clazz));
            JMenuItem item = new JMenuItem(label);
            for (int ii = 0, nn = label.length(); ii < nn; ii++) {
                char c = Character.toLowerCase(label.charAt(ii));
                if (Character.isLetter(c) && mnems.add(c)) {
                    item.setMnemonic(c);
                    break;
                }
            }
            if (++idx <= 9) {
                item.setAccelerator(KeyStroke.getKeyStroke(
                    Character.forDigit(idx, 10), KeyEvent.CTRL_MASK));
            }
            nmenu.add(item);
            item.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    newConfig(clazz);
                }
            });
        }

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("config_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file != null && (file.isDirectory() ||
                    file.toString().toLowerCase().endsWith(".dat"));
            }
            public String getDescription () {
                return _msgs.get("m.config_files");
            }
        });

        // and the export the file chooser
        _exportChooser = new JFileChooser(_prefs.get("export_dir", null));
        _exportChooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".xml");
            }
            public String getDescription () {
                return _msgs.get("m.xml_files");
            }
        });

        // create and add the editor panel
        add(_epanel = new EditorPanel(this, EditorPanel.CategoryMode.TABS, null),
            BorderLayout.CENTER);
        _epanel.addChangeListener(this);

        // open the initial config, if one was specified
        if (config != null) {
            open(new File(config));
        }
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        if (!_block.enter()) {
            return;
        }
        try {
            ManagedConfig config = (ManagedConfig)_epanel.getObject();
            config.updateFromSource(this, false);
            config.wasUpdated();
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
            _epanel.update();
        } finally {
            _block.leave();
        }
    }

    @Override
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("window")) {
            showFrame(new ResourceEditor(_msgmgr, _cfgmgr, _colorpos));
        } else if (action.equals("open")) {
            open();
        } else if (action.equals("save")) {
            if (_file != null) {
                save(_file);
            } else {
                save();
            }
        } else if (action.equals("save_as")) {
            save();
        } else if (action.equals("revert")) {
            if (showCantUndo()) {
                open(_file);
            }
        } else if (action.equals("import")) {
            importConfig();
        } else if (action.equals("export")) {
            exportConfig();
        } else if (action.equals("update")) {
            ManagedConfig config = (ManagedConfig)_epanel.getObject();
            config.updateFromSource(this, true);
            config.wasUpdated();
        } else if (action.equals("configs")) {
            showFrame(ConfigEditor.create(this));
        } else if (action.equals("tree_mode")) {
            BaseEditorPanel opanel = _epanel;
            remove(opanel);
            add(_epanel = _treeMode.isSelected() ? new TreeEditorPanel(this) :
                new EditorPanel(this, EditorPanel.CategoryMode.TABS), BorderLayout.CENTER);
            _epanel.addChangeListener(this);
            _epanel.setObject(opanel.getObject());
            _epanel.revalidate();
        } else if (action.equals("batch_validate")) {
            new BatchValidateDialog(this, this, _prefs) {
                @Override protected boolean validate (Validator validator, String path) {
                    ManagedConfig config = _cfgmgr.getResourceConfig(path);
                    return config == null || config.validateReferences(validator);
                }
            }.setVisible(true);
        } else {
            super.actionPerformed(event);
        }
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();
        setConfig(null, null);
    }

    @Override
    public ConfigManager getConfigManager ()
    {
        Object config = _epanel.getObject();
        return (config instanceof ParameterizedConfig) ?
            ((ParameterizedConfig)config).getConfigManager() : _cfgmgr;
    }

    /**
     * Creates a new configuration of the specified class.
     */
    protected void newConfig (Class<?> clazz)
    {
        try {
            ManagedConfig config = (ManagedConfig)clazz.newInstance();
            config.init(_cfgmgr);
            setConfig(config, null);
        } catch (Exception e) {
            log.warning("Error creating config.", "class", clazz, e);
        }
    }

    /**
     * Brings up the open dialog.
     */
    protected void open ()
    {
        if (_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            open(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to open the specified config file.
     */
    protected void open (File file)
    {
        ManagedConfig config;
        try {
            BinaryImporter in = new BinaryImporter(new FileInputStream(file));
            config = (ManagedConfig)in.readObject();
            config.init(_cfgmgr);
            in.close();
        } catch (IOException e) {
            log.warning("Failed to open config [file=" + file + "].", e);
            return;
        }
        // retrieve the instance through the cache
        String path = _rsrcmgr.getResourcePath(file);
        if (path != null) {
            config.setName(path);
            config = _cfgmgr.updateResourceConfig(path, config);
        }
        setConfig(config, file);
    }

    /**
     * Brings up the save dialog.
     */
    protected void save ()
    {
        if (_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            save(_chooser.getSelectedFile());
        }
        _prefs.put("config_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to save to the specified file.
     */
    protected void save (File file)
    {
        ManagedConfig config = (ManagedConfig)_epanel.getObject();
        String oname = config.getName();
        config.setName(null);
        try {
            BinaryExporter out = new BinaryExporter(new FileOutputStream(file));
            out.writeObject(config);
            out.close();
        } catch (IOException e) {
            log.warning("Failed to save config [file=" + file + "].", e);
            return;
        } finally {
            config.setName(oname);
        }
        // do some special handling to make sure we play nice with the cache
        String opath = (_file == null) ? null : _rsrcmgr.getResourcePath(_file);
        String npath = _rsrcmgr.getResourcePath(file);
        if (!Objects.equal(opath, npath)) {
            if (opath != null) {
                config = (ManagedConfig)config.clone();
                config.init(_cfgmgr);
            }
            if (npath != null) {
                config.setName(npath);
                config = _cfgmgr.updateResourceConfig(npath, config);
            }
        }
        setConfig(config, file);
    }

    /**
     * Brings up the import dialog.
     */
    protected void importConfig ()
    {
        if (_exportChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = _exportChooser.getSelectedFile();
            try {
                XMLImporter in = new XMLImporter(new FileInputStream(file));
                ManagedConfig config = (ManagedConfig)in.readObject();
                config.init(_cfgmgr);
                setConfig(config, null);
                in.close();
            } catch (IOException e) {
                log.warning("Failed to import config [file=" + file +"].", e);
            }
        }
        _prefs.put("export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the export dialog.
     */
    protected void exportConfig ()
    {
        if (_exportChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = _exportChooser.getSelectedFile();
            ManagedConfig config = (ManagedConfig)_epanel.getObject();
            String oname = config.getName();
            config.setName(null);
            try {
                XMLExporter out = new XMLExporter(new FileOutputStream(file));
                out.writeObject(config);
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export config [file=" + file + "].", e);
            } finally {
                config.setName(oname);
            }
        }
        _prefs.put("export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Sets the configuration being edited.
     */
    protected void setConfig (ManagedConfig config, File file)
    {
        ManagedConfig oconfig = (ManagedConfig)_epanel.getObject();
        if (oconfig != null) {
            oconfig.removeListener(this);
        }
        _epanel.setObject(config);
        boolean enable = (config != null);
        if (enable) {
            config.addListener(this);
        }
        _file = file;
        _save.setEnabled(enable);
        _saveAs.setEnabled(enable);
        _revert.setEnabled(file != null);
        _export.setEnabled(enable);
        setTitle(_msgs.get("m.title") + (file == null ? "" : (": " + file)));
    }

    /**
     * Shows a confirm dialog.
     */
    protected boolean showCantUndo ()
    {
        return JOptionPane.showConfirmDialog(this, _msgs.get("m.cant_undo"),
                _msgs.get("t.cant_undo"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == 0;
    }

    @Override
    protected BaseEditorPanel getFindEditorPanel ()
    {
        return _epanel;
    }

    /** The file menu items. */
    protected JMenuItem _save, _saveAs, _revert, _export;

    /** The tree mode toggle. */
    protected JCheckBoxMenuItem _treeMode;

    /** The file chooser for opening and saving config files. */
    protected JFileChooser _chooser;

    /** The file chooser for opening and saving export files. */
    protected JFileChooser _exportChooser;

    /** The editor panel. */
    protected BaseEditorPanel _epanel;

    /** The loaded config file. */
    protected File _file;

    /** Indicates that we should ignore any changes, because we're the one effecting them. */
    protected ChangeBlock _block = new ChangeBlock();
}
