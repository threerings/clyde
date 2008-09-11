//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import java.io.IOException;

import java.lang.reflect.Array;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.CollapsiblePanel;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.util.ListUtil;

import com.threerings.media.image.ImageUtil;

import com.threerings.editor.swing.ObjectPanel;

import static com.threerings.editor.Log.*;

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
        int idx = _panels.getComponentZOrder(panel.getParent());
        setValue(idx, panel.getValue());
        fireStateChanged(true);
    }

    @Override // documentation inherited
    public void update ()
    {
        int pcount = _panels.getComponentCount();
        int length = getLength();
        for (int ii = 0; ii < length; ii++) {
            Object value = getValue(ii);
            if (ii < pcount) {
                EntryPanel panel = (EntryPanel)_panels.getComponent(ii);
                panel.getObjectPanel().setValue(value);
            } else {
                addPanel(value);
            }
        }
        while (pcount > length) {
            _panels.remove(--pcount);
        }
        updatePanels();
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
        updatePanels();
    }

    @Override // documentation inherited
    protected void removeValue (int idx)
    {
        super.removeValue(idx);
        _panels.remove(idx);
        updatePanels();
    }

    /**
     * Swaps two values in the list.
     */
    protected void swapValues (int idx1, int idx2)
    {
        Object tmp = getValue(idx1);
        setValue(idx1, getValue(idx2));
        setValue(idx2, tmp);
        _panels.setComponentZOrder(_panels.getComponent(idx1), idx2);
        fireStateChanged(true);
        updatePanels();
    }

    /**
     * Adds an object panel for the specified entry.
     */
    protected void addPanel (Object value)
    {
        _panels.add(new EntryPanel(value));
    }

    /**
     * Updates the panels' button states and revalidates.
     */
    protected void updatePanels ()
    {
        for (int ii = 0, nn = _panels.getComponentCount(); ii < nn; ii++) {
            ((EntryPanel)_panels.getComponent(ii)).updateButtons();
        }
        _panels.revalidate();
    }

    /**
     * A panel for a single entry.
     */
    protected class EntryPanel extends CollapsiblePanel
        implements ActionListener
    {
        public EntryPanel (Object value)
        {
            // create the object panel
            ObjectPanel opanel = new ObjectPanel(
                _ctx, _property.getComponentTypeLabel(),
                _property.getComponentSubtypes(), _lineage, _object);
            opanel.setValue(value);
            opanel.addChangeListener(PanelArrayListEditor.this);

            // make sure we have the icons loaded
            if (_expandIcon == null) {
                _expandIcon = loadIcon("expand");
                _collapseIcon = loadIcon("collapse");
                _raiseIcon = loadIcon("raise");
                _lowerIcon = loadIcon("lower");
                _deleteIcon = loadIcon("delete");
            }

            // create the button panel and buttons
            JPanel tcont = GroupLayout.makeHBox(
                GroupLayout.NONE, GroupLayout.RIGHT, GroupLayout.NONE);
            tcont.setOpaque(false);
            JButton expand = createButton(_expandIcon);
            tcont.add(expand);
            tcont.add(_raise = createButton(_raiseIcon));
            _raise.addActionListener(this);
            tcont.add(_lower = createButton(_lowerIcon));
            _lower.addActionListener(this);
            tcont.add(_delete = createButton(_deleteIcon));
            _delete.addActionListener(this);

            // initialize
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 0, 0, 0),
                BorderFactory.createTitledBorder("")));
            setBackground(null);
            setTrigger(expand, _expandIcon, _collapseIcon);
            expand.setHorizontalAlignment(JButton.CENTER);
            add(new Spacer(1, -18));
            setTriggerContainer(tcont, opanel);
            setGap(5);
            setCollapsed(false);
        }

        /**
         * Returns a reference to the object (content) panel.
         */
        public ObjectPanel getObjectPanel ()
        {
            return (ObjectPanel)_content;
        }

        /**
         * Updates the state of the buttons.
         */
        public void updateButtons ()
        {
            int idx = getIndex();
            int count = _panels.getComponentCount();
            _raise.setEnabled(idx > 0);
            _lower.setEnabled(idx < count - 1);
            _delete.setEnabled(count > _min);
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Object source = event.getSource();
            if (source == _raise) {
                int idx = getIndex();
                swapValues(idx, idx - 1);
            } else if (source == _lower) {
                int idx = getIndex();
                swapValues(idx, idx + 1);
            } else if (source == _delete) {
                removeValue(getIndex());
            } else {
                super.actionPerformed(event);
            }
        }

        /**
         * Returns this entry's array index.
         */
        protected int getIndex ()
        {
            return ListUtil.indexOfRef(_panels.getComponents(), this);
        }

        /**
         * Loads the named icon.
         */
        protected Icon loadIcon (String name)
        {
            BufferedImage image;
            try {
                image = _ctx.getResourceManager().getImageResource(
                    "media/editor/" + name + ".png");
            } catch (IOException e) {
                log.warning("Error loading image.", "name", name, e);
                image = ImageUtil.createErrorImage(12, 12);
            }
            return new ImageIcon(image);
        }

        /**
         * Creates a button with the supplied text.
         */
        protected JButton createButton (Icon icon)
        {
            JButton button = new JButton(icon);
            button.setPreferredSize(PANEL_BUTTON_SIZE);
            return button;
        }

        /** The action buttons. */
        protected JButton _raise, _lower, _delete;
    }

    /** The container holding the panels. */
    protected JPanel _panels;

    /** Entry panel icons. */
    protected static Icon _expandIcon, _collapseIcon, _raiseIcon, _lowerIcon, _deleteIcon;

    /** The size of the panel buttons. */
    protected static final Dimension PANEL_BUTTON_SIZE = new Dimension(16, 16);
}
