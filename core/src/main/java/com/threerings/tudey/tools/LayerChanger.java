package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;

import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.log;

/**
 * A tool for easily and quickly moving entries to another layer.
 */
public class LayerChanger extends EditorTool
{
    /**
     * Construct the layer changer.
     */
    public LayerChanger (SceneEditor editor, Layers layers)
    {
        super(editor);

        layers.addChangeListener(_layerListener);

        // TODO little label group
        add(_selectedLayer);
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        updateLayers();
    }

    @Override
    public void mouseMoved (MouseEvent event)
    {
        super.mouseMoved(event);
        selectMouseEntry();
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
        super.mousePressed(event);

        if (event.getButton() != MouseEvent.BUTTON1 || _editor.isSpecialDown()) {
            return;
        }
        selectMouseEntry();
    }

    @Override
    public void mouseReleased (MouseEvent event)
    {
        super.mouseReleased(event);
        moveSelection();
    }

    /**
     * Select whichever entry is under the mouse.
     */
    protected void selectMouseEntry ()
    {
        TudeySceneModel.Entry entry = _editor.getMouseEntry();
        if (entry != null) {
            _editor.setSelection(entry);
        } else {
            _editor.setSelection();
        }
    }

    /**
     * Move the selected entries to the target layer.
     */
    protected void moveSelection ()
    {
        int layer = _selectedLayer.getSelectedIndex();
        for (TudeySceneModel.Entry entry : _editor.getSelection()) {
            _scene.setLayer(entry.getKey(), layer);
        }
    }

    /**
     * Update the layers displayed in the combo box.
     */
    protected void updateLayers ()
    {
        // see what used to be selected
        int oldSize = _selectedLayer.getItemCount();
        LayerInfo selected = (LayerInfo)_selectedLayer.getSelectedItem();

        // build an array of the new layers
        List<String> layers = _scene.getLayers();
        int nn = layers.size();
        LayerInfo[] infos = new LayerInfo[nn];
        for (int ii = 0; ii < nn; ii++) {
            infos[ii] = new LayerInfo(ii, layers.get(ii));
        }

        // create the new model
        DefaultComboBoxModel model = new DefaultComboBoxModel(infos);

        // Transfer the selection to the new model.
        if (selected != null) {
            int dex = Arrays.asList(infos).indexOf(selected);
            if (dex == -1) {
                // if things are the same size as before, select by index?
                if (oldSize == infos.length) {
                    dex = selected.layer;

                } else {
                    // exact match not found. See if there's a single name that matches
                    for (int ii = 0; ii < nn; ii++) {
                        if (selected.name.equals(infos[ii].name)) {
                            if (dex == -1) {
                                dex = ii;
                            } else {
                                // multiple name matches were found! Set dex to -1 and break;
                                dex = -1;
                                break;
                            }
                        }
                    }

                    // name match not found. Reset to base?
                    if (dex == -1) {
                        dex = 0;
                    }
                }
            }
            model.setSelectedItem(infos[dex]);
        }

        // set the model
        _selectedLayer.setModel(model);
    }

    /**
     * Information on a layer.
     */
    protected static class LayerInfo
    {
        public final int layer;
        public final String name;

        public LayerInfo (int layer, String name)
        {
            this.layer = layer;
            this.name = name;
        }

        @Override
        public int hashCode ()
        {
            return layer;
        }

        @Override
        public boolean equals (Object other)
        {
            LayerInfo that;
            return (other instanceof LayerInfo) &&
                ((that = (LayerInfo)other).layer == this.layer) &&
                that.name.equals(this.name);
        }

        @Override
        public String toString ()
        {
            return (layer + ": " + name);
        }
    }

    /** Listens to changes in layers. */
    protected ChangeListener _layerListener = new ChangeListener() {
        public void stateChanged (ChangeEvent event)
        {
            updateLayers();
        }
    };

    /** Allows the user to select the target layer. */
    protected JComboBox _selectedLayer = new JComboBox();
}
