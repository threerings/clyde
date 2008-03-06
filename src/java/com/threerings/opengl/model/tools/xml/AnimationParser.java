//
// $Id$

package com.threerings.opengl.model.tools.xml;

import java.io.FileInputStream;
import java.io.IOException;

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
        _animation = null;
        _digester.push(this);
        _digester.parse(new FileInputStream(path));
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
