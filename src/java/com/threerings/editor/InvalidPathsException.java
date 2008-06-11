//
// $Id$

package com.threerings.editor;

/**
 * Thrown to indicate that none of the paths provided to {@link PathProperty} are valid.
 */
public class InvalidPathsException extends Exception
{
    /**
     * Creates a new exception with the supplied message.
     */
    public InvalidPathsException (String message)
    {
        super(message);
    }
}
