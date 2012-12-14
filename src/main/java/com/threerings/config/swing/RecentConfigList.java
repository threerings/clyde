//
// $Id$

package com.threerings.config.swing;

import java.awt.BorderLayout;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.samskivert.util.ObserverList;

import com.threerings.config.ConfigReference;

/**
 * Displays and saves recent configs.
 */
public class RecentConfigList extends JPanel
{
    /**
     * Observer class for RecentConfigList.
     */
    public static abstract class Observer
    {
        /**
         * Called when a config is selected from the list.
         */
        public void configSelected (ConfigReference<?> ref) {}
    }

    /**
     * Create a RecentConfigList.
     */
    public RecentConfigList ()
    {
        super(new BorderLayout());

        // create the list and add it to the hierarchy
        _list = new JList(_listModel);
        _list.setVisibleRowCount(5);

        JScrollPane pane = new JScrollPane(
            _list,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(pane, BorderLayout.CENTER);

        _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent event) {
                Object selected = _list.getSelectedValue();
                if (selected != null) {
                    _list.setSelectedIndex(-1);
                    configSelected((ConfigReference<?>)selected);
                }
            }
        });
    }

    /**
     * Add an Observer of this RecentConfigList.
     */
    public void addObserver (Observer obs)
    {
        _observers.add(obs);
    }

    /**
     * Remove an Observer of this RecentConfigList.
     */
    public void removeObserver (Observer obs)
    {
        _observers.remove(obs);
    }

    /**
     * Add a config reference that's been used recently.
     */
    public void addRecent (ConfigReference<?> ref)
    {
        int curIdx = -1;
        // first see where it is in the current model
        for (int ii = 0, nn = _listModel.getSize(); ii < nn; ii++) {
            if (ref.equals(_listModel.getElementAt(ii))) {
                curIdx = ii;
                break;
            }
        }

        if (curIdx == 0) {
            // nothing to do
            return;
        }

        // maybe remove it
        if (curIdx > 0) {
            _listModel.removeElementAt(curIdx);
        }

        // add it in
        _listModel.insertElementAt(ref, 0);
    }

    /**
     * Dispatch a selection to our observers.
     */
    protected void configSelected (final ConfigReference<?> ref)
    {
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer obs) {
                obs.configSelected(ref);
                return true;
            }
        });
    }

    /** The actual list widget. */
    protected JList _list;

    /** Our list model. */
    protected DefaultListModel _listModel = new DefaultListModel();

    /** The observers of this recent list. */
    protected ObserverList<Observer> _observers = ObserverList.newFastUnsafe();
}
