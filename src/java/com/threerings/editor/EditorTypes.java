//
// $Id$

package com.threerings.editor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the classes available for selection in the editor.  Can be used both on superclasses
 * and on editable properties.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface EditorTypes
{
    /** The default type label. */
    public static final String DEFAULT_LABEL = "type";

    /** The subtypes from which to choose. */
    Class[] value ();

    /** An optional configuration key whose value represents other available classes. */
    String key () default "";

    /** The label to use for the type chooser. */
    String label () default DEFAULT_LABEL;
}
