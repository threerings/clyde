package com.threerings.opengl.lwjgl2;

/**
 * Compatibility replacement for LWJGL 2's org.lwjgl.opengl.DisplayMode.
 * Represents a display resolution with optional fullscreen capabilities.
 */
public class DisplayMode
{
    private final int _width;
    private final int _height;
    private final int _bpp;
    private final int _freq;
    private final boolean _fullscreenCapable;

    /**
     * Creates a windowed display mode with the given dimensions.
     */
    public DisplayMode (int width, int height)
    {
        this(width, height, 0, 0, false);
    }

    /**
     * Creates a display mode with full parameters.
     */
    public DisplayMode (int width, int height, int bpp, int freq)
    {
        this(width, height, bpp, freq, bpp > 0 && freq > 0);
    }

    /**
     * Creates a display mode with full parameters including fullscreen capability.
     */
    public DisplayMode (int width, int height, int bpp, int freq, boolean fullscreenCapable)
    {
        _width = width;
        _height = height;
        _bpp = bpp;
        _freq = freq;
        _fullscreenCapable = fullscreenCapable;
    }

    public int getWidth ()
    {
        return _width;
    }

    public int getHeight ()
    {
        return _height;
    }

    public int getBitsPerPixel ()
    {
        return _bpp;
    }

    public int getFrequency ()
    {
        return _freq;
    }

    public boolean isFullscreenCapable ()
    {
        return _fullscreenCapable;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof DisplayMode)) return false;
        DisplayMode o = (DisplayMode)other;
        return _width == o._width && _height == o._height &&
            _bpp == o._bpp && _freq == o._freq &&
            _fullscreenCapable == o._fullscreenCapable;
    }

    @Override
    public int hashCode ()
    {
        return _width * 10000 + _height + (_bpp * 100) + _freq +
            (_fullscreenCapable ? 1 : 0);
    }

    @Override
    public String toString ()
    {
        return _width + " x " + _height + " x " + _bpp + " @" + _freq + "Hz";
    }
}
