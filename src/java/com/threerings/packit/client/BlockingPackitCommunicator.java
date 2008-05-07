//
// $Id$

package com.threerings.packit.client;

import java.io.IOException;

import com.samskivert.util.LoopingThread;

import com.threerings.presents.client.Client;
import com.threerings.presents.net.UpstreamMessage;

import com.threerings.packit.net.PackitSocket;

import static com.threerings.packit.Log.*;

/**
 * Like {@link com.threerings.presents.client.BlockingCommunicator}, but for Packit channels.
 */
public class BlockingPackitCommunicator extends PackitCommunicator
{
    /**
     * Creates a new communicator instance which is associated with the supplied client.
     */
    public BlockingPackitCommunicator (Client client)
    {
        super(client);
    }

    @Override // documentation inherited
    public void logon ()
    {
        // make sure things are copacetic
        if (_writer != null) {
            throw new RuntimeException("Communicator already started.");
        }

        // start up the writer thread.  it will start up the reader thread if possible
        _writer = new Writer();
        _writer.start();
    }

    @Override // documentation inherited
    public void logoff ()
    {
    }

    @Override // documentation inherited
    public void postMessage (UpstreamMessage msg)
    {
    }

    @Override // documentation inherited
    public void setClassLoader (ClassLoader loader)
    {
        _loader = loader;
    }

    @Override // documentation inherited
    public long getLastWrite ()
    {
        return _lastWrite;
    }

    /**
     * Callback called by the reader thread when it goes away.
     */
    protected synchronized void readerDidExit ()
    {
        // clear out our reader reference
        _reader = null;
    }

    /**
     * Callback called by the writer thread when it goes away.
     */
    protected synchronized void writerDidExit ()
    {
        // clear out our writer reference
        _writer = null;
    }

    /**
     * Makes a note of the time at which we last communicated with the server.
     */
    protected synchronized void updateWriteStamp ()
    {
        _lastWrite = System.currentTimeMillis();
    }

    /**
     * The message writing process.
     */
    protected class Writer extends LoopingThread
    {
        @Override // documentation inherited
        protected void willStart ()
        {
            // try to connect to the server
            try {
                connect();
            } catch (Exception e) {
                _logonError = e;
                shutdown();
            }
        }

        /**
         * Attempts to connect to the server.
         */
        protected void connect ()
            throws IOException
        {

        }

        @Override // documentation inherited
        protected void didShutdown ()
        {
            // let the communicator know when we finally go away
            writerDidExit();
        }
    }

    /**
     * The message reading process.
     */
    protected class Reader extends LoopingThread
    {
        @Override // documentation inherited
        protected void didShutdown ()
        {
            // let the communicator know when we finally go away
            readerDidExit();
        }
    }

    /** The writer process. */
    protected Writer _writer;

    /** The reader process. */
    protected Reader _reader;

    /** The underlying socket. */
    protected PackitSocket _socket;

    /** The time of our last write. */
    protected long _lastWrite;

    /** An error encountered during logon. */
    protected Exception _logonError;

    /** A custom classloader for reading and writing objects. */
    protected ClassLoader _loader;
}
