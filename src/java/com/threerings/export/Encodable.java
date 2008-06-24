//
// $Id$

package com.threerings.export;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An interface for objects that can be encoded to and decoded from strings and binary streams.
 */
public interface Encodable extends Exportable
{
    /**
     * Returns a string representation of this object.
     */
    public String encodeToString ();

    /**
     * Initializes this object with the contents of the specified string.
     */
    public void decodeFromString (String string)
        throws Exception;

    /**
     * Encodes this object to the specified stream.
     */
    public void encodeToStream (DataOutputStream out)
        throws IOException;

    /**
     * Initializes this object with data read from the specified stream.
     */
    public void decodeFromStream (DataInputStream in)
        throws IOException;
}
