//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.samskivert.swing.event.CommandEvent;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ObjectUtil;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.util.EditorContext;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

import static com.threerings.ClydeLog.*;

/**
 * Allows editing single configurations stored as resources.
 */
public class ResourceEditor extends BaseConfigEditor
    implements EditorContext, ChangeListener
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, "config/");
        new ResourceEditor(
            msgmgr, cfgmgr, true, args.length > 0 ? args[0] : null).setVisible(true);
    }

    /**
     * Creates a new resource editor.
     */
    public ResourceEditor (MessageManager msgmgr, ConfigManager cfgmgr)
    {
        this(msgmgr, cfgmgr, false, null);
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
        return _cfgmgr;
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        ((ManagedConfig)_epanel.getObject()).wasUpdated();
    }

    /**
     * Creates a new config editor.
     */
    protected ResourceEditor (
        MessageManager msgmgr, ConfigManager cfgmgr, boolean standalone, String config)
    {
        super(cfgmgr.getResourceManager(), msgmgr, "resource", standalone);
        _cfgmgr = cfgmgr;

        setSize(550, 600);
        SwingUtil.centerWindow(this);

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);

        JMenu nmenu = createMenu("new", KeyEvent.VK_N);
        file.add(nmenu);
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
        if (_standalone) {
            file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));
        } else {
            file.add(createMenuItem("close", KeyEvent.VK_C, KeyEvent.VK_W));
        }

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));

        // add the edit preferences option if running standalone
        if (_standalone) {
            edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));
            _eprefs = new ToolUtil.EditablePrefs(_prefs);
            _eprefs.init(_rsrcmgr);

            // initialize the configuration manager here, after we have set the resource dir
            cfgmgr.init();
        }

        // add the new items now that we've initialized the config manager
        ArrayIntSet mnems = new ArrayIntSet();
        int idx = 0;
        MessageBundle cmsgs = _msgmgr.getBundle("config");
        for (final Class clazz : cfgmgr.getResourceClasses()) {
            String name = ConfigGroup.getName(clazz);
            String key = "m." + name;
            String label = cmsgs.exists(key) ? cmsgs.get(key) : name;
            JMenuItem item = new JMenuItem(label);
            for (int ii = 0, nn = label.length(); ii < nn; ii++) {
                char c = Character.toLowerCase(label.charAt(ii));
                if (Character.isLetter(c) && !mnems.contains(c)) {
                    mnems.add(c);
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
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".dat");
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
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("open")) {
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
            open(_file);
        } else if (action.equals("import")) {
            importConfig();
        } else if (action.equals("export")) {
            exportConfig();
        } else if (action.equals("configs")) {
            if (_configEditor == null) {
                _configEditor = new ConfigEditor(_msgmgr, _cfgmgr);
            }
            _configEditor.setVisible(true);
        } else if (action.equals("preferences")) {
            if (_pdialog == null) {
                _pdialog = EditorPanel.createDialog(
                    this, this, _msgs.get("t.preferences"), _eprefs);
            }
            _pdialog.setVisible(true);
        } else {
            super.actionPerformed(event);
        }
    }

    /**
     * Creates a new configuration of the specified class.
     */
    protected void newConfig (Class clazz)
    {
        try {
            setConfig((ManagedConfig)clazz.newInstance(), null);
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
            in.close();
        } catch (IOException e) {
            log.warning("Failed to open config [file=" + file + "].", e);
            return;
        }
        // if not running standalone, we must retrieve the instance through the cache
        if (!_standalone) {
            String path = _rsrcmgr.getResourcePath(file);
            if (path != null) {
                config = _cfgmgr.updateResourceConfig(path, config);
            }
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
        ManagedConfig config;
        try {
            BinaryExporter out = new BinaryExporter(new FileOutputStream(file));
            config = (ManagedConfig)_epanel.getObject();
            out.writeObject(config);
            out.close();
        } catch (IOException e) {
            log.warning("Failed to save config [file=" + file + "].", e);
            return;
        }
        // if not running standalone, we must do some special handling to make sure we
        // play nice with the cache
        if (!_standalone) {
            String opath = (_file == null) ? null : _rsrcmgr.getResourcePath(_file);
            String npath = _rsrcmgr.getResourcePath(file);
            if (!ObjectUtil.equals(opath, npath)) {
                if (opath != null) {
                    config = (ManagedConfig)config.clone();
                }
                if (npath != null) {
                    config = _cfgmgr.updateResourceConfig(npath, config);
                }
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
                setConfig((ManagedConfig)in.readObject(), null);
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
            try {
                XMLExporter out = new XMLExporter(new FileOutputStream(file));
                out.writeObject(_epanel.getObject());
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export config [file=" + file + "].", e);
            }
        }
        _prefs.put("export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Sets the configuration being edited.
     */
    protected void setConfig (ManagedConfig config, File file)
    {
        _epanel.setObject(config);
        _file = file;
        _save.setEnabled(true);
        _saveAs.setEnabled(true);
        _revert.setEnabled(file != null);
        _export.setEnabled(true);
        setTitle(_msgs.get("m.title") + (file == null ? "" : (": " + file)));
    }

    /** The config manager. */
    protected ConfigManager _cfgmgr;

    /** The file menu items. */
    protected JMenuItem _save, _saveAs, _revert, _export;

    /** The file chooser for opening and saving config files. */
    protected JFileChooser _chooser;

    /** The file chooser for opening and saving export files. */
    protected JFileChooser _exportChooser;

    /** The config editor. */
    protected ConfigEditor _configEditor;

    /** The editable preferences object. */
    protected ToolUtil.EditablePrefs _eprefs;

    /** The preferences dialog. */
    protected JDialog _pdialog;

    /** The editor panel. */
    protected EditorPanel _epanel;

    /** The loaded config file. */
    protected File _file;

    /** The resource path of the loaded config file. */
    protected String _path;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ResourceEditor.class);
}
