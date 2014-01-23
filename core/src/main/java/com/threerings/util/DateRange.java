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

    /** Do we have a start time? */
    @Editable // see setter
    public boolean getHasStart ()
    {
        return (_startTime > Long.MIN_VALUE);
    }

    /** Do we have a start time? */
    @Editable(depends={"start_time", "stop_time", "has_stop"}, hgroup="a", weight=1)
    public void setHasStart (boolean hasStart)
    {
        if (hasStart != getHasStart()) {
            _startTime = hasStart ? aboutNow() : Long.MIN_VALUE;
            // TODO: sanitize
        }
    }

    /** The start time. */
    @Editable // see setter
    public long getStartTime ()
    {
        if (_startTime == 0) {
            _startTime = aboutNow();
        }
        return _startTime;
    }

    /** The start time. */
    @Editable(depends={"has_start", "stop_time", "has_stop"},
              editor="datetime", hgroup="a", weight=2)
    public void setStartTime (long time)
    {
        _startTime = time;
    }

    /** The stop time. */
    @Editable // see setter
    public long getStopTime ()
    {
        if (_stopTime == 0) {
            _stopTime = aboutNow();
        }
        return _stopTime;
    }

    /** The stop time. */
    @Editable(depends={"has_start", "start_time", "has_stop"},
              editor="datetime", hgroup="a", weight=3)
    public void setStopTime (long time)
    {
        _stopTime = time;
    }

    /** Do we have a stop time? */
    @Editable // see setter
    public boolean getHasStop ()
    {
        return (_stopTime < Long.MAX_VALUE);
    }

    /** Do we have a stop time? */
    @Editable(depends={"has_start", "start_time", "stop_time"}, hgroup="a", weight=4)
    public void setHasStop (boolean hasStop)
    {
        if (hasStop != getHasStop()) {
            _stopTime = hasStop ? aboutNow() : Long.MAX_VALUE;
            // TODO: sanitize
        }
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
