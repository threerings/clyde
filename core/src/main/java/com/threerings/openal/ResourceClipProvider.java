//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.openal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
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
        Clip clip = new Clip();
        if (in.available() == 0) {
            // if it's a 0-length file then we just cope
            clip.format = AL10.AL_FORMAT_MONO8;
            clip.data = BufferUtils.createByteBuffer(0);
            return clip;
        }
        decoder.init(in);
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
