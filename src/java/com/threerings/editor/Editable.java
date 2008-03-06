//
// $Id$

package com.threerings.editor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a field or getter/setter method as an editable property.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Editable
{
    /** Indicates that the (double) attribute should be inherited from the parent property. */
    public static final double INHERIT_DOUBLE = Double.NEGATIVE_INFINITY;

    /** Indicates that the (integer) attribute should be inherited from the parent property. */
    public static final int INHERIT_INTEGER = Integer.MIN_VALUE;

    /** Indicates that the (string) attribute should be inherited from the parent property. */
    public static final String INHERIT_STRING = "%INHERIT%";

    /** The category under which property falls (empty string for the default category). */
    String category () default "";

    /** The weight used to determine the display order. */
    double weight () default 0.0;

    /** Neighboring properties with the same hgroup will be grouped into horizontal sublayouts. */
    String hgroup () default "";

    /** The custom editor to use (empty string for the default editor for the type). */
    String editor () default "";

    /** Generic editing "mode" whose interpretation depends on the editor. */
    String mode () default "";

    /** A translatable string describing the units of the property, if any. */
    String units () default "";

    /** For string properties, the width of the text field. */
    int width () default 10;

    /** For numeric properties, the minimum value. */
    double min () default -Double.MAX_VALUE;

    /** For numeric properties, the maximum value. */
    double max () default +Double.MAX_VALUE;

    /** For numeric properties, the step size. */
    double step () default 1.0;

    /** For numeric properties, the value scale. */
    double scale () default 1.0;

    /** For list and array properties, the minimum size. */
    int minsize () default 0;

    /** For list and array properties, the maximum size. */
    int maxsize () default +Integer.MAX_VALUE;

    /** For object properties, the list of available subtypes. */
    Class[] types () default {};

    /** For object properties, whether or not the property can be null. */
    boolean nullable () default true;
}
