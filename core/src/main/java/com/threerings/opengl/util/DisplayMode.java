package com.threerings.opengl.util;

/**
 * Represents raw screen pixel dimensions.
 */
public class DisplayMode
{
  public enum Mode
  {
    /** A windowed DisplayMode. */
    WINDOWED,

    /** A traditional "full screen" display mode. */
    FULLSCREEN,

    /** Mac only: "Green button" fullscreen mode. */
    MAXED,

    ;
  }

  /** The width. */
  public final int width;

  /** The height. */
  public final int height;

  /** The frequency, only applicable for fullscreen. */
  public final int frequency;

  /** The mode. */
  public final Mode mode;

  /**
   * Creates a windowed display mode with the given dimensions.
   */
  public DisplayMode (int width, int height)
  {
    this(width, height, 0, Mode.WINDOWED);
  }

  /**
   * Creates a display mode with full parameters including fullscreen.
   */
  public DisplayMode (int width, int height, int freq)
  {
    this(width, height, freq, Mode.FULLSCREEN);
  }

  protected DisplayMode (int width, int height, int freq, Mode mode)
  {
    this.width = width;
    this.height = height;
    this.frequency = freq;
    this.mode = mode;
  }

  public boolean isWindowed ()
  {
    return mode == Mode.WINDOWED;
  }

  public boolean isFullscreen ()
  {
    return mode == Mode.FULLSCREEN;
  }

  public boolean isMaxed ()
  {
    return mode == Mode.MAXED;
  }

  /**
   * Turn this mode into a maxed mode.
   */
  public DisplayMode asMaxed ()
  {
    return new DisplayMode(width, height, 0, Mode.MAXED);
  }

  @Override
  public boolean equals (Object other)
  {
    return other instanceof DisplayMode o &&
      width == o.width && height == o.height && mode == o.mode && frequency == o.frequency;
  }

  @Override
  public int hashCode ()
  {
    return width * 10000 + height + frequency + mode.hashCode();
  }

  @Override
  public String toString ()
  {
    return width + " x " + height + switch (mode) {
      case Mode.FULLSCREEN -> " @" + frequency + "Hz";
      case Mode.MAXED -> " (maxed)";
      default -> "";
    };
  }
}
