//
// $Id$

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Collection;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.samskivert.util.QuickSort;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.editor.swing.EditorPanel;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigListener;
import com.threerings.config.ConfigManager;

/**
 * Allows editing the configuration database.  Can either be invoked standalone or from within
 * another application.
 */
public class ConfigEditor
    implements ActionListener, ItemListener
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, "config/manager.properties");
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

        // create and init the editable preferences
        _eprefs = new ToolUtil.EditablePrefs(_prefs);
        _eprefs.init(_rsrcmgr);

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

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

        JPanel bpanel = new JPanel();
        cpanel.add(bpanel, GroupLayout.FIXED);
        bpanel.add(createButton("new_config", "m.new"));
        bpanel.add(_cloneConfig = createButton("clone_config", "m.clone"));
        _cloneConfig.setEnabled(false);
        bpanel.add(_deleteConfig = createButton("delete_config", "m.delete"));
        _deleteConfig.setEnabled(false);

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
        if (action.equals("quit")) {
            shutdown();
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
     * Contains the state of a single group.
     */
    protected class GroupState
        implements Comparable<GroupState>
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
                tree = new JTree();
                tree.setRootVisible(false);
                tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
            _pane.setViewportView(tree);
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

    /** The editable preferences object. */
    protected ToolUtil.EditablePrefs _eprefs;

    /** The preferences dialog. */
    protected JDialog _pdialog;

    /** The configuration group states. */
    protected GroupState[] _gstates;

    /** Determines the selected group. */
    protected JComboBox _gbox;

    /** The scroll pane that holds the group trees. */
    protected JScrollPane _pane;

    /** The "clone configuration" button. */
    protected JButton _cloneConfig;

    /** The "delete configuration" button. */
    protected JButton _deleteConfig;

    /** The object editor panel. */
    protected EditorPanel _epanel;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ConfigEditor.class);
}
