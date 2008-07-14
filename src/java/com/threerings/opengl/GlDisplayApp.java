//
// $Id$

package com.threerings.opengl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;

/**
 * A base class for applications that use LWJGL's {@link Display} class.
 */
public abstract class GlDisplayApp extends GlApp
{
    /**
     * Starts up the application.
     */
    public void startup ()
    {
    }
    
    /**
     * Shuts down the application.
     */
    public void shutdown ()
    {
    }
}
