//
// $Id$

package com.threerings.config.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.samskivert.swing.util.SwingUtil;

import com.threerings.util.MessageBundle;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

/**
 * A simple dialog that allows the user to select a configuration from a tree.
 */
public class ConfigChooser extends JPanel
{
    /**
     * Creates a new configuration chooser for the specified group.
     */
    public ConfigChooser (MessageBundle msgs, ConfigManager cfgmgr, Class clazz)
    {
        this(msgs, cfgmgr, clazz, null);
    }

    /**
     * Creates a new configuration chooser for the specified group.
     *
     * @param config the initial selected configuration.
     */
    public ConfigChooser (MessageBundle msgs, ConfigManager cfgmgr, Class clazz, String config)
    {
        super(new BorderLayout());
        _msgs = msgs;
        _selected = config;

        // get the list of configs
        ArrayList<ConfigGroup> groups = new ArrayList<ConfigGroup>();
        @SuppressWarnings("unchecked") Class<ManagedConfig> cclass = (Class<ManagedConfig>)clazz;
        for (; cfgmgr != null; cfgmgr = cfgmgr.getParent()) {
            ConfigGroup group = cfgmgr.getGroup(cclass);
            if (group != null) {
                groups.add(group);
            }
        }
        _groups = groups.toArray(new ConfigGroup[groups.size()]);
        
        JPanel bpanel = new JPanel();
        add(bpanel, BorderLayout.SOUTH);
        bpanel.add(_ok = new JButton(msgs.get("m.ok")));
        bpanel.add(_cancel = new JButton(msgs.get("m.cancel")));
    }

    /**
     * Displays the dialog.
     *
     * @return true if a configuration was selected, false if not.
     */
    public boolean showDialog (Component parent)
    {
        // create the dialog
        Component root = SwingUtilities.getRoot(parent);
        String name = _groups[0].getName();
        String key = "m." + name;
        String title = _msgs.get("m.select_config", _msgs.exists(key) ? _msgs.get(key) : name);
        final JDialog dialog = (root instanceof Dialog) ?
            new JDialog((Dialog)root, title, true) :
                new JDialog((Frame)(root instanceof Frame ? root : null), title, true);
        dialog.add(this, BorderLayout.CENTER);

        // add the tree of configurations
        final ConfigTree tree = new ConfigTree(_groups);
        JScrollPane pane = new JScrollPane(tree);
        add(pane, BorderLayout.CENTER);

        // add button listeners
        final boolean[] result = new boolean[1];
        ActionListener al = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getSource() == _ok) {
                    _selected = tree.getSelectedNode().getName();
                    result[0] = true;
                }
                dialog.setVisible(false);
            }
        };
        _ok.addActionListener(al);
        _cancel.addActionListener(al);

        // listen for selection events. select the current path
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged (TreeSelectionEvent event) {
                ConfigTreeNode node = tree.getSelectedNode();
                _ok.setEnabled(node != null && node.getConfig() != null);
            }
        });
        _ok.setEnabled(false);
        tree.setSelectedNode(_selected);

        // position and show the dialog
        dialog.setSize(300, 400);
        if (parent == null || !parent.isShowing()) {
            SwingUtil.centerWindow(dialog);
        } else {
            Point pt = parent.getLocationOnScreen();
            dialog.setLocation(
                pt.x + (parent.getWidth() - dialog.getWidth()) / 2,
                pt.y + (parent.getHeight() - dialog.getHeight()) / 2);
        }
        dialog.setVisible(true);

        // remove our listeners
        _ok.removeActionListener(al);
        _cancel.removeActionListener(al);

        // dispose of the tree
        remove(pane);
        tree.dispose();
        dialog.dispose();

        // return the stored result
        return result[0];
    }

    /**
     * Sets the path of the selected config.
     */
    public void setSelectedConfig (String config)
    {
        _selected = config;
    }

    /**
     * Returns the path of the selected config.
     */
    public String getSelectedConfig ()
    {
        return _selected;
    }

    /** The bundle from which we obtain our messages. */
    protected MessageBundle _msgs;

    /** The configuration groups. */
    protected ConfigGroup[] _groups;

    /** The OK button. */
    protected JButton _ok;

    /** The cancel button. */
    protected JButton _cancel;

    /** The path of the selected configuration. */
    protected String _selected;
}
