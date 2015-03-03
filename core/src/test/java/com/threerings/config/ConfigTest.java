//
// $Id$

package com.threerings.config;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.rules.TemporaryFolder;

import com.threerings.config.ConfigManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;


/**
 * Tests create a new config to read and write 
 */
public class ConfigTest
{
	public File createFileWithContent( String path, String content )
		throws IOException
	{
		File file = new File(path);
		file.getParentFile().mkdirs();
		
		PrintWriter out = new PrintWriter(file);
		out.write( content);	
		out.close();
		
		return file;
	}
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
    
	/*
	 * This test creates the minimum Config setup from scratch in a temporary directory.
	 */
	
    @Test
    public void testCreateConfig ()
        throws IOException
    {	
        File rsrcFolder = testFolder.newFolder("tmp_rsrc");
        
        //File msgFolder = new File(rsrcFolder.getPath() + File.separator + "i18n/");
        //String msgPropPath = msgFolder.getPath()  + File.separator + "global.properties";
        //createFileWithContent( msgPropPath, "" );


        // ResourceManager is required to bootstrap the settings.
        // Resources are named relative to their location in the resource root.
        // initResourceDir required to use the filesystem as a "fallback" location, 
        // otherwise, just CLASSPATH magic is used instead.
        ResourceManager rsrcmgr = new ResourceManager(rsrcFolder.getPath());
        rsrcmgr.initResourceDir( rsrcFolder.getPath());
        
        // MessageManager NOT required for minimum setup.
        MessageManager msgmgr = null;//new MessageManager("rsrc.i18n");

        File cfgFolder = new File(rsrcFolder.getPath()  + File.separator + "config/");
        String cfgPropPath = cfgFolder.getPath()  + File.separator + "manager.properties";
        createFileWithContent( cfgPropPath, 
        		"types = global\nglobal.classes = com.threerings.config.ExampleConfig\n" );        
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config");
    	
        cfgmgr.init();
        
        cfgmgr.saveAll(cfgFolder, ".xml", true);
        cfgmgr.saveAll(cfgFolder, ".dat", false);

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(cfgFolder.list()));
        assertTrue( list.contains("example.xml"));
        assertTrue( list.contains("example.dat"));

        for (File f : cfgFolder.listFiles()) {
        	System.out.println("ReadConfigTest created " + f.getName() + " with " + f.length() + " bytes");
        }
    }
}
