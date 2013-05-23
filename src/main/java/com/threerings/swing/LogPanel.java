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
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Handler;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import com.google.common.collect.Lists;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

/**
 * A panel that alerts the user when a warning is logged and allows viewing the complete log.
 */
public class LogPanel extends JPanel
    implements ActionListener
{
    /**
     * Default constructor.
     *
     * @param addHandler if true, add the log handler immediately
     * (don't wait until the component is added).
     */
    public LogPanel (MessageManager msgmgr, boolean addHandler)
    {
        super(new HGroupLayout(GroupLayout.STRETCH));
        _msgs = msgmgr.getBundle("swing");

        add(_warning = new JLabel());

        add(_show = new JButton(_msgs.get("m.show")), GroupLayout.FIXED);
        _show.addActionListener(this);
        _show.setVisible(false);

        add(_clear = new JButton(_msgs.get("m.clear")), GroupLayout.FIXED);
        _clear.addActionListener(this);
        _clear.setVisible(false);

        _handler = new Handler() {
            @Override public void publish (LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                _warning.setText(record.getMessage());
                _show.setVisible(true);
                _clear.setVisible(true);
                _records.add(record);
            }
            @Override public boolean isLoggable (LogRecord record) {
                return super.isLoggable(record) && !record.getMessage().startsWith("Long dobj");
            }
            @Override public void flush () {
                // no-op
            }
            @Override public void close () {
                // no-op
            }
        };
        _handler.setLevel(Level.WARNING);
        if (addHandler) {
            maybeAddHandler();
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() == _show) {
            if (_dialog == null) {
                Object win = SwingUtilities.getWindowAncestor(this);
                if (win instanceof Frame) {
                    _dialog = new LogDialog((Frame)win);
                } else { // win instanceof Dialog
                    _dialog = new LogDialog((Dialog)win);
                }
            }
            _dialog.setVisible(true);
        }
        // clear all open panels
        for (int ii = 0, nn = _panels.size(); ii < nn; ii++) {
            _panels.get(ii).clear();
        }
    }

    @Override
    public void addNotify ()
    {
        super.addNotify();
        maybeAddHandler();
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();
        LogManager.getLogManager().getLogger("").removeHandler(_handler);
        _panels.remove(this);
        clear();
    }

    /**
     * Adds the handler if appropriate and not already added.
     */
    protected void maybeAddHandler ()
    {
        if (!(Boolean.getBoolean("no_log_redir") || _panels.contains(this))) {
            LogManager.getLogManager().getLogger("").addHandler(_handler);
            _panels.add(this);
        }
    }

    /**
     * Clears out the panel.
     */
    protected void clear ()
    {
        _warning.setText("");
        _show.setVisible(false);
        _clear.setVisible(false);
        _records.clear();
    }

    /**
     * The log dialog window.
     */
    protected class LogDialog extends JDialog
        implements ActionListener
    {
        /**
         * Dialog-owned constructor.
         */
        public LogDialog (Dialog owner)
        {
            super(owner, _msgs.get("m.error_log"));
            init();
        }

        /**
         * Frame-owned constructor.
         */
        public LogDialog (Frame owner)
        {
            super(owner, _msgs.get("m.error_log"));
            init();
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            setVisible(false);
        }

        @Override
        public void setVisible (boolean visible)
        {
            if (!visible) {
                super.setVisible(false);
                return;
            }

            // clear any existing text
            _text.setText("");

            // add the formatted log messages
            int lines = 0;
            for (int ii = 0, nn = _records.size(); ii < nn; ii++) {
                lines = _text.getLineCount();
                _text.append(_formatter.format(_records.get(ii)));
            }

            super.setVisible(true);

            // scroll to the appropriate line once we're laid out
            final int line = lines - 1;
            EventQueue.invokeLater(new Runnable() {
                public void run () {
                    try {
                        Rectangle view = _text.modelToView(_text.getLineStartOffset(line));
                        _pane.getVerticalScrollBar().setValue(view.y);
                    } catch (BadLocationException e) {
                        // shouldn't happen
                    }
                }
            });
        }

        /**
         * Initializes the dialog.
         */
        protected void init ()
        {
            _pane = new JScrollPane(
                _text = new JTextArea(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(_pane, BorderLayout.CENTER);
            _text.setTabSize(4);
            _text.setLineWrap(true);

            JPanel bpanel = new JPanel();
            add(bpanel, BorderLayout.SOUTH);
            JButton close = new JButton(_msgs.get("m.close"));
            bpanel.add(close);
            close.addActionListener(this);

            setSize(800, 500);
            setLocationRelativeTo(LogPanel.this);
        }

        /** The text area. */
        protected JTextArea _text;

        /** The scroll pane. */
        protected JScrollPane _pane;

        /** The log formatter. */
        protected OneLineLogFormatter _formatter = new OneLineLogFormatter();
    }

    /** The translation bundle. */
    protected MessageBundle _msgs;

    /** The label that displays the most recent logged warning. */
    protected JLabel _warning;

    /** The button that pops up a window to display the entire warning. */
    protected JButton _show;

    /** The button that clears out the displayed warning. */
    protected JButton _clear;

    /** The log message handler. */
    protected Handler _handler;

    /** Error records stored for future display. */
    protected List<LogRecord> _records = Lists.newArrayList();

    /** The dialog window. */
    protected LogDialog _dialog;

    /** The list of all open panels. */
    protected static List<LogPanel> _panels = Lists.newArrayList();
}
