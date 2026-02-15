package com.threerings.opengl.lwjgl2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * Compatibility replacement for LWJGL 2's org.lwjgl.util.WaveData.
 * Parses WAV files and provides the data in a format suitable for OpenAL.
 */
public class WaveData
{
    /** The OpenAL format of this wave data. */
    public final int format;

    /** The sample data. */
    public final ByteBuffer data;

    /** The sample rate. */
    public final int samplerate;

    public WaveData (int format, ByteBuffer data, int samplerate)
    {
        this.format = format;
        this.data = data;
        this.samplerate = samplerate;
    }

    /**
     * Creates a WaveData from the given input stream (WAV format).
     */
    public static WaveData create (InputStream is)
    {
        try {
            // Read RIFF header
            byte[] header = new byte[44];
            int read = 0;
            while (read < 44) {
                int r = is.read(header, read, 44 - read);
                if (r < 0) return null;
                read += r;
            }

            // Verify RIFF/WAVE header
            if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F') {
                return null;
            }
            if (header[8] != 'W' || header[9] != 'A' || header[10] != 'V' || header[11] != 'E') {
                return null;
            }

            int channels = (header[22] & 0xFF) | ((header[23] & 0xFF) << 8);
            int sampleRate = (header[24] & 0xFF) | ((header[25] & 0xFF) << 8) |
                ((header[26] & 0xFF) << 16) | ((header[27] & 0xFF) << 24);
            int bitsPerSample = (header[34] & 0xFF) | ((header[35] & 0xFF) << 8);
            int dataSize = (header[40] & 0xFF) | ((header[41] & 0xFF) << 8) |
                ((header[42] & 0xFF) << 16) | ((header[43] & 0xFF) << 24);

            // Read the actual audio data
            byte[] audioData = new byte[dataSize];
            read = 0;
            while (read < dataSize) {
                int r = is.read(audioData, read, dataSize - read);
                if (r < 0) break;
                read += r;
            }

            ByteBuffer buffer = BufferUtils.createByteBuffer(read);
            buffer.put(audioData, 0, read).flip();

            int format;
            if (channels == 1) {
                format = (bitsPerSample == 16) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_MONO8;
            } else {
                format = (bitsPerSample == 16) ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_STEREO8;
            }

            return new WaveData(format, buffer, sampleRate);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Disposes of the resources.
     */
    public void dispose ()
    {
        // Nothing to do for direct buffers
    }
}
