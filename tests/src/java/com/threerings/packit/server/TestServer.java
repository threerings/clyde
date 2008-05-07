//
// $Id$

package com.threerings.packit.server;

import com.threerings.packit.net.PackitServerSocket;
import com.threerings.packit.net.PackitSocket;

/**
 * A simple test server.
 */
public class TestServer
{
    /**
     * Program entry point.
     */
    public static void main (String[] args)
        throws Exception
    {
        // create and bind the server socket
        PackitServerSocket ssocket = new PackitServerSocket(4224);

        // listen for connections
        while (true) {
            ssocket.receive();
        }
    }
}
