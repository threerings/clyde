//
// $Id$

package com.threerings.editor.util;

import com.threerings.editor.Editable;
import com.threerings.editor.Property;

/**
 * Some general utility methods relating to editable properties.
 */
public class PropertyUtil
{
    /**
     * Finds the mode string by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static String getMode (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            String mode = lineage[ii].getMode();
            if (!Editable.INHERIT_STRING.equals(mode)) {
                return mode;
            }
        }
        return "";
    }

    /**
     * Finds the units string by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static String getUnits (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            String units = lineage[ii].getUnits();
            if (!Editable.INHERIT_STRING.equals(units)) {
                return units;
            }
        }
        return "";
    }

    /**
     * Finds the minimum value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getMinimum (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double min = lineage[ii].getMinimum();
            if (min != Editable.INHERIT_DOUBLE) {
                return min;
            }
        }
        return -Double.MAX_VALUE;
    }

    /**
     * Finds the maximum value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getMaximum (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double max = lineage[ii].getMaximum();
            if (max != Editable.INHERIT_DOUBLE) {
                return max;
            }
        }
        return +Double.MAX_VALUE;
    }

    /**
     * Finds the step value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getStep (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double step = lineage[ii].getStep();
            if (step != Editable.INHERIT_DOUBLE) {
                return step;
            }
        }
        return 1.0;
    }

    /**
     * Finds the scale value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getScale (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double scale = lineage[ii].getScale();
            if (scale != Editable.INHERIT_DOUBLE) {
                return scale;
            }
        }
        return 1.0;
    }

    /**
     * Finds the minimum size by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static int getMinSize (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            int min = lineage[ii].getMinSize();
            if (min != Editable.INHERIT_INTEGER) {
                return min;
            }
        }
        return 0;
    }

    /**
     * Finds the maximum size by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static int getMaxSize (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            int max = lineage[ii].getMaxSize();
            if (max != Editable.INHERIT_INTEGER) {
                return max;
            }
        }
        return +Integer.MAX_VALUE;
    }
}

