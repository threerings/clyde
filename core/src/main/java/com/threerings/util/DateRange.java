//
// $Id$

package com.threerings.util;

import com.google.common.base.Objects;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * An Editable date range.
 * Honors the 'mode' attribute and interprets it according to the DateTimeEditor.
 */
public class DateRange extends DeepObject
    implements Streamable, Exportable, Validatable
{
    /**
     * Is this date range active?
     */
    public boolean isActive (long now)
    {
        return (now >= _startTime) && (now < _stopTime);
    }

    // from Validatable
    public boolean isValid ()
    {
        return (_startTime <= _stopTime);
    }

    /**
     * Return the next start/stop time, or null for none ('now' is past 'stopTime').
     */
    public Long getNextTime (long now)
    {
        // if we're misconfigured (start after stop) or we're already after the stop time
        return ((_startTime >= _stopTime) || (now >= _stopTime))
            ? null // return null
            // otherwise, return that which is next
            : (now < _startTime)
                ? _startTime
                : _stopTime;
    }

    /** The start time. */
    @Editable // see setter
    public Long getStartTime ()
    {
        return (_startTime == Long.MIN_VALUE) ? null : _startTime;
    }

    /** The start time. */
    @Editable(editor="datetime", nullable=true, mode=Editable.INHERIT_STRING, hgroup="a", weight=1)
    public void setStartTime (Long time)
    {
        _startTime = (time == null) ? Long.MIN_VALUE : time;
    }

    /** The stop time. */
    @Editable // see setter
    public Long getStopTime ()
    {
        return (_stopTime == Long.MAX_VALUE) ? null : _stopTime;
    }

    /** The stop time. */
    @Editable(editor="datetime", nullable=true, mode=Editable.INHERIT_STRING, hgroup="a", weight=2)
    public void setStopTime (Long time)
    {
        _stopTime = (time == null) ? Long.MAX_VALUE : time;
    }

    /**
     * Get the start time as a primitive long. If there's no start time, Long.MIN_VALUE is
     * returned.
     */
    public long getStartStamp ()
    {
        return _startTime;
    }

    /**
     * Get the stop time as a primitive long. If there's no stop time, Long.MAX_VALUE is returned.
     */
    public long getStopStamp ()
    {
        return _stopTime;
    }

    @Override
    public String toString ()
    {
        return Objects.toStringHelper(this)
            .add("startTime", getStartTime())
            .add("stopTime", getStopTime())
            .toString();
    }

    @Override
    public DateRange clone ()
    {
        return (DateRange)super.clone();
    }

    /** Internal storage for our start and end times. */
    protected long _startTime, _stopTime;
}
