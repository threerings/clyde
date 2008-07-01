//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.editor.swing.DraggableSpinner;
import com.threerings.editor.swing.PropertyEditor;

import com.threerings.opengl.renderer.Color4f;

/**
 * Editor for color properties.
 */
public class Color4fEditor extends PropertyEditor
    implements ActionListener, ChangeListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (_chooser == null) {
            _chooser = new JColorChooser();
            _chooser.getSelectionModel().addChangeListener(this);
            _dialog = JColorChooser.createDialog(
                this, getPropertyLabel(), false, _chooser, null, null);
        }
        Color4f color = (Color4f)_property.get(_object);
        _chooser.setColor(color.getColor(false));
        _dialog.setVisible(true);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Color4f ocolor = (Color4f)_property.get(_object), ncolor;
        Color awtColor;
        if (event.getSource() == _spinner) {
            awtColor = ocolor.getColor(false);
            ncolor = new Color4f(
                ocolor.r, ocolor.g, ocolor.b, ((Number)_spinner.getValue()).floatValue());

        } else { // source == _chooser.getSelectionModel()
            awtColor = _chooser.getColor();
            ncolor = new Color4f(awtColor);
            ncolor.a = ocolor.a;
        }
        if (!ocolor.equals(ncolor)) {
            _property.set(_object, ncolor);
            _button.setBackground(awtColor);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        Color4f color = (Color4f)_property.get(_object);
        Color awtColor = color.getColor(false);
        _button.setBackground(awtColor);
        if (_spinner != null) {
            _spinner.setValue(color.a);
        }
        if (_chooser != null && _chooser.isShowing()) {
            _chooser.setColor(awtColor);
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        add(_button = new JButton());
        _button.setPreferredSize(new Dimension(40, 20));
        _button.addActionListener(this);
        if (getMode().equals("alpha")) {
            add(_spinner = new DraggableSpinner(1f, 0f, 1f, 0.01f));
            _spinner.addChangeListener(this);
        }
    }

    /** The button that brings up the color selection dialog. */
    protected JButton _button;

    /** The color chooser. */
    protected JColorChooser _chooser;

    /** The color chooser dialog. */
    protected JDialog _dialog;

    /** The alpha spinner. */
    protected DraggableSpinner _spinner;
}
