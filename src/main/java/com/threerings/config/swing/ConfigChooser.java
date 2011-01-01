//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

package com.threerings.config.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;

import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.editor.Introspector;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

/**
 * A simple dialog that allows the user to select a configuration from a tree.
 */
public abstract class ConfigChooser extends JPanel
{
    /**
     * Creates a new configuration chooser for the specified config class.
     */
    public static ConfigChooser createInstance (
        MessageManager msgmgr, ConfigManager cfgmgr, Class<?> clazz)
    {
        return createInstance(msgmgr, cfgmgr, clazz, null);
    }

    /**
     * Creates a new configuration chooser for the specified config class.
     *
     * @param config the initial selected configuration.
     */
    public static ConfigChooser createInstance (
        MessageManager msgmgr, ConfigManager cfgmgr, Class<?> clazz, String config)
    {
        ConfigChooser chooser = cfgmgr.isResourceClass(clazz) ?
            new ResourceChooser(msgmgr, cfgmgr.getResourceManager(), clazz) :
                new TreeChooser(msgmgr, cfgmgr, clazz);
        if (config != null) {
            chooser.setSelectedConfig(config);
        }
        return chooser;
    }

    /**
     * Displays the dialog.
     *
     * @return true if a configuration was selected, false if not.
     */
    public abstract boolean showDialog (Component parent);

    /**
     * Sets the path of the selected config.
     */
    public abstract void setSelectedConfig (String config);

    /**
     * Returns the path of the selected config.
     */
    public abstract String getSelectedConfig ();

    /**
     * Returns the label for the specified class.
     */
    protected String getLabel (MessageManager msgmgr, Class<?> clazz, String type)
    {
        MessageBundle msgs = msgmgr.getBundle(Introspector.getMessageBundle(clazz));
        String key = "m." + type;
        return msgs.exists(key) ? msgs.get(key) : type;
    }

    /**
     * Selects a resource-loaded configuration using a file chooser.
     */
    protected static class ResourceChooser extends ConfigChooser
    {
        public ResourceChooser (final MessageManager msgmgr, ResourceManager rsrcmgr, Class<?> clazz)
        {
            _rsrcmgr = rsrcmgr;
            final MessageBundle msgs = msgmgr.getBundle("config");
            String ddir = rsrcmgr.getResourceFile("").toString();
            String type = ConfigGroup.getName(clazz);
            final String label = getLabel(msgmgr, clazz, type);
            _chooser = new JFileChooser(_prefs.get(_prefdir = type + "_dir", ddir));
            _chooser.setDialogTitle(msgs.get("m.select_config", label));
            _chooser.setFileFilter(new FileFilter() {
                public boolean accept (File file) {
                    return file.isDirectory() ||
                        StringUtil.toUSLowerCase(file.getName()).endsWith(".dat");
                }
                public String getDescription () {
                    return msgs.get("m.config_files", label);
                }
            });
        }

        @Override // documentation inherited
        public boolean showDialog (Component parent)
        {
            boolean approved = (_chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION);
            _prefs.put(_prefdir, _chooser.getCurrentDirectory().toString());
            return approved;
        }

        @Override // documentation inherited
        public void setSelectedConfig (String config)
        {
            _chooser.setSelectedFile(config == null ? null : _rsrcmgr.getResourceFile(config));
        }

        @Override // documentation inherited
        public String getSelectedConfig ()
        {
            File file = _chooser.getSelectedFile();
            return (file == null) ? null : _rsrcmgr.getResourcePath(file);
        }

        /** The resource manager. */
        protected ResourceManager _rsrcmgr;

        /** The file chooser. */
        protected JFileChooser _chooser;

        /** The directory preference key. */
        protected String _prefdir;
    }

    /**
     * Selects a configuration from a {@link ConfigTree}.
     */
    protected static class TreeChooser extends ConfigChooser
    {
        public TreeChooser (MessageManager msgmgr, ConfigManager cfgmgr, Class<?> clazz)
        {
            _msgs = msgmgr.getBundle("config");
            _label = getLabel(msgmgr, clazz, ConfigGroup.getName(clazz));

            // get the list of groups
            @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                (Class<ManagedConfig>)clazz;
            _groups = cfgmgr.getGroups(cclass);

            setLayout(new BorderLayout());
            JPanel bpanel = new JPanel();
            add(bpanel, BorderLayout.SOUTH);
            bpanel.add(_ok = new JButton(_msgs.get("m.ok")));
            bpanel.add(_cancel = new JButton(_msgs.get("m.cancel")));
        }

        @Override // documentation inherited
        public boolean showDialog (Component parent)
        {
            // create the dialog
            Component root = SwingUtilities.getRoot(parent);
            String title = _msgs.get("m.select_config", _label);
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
            dialog.setLocationRelativeTo(parent);
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

        @Override // documentation inherited
        public void setSelectedConfig (String config)
        {
            _selected = config;
        }

        @Override // documentation inherited
        public String getSelectedConfig ()
        {
            return _selected;
        }

        /** The bundle from which we obtain our messages. */
        protected MessageBundle _msgs;

        /** The group label. */
        protected String _label;

        /** The configuration groups. */
        protected ConfigGroup[] _groups;

        /** The OK button. */
        protected JButton _ok;

        /** The cancel button. */
        protected JButton _cancel;

        /** The path of the selected configuration. */
        protected String _selected;
    }

    /** User preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ConfigChooser.class);
}
