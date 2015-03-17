//
// $Id$

package com.threerings.config;

import com.threerings.config.ManagedConfig;

/**
 * Example Config class to use in testing.  
 */
public class ExampleConfig extends ManagedConfig
{
	int value1;
	String value2;
	
	public ExampleConfig()
	{
	}
	
	public ExampleConfig(int v1, String v2)
	{
		value1 = v1;
		value2 = v2;
	}
}
