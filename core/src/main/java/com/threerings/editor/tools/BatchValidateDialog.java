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

package com.threerings.editor.tools;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.editor.util.EditorContext;
import com.threerings.editor.util.Validator;
import com.threerings.expr.MutableBoolean;
import com.threerings.expr.MutableInteger;
import com.threerings.util.MessageBundle;

import static com.threerings.editor.Log.log;

/**
 * Allows users to validate a batch of files defined by an Ant-style fileset.
 */
public abstract class BatchValidateDialog extends JDialog
    implements ActionListener
{
    /**
     * Creates a new batch validate dialog.
     */
    public BatchValidateDialog (EditorContext ctx, JFrame parent, Preferences prefs)
    {
        super(parent, true);
        _ctx = ctx;
        _prefs = prefs;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(400, 500);
        SwingUtil.centerWindow(this);

        JPanel cont = GroupLayout.makeVStretchBox(5);
        setContentPane(cont);

        MessageBundle msgs = ctx.getMessageManager().getBundle("editor.default");
        setTitle(msgs.get("m.batch_validate"));

        JPanel ipanel = GroupLayout.makeHStretchBox(5);
        cont.add(ipanel, GroupLayout.FIXED);
        ipanel.add(new JLabel(msgs.get("m.includes")), GroupLayout.FIXED);
        ipanel.add(_includes = new JTextField(prefs.get("validate_includes", "")));

        JPanel epanel = GroupLayout.makeHStretchBox(5);
        cont.add(epanel, GroupLayout.FIXED);
        epanel.add(new JLabel(msgs.get("m.excludes")), GroupLayout.FIXED);
        epanel.add(_excludes = new JTextField(prefs.get("validate_excludes", "")));

        JPanel bpanel = new JPanel();
        cont.add(bpanel, GroupLayout.FIXED);
        bpanel.add(_start = new JButton(msgs.get("m.start")));
        _start.addActionListener(this);

        cont.add(new JScrollPane(_results = new JTextArea()));
        _results.setEditable(false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _start.setEnabled(false);

        // store the preferred includes and excludes
        String includes = _includes.getText();
        _prefs.put("validate_includes", includes);
        String excludes = _excludes.getText();
        _prefs.put("validate_excludes", excludes);

        // create a stream that writes to the results area on flush
        _results.setText("");
        final PrintStream out = new PrintStream(new ByteArrayOutputStream() {
            @Override public void flush () throws IOException {
                super.flush();
                _results.append(toString());
                reset();
            }
        }, true);

        // find all matching files
        FileSet fs = new FileSet();
        final File dir = _ctx.getResourceManager().getResourceFile("");
        fs.setDir(dir);
        fs.setIncludes(includes);
        fs.setExcludes(excludes);
        DirectoryScanner ds = fs.getDirectoryScanner(new Project());
        final String[] files = ds.getIncludedFiles();
        final MutableInteger idx = new MutableInteger();
        final MutableBoolean valid = new MutableBoolean(true);

        // perform each validation as a separate element on the run queue so that we can display
        // results as they come in
        Runnable runnable = new Runnable() {
            public void run () {
                if (idx.value < files.length && isVisible()) {
                    String file = files[idx.value++];
                    try {
                        valid.value &= validate(createValidator(file, out), file);
                    } catch (Exception e) {
                        log.warning("Error in validation.", "file", file, e);
                    }
                    EventQueue.invokeLater(this);
                } else {
                    _start.setEnabled(true);
                }
            }
        };
        EventQueue.invokeLater(runnable);
    }

    /**
     * Create the validator to use for this batch validate.
     */
    protected Validator createValidator (String where, PrintStream out)
    {
        return new Validator(where, out);
    }

    /**
     * Performs the actual validation.
     */
    protected abstract boolean validate (Validator validator, String path) throws Exception;

    /** The application context. */
    protected EditorContext _ctx;

    /** Our preferences object. */
    protected Preferences _prefs;

    /** The include and exclude fields. */
    protected JTextField _includes, _excludes;

    /** The start button. */
    protected JButton _start;

    /** The text area for the results. */
    protected JTextArea _results;
}
