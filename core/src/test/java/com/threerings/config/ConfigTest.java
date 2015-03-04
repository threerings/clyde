//
// $Id$

package com.threerings.config;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.rules.TemporaryFolder;

import com.threerings.config.ConfigManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

// for sample config lookup.
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.model.config.ModelConfig;
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
        
        ConfigGroup exampleGroup = cfgmgr.getGroup(ExampleConfig.class);
        exampleGroup.addConfig( new ExampleConfig(123,"MyString"));
        exampleGroup.save();
        
        cfgmgr.saveAll(cfgFolder, ".xml", true);
        cfgmgr.saveAll(cfgFolder, ".dat", false);

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(cfgFolder.list()));
        assertTrue( list.contains("example.xml"));
        assertTrue( list.contains("example.dat"));

        for (File f : cfgFolder.listFiles()) {
        	System.out.println("ReadConfigTest created " + f.getName() + " with " + f.length() + " bytes");
        }
    }

    /*
     * Load the default testing resources and modify their contents.
     */
    
    @Test
    public void testLoadConfig ()
        throws IOException
    {
    	ResourceManager rsrcmgr = new ResourceManager("rsrc/");
    	MessageManager msgmgr = new MessageManager("rsrc.i18n");
    	ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
    	
    	cfgmgr.init();
    	
    	Collection<ConfigGroup<?>> groups = cfgmgr.getGroups();
    	for (ConfigGroup<?> group : groups ) {
    		//System.out.println("got group " + group.getName() + " class " + group._cclass );
    		for (Object configObj : group.getConfigs()) {
    			ManagedConfig config = (ManagedConfig) configObj;
    			//System.out.println("   " + config.getName() );
    		}	
    	}
    	
    	RenderSchemeConfig renderConfig = cfgmgr.getConfig(RenderSchemeConfig.class, "Translucent");
    	assertTrue( renderConfig != null );
    	//System.out.println("Got config " + renderConfig.getName() );

    	ModelConfig modelConfig = cfgmgr.getConfig(ModelConfig.class, "model/knight/model.dat");
    	assertTrue( modelConfig != null );
    	//System.out.println("Got config " + modelConfig.getName());
    }
}
