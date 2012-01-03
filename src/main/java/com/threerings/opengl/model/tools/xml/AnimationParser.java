//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.model.tools.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;
import org.apache.commons.digester.Digester;

import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.opengl.model.tools.AnimationDef;

/**
 * Parses XML files containing animations.
 */
public class AnimationParser
{
    public AnimationParser ()
    {
        // create and configure our digester
        _digester = new Digester();

        // add the rules
        String anim = "animation";
        _digester.addObjectCreate(anim, AnimationDef.class.getName());
        _digester.addRule(anim, new SetPropertyFieldsRule());
        _digester.addSetNext(anim, "setAnimation",
            AnimationDef.class.getName());

        String frame = anim + "/frame";
        _digester.addObjectCreate(frame,
            AnimationDef.FrameDef.class.getName());
        _digester.addSetNext(frame, "addFrame",
            AnimationDef.FrameDef.class.getName());

        String xform = frame + "/transform";
        _digester.addObjectCreate(xform,
            AnimationDef.TransformDef.class.getName());
        _digester.addRule(xform, new SetPropertyFieldsRule());
        _digester.addSetNext(xform, "addTransform",
            AnimationDef.TransformDef.class.getName());
    }

    /**
     * Parses the XML file at the specified path into an animation
     * definition.
     */
    public AnimationDef parseAnimation (String path)
        throws IOException, SAXException
    {
        return parseAnimation(new FileInputStream(path));
    }

    /**
     * Parses the supplied XML stream into an animation definition.
     */
    public AnimationDef parseAnimation (InputStream in)
        throws IOException, SAXException
    {
        _animation = null;
        _digester.push(this);
        _digester.parse(in);
        return _animation;
    }

    /**
     * Called by the parser once the animation is parsed.
     */
    public void setAnimation (AnimationDef animation)
    {
        _animation = animation;
    }

    protected Digester _digester;
    protected AnimationDef _animation;
}
