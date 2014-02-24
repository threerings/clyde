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

package com.threerings.opengl.gui.layout;

import java.util.Arrays;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Container;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Lays out components in a simple grid arrangement, wherein the width and height of each column
 * and row is defined by the widest preferred width and height of any component in that column and
 * row.
 *
 * <p> The table layout defaults to left horizontal alignment and top vertical alignment.
 */
public class TableLayout extends LayoutManager
{
    /** An enumeration class representing alignments. */
    public static class Alignment
    {
    }

    /** Left justifies the table contents within the container. */
    public static final Alignment LEFT = new Alignment();

    /** Centers the table contents within the container. */
    public static final Alignment CENTER = new Alignment();

    /** Right justifies the table contents within the container. */
    public static final Alignment RIGHT = new Alignment();

    /** Top justifies the table contents within the container. */
    public static final Alignment TOP = new Alignment();

    /** Bottom justifies the table contents within the container. */
    public static final Alignment BOTTOM = new Alignment();

    /** Divides the column space among the columns in proportion to their preferred size. This only
     * works with {@link #setHorizontalAlignment}. */
    public static final Alignment STRETCH = new Alignment();

    /**
     * Creates a table layout with the specified number of columns and a zero pixel gap between
     * rows and columns.
     */
    public TableLayout (int columns)
    {
        this(columns, 0, 0);
    }

    /**
     * Creates a table layout with the specified number of columns and the specifeid gap between
     * rows and columns.
     */
    public TableLayout (int columns, int rowgap, int colgap)
    {
        // A table must have at least a column
        columns = Math.max(1, columns);
        _fixedColumns = new boolean[columns];
        _rowgap = rowgap;
        _colgap = colgap;
    }

    /**
     * Configures the horizontal alignment (or stretching) of this table. This must be called
     * before the container using this layout is validated.
     */
    public TableLayout setHorizontalAlignment (Alignment align)
    {
        _halign = align;
        return this;
    }

    /**
     * Configures the vertical alignment of this table. This must be called before the container
     * using this layout is validated.
     */
    public TableLayout setVerticalAlignment (Alignment align)
    {
        _valign = align;
        return this;
    }

    /**
     * Configures a column as fixed or free. If a table layout is configured with
     * <code>STRETCH</code> horizontal alignment, extra space is divided up among all of the
     * non-fixed columns. All columns are non-fixed by default.
     */
    public TableLayout setFixedColumn (int column, boolean fixed)
    {
        _fixedColumns[column] = fixed;
        return this;
    }

    /**
     * Configures whether or not the table will force all rows to be a uniform size. This must be
     * called before the container using this layout is validated.
     */
    public TableLayout setEqualRows (boolean equalRows)
    {
        _equalRows = equalRows;
        return this;
    }

    // documentation inherited
    public Dimension computePreferredSize (Container target, int whint, int hhint)
    {
        Metrics metrics = computeMetrics(target, true, whint);
        int cx = (metrics.columnWidths.length-1) * _colgap;
        int rx = (computeRows(target, true)-1) * _rowgap;
        return new Dimension(sum(metrics.columnWidths) + cx, sum(metrics.rowHeights) + rx);
    }

    // documentation inherited
    public void layoutContainer (Container target)
    {
        Insets insets = target.getInsets();
        int availwid = target.getWidth() - insets.getHorizontal();

        Metrics metrics = computeMetrics(target, false, availwid);
        int totwidth = sum(metrics.columnWidths) + (metrics.columnWidths.length-1) * _colgap;
        int totheight = sum(metrics.rowHeights) + (computeRows(target, false)-1) * _rowgap;

        // account for our horizontal alignment
        int sx = insets.left;
        if (_halign == RIGHT) {
            sx += target.getWidth() - insets.getHorizontal() - totwidth;
        } else if (_halign == CENTER) {
            sx += (target.getWidth() - insets.getHorizontal() - totwidth)/2;
        }

        // account for our vertical alignment
        int y = insets.bottom + totheight;
        if (_valign == CENTER) {
            y += (target.getHeight() - insets.getVertical() - totheight)/2;
        } else if (_valign == TOP) {
            y = target.getHeight() - insets.top;
        }

        int row = 0, col = 0, x = sx;
        for (Component child : visibleChildren(target)) {
            int width = Math.min(metrics.columnWidths[col], availwid);
            child.setBounds(x, y - metrics.rowHeights[row], width, metrics.rowHeights[row]);
            x += (metrics.columnWidths[col] + _colgap);
            if (++col == metrics.columnWidths.length) {
                y -= (metrics.rowHeights[row] + _rowgap);
                row++;
                col = 0;
                x = sx;
            }
        }
    }

    protected Metrics computeMetrics (Container target, boolean preferred, int whint)
    {
        Metrics metrics = new Metrics();
        metrics.columnWidths = new int[_fixedColumns.length];

        int rows = computeRows(target, preferred);
        if (metrics.rowHeights == null || metrics.rowHeights.length != rows) {
            metrics.rowHeights = new int[rows];
        } else {
            Arrays.fill(metrics.rowHeights, 0);
        }
        Arrays.fill(metrics.columnWidths, 0);

        int row = 0, col = 0, maxrh = 0, count = 0;
        for (Component child : visibleChildren(target)) {
            Dimension psize = _pscache.get(child);
            if (psize == null || !child.isValid()) {
                _pscache.put(child, psize = child.getPreferredSize(whint, -1));
            }
            if (psize.height > metrics.rowHeights[row]) {
                metrics.rowHeights[row] = psize.height;
                if (maxrh < metrics.rowHeights[row]) {
                    maxrh = metrics.rowHeights[row];
                }
            }
            if (psize.width > metrics.columnWidths[col]) {
                metrics.columnWidths[col] = psize.width;
            }
            if (++col == metrics.columnWidths.length) {
                col = 0;
                row++;
            }
            count++;
        }

        // if we have more (or less?) components in our preferred size cache than are in the
        // container, flush the cache; this won't happen often (compared to invalidations which
        // happen all the damned time) and we don't want to leak memory if someone is removing old
        // components and adding new ones to a table-layout using container willy nilly
        if (_pscache.size() != count) {
            _pscache.clear();
        }

        // if we are stretching, adjust the column widths accordingly (however, no adjusting if
        // we're computing our preferred size)
        int naturalWidth;
        if (!preferred && _halign == STRETCH && (naturalWidth = sum(metrics.columnWidths)) > 0) {
            // sum the width of the non-fixed columns
            int freewid = 0;
            for (int ii = 0; ii < _fixedColumns.length; ii++) {
                if (!_fixedColumns[ii]) {
                    freewid += metrics.columnWidths[ii];
                }
            }

            // now divide up the extra space among said non-fixed columns
            int avail = target.getWidth() - target.getInsets().getHorizontal() -
                naturalWidth - (_colgap * (metrics.columnWidths.length-1));
            int used = 0;
            for (int ii = 0; ii < metrics.columnWidths.length; ii++) {
                if (_fixedColumns[ii]) {
                    continue;
                }
                int adjust = metrics.columnWidths[ii] * avail / freewid;
                metrics.columnWidths[ii] += adjust;
                used += adjust;
            }

            // add any rounding error to the first non-fixed column
            if (metrics.columnWidths.length > 0) {
                for (int ii = 0; ii < _fixedColumns.length; ii++) {
                    if (!_fixedColumns[ii]) {
                        metrics.columnWidths[ii] += (avail - used);
                        break;
                    }
                }
            }
        }

        // if we're equalizing rows, make all row heights the max
        if (_equalRows) {
            Arrays.fill(metrics.rowHeights, maxrh);
        }

        return metrics;
    }

    protected int computeRows (Container target, boolean preferred)
    {
        int ccount = Iterables.size(visibleChildren(target));
        int rows = ccount / _fixedColumns.length;
        if (ccount % _fixedColumns.length != 0) {
            rows++;
        }
        return rows;
    }

    protected Iterable<Component> visibleChildren (Container target)
    {
        return Iterables.filter(target.getChildren(), new Predicate<Component>() {
            public boolean apply (Component c) {
                return c.isVisible();
            }
        });
    }

    protected int sum (int[] values)
    {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    protected class Metrics
    {
        public int cachedHint = Integer.MIN_VALUE;
        public int[] columnWidths;
        public int[] rowHeights;
    }

    protected Alignment _halign = LEFT, _valign = TOP;
    protected boolean _equalRows;
    protected int _rowgap, _colgap;
    protected boolean[] _fixedColumns;

    // TODO: expire from this cache (rarely needed)
    protected Map<Component,Dimension> _pscache = Maps.newIdentityHashMap();
}
