//
// $Id$

package com.threerings.util;

import java.util.Calendar;

import com.samskivert.util.Calendars;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * An Editable date range.
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
        if (_startTime == 0) {
            _startTime = aboutNow();
        }
        return (_startTime == Long.MIN_VALUE) ? null : _startTime;
    }

    /** The start time. */
    @Editable(depends={"stop_time"}, editor="datetime", nullable=true, hgroup="a", weight=2)
    public void setStartTime (Long time)
    {
        _startTime = (time == null) ? Long.MIN_VALUE : time;
        _stopTime = Math.max(_stopTime, _startTime);
    }

    /** The stop time. */
    @Editable // see setter
    public Long getStopTime ()
    {
        if (_stopTime == 0) {
            _stopTime = aboutNow();
        }
        return (_stopTime == Long.MAX_VALUE) ? null : _stopTime;
    }

    /** The stop time. */
    @Editable(depends={"start_time"}, editor="datetime", nullable=true, hgroup="a", weight=3)
    public void setStopTime (Long time)
    {
        _stopTime = (time == null) ? Long.MAX_VALUE : time;
        _startTime = Math.min(_startTime, _stopTime);
    }

    /**
     * Get a time that's around about now, but at the next hour break for cleanliness.
     */
    protected long aboutNow ()
    {
        return Calendars.now()
            .addHours(1)
            .set(Calendar.MILLISECOND, 0)
            .set(Calendar.SECOND, 0)
            .set(Calendar.MINUTE, 0)
            .toTime();
    }

    /** Internal storage for our start and end times. */
    protected long _startTime, _stopTime;
}
