package com.threerings.tudey.tools;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;

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

    _moveMulti = new AbstractAction(_msgs.get("b.move_multi_layer")) {
      public void actionPerformed (ActionEvent e) {
        processSelection(false);
        setEnabled(false);
      }
    };
    _copyMulti = new AbstractAction(_msgs.get("b.copy_multi_layer")) {
      public void actionPerformed (ActionEvent e) {
        processSelection(true);
        setEnabled(false);
      }
    };

    layers.addChangeListener(_layerListener);

    JPanel vbox = GroupLayout.makeVBox(GroupLayout.NONE, GroupLayout.TOP);
    JPanel hbox = GroupLayout.makeHStretchBox(5);
    hbox.add(new JLabel(_msgs.get("l.target_layer")), GroupLayout.FIXED);
    hbox.add(_layerBox);
    vbox.add(hbox, GroupLayout.FIXED);

    hbox = GroupLayout.makeHStretchBox(5);
    hbox.add(new JButton(_moveMulti), GroupLayout.FIXED);
    hbox.add(new JButton(_copyMulti), GroupLayout.FIXED);
    vbox.add(hbox, GroupLayout.FIXED);
    add(vbox);
  }

  @Override
  public void addNotify ()
  {
    super.addNotify();
    int selectionCount = _editor.getSelection().length;
    boolean multi = selectionCount > 1;
    _moveMulti.setEnabled(multi);
    _copyMulti.setEnabled(multi);
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
    processSelection(false);
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
    _moveMulti.setEnabled(false);
    _copyMulti.setEnabled(false);
  }

  /**
   * Move the selected entries to the target layer.
   */
  protected void processSelection (boolean copy)
  {
    int layer = _layerBox.getSelectedIndex();
    for (TudeySceneModel.Entry entry : _editor.getSelection()) {
      if (copy) {
        _scene.addEntry((TudeySceneModel.Entry)entry.clone(), layer);
      } else {
        _scene.setLayer(entry.getKey(), layer);
      }
    }
    _editor.setSelection();
  }

  /**
   * Update the layers displayed in the combo box.
   */
  protected void updateLayers ()
  {
    // see what used to be selected
    int oldSize = _layerBox.getItemCount();
    LayerInfo selected = (LayerInfo)_layerBox.getSelectedItem();

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
    _layerBox.setModel(model);
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

  /** Allows the user to select the target layer. */
  protected JComboBox _layerBox = new JComboBox();

  /** Listens to changes in layers. */
  protected ChangeListener _layerListener = new ChangeListener() {
    public void stateChanged (ChangeEvent event)
    {
      updateLayers();
    }
  };

  /** The action for moving multiple items to the layer. */
  protected Action _moveMulti, _copyMulti;
}
