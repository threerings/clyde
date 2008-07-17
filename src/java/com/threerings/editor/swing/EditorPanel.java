//
// $Id$

package com.threerings.editor.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.DynamicallyEditable;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.*;

/**
 * Allows editing properties of an object as determined through reflection.
 */
public class EditorPanel extends BasePropertyEditor
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
        JButton ok = new JButton(msgs.get("m.ok"));
        bpanel.add(ok);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                dialog.setVisible(false);
            }
        });
        dialog.pack();
        if (parent == null || !parent.isShowing()) {
            SwingUtil.centerWindow(dialog);
        } else {
            Point pt = parent.getLocationOnScreen();
            dialog.setLocation(
                pt.x + (parent.getWidth() - dialog.getWidth()) / 2,
                pt.y + (parent.getHeight() - dialog.getHeight()) / 2);
        }
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
        _ctx = ctx;
        _msgmgr = ctx.getMessageManager();
        _msgs = _msgmgr.getBundle(EditorMessageBundle.DEFAULT);
        _catmode = catmode;
        _ancestors = ancestors;
        _omitColumns = omitColumns;

        // add a mapping to copy the path of the property under the mouse cursor to the clipboard
        if (ancestors == null) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK),
                "copy_path");
            getActionMap().put("copy_path", new AbstractAction() {
                public void actionPerformed (ActionEvent event) {
                    copyPropertyPath();
                }
            });
        }

        setLayout(new VGroupLayout(
            isEmbedded() ? GroupLayout.NONE : GroupLayout.STRETCH,
            GroupLayout.STRETCH, 5, GroupLayout.TOP));
    }

    /**
     * Copies the path of the property under the mouse cursor to the clipboard.
     */
    protected void copyPropertyPath ()
    {
        StringBuilder buf = new StringBuilder();
        getMousePath(buf);
        if (buf.length() > 0) {
            StringSelection contents = new StringSelection(buf.toString());
            getToolkit().getSystemClipboard().setContents(contents, contents);
        }
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

        // if the object is the same class as the current object, we can reuse the existing editors
        Class oclazz = (_object == null) ? null : _object.getClass();
        Class nclazz = (object == null) ? null : object.getClass();
        _object = object;
        if (oclazz == nclazz) {
            for (PropertyEditor editor : _editors) {
                editor.setObject(_object);
            }
            updateDynamicProperties();
            return;
        }

        // remove any existing editors
        removeAll();
        _editors.clear();

        // find the object's editable categories/properties
        if (_object == null) {
            revalidate();
            repaint();
            return;
        }
        Class clazz = _object.getClass();
        final String[] cats = Introspector.getCategories(clazz);
        Property[] props = Introspector.getProperties(clazz);
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
                tabs.addTab(getLabel(cat, cmsgs), isEmbedded() ? inner : new JScrollPane(
                    inner, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
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
                inner.add(isEmbedded() ? panel : new JScrollPane(
                    panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cat);
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
                    }
                }
            }
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

    /**
     * Adds and returns a scrolling panel (if not embedded).
     */
    protected JPanel addScrollPanel ()
    {
        JPanel inner = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        inner.setBackground(null);
        add(isEmbedded() ? inner : new JScrollPane(
            inner, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        return inner;
    }

    /**
     * Determines whether this editor panel is embedded within another.
     */
    protected boolean isEmbedded ()
    {
        return (_ancestors != null);
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
        editor.addChangeListener(this);
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

    /** Provides access to common services. */
    protected EditorContext _ctx;

    /** How to present different categories of properties. */
    protected CategoryMode _catmode;

    /** The ancestor properties from which constraints are inherited. */
    protected Property[] _ancestors;

    /** If true, do not add editors for the properties flagged as columns. */
    protected boolean _omitColumns;

    /** The object being edited. */
    protected Object _object;

    /** The current list of editors. */
    protected ArrayList<PropertyEditor> _editors = new ArrayList<PropertyEditor>();

    /** A container for the dynamic properties. */
    protected JPanel _dynamic;
}
