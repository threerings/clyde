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

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.threerings.util.MessageBundle;

import com.threerings.editor.FileConstraints;
import com.threerings.editor.swing.PropertyEditor;

/**
 * Edits file properties.
 */
public class FileEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        File value;
        Object source = event.getSource();
        if (source == _file) {
            final FileConstraints constraints = _property.getAnnotation(FileConstraints.class);
            String key = (constraints == null || constraints.directory().isEmpty()) ?
                null : constraints.directory();
            if (_chooser == null) {
                String ddir = getDefaultDirectory();
                _chooser = new JFileChooser(key == null ? ddir : _prefs.get(key, ddir));
                if (getMode().equals("directory")) {
                    _chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                } else if (constraints != null) {
                    final MessageBundle msgs = _msgmgr.getBundle(_property.getMessageBundle());
                    _chooser.setFileFilter(new FileFilter() {
                        public boolean accept (File file) {
                            if (file.isDirectory()) {
                                return true;
                            }
                            String name = file.getName();
                            for (String extension : constraints.extensions()) {
                                if (name.endsWith(extension)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        public String getDescription () {
                            return msgs.xlate(constraints.description());
                        }
                    });
                }
            }
            _chooser.setSelectedFile(getPropertyFile());
            int result = _chooser.showOpenDialog(this);
            if (key != null) {
                _prefs.put(key, _chooser.getCurrentDirectory().toString());
            }
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            value = _chooser.getSelectedFile();
            // possibly strip the extension
            if (constraints != null && constraints.stripExtension()) {
                String filename = value.toString();
                int dot = filename.lastIndexOf('.');
                if (dot > -1 && dot > filename.lastIndexOf(File.pathSeparatorChar)) {
                    value = new File(filename.substring(0, dot));
                }
            }

        } else { // source == _clear
            value = null;
        }
        try {
            setPropertyFile(value);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this, e.getMessage(), _msgs.get("t.invalid_value"), JOptionPane.ERROR_MESSAGE);
        }
        update();
        fireStateChanged();
    }

    @Override
    public void update ()
    {
        updateButtons(getPropertyFile());
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        add(_file = new JButton(" "));
        _file.addActionListener(this);
        if (_property.nullable()) {
            add(_clear = new JButton(_msgs.get("m.clear")));
            _clear.addActionListener(this);
        }
    }

    /**
     * Updates the state of the buttons.
     */
    protected void updateButtons (File value)
    {
        boolean enable = (value != null);
        _file.setText(enable ? value.getName() : _msgs.get("m.null_value"));
        if (_clear != null) {
            _clear.setEnabled(enable);
        }
    }

    /**
     * Returns the default directory to start in, if there is no stored preference.
     */
    protected String getDefaultDirectory ()
    {
        return null;
    }

    /**
     * Returns the value of the property as a {@link File}.
     */
    protected File getPropertyFile ()
    {
        return (File)_property.get(_object);
    }

    /**
     * Sets the value of the property as a {@link File}.
     */
    protected void setPropertyFile (File file)
    {
        _property.set(_object, file);
    }

    /** The file button. */
    protected JButton _file;

    /** The clear button. */
    protected JButton _clear;

    /** The file chooser. */
    protected JFileChooser _chooser;

    /** User preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(FileEditor.class);
}
