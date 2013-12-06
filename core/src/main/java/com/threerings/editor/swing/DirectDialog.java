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
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Parameter;
import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.log;

/**
 * Creates a direct parameter.
 */
public class DirectDialog extends JDialog
    implements ActionListener
{
    public static DirectDialog createDialog (Component parent, EditorContext ctx)
    {
        Component root = SwingUtilities.getRoot(parent);
        MessageBundle msgs = ctx.getMessageManager().getBundle(EditorMessageBundle.DEFAULT);
        String title = msgs.get("m.direct");
        DirectDialog dialog = (root instanceof Dialog) ?
            new DirectDialog((Dialog)root, ctx, title) :
            new DirectDialog((Frame)(root instanceof Frame ? root : null), ctx, title);
        SwingUtil.centerWindow(dialog);
        dialog.setAlwaysOnTop(true);
        return dialog;
    }

    /**
     * Create the dialog.
     */
    public DirectDialog (Dialog parent, EditorContext ctx, String title)
    {
        super(parent, title);
        init(ctx);
    }

    /**
     * Create the dialog.
     */
    public DirectDialog (Frame parent, EditorContext ctx, String title)
    {
        super(parent, title);
        init(ctx);
    }

    /**
     * Show the dialog.
     */
    public void show (BaseEditorPanel epanel, String name, String path)
    {
        setEditorPanel(epanel);
        setVisible(true);
        _field.requestFocus();
        _field.setText(name);
        _field.selectAll();
        _path.setText(path);
        _status.setText("");
        pack();
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
     * Creates the direct parameter.
     */
    public void create ()
    {
        if (_epanel == null) {
            return;
        }
        ParameterizedConfig pc = (ParameterizedConfig)_epanel.getObject();
        Parameter.Direct direct = null;
        String name = _field.getText().trim();
        for (Parameter param : pc.parameters) {
            if (param.name.equals(name)) {
                if (param instanceof Parameter.Direct) {
                    direct = (Parameter.Direct)param;
                    break;
                } else {
                    _status.setText(_msgs.get("m.param_name_in_use"));
                    return;
                }
            }
        }
        if (direct == null) {
            direct = new Parameter.Direct();
            pc.parameters = ArrayUtil.append(pc.parameters, direct);
            direct.name = name;
        }
        String path = _path.getText().trim();
        for (String opath : direct.paths) {
            if (path.equals(opath)) {
                path = null;
                break;
            }
        }
        if (path != null && path.length() > 0) {
            direct.paths = ArrayUtil.append(direct.paths, path);
            pc.wasUpdated();
        }
        setVisible(false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object source = event.getSource();
        if (source == _field) {
            create();
        }
    }

    /**
     * Initialize the dialog.
     */
    protected void init (EditorContext ctx)
    {
        _msgs = ctx.getMessageManager().getBundle(EditorMessageBundle.DEFAULT);
        JPanel cont = GroupLayout.makeVBox();
        cont.add(_field = new JTextField(20));
        cont.add(_path = new JLabel());
        _field.addActionListener(this);
        cont.add(_status = new JLabel());
        add(cont);
        pack();
    }

    @Override
    protected JRootPane createRootPane ()
    {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane rootPane = new JRootPane();
        rootPane.registerKeyboardAction(new ActionListener () {
                public void actionPerformed (ActionEvent event) {
                    DirectDialog.this.setVisible(false);
                }
            }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }

    /** Our message bundle. */
    protected MessageBundle _msgs;

    /** The name field. */
    protected JTextField _field;

    /** The path label. */
    protected JLabel _path;

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
