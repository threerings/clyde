//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.samskivert.util.ObjectUtil;

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
        if (event.getSource() == _file) {
            final FileConstraints constraints = _property.getAnnotation(FileConstraints.class);
            String key = (constraints == null || constraints.directory().length() == 0) ?
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
                            return msgs.get(constraints.description());
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

        } else { // event.getSource() == _clear
            value = null;
        }
        if (!ObjectUtil.equals(getPropertyFile(), value)) {
            setPropertyFile(value);
            updateButtons(value);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        add(_file = new JButton(" "));
        _file.setPreferredSize(new Dimension(75, _file.getPreferredSize().height));
        _file.addActionListener(this);
        if (_property.getAnnotation().nullable()) {
            add(_clear = new JButton(_msgs.get("m.clear")));
            _clear.addActionListener(this);
        }
    }

    @Override // documentation inherited
    protected void update ()
    {
        updateButtons(getPropertyFile());
    }

    /**
     * Updates the state of the buttons.
     */
    protected void updateButtons (File value)
    {
        _file.setText(value == null ? _msgs.get("m.none") : value.getName());
        if (_clear != null) {
            _clear.setEnabled(value != null);
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
