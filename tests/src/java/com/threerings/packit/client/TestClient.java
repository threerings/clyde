//
// $Id$

package com.threerings.packit.client;

import com.threerings.packit.net.Datagram;
import com.threerings.packit.net.PackitSocket;

/**
 * A simple test client.
 */
public class TestClient
{
    /**
     * Program entry point.
     */
    public static void main (String[] args)
        throws Exception
    {
        // connect to server
        PackitSocket socket = new PackitSocket("localhost", 4224);

        // send datagrams
        while (true) {
            Datagram datagram = new Datagram();
            datagram.timestamp = (int)(System.currentTimeMillis() & 0xFFFFFFFF);
            socket.send(datagram);
            Thread.sleep(1000);
        }
    }
}
