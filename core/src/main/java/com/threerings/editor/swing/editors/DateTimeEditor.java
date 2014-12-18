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
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import com.samskivert.util.Calendars;

import com.threerings.util.MessageBundle;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.log;

/**
 * Editor for date-time, with included subclasses for just date or just time.
 * Supports Date, Long, or long fields.
 * When editing long or Long, a read value of 0 will immediately be rewritten
 * with a sensible value. No other editor will do this. Viewing a blank alters it.
 * Note that because of this, you can't save the value of the epoch at UTC. 1 ms will be added.
 *
 * The 'mode' of the @Editable annotation can contain a comma separated list of attributes:
 * - mode  (datetime, date, or time)
 * - style (SHORT, MEDIUM, LONG, FULL)
 * - timezone (any timezone)
 * - locale (up to three specifiers separated by spaces: language country variant)
 * - format (to specify a SimpleDateFormat format, instead of specifying 'style')
 * - nullKey to specify the translation key to use for null values (defaults to 'm.null_value')
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
        @Override protected Mode getDefaultMode ()
        {
            return Mode.DATE;
        }
    }

    /**
     * A subclass that is time-only.
     */
    public static class TimeOnlyEditor extends DateTimeEditor
    {
        @Override protected Mode getDefaultMode ()
        {
            return Mode.TIME;
        }
    }

    @Override
    public void actionPerformed (ActionEvent event)
    {
        Object source = event.getSource();
        if (source == _field) {
            canonicalize();

        } else {
            super.actionPerformed(event);
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
        boolean newInvalid;
        try {
            Object oldVal = _property.get(_object);
            Object newVal;
            if (("".equals(text) || _nullStr.equals(text)) && _property.nullable()) {
                newVal = null;

            } else {
                Date d = _format.parse(text);
                if (oldVal instanceof Date) {
                    newVal = d;
                } else {
                    long time = d.getTime();
                    if (time == 0) {
                        // because we rewrite when read: save something non-zero
                        time = 1;
                    }
                    newVal = time;
                }
            }
            if (!Objects.equal(newVal, oldVal)) {
                _property.set(_object, newVal);
                fireStateChanged();
            }
            //log.info(   "Parse valid", "text", text);
            newInvalid = false;

        } catch (Exception e) {
            //log.warning("Parse error", "text", e);
            newInvalid = true;
        }

        if (newInvalid != _invalid) {
            _invalid = newInvalid;
            updateBorder();
        }
    }

    // from FocusListener
    public void focusGained (FocusEvent e) {} // nada

    // from FocusListener
    public void focusLost (FocusEvent e)
    {
        canonicalize();
    }

    /**
     * If our last edit was good, redisplay the value by re-reading it.
     */
    public void canonicalize ()
    {
        if (!_invalid) {
            update();
        }
    }

    @Override
    public void update ()
    {
        // remove ourselves as a listener during update
        _field.getDocument().removeDocumentListener(this);
        Object prop = _property.get(_object);
        boolean rewrite = false;
        if (prop instanceof Long && (0 == ((Long)prop).longValue())) {
            rewrite = true;
            prop = Calendars.now()
                .addHours(1)
                .set(Calendar.MILLISECOND, 0)
                .set(Calendar.SECOND, 0)
                .set(Calendar.MINUTE, 0)
                .toTime();
        }
        _field.setText((prop == null) ? _nullStr : _format.format(prop));
        _field.getDocument().addDocumentListener(this);
        if (rewrite) {
            changedUpdate(null);
        }
    }

    @Override
    protected void didInit ()
    {
        // create the widgets
        add(new JLabel(getPropertyLabel() + ":"));
        _field = new JTextField(_property.getWidth(12));
        add(_field);
        _field.getDocument().addDocumentListener(this);
        _field.addFocusListener(this);
        _field.addActionListener(this);

        configureMode();
        addUnits(this);

        // disable the field if we're in constant mode
        _field.setEnabled(!_property.getAnnotation().constant());
    }

    @Override
    protected String getUnits ()
    {
        String units = super.getUnits();
        return "".equals(units)
            ? MessageBundle.taint(_format.getTimeZone().getID())
            : units;
    }

    /**
     * Configure the DateFormat we'll be using.
     */
    protected void configureMode ()
    {
        // create a mutable map of the args: we'll remove them as we deal with each one
        Map<String, String> modeArgs = Maps.newHashMap(Splitter.on(',')
            .trimResults()
            .omitEmptyStrings()
            .withKeyValueSeparator('=')
            .split(getMode()));

        // read the mode first
        String modeSpec = modeArgs.remove("mode");
        Mode mode = getDefaultMode();
        if (modeSpec != null) {
            for (Mode m : Mode.values()) {
                if (modeSpec.equalsIgnoreCase(m.name())) {
                    mode = m;
                    break;
                }
            }
        }

        // set up our defaults
        int style = DateFormat.SHORT;
        TimeZone timezone = TimeZone.getDefault();
        Locale locale = Locale.getDefault();
        String format = getDefaultFormat(mode);

        // read the style which may reset the format
        String styleSpec = modeArgs.remove("style");
        if (styleSpec != null) {
            // reset the format if they specify a style
            format = null;

            if ("short".equalsIgnoreCase(styleSpec)) {
                style = DateFormat.SHORT;
            } else if ("medium".equalsIgnoreCase(styleSpec)) {
                style = DateFormat.MEDIUM;
            } else if ("long".equalsIgnoreCase(styleSpec)) {
                style = DateFormat.LONG;
            } else if ("full".equalsIgnoreCase(styleSpec)) {
                style = DateFormat.FULL;
            } else {
                log.warning("Unknown style mode: " + styleSpec);
            }
        }

        // possibly override the format
        format = Objects.firstNonNull(modeArgs.remove("format"), format);

        // timezone
        String tzSpec = modeArgs.remove("timezone");
        if (tzSpec != null) {
            timezone = TimeZone.getTimeZone(tzSpec);
        }

        // locale
        String localeSpec = modeArgs.remove("locale");
        if (localeSpec != null) {
            String[] specs = Iterables.toArray(
                Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().split(localeSpec),
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
                log.warning("Too many arguments to locale: " + localeSpec);
                break;
            }
        }

        // string to use for null
        String nullKeySpec = Objects.firstNonNull(modeArgs.remove("nullKey"), "m.null_value");
        MessageBundle msgs = _msgmgr.getBundle(_property.getMessageBundle());
        if (msgs.exists(nullKeySpec)) {
            _nullStr = msgs.get(nullKeySpec);
        }

        // warn about any unparsed mode arguments
        if (!modeArgs.isEmpty()) {
            log.warning("Unknown mode arguments: " + modeArgs);
        }

        // now make the format
        _format = (format != null)
            ? new SimpleDateFormat(format, locale)
            : createFormat(mode, style, locale);
        _format.setTimeZone(timezone);
        _format.setLenient(false);
    }

    /**
     * Get the default mode for this form of the editor.
     */
    protected Mode getDefaultMode ()
    {
        return Mode.DATETIME;
    }

    /**
     * Create the DateFormat to use, with the specified style and locale.
     */
    protected DateFormat createFormat (Mode mode, int style, Locale locale)
    {
        switch (mode) {
        default: return DateFormat.getDateTimeInstance(style, style, locale);
        case DATE: return DateFormat.getDateInstance(style, locale);
        case TIME: return DateFormat.getTimeInstance(style, locale);
        }
    }

    /**
     * Get the default format to pass to a SimpleDateFormat.
     */
    protected String getDefaultFormat (Mode mode)
    {
        switch (mode) {
        default: return "yyyy-MM-dd HH:mm";
        case DATE: return "yyyy-MM-dd";
        case TIME: return "HH:mm:ss";
        }
    }

    /** The possible modes in which we can operate. */
    protected enum Mode
    {
        DATETIME,
        DATE,
        TIME;
    }

    /** The text field. */
    protected JTextField _field;

    /** The string we use for null values. */
    protected String _nullStr = "";

    /** The DateFormat we're using for formatting/parsing. */
    protected DateFormat _format;
}
