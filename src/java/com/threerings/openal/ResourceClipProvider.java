//
// $Id$

package com.threerings.openal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.WaveData;

import com.threerings.resource.ResourceManager;

/**
 * Loads clips from resources.
 */
public class ResourceClipProvider
    implements ClipProvider
{
    /**
     * Creates a new resource clip provider that will obtain resources from the given manager.
     */
    public ResourceClipProvider (ResourceManager rsrcmgr)
    {
        _rsrcmgr = rsrcmgr;
    }

    /**
     * Loads the specified clip from the appropriate source.
     */
    public Clip loadClip (String path)
        throws IOException
    {
        InputStream in = _rsrcmgr.getResource(path);
        if (path.endsWith(".ogg")) {
            return loadOggClip(in);
        }
        WaveData data = WaveData.create(in);
        if (data == null) {
            throw new IOException("Error loading " + path);
        }
        return new Clip(data);
    }

    /**
     * Loads an Ogg sound clip.
     */
    protected static Clip loadOggClip (InputStream in)
        throws IOException
    {
        OggStreamDecoder decoder = new OggStreamDecoder();
        decoder.init(in);
        Clip clip = new Clip();
        clip.format = decoder.getFormat();
        clip.frequency = decoder.getFrequency();

        // decode the stream piece by piece
        ByteBuffer buf = ByteBuffer.allocate(1024).order(ByteOrder.nativeOrder());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = decoder.read(buf)) > 0) {
            out.write(buf.array(), 0, read);
            buf.clear();
        }
        byte[] bytes = out.toByteArray();
        clip.data = BufferUtils.createByteBuffer(bytes.length);
        clip.data.put(bytes).rewind();
        return clip;
    }

    /** The resource manager from which we load resources. */
    protected ResourceManager _rsrcmgr;
}
