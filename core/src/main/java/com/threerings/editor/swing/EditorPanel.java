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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.ArrayUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.DynamicallyEditable;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.log;

/**
 * Allows editing properties of an object as determined through reflection.
 */
public class EditorPanel extends BaseEditorPanel
    implements ChangeListener
{
    /**
     * Determines how the different property categories are displayed (in sequential collapsible
     * panels, in separate tabs, or in a card layout with a combo box at the top).
     */
    public enum CategoryMode { PANELS, TABS, CHOOSER };

    /**
     * Creates and returns a simple dialog for editing the supplied object.
     *
     * @param title the translated title of the dialog.
     */
    public static JDialog createDialog (
        Component parent, EditorContext ctx, String title, Object object)
    {
        return createDialog(parent, ctx, CategoryMode.TABS, title, object);
    }

    /**
     * Creates and returns a simple dialog for editing the supplied object.
     *
     * @param title the translated title of the dialog.
     */
    public static JDialog createDialog (
        Component parent, EditorContext ctx, CategoryMode catmode, String title, Object object)
    {
        Component root = SwingUtilities.getRoot(parent);
        MessageBundle msgs = ctx.getMessageManager().getBundle(EditorMessageBundle.DEFAULT);
        final JDialog dialog = (root instanceof Dialog) ?
            new JDialog((Dialog)root, title) :
                new JDialog((Frame)(root instanceof Frame ? root : null), title);
        EditorPanel epanel = new EditorPanel(ctx, catmode);
        dialog.add(epanel, BorderLayout.CENTER);
        epanel.setObject(object);
        JPanel bpanel = new JPanel();
        dialog.add(bpanel, BorderLayout.SOUTH);
        JButton ok = new JButton(msgs.get("b.ok"));
        bpanel.add(ok);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                dialog.setVisible(false);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        return dialog;
    }

    /**
     * Creates an empty editor panel.
     */
    public EditorPanel (EditorContext ctx)
    {
        this(ctx, CategoryMode.PANELS);
    }

    /**
     * Creates an empty editor panel.
     *
     * @param catmode determines how different property categories will be displayed.
     */
    public EditorPanel (EditorContext ctx, CategoryMode catmode)
    {
        this(ctx, catmode, null);
    }

    /**
     * Creates an empty editor panel.
     *
     * @param catmode determines how different property categories will be displayed.
     * @param ancestors the ancestor properties from which constraints are inherited.  If this is
     * non-null, the panel is assumed to be embedded within another.
     */
    public EditorPanel (EditorContext ctx, CategoryMode catmode, Property[] ancestors)
    {
        this(ctx, catmode, ancestors, false);
    }

    /**
     * Creates an empty editor panel.
     *
     * @param catmode determines how different property categories will be displayed.
     * @param ancestors the ancestor properties from which constraints are inherited.  If this is
     * non-null, the panel is assumed to be embedded within another.
     * @param omitColumns if true, do not add editors for the properties flagged as columns.
     */
    public EditorPanel (
        EditorContext ctx, CategoryMode catmode, Property[] ancestors, boolean omitColumns)
    {
        super(ctx, ancestors, omitColumns);
        _catmode = catmode;
    }

    /**
     * Sets the object being edited.
     */
    public void setObject (Object object)
    {
        // make sure it's not the same object
        if (object == _object) {
            return;
        }

        // COMMENTED OUT: Ray Greenwell 2015 Feb 11
        // This has caused us various issues over the years, and it's a completely pointless
        // optimization. It's ok if we wholly remove and re-create all the editors..
        // - this only happens in response to the user making a selection
        // - we're editing, not in a game render loop
//        // if the object is the same class as the current object, we can reuse the existing editors
//        Class<?> oclazz = (_object == null) ? null : _object.getClass();
//        Class<?> nclazz = (object == null) ? null : object.getClass();
        super.setObject(object);
//        if (oclazz == nclazz) {
//            for (PropertyEditor editor : _editors) {
//                editor.setObject(_object);
//            }
//            updateDynamicProperties();
//            return;
//        }

        // remove any existing editors
        removeAll();
        _editors.clear();

        // find the object's editable categories/properties
        if (_object == null) {
            revalidate();
            repaint();
            return;
        }
        Class<?> clazz = _object.getClass();
        Property[] props = Introspector.getProperties(clazz);
        final String[] cats = getFilteredCategories(clazz, props);
        MessageBundle cmsgs = _msgmgr.getBundle(Introspector.getMessageBundle(clazz));
        if (cats.length <= 1) {
            // if there's only one category, add them in a single scroll panel
            JPanel inner = addScrollPanel();
            addEditors(props, null, inner);

        } else if (_catmode == CategoryMode.PANELS) {
            JPanel inner = addScrollPanel();
            for (String cat : cats) {
                JPanel content = new JPanel();
                inner.add(content = new JPanel());
                if (cat.length() > 0) {
                    content.setBorder(BorderFactory.createTitledBorder(getLabel(cat, cmsgs)));
                    content.setBackground(getDarkerBackground(
                        (_ancestors == null ? 0 : _ancestors.length) + 0.5f));
                } else {
                    content.setBackground(null);
                }
                content.setLayout(new VGroupLayout(
                    GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
                addEditors(props, cat, content);
            }
        } else if (_catmode == CategoryMode.TABS) {
            JTabbedPane tabs = new JTabbedPane();
            add(tabs);
            for (String cat : cats) {
                JPanel inner = GroupLayout.makeVBox(
                    GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
                inner.setBackground(null);
                tabs.addTab(getLabel(cat, cmsgs), isEmbedded() ? inner : createScrollPane(inner));
                addEditors(props, cat, inner);
            }
        } else { // _catmode == CategoryMode.CHOOSER
            JPanel cpanel = new JPanel();
            add(cpanel, GroupLayout.FIXED);
            cpanel.add(new JLabel(_msgs.get("m.category")));
            final JComboBox cbox = new JComboBox(getLabels(cats, cmsgs));
            cpanel.add(cbox);
            final CardLayout cards = new CardLayout();
            final JPanel inner = new JPanel(cards);
            add(inner);
            for (String cat : cats) {
                JPanel panel = GroupLayout.makeVBox(
                    GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
                panel.setBackground(null);
                inner.add(isEmbedded() ? panel : createScrollPane(panel), cat);
                addEditors(props, cat, panel);
            }
            cbox.addItemListener(new ItemListener() {
                public void itemStateChanged (ItemEvent event) {
                    cards.show(inner, cats[cbox.getSelectedIndex()]);
                }
            });
        }

        // add listeners for dependencies
        for (final PropertyEditor editor : _editors) {
            String[] depends = editor.getProperty().getAnnotation().depends();
            if (depends.length > 0) {
                ChangeListener cl = new ChangeListener() {
                    public void stateChanged (ChangeEvent event) {
                        editor.update();
                    }
                };
                for (String depend : depends) {
                    PropertyEditor deditor = getEditor(depend);
                    if (deditor != null) {
                        deditor.addChangeListener(cl);
                    } else {
                        log.warning("Dependant property not found: " + depend,
                            "editor", editor);
                    }
                }
            }
        }

        // listen for changes (we want these notifications *before* the dependency ones, so because
        // we iterate backwards through the listener list when notifying, we add them afterwards)
        for (PropertyEditor editor : _editors) {
            editor.addChangeListener(this);
        }

        // update the dynamic properties
        updateDynamicProperties();

        // update the layout
        revalidate();
    }

    /**
     * Returns the object being edited.
     */
    public Object getObject ()
    {
        return _object;
    }

    /**
     * Returns the property editor for the property with the supplied name.
     */
    public PropertyEditor getPropertyEditor (String name)
    {
        for (PropertyEditor editor : _editors) {
            if (name.equals(editor.getProperty().getName())) {
                return editor;
            }
        }
        return null;
    }

    /**
     * Updates the editor state in response to an external change in the object's state.
     */
    public void update ()
    {
        for (PropertyEditor editor : _editors) {
            editor.update();
        }
        updateDynamicProperties();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    @Override
    public String getComponentPath (Component comp, boolean mouse)
    {
        PropertyEditor pe = getNextChildComponent(PropertyEditor.class, comp);
        return pe == null ?
            "" : "." + pe.getProperty().getName() + pe.getComponentPath(comp, mouse);
    }

    /**
     * Returns the list of categories, minus any made empty by omission.
     */
    protected String[] getFilteredCategories (Class<?> clazz, Property[] props)
    {
        String[] cats = Introspector.getCategories(clazz);
        if (!_omitColumns) {
            return cats;
        }
        for (int ii = cats.length - 1; ii >= 0; ii--) {
            if (!isCategoryPresent(props, cats[ii])) {
                cats = ArrayUtil.splice(cats, ii, 1);
            }
        }
        return cats;
    }

    /**
     * Determines whether the specified category is represented in the property array.
     */
    protected boolean isCategoryPresent (Property[] props, String cat)
    {
        if (_object instanceof DynamicallyEditable && cat.equals("")) {
            return true;
        }
        for (Property prop : props) {
            Editable annotation = prop.getAnnotation();
            if (!annotation.column() && annotation.category().equals(cat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds and returns a scrolling panel (if not embedded).
     */
    protected JPanel addScrollPanel ()
    {
        JPanel inner = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        inner.setBackground(null);
        add(isEmbedded() ? inner : createScrollPane(inner));
        return inner;
    }

    /**
     * Creates and adds editors for all properties in the specified category (or all properties,
     * if category is null).
     */
    protected void addEditors (Property[] props, String category, JPanel panel)
    {
        JPanel hpanel = null;
        String hgroup = null;
        for (Property prop : props) {
            Editable annotation = prop.getAnnotation();
            if ((category == null || annotation.category().equals(category)) &&
                    !(_omitColumns && annotation.column())) {
                String ahgroup = annotation.hgroup();
                if (ahgroup.length() > 0) {
                    if (hpanel == null || !ahgroup.equals(hgroup)) {
                        panel.add(hpanel = GroupLayout.makeHBox(
                            GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
                        hpanel.setBackground(null);
                        hgroup = ahgroup;
                    }
                    hpanel.add(createEditor(prop));
                } else {
                    panel.add(createEditor(prop));
                    hpanel = null;
                }
            }
        }
        if (_object instanceof DynamicallyEditable && (category == null || category.equals(""))) {
            panel.add(_dynamic = GroupLayout.makeVBox(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
            _dynamic.setBackground(null);
        }
    }

    /**
     * Creates an editor for the specified property.
     */
    protected PropertyEditor createEditor (Property prop)
    {
        PropertyEditor editor = PropertyEditor.createEditor(_ctx, prop, _ancestors);
        editor.setObject(_object);
        _editors.add(editor);
        return editor;
    }

    /**
     * Returns the editor with the given property name.
     */
    protected PropertyEditor getEditor (String name)
    {
        for (PropertyEditor editor : _editors) {
            if (editor.getProperty().getName().equals(name)) {
                return editor;
            }
        }
        return null;
    }

    /**
     * Updates the dynamic properties.
     */
    protected void updateDynamicProperties ()
    {
        if (!(_object instanceof DynamicallyEditable)) {
            return;
        }
        int ocount = _dynamic.getComponentCount();
        Property[] properties = ((DynamicallyEditable)_object).getDynamicProperties();
        int idx = 0;
        for (; idx < properties.length; idx++) {
            Property property = properties[idx];
            PropertyEditor editor = null;
            if (idx < ocount) {
                // see if we can reuse the existing editor
                PropertyEditor oeditor = (PropertyEditor)_dynamic.getComponent(idx);
                if (oeditor.getProperty().equals(property)) {
                    editor = oeditor;
                } else {
                    _dynamic.remove(idx);
                }
            }
            if (editor == null) {
                editor = PropertyEditor.createEditor(_ctx, property, _ancestors);
                editor.addChangeListener(this);
                _dynamic.add(editor, idx);
            }
            editor.setObject(_object);
        }
        // remove the remaining editors
        while (ocount > idx) {
            _dynamic.remove(--ocount);
        }
    }

    /**
     * Creates a scroll pane with an increment that's more useful than the default.
     */
    protected static JScrollPane createScrollPane (Component view)
    {
        JScrollPane pane = new JScrollPane(
            view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getVerticalScrollBar().setUnitIncrement(32);
        return pane;
    }

    /** How to present different categories of properties. */
    protected CategoryMode _catmode;

    /** If true, do not add editors for the properties flagged as columns. */
    protected boolean _omitColumns;

    /** The current list of editors. */
    protected ArrayList<PropertyEditor> _editors = new ArrayList<PropertyEditor>();

    /** A container for the dynamic properties. */
    protected JPanel _dynamic;
}
