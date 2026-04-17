package com.threerings.opengl.util;

/**
 * Represents raw screen pixel dimensions.
 */
public class DisplayMode
{
  public final int width;
  public final int height;
  public final int frequency;
  public final boolean fullscreen;

  /**
   * Creates a windowed display mode with the given dimensions.
   */
  public DisplayMode (int width, int height)
  {
    this(width, height, 0, false);
  }

  /**
   * Creates a display mode with full parameters including fullscreen.
   */
  public DisplayMode (int width, int height, int freq, boolean fullscreen)
  {
    this.width = width;
    this.height = height;
    this.frequency = freq;
    this.fullscreen = fullscreen;
  }

  @Override
  public boolean equals (Object other)
  {
    return other instanceof DisplayMode o &&
      width == o.width && height == o.height &&
      frequency == o.frequency &&
      fullscreen == o.fullscreen;
  }

  @Override
  public int hashCode ()
  {
    return width * 10000 + height + frequency + (fullscreen ? 1 : 0);
  }

  @Override
  public String toString ()
  {
    return width + " x " + height + (fullscreen ? " @" + frequency + "Hz" : "");
  }
}
