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

package com.threerings.opengl.gui;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.layout.GroupLayout;

import static com.threerings.opengl.gui.Log.log;

/**
 * Displays one of a set of components (tabs) depending on which tab is selected.
 */
public class TabbedPane extends Container
    implements Selectable<Component>
{
    /**
     * Creates a tabbed pane with left justified buttons.
     */
    public TabbedPane (GlContext ctx)
    {
        this(ctx, GroupLayout.LEFT);
    }

    /**
     * Creates a tabbed pane.
     *
     * @param tabAlign the justification for the tab buttons.
     */
    public TabbedPane (GlContext ctx, GroupLayout.Justification tabAlign)
    {
        this(ctx, tabAlign, GroupLayout.DEFAULT_GAP);
    }

    /**
     * Creates a tabbed pane.
     *
     * @param tabAlign the justification for the tab buttons.
     * @param gap the number of pixels space between each tab button.
     */
    public TabbedPane (GlContext ctx, GroupLayout.Justification tabAlign, int gap)
    {
        super(ctx, new BorderLayout());

        GroupLayout gl = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.CONSTRAIN);
        _top = new Container(_ctx, gl);
        gl = GroupLayout.makeHoriz(GroupLayout.CONSTRAIN, tabAlign, GroupLayout.CONSTRAIN);
        _top.add(_buttons = new Container(_ctx, gl) {
            protected void renderComponent (Renderer renderer) {
                // render the selected button last
                for (int ii = 0, nn = getComponentCount(); ii < nn; ii++) {
                    if (ii != _selidx) {
                        getComponent(ii).render(renderer);
                    }
                }
                if (_selidx != -1) {
                    getComponent(_selidx).render(renderer);
                }
            }
        });
        gl.setGap(gap);
        add(_top, BorderLayout.NORTH);

        _close = new Button(_ctx, "", _closer, "close");
        _close.setStyleConfig("Default/TabbedPaneClose");
    }

    /**
     * Sets the justification for the tab buttons.
     */
    public void setTabAlignment (GroupLayout.Justification tabAlign)
    {
        ((GroupLayout)_buttons.getLayoutManager()).setJustification(tabAlign);
    }

    /**
     * Sets the gap between the tab buttons.
     */
    public void setGap (int gap)
    {
        ((GroupLayout)_buttons.getLayoutManager()).setGap(gap);
    }

    /**
     * Adds a tab to the pane using the specified title with no close button.
     */
    public void addTab (String title, Component tab)
    {
        addTab(title, tab, false);
    }

    /**
     * Adds a tab to the pane using the specified title.
     */
    public void addTab (String title, Component tab, boolean hasClose)
    {
        addTab(title, tab, hasClose, null);
    }

    /**
     * Adds a tab to the pane using the specified title.
     */
    public void addTab (
        String title, Component tab, boolean hasClose, ConfigReference<StyleConfig> style)
    {
        ToggleButton tbutton = new ToggleButton(_ctx, title, String.valueOf(_tabs.size())) {
            protected void fireAction (long when, int modifiers) {
                if (!_selected) {
                    super.fireAction(when, modifiers);
                }
            }
        };
        if (style == null) {
            tbutton.setStyleConfig("Default/Tab");
        } else {
            tbutton.setStyleConfig(style);
        }
        tbutton.addListener(_selector);
        tbutton.setFit(Label.Fit.TRUNCATE);
        _buttons.add(tbutton);

        _tabs.add(new Tab(title, hasClose, tab));

        // if we have no selected tab, select this one
        if (_selidx == -1) {
            setSelectedIndex(0);
        }
    }

    /**
     * Replaces the specified tab component.
     */
    public void replaceTab (Component otab, Component ntab)
    {
        int idx = indexOfTab(otab);
        if (idx != -1) {
            replaceTab(idx, ntab);
        } else {
            log.warning("Requested to replace non-added tab.", "pane", this, "tab", otab);
        }
    }

    /**
     * Replaces the tab component at the specified index.
     */
    public void replaceTab (int tabidx, Component ntab)
    {
        Tab tab = _tabs.get(tabidx);

        // if we're replacing the selected tab...
        if (_selidx == tabidx) {
            remove(tab.component);
            add(ntab, BorderLayout.CENTER);
        }
        tab.component = ntab;
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
            log.warning("Requested to remove non-added tab", "pane", this, "tab", tab);
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
                setSelectedIndex(tabidx);
            } else {
                setSelectedIndex(tabidx - 1); // no-op if -1
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

    // from Selectable<Component>
    public Component getSelected ()
    {
        return (_selidx == -1) ? null : _tabs.get(_selidx).component;
    }

    // from Selectable<Component>
    public void setSelected (Component tab)
    {
        setSelectedIndex(indexOfTab(tab));
    }

    // from Selectable<Component>
    public int getSelectedIndex ()
    {
        return _selidx;
    }

    // from Selectable<Component>
    public void setSelectedIndex (int index)
    {
        selectTab(index, -1L, 0);
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
     * Returns the tab at the specified index.
     */
    public Component getTab (int idx)
    {
        return _tabs.get(idx).component;
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
     * Selects the tab with the specified index.
     */
    protected void selectTab (int tabidx, long when, int modifiers)
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

        // emit an action event
        emitEvent(new ActionEvent(this, when, modifiers, SELECT, tab.component));
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

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/TabbedPane";
    }

    protected ActionListener _selector = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            try {
                selectTab(Integer.parseInt(event.getAction()),
                    event.getWhen(), event.getModifiers());
            } catch (Exception e) {
                log.warning("Exception thrown handling selection event.", "event", event, e);
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
