//
// $Id$

package com.threerings.opengl.model.tools;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.xml.sax.SAXException;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.tools.xml.AnimationParser;
import com.threerings.opengl.util.GlUtil;

/**
 * Parses animation properties and XML files and turns them into animation objects.
 */
public class AnimationReader
{
    /**
     * Creates an animation object using the supplied properties file.
     */
    public static Animation read (File pfile)
        throws IOException, SAXException
    {
        // read the animation from the corresponding model file
        return read(pfile, getAnimationFile(pfile));
    }

    /**
     * Creates an animation object using the supplied properties file and animation XML file.
     */
    public static Animation read (File pfile, File afile)
        throws IOException, SAXException
    {
        // load the properties
        Properties props = GlUtil.loadProperties(pfile);

        // parse and return the animation
        return _aparser.parseAnimation(afile.toString()).createAnimation(props);
    }

    /**
     * Given a properties file, returns the corresponding animation file.
     */
    public static File getAnimationFile (File pfile)
    {
        String pname = pfile.getName();
        int didx = pname.lastIndexOf('.');
        String root = (didx == -1) ? pname : pname.substring(0, didx);
        return new File(pfile.getParentFile(), root + ".mxml");
    }

    /** A parser for the animation definitions. */
    protected static AnimationParser _aparser = new AnimationParser();
}
