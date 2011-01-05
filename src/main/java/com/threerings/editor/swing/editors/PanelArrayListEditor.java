//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

package com.threerings.editor.swing.editors;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.CollapsiblePanel;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
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
                ObjectPanel opanel = ((EntryPanel)_panels.getComponent(ii)).getObjectPanel();
                opanel.setOuter(_object);
                opanel.setValue(value);
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
    public void makeVisible (int idx)
    {
        EntryPanel panel = (EntryPanel)_panels.getComponent(idx);
        panel.setCollapsed(false);
        _panels.scrollRectToVisible(panel.getBounds());
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
    protected String getMousePath (Point pt)
    {
        Component comp = _panels.getComponentAt(
            SwingUtilities.convertPoint(this, pt, _panels));
        int idx = _panels.getComponentZOrder(comp);
        return (idx == -1) ? "" : ("[" + idx + "]" +
            ((EntryPanel)comp).getObjectPanel().getMousePath());
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
                _expandIcon = loadIcon("expand", _ctx);
                _collapseIcon = loadIcon("collapse", _ctx);
            }
            if (_raiseIcon == null) {
                _raiseIcon = loadIcon("raise", _ctx);
                _lowerIcon = loadIcon("lower", _ctx);
                _deleteIcon = loadIcon("delete", _ctx);
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
            _title = BorderFactory.createTitledBorder("");
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 0, 0, 0),
                _title));
            setBackground(null);
            setTrigger(expand, _expandIcon, _collapseIcon);
            expand.setHorizontalAlignment(JButton.CENTER);
            add(new Spacer(1, -25));
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
            _title.setTitle(PanelArrayListEditor.this.getPropertyLabel() + " (" + idx + ")");
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
            } else { // source == _trigger
                super.actionPerformed(event);
            }
        }

        @Override // documentation inherited
        public void scrollRectToVisible (Rectangle rect)
        {
            // block this to avoid excess scrolling
        }

        /**
         * Returns this entry's array index.
         */
        protected int getIndex ()
        {
            return ListUtil.indexOfRef(_panels.getComponents(), this);
        }

        /** The action buttons. */
        protected JButton _raise, _lower, _delete;

        /** The titled border. */
        protected TitledBorder _title;
    }

    /** The container holding the panels. */
    protected JPanel _panels;

    /** Entry panel icons. */
    protected static Icon _raiseIcon, _lowerIcon, _deleteIcon;
}
