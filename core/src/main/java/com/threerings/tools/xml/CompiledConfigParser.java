//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.tools.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import org.xml.sax.SAXException;

import org.apache.commons.digester.Digester;

import com.threerings.util.CompiledConfig;

import com.threerings.tools.CompiledConfigTask;

/**
 * An abstract base implementation of a parser that is used to compile configuration definitions
 * into config objects for use by the client and server.
 * 
 * @see CompiledConfig
 * @see CompiledConfigTask
 */
public abstract class CompiledConfigParser
{
    /**
     * Parses the supplied configuration file into a serializable configuration object.
     */
    public Serializable parseConfig (File source)
        throws IOException, SAXException
    {
        Digester digester = new Digester();
        Serializable config = createConfigObject();
        addRules(digester);
        digester.push(config);
        digester.parse(new FileInputStream(source));
        return config;
    }

    /**
     * Creates the config object instance that will be populated during the parsing process.
     */
    protected abstract Serializable createConfigObject ();

    /**
     * Adds the necessary digester rules for parsing the config object.
     */
    protected abstract void addRules (Digester digester);
}
