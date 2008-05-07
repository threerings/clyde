//
// $Id$

package com.threerings.packit.net;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import com.threerings.io.ByteBufferInputStream;
import com.threerings.io.ByteBufferOutputStream;
import com.threerings.io.UnreliableObjectInputStream;
import com.threerings.io.UnreliableObjectOutputStream;

/**
 * Handles one side of a connection.
 */
public class PackitSocket
{
    /** The maximum expected size of any datagram. */
    public static final int MAX_DATAGRAM_SIZE = 65536;

    /**
     * Creates a socket and attempts to connect to the specified host and port.
     */
    public PackitSocket (String host, int port)
        throws IOException
    {
        this(host, port, null, 0);
    }

    /**
     * Creates a socket and attempts to connect to the specified host and port.
     */
    public PackitSocket (String host, int port, InetAddress localAddr, int localPort)
        throws IOException
    {
        this(InetAddress.getByName(host), port, localAddr, localPort);
    }

    /**
     * Creates a socket and attempts to connect to the specified address and port.
     */
    public PackitSocket (InetAddress address, int port)
        throws IOException
    {
        this(address, port, null, 0);
    }

    /**
     * Creates a socket and attempts to connect to the specified address and port.
     */
    public PackitSocket (InetAddress address, int port, InetAddress localAddr, int localPort)
        throws IOException
    {
        this();
        bind(new InetSocketAddress(localAddr, localPort));
        connect(new InetSocketAddress(address, port));
    }

    /**
     * Creates an unconnected socket.
     */
    public PackitSocket ()
        throws IOException
    {
        _channel = DatagramChannel.open();
        _buf = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
        _bin = new ByteBufferInputStream(_buf);
        _oin = new UnreliableObjectInputStream(_bin);
        _bout = new ByteBufferOutputStream();
        _oout = new UnreliableObjectOutputStream(_bout);
    }

    /**
     * Binds the socket.
     */
    public void bind (SocketAddress bindpoint)
        throws IOException
    {
        // bind the underlying channel
        _channel.socket().bind(bindpoint);
    }

    /**
     * Attempts to connect this socket to the server.
     */
    public void connect (SocketAddress endpoint)
        throws IOException
    {
        // connect the channel
        _channel.connect(endpoint);
    }

    /**
     * Sends a datagram through the socket.
     */
    public void send (Datagram datagram)
        throws IOException
    {
        _oout.getMetadataClasses().clear();

        // reset the buffer stream and encode the datagram
        _bout.reset();
        _oout.writeObject(datagram);

        // flip the buffer and write the datagram
        if (_target == null) {
            _channel.write(_bout.flip());
        } else {
            _channel.send(_bout.flip(), _target);
        }
    }

    /**
     * Constructor for sockets created by {@link PackitServerSocket} that create their own
     * channels.
     */
    protected PackitSocket (
        PackitServerSocket parent, SocketAddress target, Datagram datagram,
        ByteBufferInputStream bin, UnreliableObjectInputStream oin,
        ByteBufferOutputStream bout, InetAddress bindAddr)
            throws IOException
    {
        this(parent, null, datagram, bin, oin, bout, DatagramChannel.open());
        _channel.socket().bind(new InetSocketAddress(bindAddr, 0));
        _channel.connect(target);
    }

    /**
     * Constructor for sockets created by {@link PackitServerSocket} that share a channel.
     */
    protected PackitSocket (
        PackitServerSocket parent, SocketAddress target, Datagram datagram,
        ByteBufferInputStream bin, UnreliableObjectInputStream oin,
        ByteBufferOutputStream bout, DatagramChannel channel)
    {
        _parent = parent;
        _target = target;
        _channel = channel;
        _buf = bin.getBuffer();
        _bin = bin;
        _oin = oin;
        _oout = new UnreliableObjectOutputStream(_bout = bout);

        System.out.println("first: " + datagram);
    }

    /**
     * Decodes the datagram in the buffer.
     */
    protected void decode ()
        throws IOException
    {
        // decode the datagram
        Datagram datagram;
        try {
            datagram = (Datagram)_oin.readObject();
        } catch (Exception e) {
            throw new IOException("Error decoding datagram [error=" + e + "].");
        }

        System.out.println("decoded: " + datagram);
    }

    /** The server socket that spawned this socket, if any. */
    protected PackitServerSocket _parent;

    /** The address to which we send packets. */
    protected SocketAddress _target;

    /** The underlying datagram channel. */
    protected DatagramChannel _channel;

    /** Used to decode incoming messages. */
    protected ByteBuffer _buf;
    protected ByteBufferInputStream _bin;
    protected UnreliableObjectInputStream _oin;

    /** Used to encode outgoing messages. */
    protected ByteBufferOutputStream _bout;
    protected UnreliableObjectOutputStream _oout;
}
