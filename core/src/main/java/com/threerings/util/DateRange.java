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
    implements Streamable, Exportable
{
    /**
     * Is this date range active?
     */
    public boolean isActive (long now)
    {
        return (now >= _startTime) && (now < _stopTime);
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
    @Editable(depends={"stop_time"}, editor="datetime", nullable=true, mode="%INHERIT%",
            hgroup="a", weight=1)
    public void setStartTime (Long time)
    {
        _startTime = (time == null) ? Long.MIN_VALUE : time;
        _stopTime = Math.max(_stopTime, _startTime);
    }

    /** The stop time. */
    @Editable // see setter
    public Long getStopTime ()
    {
        return (_stopTime == Long.MAX_VALUE) ? null : _stopTime;
    }

    /** The stop time. */
    @Editable(depends={"start_time"}, editor="datetime", nullable=true, mode="%INHERIT%",
            hgroup="a", weight=2)
    public void setStopTime (Long time)
    {
        _stopTime = (time == null) ? Long.MAX_VALUE : time;
        _startTime = Math.min(_startTime, _stopTime);
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
