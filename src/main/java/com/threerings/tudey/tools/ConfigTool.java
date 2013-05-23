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

package com.threerings.tudey.tools;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.swing.ConfigTree;
import com.threerings.config.swing.ConfigTreeFilterPanel;
import com.threerings.config.swing.ConfigTreeNode;
import com.threerings.config.swing.RecentConfigList;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.data.TudeySceneModel;

/**
 * Base class for tools using config libraries.
 */
public abstract class ConfigTool<T extends ManagedConfig> extends EditorTool
    implements TreeSelectionListener, ChangeListener
{
    /**
     * Creates the config tool.
     */
    public ConfigTool (SceneEditor editor, Class<T> clazz, EditableReference<T> eref)
    {
        super(editor);
        _clazz = clazz;
        _eref = eref;

        _recentConfigs = new RecentConfigList(getClass().getSimpleName());
        _recentConfigs.addObserver(new RecentConfigList.Observer() {
            @Override
            public void configSelected (ConfigReference<?> ref) {
                @SuppressWarnings("unchecked") // we only add the right type
                ConfigReference<T> casted = (ConfigReference<T>)ref;
                setReference(casted);
            }
        });

        JPanel panel = new JPanel(
            new VGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        JSplitPane recentSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
            _recentConfigs, panel);
        add(recentSplit);

        // add the filter panel for configs
        _filterPanel = new ConfigTreeFilterPanel(editor.getMessageManager());
        panel.add(_filterPanel, GroupLayout.FIXED);

        // create and add the split pane
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
            _pane = new JScrollPane(), _epanel = new EditorPanel(editor));
        split.setResizeWeight(1.0);
        panel.add(split);
        _epanel.setMinimumSize(new Dimension(120, 120));
        _epanel.setObject(eref);
        _epanel.addChangeListener(this);
    }

    /**
     * Add the specified reference as a "recent" one, even if it was not directly selected
     * by this tool.
     */
    public void addAsRecent (ConfigReference<?> ref)
    {
        _recentConfigs.addRecent(ref);
    }

    /**
     * Sets the reference.
     */
    public void setReference (ConfigReference<T> ref)
    {
        _tree.setSelectedNode(ref == null ? null : ref.getName());
        _eref.setReference(ref);
        _epanel.update();
        referenceChanged(ref);
    }

    // documentation inherited from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent event)
    {
        ConfigTreeNode node = _tree.getSelectedNode();
        String name = (node == null || node.getConfig() == null) ? null : node.getName();
        ConfigReference<T> ref = (name == null) ? null : new ConfigReference<T>(name);
        _eref.setReference(ref);
        _epanel.update();
        referenceChanged(ref);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        ConfigReference<T> ref = _eref.getReference();
        _tree.setSelectedNode(ref == null ? null : ref.getName());
        referenceChanged(ref);
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);

        // (re)create the config tree
        if (_tree != null) {
            _tree.dispose();
        }
        _pane.setViewportView(_tree = new ConfigTree(scene.getConfigManager().getGroups(_clazz)));
        _tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _tree.addTreeSelectionListener(this);
        _filterPanel.setTree(_tree);
    }

    /**
     * Called when the reference changes.
     */
    protected void referenceChanged (ConfigReference<T> ref)
    {
        // add it as a recent reference
        if (ref != null) {
            addAsRecent(ref);
        }
    }

    /**
     * Allows us to edit the placeable reference.
     */
    protected static abstract class EditableReference<T extends ManagedConfig> extends DeepObject
        implements Exportable
    {
        /**
         * Returns a reference to the config reference.
         */
        public abstract ConfigReference<T> getReference ();

        /**
         * Sets the config reference.
         */
        public abstract void setReference (ConfigReference<T> ref);
    }

    /** The config class for the brush. */
    protected Class<T> _clazz;

    /** Our recent configs. */
    protected RecentConfigList _recentConfigs;

    /** The editable reference. */
    protected EditableReference<T> _eref;

    /** The scroll pane containing the tree. */
    protected JScrollPane _pane;

    /** The tree of configs. */
    protected ConfigTree _tree;

    /** The filter panel for the config tree. */
    protected ConfigTreeFilterPanel _filterPanel;

    /** The editor panel that we use to adjust placeable arguments. */
    protected EditorPanel _epanel;
}
