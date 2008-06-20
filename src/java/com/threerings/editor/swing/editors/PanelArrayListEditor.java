//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Array;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ListUtil;

import com.threerings.editor.swing.ObjectPanel;

/**
 * An editor for arrays or lists of objects.  Uses embedded panels.
 */
public class PanelArrayListEditor extends ArrayListEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        ObjectPanel panel = (ObjectPanel)event.getSource();
        int idx = _panels.getComponentZOrder(panel);
        if (_property.getType().isArray()) {
            Array.set(_property.get(_object), idx, panel.getValue());
        } else {
            @SuppressWarnings("unchecked") List<Object> values =
                (List<Object>)_property.get(_object);
            values.set(idx, panel.getValue());
        }
        fireStateChanged();
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        add(_panels = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _panels.setBackground(null);

        JPanel bpanel = new JPanel();
        bpanel.setBackground(null);
        add(bpanel);
        bpanel.add(_add = new JButton(getActionLabel("new")));
        _add.addActionListener(this);
    }

    @Override // documentation inherited
    protected void update ()
    {
        int pcount = _panels.getComponentCount();
        int length = getLength();
        for (int ii = 0; ii < length; ii++) {
            Object value = getValue(ii);
            if (ii < pcount) {
                ObjectPanel panel = (ObjectPanel)_panels.getComponent(ii);
                panel.removeChangeListener(this);
                panel.setValue(value);
                panel.addChangeListener(this);
            } else {
                addPanel(value);
            }
        }
        while (pcount > length) {
            _panels.remove(--pcount);
        }
        _panels.revalidate();
    }

    @Override // documentation inherited
    protected String getPathComponent (Point pt)
    {
        Component comp = _panels.getComponentAt(
            SwingUtilities.convertPoint(this, pt, _panels));
        int idx = _panels.getComponentZOrder(comp);
        return _property.getName() + (idx == -1 ? "" : ("[" + idx + "]"));
    }

    @Override // documentation inherited
    protected void addValue (Object value)
    {
        super.addValue(value);
        addPanel(value);
        _panels.revalidate();
    }

    @Override // documentation inherited
    protected void removeValue (int idx)
    {
        super.removeValue(idx);
        _panels.remove(idx);
        _panels.revalidate();
    }

    /**
     * Adds an object panel for the specified entry.
     */
    protected void addPanel (Object value)
    {
        final ObjectPanel panel = new ObjectPanel(
            _ctx, _property.getComponentTypeLabel(),
            _property.getComponentSubtypes(), _lineage);
        _panels.add(panel);
        panel.setValue(value);
        panel.addChangeListener(this);

        JPanel bpanel = new JPanel();
        bpanel.setBackground(null);
        panel.add(bpanel);
        if (getLength() > _min) {
            JButton delete = new JButton(getActionLabel("delete"));
            bpanel.add(delete);
            delete.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    int idx = ListUtil.indexOfRef(_panels.getComponents(), panel);
                    removeValue(idx);
                }
            });
        }

        panel.add(new JSeparator());
    }

    /** The container holding the panels. */
    protected JPanel _panels;
}
