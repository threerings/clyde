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
import com.threerings.config.ManagedConfig;
import com.threerings.config.swing.ConfigTree;
import com.threerings.config.swing.ConfigTreeNode;
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

        // create and add the split pane
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
            _pane = new JScrollPane(), _epanel = new EditorPanel(editor));
        split.setResizeWeight(1.0);
        add(split);
        _epanel.setObject(eref);
        _epanel.addChangeListener(this);
    }

    // documentation inherited from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent event)
    {
        ConfigTreeNode node = _tree.getSelectedNode();
        ConfigReference<T> ref = (node == null) ? null : new ConfigReference<T>(node.getName());
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

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        // (re)create the config tree
        if (_tree != null) {
            _tree.dispose();
        }
        _pane.setViewportView(_tree = new ConfigTree(scene.getConfigManager().getGroups(_clazz)));
        _tree.addTreeSelectionListener(this);
    }

    /**
     * Called when the reference changes.
     */
    protected void referenceChanged (ConfigReference<T> ref)
    {
        // nothing by default
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

    /** The editable reference. */
    protected EditableReference<T> _eref;

    /** The scroll pane containing the tree. */
    protected JScrollPane _pane;

    /** The tree of configs. */
    protected ConfigTree _tree;

    /** The editor panel that we use to adjust placeable arguments. */
    protected EditorPanel _epanel;
}
