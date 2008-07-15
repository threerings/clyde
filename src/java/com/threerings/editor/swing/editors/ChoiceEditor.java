//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.threerings.editor.swing.PropertyEditor;

/**
 * Provides a means of selecting between several different objects.
 */
public class ChoiceEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {

    }

    @Override // documentation inherited
    public void update ()
    {

    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        add(_box = new JComboBox());
        _box.addActionListener(this);
    }

    /** The combo box. */
    protected JComboBox _box;
}
