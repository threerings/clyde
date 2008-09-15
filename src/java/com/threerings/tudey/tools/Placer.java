//
// $Id$

package com.threerings.tudey.tools;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.threerings.config.ConfigReference;
import com.threerings.config.swing.ConfigTree;
import com.threerings.config.swing.ConfigTreeNode;
import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel;

/**
 * The placeable placer tool.
 */
public class Placer extends SceneEditor.Tool
    implements TreeSelectionListener, ChangeListener
{
    /**
     * Creates the placer tool.
     */
    public Placer (SceneEditor editor)
    {
        super(editor);

        // create and add the split pane
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
            _pane = new JScrollPane(), _epanel = new EditorPanel(editor));
        split.setResizeWeight(1.0);
        add(split);
        _epanel.setObject(new EditableReference());
        _epanel.addChangeListener(this);
    }

    // documentation inherited from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent event)
    {
        EditableReference ref = (EditableReference)_epanel.getObject();
        ConfigTreeNode node = _tree.getSelectedNode();
        ref.placeable = (node == null) ? null :
            new ConfigReference<PlaceableConfig>(node.getName());
        _epanel.update();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        EditableReference ref = (EditableReference)_epanel.getObject();
        _tree.setSelectedNode(ref.placeable == null ? null : ref.placeable.getName());
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        // (re)create the config tree
        if (_tree != null) {
            _tree.dispose();
        }
        _pane.setViewportView(_tree = new ConfigTree(
            scene.getConfigManager().getGroups(PlaceableConfig.class)));
        _tree.addTreeSelectionListener(this);
    }

    /**
     * Allows us to edit the placeable reference.
     */
    protected static class EditableReference extends DeepObject
        implements Exportable
    {
        /** The placeable reference. */
        @Editable(nullable=true)
        public ConfigReference<PlaceableConfig> placeable;
    }

    /** The scroll pane containing the tree. */
    protected JScrollPane _pane;

    /** The tree of configs. */
    protected ConfigTree _tree;

    /** The editor panel that we use to adjust placeable arguments. */
    protected EditorPanel _epanel;
}
