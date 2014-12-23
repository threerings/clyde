//
// $Id$

package com.threerings.tools;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;

/**
 * Ant task for reading and writing transformed Properties.
 */
public abstract class PropertyTransformerTask extends FileSetTask
{
    @Override
    public void execute ()
        throws BuildException
    {
        try {
            for (File file : getFiles()) {
                Properties props = new Properties();
                props.load(new FileReader(file));
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    entry.setValue(transformProperty(
                            (String)entry.getKey(), (String)entry.getValue()));
                }
                props.store(new FileWriter(transformFilename(file)), " (Generated)");
            }

        } catch (IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    /**
     * Return the new property value, given the existing key and value.
     */
    protected abstract String transformProperty (String key, String value);

    /**
     * Return the new filename at which to store the transformed properties.
     */
    protected abstract File transformFilename (File source);
}
