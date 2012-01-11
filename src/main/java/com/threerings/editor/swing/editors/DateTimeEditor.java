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

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.log;

/**
 * Editor for date-time, with included subclasses for just date or just time.
 * Supports Date, Long, or long fields.
 *
 * The 'mode' of the @Editable annotation can contain a comma separated list of attributes:
 * - style (SHORT, MEDIUM, LONG, FULL)
 * - timezone (any timezone)
 * - locale (up to three specifiers separated by spaces: language country variant)
 * - format (to specify a SimpleDateFormat format, instead of specifying 'style')
 *
 * Examples:
 * &at;Editable(editor="datetime", mode="style=full, timezone=PST8PDT, locale=en us")
 * &at;Editable(editor="datetime", mode="style=short, locale=es es Traditional_WIN")
 * &at;Editable(editor="datetime", mode="format=yyyy-MM-dd hh:mm aaa")
 */
public class DateTimeEditor extends PropertyEditor
    implements DocumentListener, FocusListener
{
    /**
     * A subclass that is date-only.
     */
    public static class DateOnlyEditor extends DateTimeEditor
    {
        @Override protected DateFormat createFormat (int style, Locale locale)
        {
            return DateFormat.getDateInstance(style, locale);
        }
    }

    /**
     * A subclass that is time-only.
     */
    public static class TimeOnlyEditor extends DateTimeEditor
    {
        @Override protected DateFormat createFormat (int style, Locale locale)
        {
            return DateFormat.getTimeInstance(style, locale);
        }
    }

    // from DocumentListener
    public void insertUpdate (DocumentEvent event)
    {
        changedUpdate(null);
    }

    // from DocumentListener
    public void removeUpdate (DocumentEvent event)
    {
        changedUpdate(null);
    }

    // from DocumentListener
    public void changedUpdate (DocumentEvent event)
    {
        String text = _field.getText().trim();
        try {
            Object oldVal = _property.get(_object);
            Object newVal;
            if ("".equals(text) && _property.getAnnotation().nullable()) {
                newVal = null;

            } else {
                Date d = _format.parse(text);
                newVal = (oldVal instanceof Date) ? d : d.getTime();
            }
            if (!Objects.equal(newVal, oldVal)) {
                _property.set(_object, newVal);
                fireStateChanged();
            }
            //log.info(   "Parse valid", "text", text);
            _invalid = false;

        } catch (Exception e) {
            //log.warning("Parse error", "text", e);
            _invalid = true;
        }

        updateBorder();
    }

    // from FocusListener
    public void focusGained (FocusEvent e) {} // nada

    // from FocusListener
    public void focusLost (FocusEvent e)
    {
        if (!_invalid) {
            update(); // show what we've really got
        }
    }

    @Override
    public void update ()
    {
        // remove ourselves as a listener during update
        _field.getDocument().removeDocumentListener(this);
        Object prop = _property.get(_object);
        _field.setText((prop == null) ? "" : _format.format(prop));
        _field.getDocument().addDocumentListener(this);
    }

    @Override
    protected void didInit ()
    {
        // create the widgets
        add(new JLabel(getPropertyLabel() + ":"));
        _field = new JTextField(_property.getAnnotation().width());
        add(_field);
        _field.getDocument().addDocumentListener(this);
        _field.addFocusListener(this);
        addUnits();

        configureFormat();
    }

    /**
     * Configure the DateFormat we'll be using.
     */
    protected void configureFormat ()
    {
        // parse the mode arguments
        String mode = _property.getAnnotation().mode();

        // set up our defaults
        int style = DateFormat.SHORT;
        TimeZone timezone = TimeZone.getDefault();
        Locale locale = Locale.getDefault();
        String format = null;

        for (String attr : Splitter.on(',').trimResults().omitEmptyStrings().split(mode)) {
            int eq = attr.indexOf('=');
            if (eq == -1) {
                log.warning("No '=' found in mode attribute: " + attr);
                continue;
            }
            String kind = attr.substring(0, eq);
            String spec = attr.substring(eq + 1);
            if ("".equals(spec)) {
                log.warning("Unspecified mode attribute: " + attr);
                continue;
            }
            if ("style".equalsIgnoreCase(kind)) {
                if ("short".equalsIgnoreCase(spec)) {
                    style = DateFormat.SHORT;
                } else if ("medium".equalsIgnoreCase(spec)) {
                    style = DateFormat.MEDIUM;
                } else if ("long".equalsIgnoreCase(spec)) {
                    style = DateFormat.LONG;
                } else if ("full".equalsIgnoreCase(spec)) {
                    style = DateFormat.FULL;
                } else {
                    log.warning("Unknown style mode: " + attr);
                }

            } else if ("timezone".equalsIgnoreCase(kind)) {
                timezone = TimeZone.getTimeZone(spec);

            } else if ("locale".equalsIgnoreCase(kind)) {
                String[] specs = Iterables.toArray(
                    Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().split(spec),
                    String.class);
                switch (specs.length) {
                case 1:
                    locale = new Locale(specs[0]);
                    break;
                case 2:
                    locale = new Locale(specs[0], specs[1]);
                    break;
                case 3:
                    locale = new Locale(specs[0], specs[1], specs[2]);
                    break;
                default:
                    log.warning("Too many arguments to locale: " + attr);
                    break;
                }

            } else if ("format".equalsIgnoreCase(kind)) {
                // TODO: allow commas in the format
                format = spec;

            } else {
                log.warning("Unknown mode attribute: " + attr);
            }
        }

        _format = (format != null)
            ? new SimpleDateFormat(format, locale)
            : createFormat(style, locale);
        _format.setTimeZone(timezone);
    }

    /**
     * Create the DateFormat to use, with the specified style and locale.
     */
    protected DateFormat createFormat (int style, Locale locale)
    {
        return DateFormat.getDateTimeInstance(style, style, locale);
    }

    /**
     * Update the border...
     */
    protected void updateBorder ()
    {
        Border b = getBorder();
        String title = (b instanceof TitledBorder) ? ((TitledBorder)b).getTitle() : null;

        if (!_invalid && (title != null)) {
            updateBorder(title); // in BasePropertyEditor
            return;
        }

        // set up a new border (or none)
        b = null;
        if (_invalid) {
            b = BorderFactory.createLineBorder(Color.RED, _highlighted ? 2 : 1);
            if (title != null) {
                b = BorderFactory.createTitledBorder(b, title);
            }
        }
        setBorder(b);
    }

    /** The text field. */
    protected JTextField _field;

    /** The DateFormat we're using for formatting/parsing. */
    protected DateFormat _format;

    /** Is the current text value invalid? */
    protected boolean _invalid;
}
