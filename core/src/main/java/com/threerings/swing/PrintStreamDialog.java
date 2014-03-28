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

package com.threerings.swing;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Provides a {@link PrintStream} that writes to a buffer.  If the buffer is non-empty when
 * {@link #maybeShow} is called, the dialog is shown with the contents of the buffer and an
 * OK button to close.
 */
public class PrintStreamDialog extends JDialog
{
    /**
     * Creates a new dialog.
     */
    public PrintStreamDialog (Frame owner, String title, String close)
    {
        super(owner, title);
        _close = close;
        _printStream = new PrintStream(_out = new ByteArrayOutputStream());
    }

    /**
     * Returns a reference to the dialog's print stream.
     */
    public PrintStream getPrintStream ()
    {
        return _printStream;
    }

    /**
     * Shows the dialog with the contents of its buffer if the buffer isn't empty.
     */
    public void maybeShow ()
    {
        _printStream.close();
        String contents = _out.toString();
        if (contents.isEmpty()) {
            return;
        }
        JTextArea text = new JTextArea(contents);
        text.setTabSize(4);
        text.setLineWrap(true);
        JScrollPane pane = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(pane, BorderLayout.CENTER);

        JPanel bpanel = new JPanel();
        add(bpanel, BorderLayout.SOUTH);
        bpanel.add(new JButton(new AbstractAction(_close) {
            public void actionPerformed (ActionEvent event) {
                dispose();
            }
        }));

        setSize(800, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    /** The buffer output stream. */
    protected ByteArrayOutputStream _out;

    /** The print stream that writes to the buffer. */
    protected PrintStream _printStream;

    /** The text for the close button. */
    protected String _close;
}
