package com.threerings.opengl.lwjgl2;

/**
 * Compatibility replacement for LWJGL 2's org.lwjgl.opengl.DisplayMode.
 * Represents a display resolution with optional fullscreen capabilities.
 */
public class DisplayMode
{
  public final int width;
  public final int height;
  public final int bpp;
  public final int frequency;
  public final boolean fullscreenCapable;

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
    this.width = width;
    this.height = height;
    this.bpp = bpp;
    this.frequency = freq;
    this.fullscreenCapable = fullscreenCapable;
  }

  @Override
  public boolean equals (Object other)
  {
    if (!(other instanceof DisplayMode)) return false;
    DisplayMode o = (DisplayMode)other;
    return width == o.width && height == o.height &&
      bpp == o.bpp && frequency == o.frequency &&
      fullscreenCapable == o.fullscreenCapable;
  }

  @Override
  public int hashCode ()
  {
    return width * 10000 + height + (bpp * 100) + frequency +
      (fullscreenCapable ? 1 : 0);
  }

  @Override
  public String toString ()
  {
    return width + " x " + height + " x " + bpp + " @" + frequency + "Hz";
  }
}
