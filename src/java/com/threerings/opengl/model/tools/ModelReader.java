//
// $Id$

package com.threerings.opengl.model.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.xml.sax.SAXException;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.tools.xml.ModelParser;
import com.threerings.opengl.util.GlUtil;

/**
 * Parses model properties and XML files and turns them into model objects.
 */
public class ModelReader
{
    /**
     * Creates a model object using the supplied properties file.
     */
    public static Model read (File pfile)
        throws IOException, SAXException
    {
        // read the model from the corresponding model file
        return read(pfile, getModelFile(pfile));
    }

    /**
     * Creates a model object using the supplied properties file and model XML file.
     */
    public static Model read (File pfile, File mfile)
        throws IOException, SAXException
    {
        // load the properties
        Properties props = GlUtil.loadProperties(pfile);

        // parse and return the model
        return _mparser.parseModel(mfile.toString()).createModel(props);
    }

    /**
     * Creates a set of models (mapped by name) using the supplied properties file.
     */
    public static HashMap<String, Model> readSet (File pfile, File mfile)
        throws IOException, SAXException
    {
        // load the properties
        Properties props = GlUtil.loadProperties(pfile);

        // parse and return the models
        return _mparser.parseModel(mfile.toString()).createModelSet(props);
    }

    /**
     * Given a properties file, returns the corresponding model file.
     */
    public static File getModelFile (File pfile)
    {
        String pname = pfile.getName();
        int didx = pname.lastIndexOf('.');
        String root = (didx == -1) ? pname : pname.substring(0, didx);
        return new File(pfile.getParentFile(), root + ".mxml");
    }

    /** A parser for the model definitions. */
    protected static ModelParser _mparser = new ModelParser();
}
