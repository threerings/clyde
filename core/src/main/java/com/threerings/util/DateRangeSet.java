package com.threerings.util;

import java.util.List;

import com.threerings.export.Exportable;

/**
 * Combines a List of DateRanges into a TimeActive.
 *
 * Note: getNextTime() may actually return a timestamp at which there is no
 * transition if the source ranges overlap. For our purposes this is OK.
 * This could be refactored to instead store things using guava's RangeSet,
 * which would combine overlapping ranges.
 */
public class DateRangeSet extends DeepObject
    implements TimeActive, Exportable
{
    /**
     * Construct a DateRangeSet with the specified ranges.
     */
    public DateRangeSet (List<DateRange> ranges)
    {
        // Presently, we just keep the source list without making a copy.
        // DateRanges themselves are mutable, so to be truly safely immutable
        // we'd need to copy the ranges themselves too...
        // Or: we reimplement with RangeSet.
        _ranges = ranges;
    }

    // from TimeActive
    public boolean isActive (long now)
    {
        for (DateRange range : _ranges) {
            if (range.isActive(now)) {
                return true;
            }
        }
        return false;
    }

    // from TimeActive
    public Long getNextTime (long now)
    {
        Long next = null;
        for (DateRange range : _ranges) {
            Long candidate = range.getNextTime(now);
            if ((candidate != null) && (next == null || (candidate < next))) {
                next = candidate;
            }
        }
        return next;
    }

    /** The underlying ranges. */
    protected List<DateRange> _ranges;
}
