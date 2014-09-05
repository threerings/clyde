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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

/**
 * Base class for {@link EditorPanel} and {@link TreeEditorPanel}.
 */
public abstract class BaseEditorPanel extends BasePropertyEditor
{
    /**
     * Creates an empty editor panel.
     */
    public BaseEditorPanel (EditorContext ctx, Property[] ancestors, boolean omitColumns)
    {
        _ctx = ctx;
        _ancestors = ancestors;
        _omitColumns = omitColumns;
        _msgmgr = ctx.getMessageManager();
        _msgs = _msgmgr.getBundle(EditorMessageBundle.DEFAULT);

        // add a mapping to copy the path of the property under the mouse cursor to the clipboard
        if (ancestors == null) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK),
                "copy_path");
            getActionMap().put("copy_path", new AbstractAction() {
                public void actionPerformed (ActionEvent event) {
                    copyPropertyPath(getMousePath());
                }
            });
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK),
                "direct_path");
            getActionMap().put("direct_path", new AbstractAction() {
                public void actionPerformed (ActionEvent event) {
                    createDirectPath(getMousePath());
                }
            });

            // since we are a top-level editor, set up the parameter highlighter
            new ParameterHighlighter(this);
        }

        setLayout(new VGroupLayout(
            isEmbedded() ? GroupLayout.NONE : GroupLayout.STRETCH,
            GroupLayout.STRETCH, 5, GroupLayout.TOP));
    }

    /**
     * Returns a reference to the array of ancestor properties from which constraints are
     * inherited.
     */
    public Property[] getAncestors ()
    {
        return _ancestors;
    }

    /**
     * Returns whether or not we should omit properties flagged as columns.
     */
    public boolean getOmitColumns ()
    {
        return _omitColumns;
    }

    /**
     * Sets the object being edited.
     */
    public void setObject (Object object)
    {
        _object = object;
    }

    /**
     * Returns the object being edited.
     */
    public Object getObject ()
    {
        return _object;
    }

    /**
     * Determines whether this editor panel is embedded within another.
     */
    protected boolean isEmbedded ()
    {
        return (_ancestors != null);
    }

    /**
     * Attempts to create a new direct property path.
     */
    protected void createDirectPath (String path)
    {
        if (path.startsWith(".")) {
            path = path.substring(1);
        }
        if (path.length() > 0 && _object instanceof ParameterizedConfig) {
            if (((ParameterizedConfig)_object).isInvalidParameterPath(path)) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String name = path.substring(path.lastIndexOf(".") + 1);
            if (name.endsWith("]")) {
                int brack1 = name.lastIndexOf('[');
                int quote2 = name.lastIndexOf('"');
                name = (quote2 > brack1) // is there a " within the []?
                    ? name.substring(brack1 + 2, quote2) // get what's within the quotes
                    : name.substring(0, brack1); // everything up to the [
            }
            if (_ddialog == null) {
                _ddialog = DirectDialog.createDialog(this, _ctx);
            }
            _ddialog.show(this, name, path);
        }
    }

    /**
     * Updates the editor state in response to an external change in the object's state.
     */
    public abstract void update ();

    /** The ancestor properties from which constraints are inherited. */
    protected Property[] _ancestors;

    /** If true, omit properties flagged as columns. */
    protected boolean _omitColumns;

    /** The object being edited. */
    protected Object _object;

    /** The dialog for creating direct parameters. */
    protected DirectDialog _ddialog;
}
