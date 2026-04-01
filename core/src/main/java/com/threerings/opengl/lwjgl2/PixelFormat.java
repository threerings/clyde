package com.threerings.opengl.lwjgl2;

/**
 * Compatibility class replacing LWJGL 2's org.lwjgl.opengl.PixelFormat.
 * Stores pixel format preferences (alpha bits, depth bits, stencil bits, samples).
 */
public class PixelFormat
{
  public final int alphaBits;
  public final int depthBits;
  public final int stencilBits;
  public final int samples;

  public PixelFormat ()
  {
    this(0, 24, 0);
  }

  public PixelFormat (int alpha, int depth, int stencil)
  {
    this(alpha, depth, stencil, 0);
  }

  public PixelFormat (int alpha, int depth, int stencil, int samples)
  {
    this.alphaBits = alpha;
    this.depthBits = depth;
    this.stencilBits = stencil;
    this.samples = samples;
  }

  public PixelFormat withSamples (int samples)
  {
    return new PixelFormat(alphaBits, depthBits, stencilBits, samples);
  }

  @Override
  public String toString ()
  {
    return "PixelFormat[alpha=" + alphaBits + ", depth=" + depthBits +
      ", stencil=" + stencilBits + ", samples=" + samples + "]";
  }
}
