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

package com.threerings.config.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;

import java.util.Arrays;
import java.util.List;
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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.editor.Introspector;
import com.threerings.editor.util.PropertyUtil;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ReferenceConstraints;

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
        return cfgmgr.isResourceClass(clazz)
                ? new ResourceChooser(msgmgr, cfgmgr.getResourceManager(), clazz)
                : new TreeChooser(msgmgr, cfgmgr, clazz);
    }

    /**
     * Creates a new configuration chooser for the specified config class.
     */
    public static ConfigChooser createInstance (
        MessageManager msgmgr, ConfigManager cfgmgr, Class<?> clazz,
        ReferenceConstraints constraints)
    {
        ConfigChooser chooser = createInstance(msgmgr, cfgmgr, clazz);
        if (constraints != null) {
            if (!(chooser instanceof TreeChooser)) {
                throw new UnsupportedOperationException(
                        "Constraints can't yet be used with resources");
            }

            String desc = constraints.description();
            if ("".equals(desc)) {
                List<Class<? extends ManagedConfig>> vals = Arrays.asList(constraints.value());
                desc = Joiner.on(", ").join(Iterables.transform(vals,
                            new Function<Class<?>, String>() {
                                public String apply (Class<?> clazz) {
                                    return ConfigGroup.getName(clazz);
                                }
                            }));

            } else {
                MessageBundle msgs = msgmgr.getBundle(Introspector.getMessageBundle(clazz));
                if (msgs.exists(desc)) {
                    desc = msgs.get(desc);
                }
            }
            ((TreeChooser)chooser)._filterPanel.addConstraint(
                    desc, PropertyUtil.getRawConfigPredicate(constraints), false);
        }
        return chooser;
    }

    /**
     * Creates a new configuration chooser for the specified config class.
     *
     * @param config the initial selected configuration.
     */
    public static ConfigChooser createInstance (
        MessageManager msgmgr, ConfigManager cfgmgr, Class<?> clazz, String config)
    {
        ConfigChooser chooser = createInstance(msgmgr, cfgmgr, clazz);
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
            final MessageBundle msgs = msgmgr.getBundle("editor.config");
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

        @Override
        public boolean showDialog (Component parent)
        {
            boolean approved = (_chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION);
            _prefs.put(_prefdir, _chooser.getCurrentDirectory().toString());
            return approved;
        }

        @Override
        public void setSelectedConfig (String config)
        {
            _chooser.setSelectedFile(config == null ? null : _rsrcmgr.getResourceFile(config));
        }

        @Override
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
            _msgs = msgmgr.getBundle("editor.config");
            _label = getLabel(msgmgr, clazz, ConfigGroup.getName(clazz));

            // get the list of groups
            @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                (Class<ManagedConfig>)clazz;
            _groups = cfgmgr.getGroups(cclass);

            setLayout(new BorderLayout());

            _filterPanel = new ConfigTreeFilterPanel(msgmgr);
            add(_filterPanel, BorderLayout.NORTH);

            JPanel bpanel = new JPanel();
            add(bpanel, BorderLayout.SOUTH);
            bpanel.add(_ok = new JButton(_msgs.get("b.ok")));
            bpanel.add(_cancel = new JButton(_msgs.get("b.cancel")));
        }

        @Override
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
            _filterPanel.setTree(tree);

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

            // listen for double-clicks on the tree
            tree.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed (MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        int row = tree.getRowForLocation(event.getX(), event.getY());
                        ConfigTreeNode node = tree.getSelectedNode();
                        if (row != -1 && node != null && node.getConfig() != null) {
                            _selected = node.getName();
                            result[0] = true;
                            dialog.setVisible(false);
                        }
                    }
                }
            });

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

        @Override
        public void setSelectedConfig (String config)
        {
            _selected = config;
        }

        @Override
        public String getSelectedConfig ()
        {
            return _selected;
        }

        /** The bundle from which we obtain our messages. */
        protected MessageBundle _msgs;

        /** The group label. */
        protected String _label;

        /** The config filtering panel. */
        protected ConfigTreeFilterPanel _filterPanel;

        /** The configuration groups. */
        protected ConfigGroup<?>[] _groups;

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
