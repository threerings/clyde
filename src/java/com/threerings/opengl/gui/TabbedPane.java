//
// $Id$

package com.threerings.opengl.gui;

import java.util.ArrayList;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.layout.GroupLayout;

import static com.threerings.opengl.gui.Log.log;

/**
 * Displays one of a set of components (tabs) depending on which tab is selected.
 */
public class TabbedPane extends Container
{
    /**
     * Creates a tabbed pane with left justified buttons.
     */
    public TabbedPane ()
    {
        this(GroupLayout.LEFT);
    }

    /**
     * Creates a tabbed pane.
     *
     * @param tabAlign the justification for the tab buttons.
     */
    public TabbedPane (GroupLayout.Justification tabAlign)
    {
    	this(tabAlign, GroupLayout.DEFAULT_GAP);
    }

    /**
     * Creates a tabbed pane.
     *
     * @param tabAlign the justification for the tab buttons.
     * @param gap the number of pixels space between each tab button.
     */
    public TabbedPane (GroupLayout.Justification tabAlign, int gap)
    {
        super(new BorderLayout());

        GroupLayout gl = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.CONSTRAIN);
        _top = new Container(gl);
        gl = GroupLayout.makeHoriz(GroupLayout.CONSTRAIN, tabAlign, GroupLayout.CONSTRAIN);
        _top.add(_buttons = new Container(gl));
        gl.setGap(gap);
        add(_top, BorderLayout.NORTH);

        _close = new Button("", _closer, "close");
        _close.setStyleClass("tabbedpane_close");
    }

    /**
     * Adds a tab to the pane using the specified title with no close button.
     */
    public void addTab (String title, Component tab)
    {
        addTab(title, tab, false);
    }

    /**
     * Adds a tab to the pane using the specified tile.
     */
    public void addTab (String title, Component tab, boolean hasClose)
    {
        ToggleButton tbutton = new ToggleButton(title, String.valueOf(_tabs.size())) {
            protected void fireAction (long when, int modifiers) {
                if (!_selected) {
                    super.fireAction(when, modifiers);
                }
            }
        };
        tbutton.setStyleClass("tab");
        tbutton.addListener(_selector);
        tbutton.setFit(Label.Fit.TRUNCATE);
        _buttons.add(tbutton);

        _tabs.add(new Tab(title, hasClose, tab));

        // if we have no selected tab, select this one
        if (_selidx == -1) {
            selectTab(0);
        }
    }

    /**
     * Removes the specified tab.
     */
    public void removeTab (Component tab)
    {
        int idx = indexOfTab(tab);
        if (idx != -1) {
            removeTab(idx);
        } else {
            log.warning("Requested to remove non-added tab [pane=" + this + ", tab=" + tab + "].");
        }
    }

    /**
     * Removes the tab at the specified index.
     */
    public void removeTab (int tabidx)
    {
        removeTab(tabidx, false);
    }

    /**
     * Removes the tab at the specified index.
     *
     * @param btnClose set to true if the tab was removed by the close button
     */
    public void removeTab (int tabidx, boolean btnClose)
    {
        _buttons.remove(_buttons.getComponent(tabidx));
        Tab tab = _tabs.remove(tabidx);

        // if we're removing the selected tab...
        if (_selidx == tabidx) {
            // remove the tab component
            remove(tab.component);
            _selidx = -1;

            // remove the close button, we'll add it later if needed
            if (_close.getParent() != null) {
                _top.remove(_close);
            }

            // now display a new tab component
            if (tabidx < _tabs.size()) {
                selectTab(tabidx);
            } else {
                selectTab(tabidx - 1); // no-op if -1
            }

        } else if (_selidx > tabidx) {
            _selidx--;
        }

        // and let interested parties know what happened
        tabWasRemoved(tab.component, btnClose);
    }

    /**
     * Removes all tabs.
     */
    public void removeAllTabs ()
    {
        if (_selidx != -1) {
            remove(_tabs.get(_selidx).component);
        }
        _selidx = -1;
        _buttons.removeAll();
        _tabs.clear();
    }

    /**
     * Returns the number of tabs in this pane.
     */
    public int getTabCount ()
    {
        return _tabs.size();
    }

    /**
     * Selects the specified tab.
     */
    public void selectTab (Component tab)
    {
        selectTab(indexOfTab(tab));
    }

    /**
     * Selects the tab with the specified index.
     */
    public void selectTab (int tabidx)
    {
        // no NOOPing
        if (tabidx == _selidx) {
            return;
        }
        tabidx = Math.max(0, Math.min(_tabs.size() - 1, tabidx));

        // make sure the appropriate button is selected
        for (int ii = 0; ii < _tabs.size(); ii++) {
            getTabButton(ii).setSelected(ii == tabidx);
        }

        // remove the current tab
        if (_selidx != -1) {
            remove(_tabs.get(_selidx).component);
        }

        // and add the requested one
        Tab tab = _tabs.get(tabidx);
        add(tab.component, BorderLayout.CENTER);
        updateClose(tab.close);
        _selidx = tabidx;
    }

    /**
     * Returns the selected tab component.
     */
    public Component getSelectedTab ()
    {
        return (_selidx == -1) ? null : _tabs.get(_selidx).component;
    }

    /**
     * Returns the index of the selected tab.
     */
    public int getSelectedTabIndex ()
    {
        return _selidx;
    }

    /**
     * Returns a reference to the tab button for the given tab.
     */
    public ToggleButton getTabButton (Component tab)
    {
        int idx = indexOfTab(tab);
        return (idx == -1) ? null : getTabButton(idx);
    }

    /**
     * Returns a reference to the tab button at the given index.
     */
    public ToggleButton getTabButton (int idx)
    {
        return (ToggleButton)_buttons.getComponent(idx);
    }

    /**
     * Returns the index of the given tab.
     */
    public int indexOfTab (Component tab)
    {
        for (int ii = 0; ii < _tabs.size(); ii++) {
            if (_tabs.get(ii).component == tab) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Updates the visibility of the close tab button.
     */
    protected void updateClose (boolean showClose)
    {
        if (showClose && _close.getParent() == null) {
            _top.add(_close, GroupLayout.FIXED);
        } else if (!showClose && _close.getParent() != null) {
            _top.remove(_close);
        }
    }

    /**
     * Called when a tab was removed.
     *
     * @param btnClose set to true if the tab was removed by the close button
     */
    protected void tabWasRemoved (Component tab, boolean btnClose)
    {
        // update the button actions
        for (int ii = 0, ll = _buttons.getComponentCount(); ii < ll; ii++) {
            getTabButton(ii).setAction("" + ii);
        }
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "tabbedpane";
    }

    protected ActionListener _selector = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            try {
                selectTab(Integer.parseInt(event.getAction()));
            } catch (Exception e) {
                log.warning("Got weird action event " + event + ".");
            }
        }
    };

    protected ActionListener _closer = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (_selidx >= 0) {
                removeTab(_selidx, true);
            }
        }
    };

    protected static class Tab
    {
        public String title;
        public boolean close;
        public Component component;

        public Tab (String title, boolean close, Component component) {
            this.title = title;
            this.close = close;
            this.component = component;
        }
    }

    protected ArrayList<Tab> _tabs = new ArrayList<Tab>();
    protected int _selidx = -1;

    protected Container _top, _buttons;
    protected Button _close;
}
