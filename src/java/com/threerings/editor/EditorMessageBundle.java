//
// $Id$

package com.threerings.editor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the message bundle to use when translating type names, field names, enum constants,
 * and other editor bits.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PACKAGE })
public @interface EditorMessageBundle
{
    /** The default message bundle. */
    public static final String DEFAULT = "editor.default";

    /** The name of the message bundle. */
    String value ();
}
