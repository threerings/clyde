package com.threerings.editor.swing.editors.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.util.prefs.Preferences;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.base.Splitter;

import com.samskivert.util.ObserverList;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;

/**
 * Displays and saves recent configs.
 */
public abstract class AbstractRecentList extends JPanel
{
    /**
     * @param prefKey a unique String to identify the context in which this is being used,
     * or null to not persist.
     * @param prefs or null.
     */
    public AbstractRecentList (String prefKey, Preferences prefs)
    {
        super(new BorderLayout());
        _prefKey = prefKey;
        _prefs = prefs;
        readPrefs();

        // create the list and add it to the hierarchy
        _list = new JList(_listModel);
        _list.setVisibleRowCount(4);

        JScrollPane pane = new JScrollPane(
            _list,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(pane, BorderLayout.CENTER);

        _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent event) {
                if (_block) {
                    return;
                }
                Object selected = _list.getSelectedValue();
                if (selected != null) {
                    valueSelected((String)selected);
                }
            }
        });
        _list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent (
                JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                // first, transform the ConfigReference into a nice String
                String fullString = String.valueOf(value);
                super.getListCellRendererComponent(
                    list, fullString, index, isSelected, cellHasFocus);

                // then see if we should try to shrink the String to display ellipses
                // at the beginning rather than the end
                int maxWidth = list.getSize().width;
                for (int chop = 1, maxChop = fullString.lastIndexOf('/') + 1;
                        (chop <= maxChop) && (getPreferredSize().width > maxWidth);
                        chop++) {
                    setText("..." + fullString.substring(chop));
                }
                return this;
            }
        });
    }

    @Override
    public Dimension getMinimumSize ()
    {
        Dimension d = super.getMinimumSize();
        d.height = Math.max(d.height, _list.getPreferredScrollableViewportSize().height);
        return d;
    }

    /**
     * Called when a value is actually selected.
     */
    protected abstract void valueSelected (String value);

    /**
     * Add a value that's been used recently.
     */
    protected void addRecent (String value)
    {
        int size = _listModel.getSize();
        int curIdx = -1;
        // first see where it is in the current model
        for (int ii = 0; ii < size; ii++) {
            if (value.equals(_listModel.getElementAt(ii))) {
                curIdx = ii;
                break;
            }
        }

        if (curIdx == 0) {
            // nothing to do
            return;
        }

        _block = true;
        try {
            // maybe remove it, or the last one.
            if (curIdx > 0) {
                _listModel.removeElementAt(curIdx);

            } else if (size == MAX_SIZE) {
                _listModel.removeElementAt(size - 1);
            }

            // add it in
            _listModel.insertElementAt(value, 0);
            _list.setSelectedIndex(0);

        } finally {
            _block = false;
        }

        // and save out these values
        writePrefs();
    }

    /**
     * Read the values we've saved.
     */
    protected void readPrefs ()
    {
        if (_prefKey == null) {
            return;
        }
        _block = true;
        try {
            String encoded = _prefs.get(_prefKey, "");
            _listModel.removeAllElements();
            for (String piece : Splitter.on('|').omitEmptyStrings().split(encoded)) {
                _listModel.addElement(piece.replace("%BAR%", "|"));
            }
        } finally {
            _block = false;
        }
    }

    /**
     * Save the current state of the list to preferences.
     */
    protected void writePrefs ()
    {
        if (_prefKey == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int ii = 0, nn = _listModel.getSize(); ii < nn; ii++) {
            String value = (String)_listModel.getElementAt(ii);
            value = value.replace("|", "%BAR%");
            if (builder.length() + value.length() + 1 > Preferences.MAX_VALUE_LENGTH) {
                break;
            }
            builder.append(value).append('|'); // terminate, don't separate!
        }

        _prefs.put(_prefKey, builder.toString());
    }

    /** The actual list widget. */
    protected JList _list;

    /** Our list model. */
    protected DefaultListModel _listModel = new DefaultListModel();

    /** The preferences key. */
    protected String _prefKey;

    /** Our preferences. */
    protected Preferences _prefs;

    /** Do we block selection changes? */
    protected boolean _block;

    /** The absolute maximum number of recents we'll display. */
    protected static final int MAX_SIZE = 50;
}
