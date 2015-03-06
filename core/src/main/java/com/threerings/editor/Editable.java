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

package com.threerings.editor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a field or getter/setter method as an editable property.
 * If using a getter and setter, only the annotation on the setter method will
 * be checked for any specified attributes.
 */
@Documented
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

    /** The names of any properties upon whose values this property depends.
     * Note that because these are "property names" they don't match up to the variable names,
     * so the variable called "bigText" would be called "big_text" here in the depends section. */
    // TODO: fix, maybe add Property.getPlainName();
    String[] depends () default {};

    /** The custom editor to use (empty string for the default editor for the type). */
    String editor () default "";

    /** Generic editing "mode" whose interpretation depends on the editor. */
    String mode () default "";

    /** Whether or not this property should be displayed as a column when editing in table mode. */
    boolean column () default false;

    /** A translatable string describing the units of the property, if any. */
    String units () default "";

    /** A width hint for the editor of this property.
     * Typically this is the width (in text columns) of the editor for this property, although
     * the editors are free to interpret this in a way that makes sense for them, or disregard it.
     * The default value of 0 means that each editor can use a sensible width of its choosing.
     */
    int width () default 0;

    /** A height hint for the editor of this property.
     * Typically this is the height (in text rows) of the editor for this property, although
     * the editors are free to interpret this in a way that makes sense for them, or disregard it.
     * The default value of 0 means that each editor can use a sensible height of its choosing.
     */
    int height () default 0;

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

    /** For String, or list and array properties, the maximum size. */
    int maxsize () default +Integer.MAX_VALUE;

    /** For list and array properties, fix the size of the array/list. */
    boolean fixedsize () default false;

    /** For object properties, whether or not the property can be null. */
    boolean nullable () default false;

    /** Specify if the property should be collapsible (for those that aren't automatically). */
    boolean collapsible () default false;

    /** Specify if the property should not actually be editable. */
    boolean constant () default false;
}
