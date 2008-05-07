//
// $Id$

package com.threerings.packit.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.Communicator;

/**
 * Extends {@link Communicator} with Packit-specific bits.
 */
public abstract class PackitCommunicator extends Communicator
{
    /**
     * Creates a new communicator instance which is associated with the supplied client.
     */
    public PackitCommunicator (Client client)
    {
        super(client);
    }
}
