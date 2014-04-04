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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.log;

/**
 * Finds text in a container.
 */
public class FindDialog extends JDialog
    implements ActionListener
{
    public static FindDialog createDialog (Component parent, EditorContext ctx)
    {
        Component root = SwingUtilities.getRoot(parent);
        MessageBundle msgs = ctx.getMessageManager().getBundle(EditorMessageBundle.DEFAULT);
        String title = msgs.get("m.find");
        FindDialog dialog = (root instanceof Dialog) ?
            new FindDialog((Dialog)root, ctx, title) :
            new FindDialog((Frame)(root instanceof Frame ? root : null), ctx, title);
        SwingUtil.centerWindow(dialog);
        dialog.setAlwaysOnTop(true);
        return dialog;
    }

    /**
     * Create the dialog.
     */
    public FindDialog (Dialog parent, EditorContext ctx, String title)
    {
        super(parent, title);
        init(ctx);
    }

    /**
     * Create the dialog.
     */
    public FindDialog (Frame parent, EditorContext ctx, String title)
    {
        super(parent, title);
        init(ctx);
    }

    /**
     * Show the dialog.
     */
    public void show (BaseEditorPanel epanel)
    {
        setEditorPanel(epanel);
        setVisible(true);
        _field.requestFocus();
        _field.selectAll();
        _status.setText("");
    }

    /**
     * Sets the editor panel.
     */
    public void setEditorPanel (BaseEditorPanel epanel)
    {
        if (_epanel != epanel) {
            _epanel = epanel;
            _first = null;
            _last = null;
        }
    }

    /**
     * Perform a find.
     */
    public void find (BaseEditorPanel epanel)
    {
        setEditorPanel(epanel);
        find();
    }

    /**
     * Perform a find.
     */
    public void find ()
    {
        if (_epanel == null) {
            return;
        }
        String term = StringUtil.trim(_field.getText()).toLowerCase();
        if (StringUtil.isBlank(term)) {
            return;
        }
        if (!term.equals(_term)) {
            _last = null;
            _first = null;
            _term = term;
        }
        _viewport = null;
        _first = testTerm(_first);
        _last = testTerm(_last);
        goFind(_epanel);
        _status.setText("");
        if (_last == null) {
            _last = _first;
            if (_last != null) {
                _status.setText(_msgs.get("m.looped_to_top"));
            }
        }
        if (_last != null && _viewport != null) {
            _viewport.scrollRectToVisible(getViewportBounds(_last));
            _last.requestFocusInWindow();
        } else {
            _status.setText(_msgs.get("m.not_found"));
        }
        pack();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object source = event.getSource();
        if (source == _find || source == _field) {
            find();
        }
    }

    /**
     * Initialize the dialog.
     */
    protected void init (EditorContext ctx)
    {
        _msgs = ctx.getMessageManager().getBundle(EditorMessageBundle.DEFAULT);
        JPanel cont = GroupLayout.makeVBox();
        JPanel box = GroupLayout.makeHBox();
        box.add(_field = new JTextField(20));
        _field.addActionListener(this);
        box.add(_find = new JButton(_msgs.get("m.find")));
        _find.addActionListener(this);
        cont.add(box);
        cont.add(_status = new JLabel());
        add(cont);
        pack();
    }

    /**
     * Find a component with the term.
     */
    protected boolean goFind (Component comp)
    {
        if (_viewport == null) {
            if (comp instanceof JScrollPane) {
                _viewport = ((JScrollPane)comp).getViewport();
                _first = testViewport(_first);
                _last = testViewport(_last);
            }
        } else {
            JComponent found = testTerm(comp);
            if (found != null && found(found)) {
                return true;
            }
        }
        if (comp instanceof Container) {
            Container cont = (Container)comp;
            for (int ii = 0, nn = cont.getComponentCount(); ii < nn; ii++) {
                if (goFind(cont.getComponent(ii))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the component if it's a child of the viewport.
     */
    protected JComponent testViewport (JComponent comp)
    {
        if (comp != null) {
            for (Component p = comp.getParent(); p != null; p = p.getParent()) {
                if (p == _viewport) {
                    return comp;
                }
            }
        }
        return null;
    }

    /**
     * Returns the component if it's still valid for the search term.
     */
    protected JComponent testTerm (Component comp)
    {
        JComponent found = null;
        if (comp instanceof JTextComponent) {
            String text = ((JTextComponent)comp).getText().toLowerCase();
            //log.info("Found text component", "text", text);
            if (text.contains(_term)) {
                found = (JComponent)comp;
            }
        } else if (comp instanceof AbstractButton) {
            String text = ((AbstractButton)comp).getText().toLowerCase();
            //log.info("Found button", "text", text);
            if (text.contains(_term)) {
                found = (JComponent)comp;
            }
        } else if (comp instanceof JTable) {
            JTable table = (JTable)comp;
            OUTER:
            for (int ii = 0, nn = table.getRowCount(); ii < nn; ii++) {
                for (int jj = 0, mm = table.getColumnCount(); jj < mm; jj++) {
                    Object obj = table.getValueAt(ii, jj);
                    if (obj != null && obj.toString().toLowerCase().contains(_term)) {
                        found = table;
                        break OUTER;
                    }
                }
            }
        }
        return found;
    }

    /**
     * Marks a component as matching the search criteria.
     */
    protected boolean found (JComponent comp)
    {
        if (_first == null) {
            _first = comp;
        }
        if (_last == null) {
            _last = comp;
            return true;
        } else if (_last == comp) {
            _last = null;
        }
        return false;
    }

    /**
     * Calculates the bounds of a nested component in the viewport.
     */
    protected Rectangle getViewportBounds(Component comp)
    {
        if (comp == _viewport) {
            return new Rectangle();
        }
        Rectangle bounds = comp.getBounds();
        Rectangle parent = getViewportBounds(comp.getParent());
        bounds.x += parent.x;
        bounds.y += parent.y;
        return bounds;
    }

    @Override
    protected JRootPane createRootPane ()
    {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane rootPane = new JRootPane();
        rootPane.registerKeyboardAction(new ActionListener () {
                public void actionPerformed (ActionEvent event) {
                    FindDialog.this.setVisible(false);
                }
            }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }

    /** Our message bundle. */
    protected MessageBundle _msgs;

    /** The find field. */
    protected JTextField _field;

    /** The find button. */
    protected JButton _find;

    /** The status label. */
    protected JLabel _status;

    /** Our config editor. */
    protected BaseEditorPanel _epanel;

    /** The search term. */
    protected String _term;

    /** The last and first found component. */
    protected JComponent _last, _first;

    /** The viewport. */
    protected JViewport _viewport;
}
