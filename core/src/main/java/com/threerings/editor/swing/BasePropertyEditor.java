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

package com.threerings.editor.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import java.io.IOException;

import javax.annotation.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.CollapsiblePanel;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ImageUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.log;

/**
 * Abstract base class for {@link PropertyEditor} and {@link EditorPanel}.
 */
public abstract class BasePropertyEditor extends CollapsiblePanel
    implements ActionListener
{
    public BasePropertyEditor ()
    {
        super(new FlowLayout());
        // make sure we inherit the parent's background color
        setBackground(null);
    }

    /**
     * Adds a listener for change events.
     */
    public void addChangeListener (ChangeListener listener)
    {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Removes a change event listener.
     */
    public void removeChangeListener (ChangeListener listener)
    {
        listenerList.remove(ChangeListener.class, listener);
    }

    /**
     * Returns a label for the supplied type.
     */
    public String getLabel (Class<?> type)
    {
        if (type == null) {
            return _msgs.get("m.null_value");
        }
        String name = type.getName();
        name = name.substring(
            Math.max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1);
        name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(name));
        return getLabel(name, Introspector.getMessageBundle(type));
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names)
    {
        return getLabels(names, _msgs);
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names, String bundle)
    {
        return getLabels(names, _msgmgr.getBundle(bundle));
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names, MessageBundle msgs)
    {
        String[] labels = new String[names.length];
        for (int ii = 0; ii < names.length; ii++) {
            labels[ii] = getLabel(names[ii], msgs);
        }
        return labels;
    }

    /**
     * Returns the path of the property under the mouse cursor relative to this property.
     */
    public String getMousePath ()
    {
        Point pt = getMousePosition();
        Component c = pt == null ? null : findComponentAt(pt);
        return c == null ? "" : getComponentPath(c, true);
    }

    /**
     * Returns the path of the property for the child component.
     *
     * @param comp the component we're getting the path to.
     */
    public String getComponentPath (Component comp, boolean mouse)
    {
        return "";
    }

    /**
     * Set the information regarding how the property being edited here is parameterized out.
     * 
     * @param label the parameter name, or null to clear it.
     * @param parameterInfo additional information about the path of the parameter, typically "".
     */
    public void setParameterLabel (@Nullable String label, String parameterInfo)
    {
        _parameterLabel = label;
        if (!"".equals(parameterInfo)) {
            log.warning("Lost parameter information: " + parameterInfo, "clazz", this.getClass());
        }
        updateBorder();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object source = event.getSource();
        if (source == _highlight) {
            toggleHighlight();
            invalidate();

        } else if (source == _tree) {
            Icon icon = _tree.getIcon();
            boolean enabled = (icon == _treeIcon);
            setTreeModeEnabled(enabled);
            _tree.setIcon(enabled ? _panelIcon : _treeIcon);

        } else {
            super.actionPerformed(event);
        }
    }

    /**
     * Enables or disables tree mode.
     */
    protected void setTreeModeEnabled (boolean enabled)
    {
        // nothing by default
    }

    /**
     * Returns the name of the specified property, translating it if a translation exists.
     */
    public String getLabel (Property property)
    {
        String name = property.getName();
        return property.shouldTranslateName() ?
            getLabel(name, property.getMessageBundle()) : name;
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name)
    {
        return getLabel(name, _msgs);
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name, String bundle)
    {
        return getLabel(name, _msgmgr.getBundle(bundle));
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name, MessageBundle msgs)
    {
        name = name.isEmpty() ? "default" : name;
        String key = "m." + name;
        return msgs.exists(key) ? msgs.get(key) : name;
    }

    /**
     * A special version of getLabel for enums.
     */
    protected String getLabel (Enum<?> value, MessageBundle msgs)
    {
        if (value == null) {
            return _msgs.get("m.null_value");
        }
        String key = "m." + StringUtil.toUSLowerCase(value.name());
        return msgs.exists(key) ? msgs.get(key) : value.toString();
    }

    /**
     * Fires a state changed event.
     */
    protected void fireStateChanged ()
    {
        Object[] listeners = listenerList.getListenerList();
        ChangeEvent event = null;
        for (int ii = listeners.length - 2; ii >= 0; ii -= 2) {
            if (listeners[ii] == ChangeListener.class) {
                if (event == null) {
                    event = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[ii + 1]).stateChanged(event);
            }
        }
    }

    /**
     * Adds the collapsible button to the panel.
     *
     * @param tree if true, include the tree mode button.
     */
    protected void makeCollapsible (EditorContext ctx, String title, boolean tree)
    {
        VGroupLayout gl = new VGroupLayout(VGroupLayout.NONE);
        gl.setOffAxisPolicy(VGroupLayout.STRETCH);
        gl.setGap(0);
        gl.setJustification(VGroupLayout.TOP);
        gl.setOffAxisJustification(VGroupLayout.LEFT);
        setLayout(gl);

        // make sure we have the icons loaded
        if (_expandIcon == null) {
            _expandIcon = loadIcon("expand", ctx);
            _collapseIcon = loadIcon("collapse", ctx);
            _highlightIcon = loadIcon("highlight", ctx);
            _treeIcon = loadIcon("tree", ctx);
            _panelIcon = loadIcon("panels", ctx);
        }

        JPanel tcont = GroupLayout.makeHBox(
            GroupLayout.NONE, GroupLayout.RIGHT, GroupLayout.NONE);
        tcont.setOpaque(false);
        if (tree) {
            tcont.add(_tree = createButton(_treeIcon));
            _tree.addActionListener(this);
        }
        JButton expand = createButton(_expandIcon);
        tcont.add(expand);
        tcont.add(_highlight = createButton(_highlightIcon));
        _highlight.addActionListener(this);

        setTrigger(expand, _expandIcon, _collapseIcon);
        expand.setHorizontalAlignment(JButton.CENTER);
        add(new Spacer(1, -24));
        setTriggerContainer(tcont, new JPanel(), false);
        setGap(5);
        setCollapsed(false);
        getContent().setBackground(null);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked (MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    toggleHighlight();
                }
            }
        });

        _content.setLayout(
                new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setTitle(title);
    }

    /**
     * Loads the named icon.
     */
    protected Icon loadIcon (String name, EditorContext ctx)
    {
        BufferedImage image;
        try {
            image = ctx.getResourceManager().getImageResource(
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

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected int getColor (String name)
    {
        return getColor(name, _msgs, DEFAULT_BACKGROUND);
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected int getColor (String name, String bundle, int defaultColor)
    {
        return getColor(name, _msgmgr.getBundle(bundle), defaultColor);
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected int getColor (String name, MessageBundle msgs, int defaultColor)
    {
        if (name.isEmpty()) {
            return defaultColor;
        }
        name = "c." + name;
        return msgs.exists(name) ? Integer.parseInt(msgs.get(name), 16) : defaultColor;
    }

    protected int getPropertyColor (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            int color = getColor(lineage[ii].getColorName(), lineage[ii].getMessageBundle(), -1);
            if (color != -1) {
                return color;
            }
        }
        return DEFAULT_BACKGROUND;
    }

    /**
     * Returns a background color darkened by the specified number of shades.
     */
    protected Color getDarkerBackground (float shades)
    {
        int value = BASE_BACKGROUND - (int)(shades*SHADE_DECREMENT);
        return new Color(value, value, value);
    }

    /**
     * Returns a background color darkened and colored for this level.
     */
    protected Color getBackgroundColor (Property[] lineage)
    {
        int color = getPropertyColor(lineage);
        if (lineage == null) {
            return new Color(color);
        }
        return darkenColor(color, lineage.length / 2);
    }

    protected Color darkenColor (int color, float shades)
    {
        int darken = (int)(shades * SHADE_DECREMENT);
        int r = Math.max(0, ((color & 0xFF0000) >> 16) - darken);
        int g = Math.max(0, ((color & 0xFF00) >> 8) - darken);
        int b = Math.max(0, ((color & 0xFF)) - darken);
        return new Color(r, g, b);
    }

    /**
     * Copies the path of the property under the mouse cursor to the clipboard.
     */
    protected void copyPropertyPath (String path)
    {
        if (path.startsWith(".")) {
            path = path.substring(1);
        }
        if (!path.isEmpty()) {
            StringSelection contents = new StringSelection(path);
            getToolkit().getSystemClipboard().setContents(contents, contents);
        }
    }

    /**
     * Toggle the highlighting of this editor.
     */
    protected void toggleHighlight ()
    {
        _highlighted = !_highlighted;
        updateBorder();
    }

    /**
     * Set the title to use.
     */
    protected void setTitle (String title)
    {
        _title = title;
        updateBorder();
    }

    /**
     * Updates the border.
     */
    protected void updateBorder ()
    {
        setBorder(createBorder());
    }

    /**
     * Create the border to use under the current conditions, or null.
     */
    protected Border createBorder ()
    {
        Border border = _highlighted
            ? BorderFactory.createLineBorder(Color.BLACK, 2)
            : null;

        if (_title != null) {
            // ok to pass null as the border to a titledborder: it will use l&f default
            border = BorderFactory.createTitledBorder(border, _title);
        }
        if (_invalid) {
            // put a red border inside it
            border = BorderFactory.createCompoundBorder(
                    border, BorderFactory.createLineBorder(Color.RED, 1));
        }
        // and add any parameter information
        if (_parameterLabel != null) {
            TitledBorder tb = new TitledBorder(_parameterLabel);
            tb.setTitleJustification(TitledBorder.TRAILING);
            tb.setTitleColor(Color.BLUE);
            if (border != null) {
                // replace the standard "etched" border in the title with the existing border
                tb.setTitlePosition(TitledBorder.ABOVE_TOP);
                tb.setBorder(border);
            }
            border = tb;
        }

        return border; // ok to return null
    }

    /**
     * Follows a component up the parent path to find the nearest parent of class clazz.
     */
    protected <T extends Component> T getNextChildComponent (Class<T> clazz, Component comp)
    {
        T nearest = null;
        for (Component c = comp; c != this; c = c.getParent()) {
            if (c == null) {
                return null;
            }
            if (clazz.isInstance(c)) {
                nearest = clazz.cast(c);
            }
        }
        return nearest;
    }

    /** Provides access to common services. */
    protected EditorContext _ctx;

    /** The message manager to use for translations. */
    protected MessageManager _msgmgr;

    /** The default message bundle. */
    protected MessageBundle _msgs;

    /** The highlight button. */
    protected JButton _highlight;

    /** If the border should be highlighted. */
    protected boolean _highlighted;

    /** The tree mode button. */
    protected JButton _tree;

    /** Our title, typically used only if collapsible. */
    protected String _title;

    /** Is the value we're editing "invalid", if so, we'll draw a red border. */
    protected boolean _invalid;

    /** A label indicating our parameterization status, or null. */
    protected String _parameterLabel;

    /** Various icons. */
    protected static Icon _expandIcon, _collapseIcon, _highlightIcon, _treeIcon, _panelIcon;

    /** The base background value that we darken to indicate nesting. */
    protected static final int BASE_BACKGROUND = 0xEE;

    /** The default background value. */
    protected static final int DEFAULT_BACKGROUND = 0xEEEEEE;

    /** The number of units to darken for each shade. */
    protected static final int SHADE_DECREMENT = 8;

    /** The size of the panel buttons. */
    protected static final Dimension PANEL_BUTTON_SIZE = new Dimension(16, 16);
}
