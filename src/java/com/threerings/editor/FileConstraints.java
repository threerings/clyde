//
// $Id$

package com.threerings.editor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contains constraints on {@link java.io.File} fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface FileConstraints
{
    /** The translatable description. */
    String description ();

    /** The allowable file extensions. */
    String[] extensions ();

    /** The directory preference key (if empty, use current directory). */
    String directory () default "";
}
