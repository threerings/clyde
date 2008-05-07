//
// $Id$

package com.threerings.packit.net;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.util.ArrayList;
import java.util.HashMap;

import com.threerings.io.ByteBufferInputStream;
import com.threerings.io.ByteBufferOutputStream;
import com.threerings.io.UnreliableObjectInputStream;

import static com.threerings.packit.Log.*;

/**
 * Accepts connections from clients and creates corresponding {@link PackitSocket}s to handle them.
 */
public class PackitServerSocket
{
    /**
     * Creates an unbound server socket.
     */
    public PackitServerSocket ()
        throws IOException
    {
        this(-1);
    }

    /**
     * Creates a server socket bound to the specified port.
     *
     * @param port the port to bind to (-1 to leave the socket unbound, 0 to use any available
     * port).
     */
    public PackitServerSocket (int port)
        throws IOException
    {
        this(port, null, Integer.MAX_VALUE);
    }

    /**
     * Creates a server socket bound to the specified port and address.
     *
     * @param port the port to bind to (-1 to leave the socket unbound, 0 to use any available
     * port).
     * @param bindAddr the address to bind to (<code>null</code> to bind to any/all local
     * addresses).
     * @param clientsPerChannel the maximum number of clients to serve on any single channel.  The
     * special value {@link Integer#MAX_VALUE} will serve all clients through the server channel.
     */
    public PackitServerSocket (int port, InetAddress bindAddr, int clientsPerChannel)
        throws IOException
    {
        _channel = DatagramChannel.open();
        _clientsPerChannel = clientsPerChannel;
        _buf = ByteBuffer.allocate(PackitSocket.MAX_DATAGRAM_SIZE);
        _bin = new ByteBufferInputStream(_buf);
        _bout = new ByteBufferOutputStream();

        // create the socket mapping if we will be communicating through the server channel
        if (_clientsPerChannel == Integer.MAX_VALUE) {
            _sockets = new HashMap<SocketAddress, PackitSocket>();

        // or the shared channel list if clients will share channels
        } else if (_clientsPerChannel > 1) {
            _channels = new ArrayList<SharedChannel>();
        }

        // bind the port if specified
        if (port >= 0) {
            bind(new InetSocketAddress(bindAddr, port));
        }
    }

    /**
     * Binds the server socket.
     */
    public void bind (SocketAddress bindpoint)
        throws IOException
    {
        // bind the underlying channel
        _channel.socket().bind(bindpoint);
    }

    /**
     * Reads a datagram from the underlying channel and either creates a new {@link PackitSocket}
     * to handle the connection or forwards the datagram to a child socket.
     */
    public PackitSocket receive ()
        throws IOException
    {
        // clear the buffer and read the datagram
        _buf.clear();
        SocketAddress source = _channel.receive(_buf);
        _buf.flip();

        // make sure we actually received a packet
        if (source == null) {
            log.warning("No datagram received on PackitServerSocket.");
            return null;
        }

        // if it corresponds to a child socket, forward it
        if (_sockets != null) {
            PackitSocket socket = _sockets.get(source);
            if (socket != null) {
                socket.decode();
                return null;
            }
        }

        // attempt to decode the datagram
        UnreliableObjectInputStream oin = new UnreliableObjectInputStream(_bin);
        Datagram datagram;
        try {
            datagram = (Datagram)oin.readObject();
        } catch (Exception e) {
            throw new IOException("Error decoding datagram [error=" + e + "]");
        }

        // if we are handling connections through the server socket, create a child socket
        if (_sockets != null) {
            PackitSocket socket = new PackitSocket(
                this, source, datagram, _bin, oin, _bout, _channel);
            _sockets.put(source, socket);
            return socket;
        }

        // if we create a single channel for each client, do that
        if (_channels == null) {
            PackitSocket socket = new PackitSocket(
                this, source, datagram, _bin, oin, _bout, _channel.socket().getLocalAddress());
            return socket;
        }

        // otherwise, look for a shared channel with some space on it
        SharedChannel channel = null;
        for (int ii = 0, nn = _channels.size(); ii < nn; ii++) {
            SharedChannel schannel = _channels.get(ii);
            if (schannel.sockets.size() < _clientsPerChannel) {
                channel = schannel;
                break;
            }
        }
        if (channel == null) {
            // must create a new one
            _channels.add(channel = new SharedChannel(_channel.socket().getLocalAddress()));
        }
        PackitSocket socket = new PackitSocket(
            this, source, datagram, _bin, oin, _bout, channel.channel);
        channel.sockets.put(source, socket);
        return socket;
    }

    /**
     * Closes the socket.
     */
    public void close ()
        throws IOException
    {
        _channel.socket().close();
    }

    /**
     * Represents a channel shared between several clients.
     */
    protected class SharedChannel
    {
        /** The underlying datagram channel. */
        public DatagramChannel channel;

        /** Sockets sharing this channel. */
        public HashMap<SocketAddress, PackitSocket> sockets =
            new HashMap<SocketAddress, PackitSocket>();

        public SharedChannel (InetAddress bindAddr)
            throws IOException
        {
            // open a channel bound to any available port
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(bindAddr, 0));
        }

        /**
         * Reads a datagram from the channel and forwards it to the appropriate socket.
         */
        public void receive ()
            throws IOException
        {
            // clear the buffer and read the datagram
            _buf.clear();
            SocketAddress source = channel.receive(_buf);
            _buf.flip();

            // make sure we actually received a packet
            if (source == null) {
                log.warning("No datagram received on SharedChannel.");
                return;
            }

            // look up the destination
            PackitSocket socket = sockets.get(source);
            if (source == null) {
                log.warning("Received datagram from unknown source on SharedChannel [source=" +
                    source + ", local=" + channel.socket().getLocalSocketAddress() + "].");
                return;
            }

            // let the socket decode the datagram
            socket.decode();
        }
    }

    /** The underlying datagram channel. */
    protected DatagramChannel _channel;

    /** The number of clients to serve per channel. */
    protected int _clientsPerChannel;

    /** The datagram buffer. */
    protected ByteBuffer _buf;

    /** Used to decode datagrams. */
    protected ByteBufferInputStream _bin;

    /** Used to encode datagrams. */
    protected ByteBufferOutputStream _bout;

    /** If we are serving all clients through the server channel, this maps their addresses to
     * the child sockets. */
    protected HashMap<SocketAddress, PackitSocket> _sockets;

    /** The list of shared channels. */
    protected ArrayList<SharedChannel> _channels;
}
