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

package com.threerings.media.image.tools.xml;

import java.io.Serializable;

import org.xml.sax.Attributes;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;

import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.ColorPository.ColorRecord;

import com.threerings.tools.xml.CompiledConfigParser;

/**
 * Parses the XML color repository definition and creates a {@link ColorPository} instance that
 * reflects its contents.
 */
public class ColorPositoryParser extends CompiledConfigParser
{
    @Override
    protected Serializable createConfigObject ()
    {
        return new ColorPository();
    }

    @Override
    protected void addRules (Digester digest)
    {
        // create and configure class record instances
        String prefix = "colors/class";
        digest.addObjectCreate(prefix, ClassRecord.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addClass", ClassRecord.class.getName());

        // create and configure color record instances
        prefix += "/color";
        digest.addRule(prefix, new Rule() {
            @Override
            public void begin (String namespace, String name,
                               Attributes attributes) throws Exception {
                // we want to inherit settings from the color class when
                // creating the record, so we do some custom stuff
                ColorRecord record = new ColorRecord();
                ClassRecord clrec = (ClassRecord)digester.peek();
                record.starter = clrec.starter;
                digester.push(record);
            }

            @Override
            public void end (String namespace, String name) throws Exception {
                digester.pop();
            }
        });
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addColor", ColorRecord.class.getName());
    }
}
