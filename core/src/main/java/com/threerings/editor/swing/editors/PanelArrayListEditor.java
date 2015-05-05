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

package com.threerings.editor.swing.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.google.common.collect.ImmutableMap;

import com.samskivert.swing.CollapsiblePanel;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObjectUtil;

import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Parameter;
import com.threerings.editor.swing.BaseEditorPanel;
import com.threerings.editor.swing.BasePropertyEditor;
import com.threerings.editor.swing.ObjectPanel;

import static com.threerings.editor.Log.log;

/**
 * An editor for arrays or lists of objects.  Uses embedded panels.
 */
public abstract class PanelArrayListEditor extends ArrayListEditor
{
    @Override
    public void update ()
    {
        int pcount = _panels.getComponentCount();
        int length = getLength();
        for (int ii = 0; ii < length; ii++) {
            Object value = getValue(ii);
            if (ii < pcount) {
                updatePanel((EntryPanel)_panels.getComponent(ii), value);
            } else {
                addPanel(value);
            }
        }
        while (pcount > length) {
            _panels.remove(--pcount);
        }
        updatePanels();
        if (_add != null) {
            _add.setEnabled(getLength() < _max);
        }
    }

    @Override
    public void makeVisible (int idx)
    {
        EntryPanel panel = (EntryPanel)_panels.getComponent(idx);
        panel.setCollapsed(false);
        _panels.scrollRectToVisible(panel.getBounds());
    }

    @Override
    public String getComponentPath (Component comp, boolean mouse)
    {
        EntryPanel entry = getNextChildComponent(EntryPanel.class, comp);
        return entry == null
                ? ""
                : ("[" + entry.getIndex() + "]" + entry.getComponentPath(comp, mouse));
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        _content.add(_panels = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _panels.setBackground(null);

        if (!_property.getAnnotation().constant()) {
            JPanel bpanel = new JPanel();
            bpanel.setBackground(null);
            _content.add(bpanel);
            bpanel.add(_add = new JButton(getActionLabel("new")));
            _add.addActionListener(this);
        }
    }

    @Override
    protected void addValue (Object value)
    {
        super.addValue(value);
        addPanel(value);
        updatePanels();
    }

    @Override
    protected void removeValue (int idx)
    {
        super.removeValue(idx);
        _panels.remove(idx);
        updatePanels();
        updatePaths(idx, -1);
    }

    /**
     * Inserts a value in the list at the index, shifting remaining values.
     */
    protected void insertValue (int idx, int newIdx)
    {
        int dir = Integer.signum(newIdx - idx);
        Object tmp = getValue(idx);
        for (int ii = idx; ii != newIdx; ii += dir) {
            Object val = getValue(ii + dir);
            setValue(ii, val);
            updatePanel((EntryPanel)_panels.getComponent(ii), val);
        }
        setValue(newIdx, tmp);
        updatePanel((EntryPanel)_panels.getComponent(newIdx), tmp);
        fireStateChanged(true);
        updatePanels();
        updatePaths(idx, newIdx);
    }

    /**
     * Updates direct paths that reference this location in the array.
     *
     * @param idx The index being modified
     * @param newIdx The index being moved to, or -1 if idx is being removed.
     */
    protected void updatePaths (int idx, int newIdx)
    {
        String path = getPropertyPath();
        ParameterizedConfig pc = ObjectUtil.as(getRootObject(), ParameterizedConfig.class);
        if (pc == null) {
            return;
        }
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
        if (newIdx == -1) {
            builder.put(pathIndex(path, idx), "");
            newIdx = _panels.getComponentCount();
        } else {
            builder.put(pathIndex(path, idx), pathIndex(path, newIdx));
        }
        int dir = Integer.signum(idx - newIdx);
        for (int ii = newIdx; ii != idx; ii += dir) {
            // shift paths
            builder.put(pathIndex(path, ii), pathIndex(path, ii + dir));
        }
        Map<String, String> replace = builder.build();

        boolean updated = false;
        for (Parameter param : pc.parameters) {
            if (param instanceof Parameter.Direct) {
                updated |= updateDirect((Parameter.Direct)param, replace);
            } else if (param instanceof Parameter.Choice) {
                for (Parameter.Direct direct : ((Parameter.Choice)param).directs) {
                    updated |= updateDirect(direct, replace);
                }
            }
        }
        if (updated) {
            pc.wasUpdated();
        }
    }

    protected String pathIndex (String path, int idx)
    {
        return path + "[" + idx + "]";
    }

    /**
     * Updates all paths in a direct parameter with the replacements specified in replace.
     */
    protected boolean updateDirect (Parameter.Direct direct, Map<String, String> replace)
    {
        boolean updated = false;
        for (int ii = 0, nn = direct.paths.length; ii < nn; ii++) {
            String path = direct.paths[ii];
            for (Map.Entry<String, String> entry : replace.entrySet()) {
                String match = entry.getKey();
                if (path.startsWith(match)) {
                    String repl = entry.getValue();
                    // Store the new path. No need to update 'path' because we break
                    direct.paths[ii] = repl.isEmpty()
                        ? ""
                        : repl + path.substring(match.length());
                    log.info("Updating direct path",
                            "old", path, "new", direct.paths[ii]);
                    updated = true;
                    break;
                }
            }
        }
        return updated;
    }

    /**
     * Get the property path for the array.
     */
    protected String getPropertyPath ()
    {
        BaseEditorPanel editor = findBaseEditor();
        if (editor == null) {
            return "";
        }
        String path = editor.getComponentPath(this, false);
        if (path.startsWith(".")) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * Adds an object panel for the specified entry.
     */
    protected abstract void addPanel (Object value);

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
     * Update the entry panel.
     */
    protected abstract void updatePanel (EntryPanel panel, Object value);

    /**
     * A panel for a single entry.
     */
    protected abstract class EntryPanel extends CollapsiblePanel
        implements ActionListener
    {
        public EntryPanel (Object value)
        {
            // create the panel
            JPanel panel = createPanel(value);

            // make sure we have the icons loaded
            if (_expandIcon == null) {
                _expandIcon = loadIcon("expand", _ctx);
                _collapseIcon = loadIcon("collapse", _ctx);
                _highlightIcon = loadIcon("highlight", _ctx);
            }
            if (_raiseIcon == null) {
                _raiseIcon = loadIcon("raise", _ctx);
                _lowerIcon = loadIcon("lower", _ctx);
                _deleteIcon = loadIcon("delete", _ctx);
                _insertIcon = loadIcon("insert", _ctx);
            }

            // create the button panel and buttons
            JPanel tcont = GroupLayout.makeHBox(
                GroupLayout.NONE, GroupLayout.RIGHT, GroupLayout.NONE);
            tcont.setOpaque(false);
            JButton expand = createButton(_expandIcon);
            tcont.add(expand);
            tcont.add(_highlight = createButton(_highlightIcon));
            _highlight.addActionListener(this);
            if (!_property.getAnnotation().constant()) {
                tcont.add(_insert = createButton(_insertIcon));
                _insert.addActionListener(this);
                tcont.add(_raise = createButton(_raiseIcon));
                _raise.addActionListener(this);
                tcont.add(_lower = createButton(_lowerIcon));
                _lower.addActionListener(this);
                tcont.add(_delete = createButton(_deleteIcon));
                _delete.addActionListener(this);
            }

            // initialize
            _title = BorderFactory.createTitledBorder("");
            updateBorder();
            setBackground(null);
            setTrigger(expand, _expandIcon, _collapseIcon);
            expand.setHorizontalAlignment(JButton.CENTER);
            add(new Spacer(1, -25));
            setTriggerContainer(tcont, panel);
            setGap(5);
            setCollapsed(false);

            // add a border toggling mouse adapter
            addMouseListener(new MouseAdapter() {
                public void mouseClicked (MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        _highlighted = !_highlighted;
                        updateBorder();
                    }
                }
            });
        }

        /**
         * Get the mouse path.
         */
        public abstract String getComponentPath (Component comp, boolean mouse);

        /**
         * Updates the state of the buttons.
         */
        public void updateButtons ()
        {
            int idx = getIndex();
            int count = _panels.getComponentCount();
            if (_raise != null) {
                _raise.setEnabled(idx > 0);
                _lower.setEnabled(idx < count - 1);
                _delete.setEnabled(!_fixed && count > _min);
                _insert.setEnabled(!_fixed && getLength() < _max);
            }
            updateBorder();
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Object source = event.getSource();
            if (source == _raise) {
                int idx = getIndex();
                insertValue(idx, idx - 1);
            } else if (source == _lower) {
                int idx = getIndex();
                insertValue(idx, idx + 1);
            } else if (source == _delete) {
                removeValue(getIndex());
            } else if (source == _highlight) {
                _highlighted = !_highlighted;
                updateBorder();
            } else if (source == _insert) {
                int idx = getIndex();
                addValue();
                insertValue(getLength() - 1, idx);
            } else { // source == _trigger
                super.actionPerformed(event);
            }
        }

        @Override
        public void scrollRectToVisible (Rectangle rect)
        {
            // block this to avoid excess scrolling
        }

        /**
         * Returns this entry's array index.
         */
        public int getIndex ()
        {
            return ListUtil.indexOfRef(_panels.getComponents(), this);
        }

        protected void updateBorder ()
        {
            String title = PanelArrayListEditor.this.getPropertyLabel() + " (" + getIndex() + ")";
            _title = _highlighted ?  BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.black, 2), title) :
                BorderFactory.createTitledBorder(title);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 0, 0, 0), _title));
        }

        /**
         * Create the content panel.
         */
        protected abstract JPanel createPanel (Object value);

        /** The action buttons. */
        protected JButton _raise, _lower, _delete, _highlight, _insert;

        /** The highlighted state. */
        protected boolean _highlighted;

        /** The titled border. */
        protected TitledBorder _title;
    }

    /** The container holding the panels. */
    protected JPanel _panels;

    /** Entry panel icons. */
    protected static Icon _raiseIcon, _lowerIcon, _deleteIcon, _insertIcon;
}
