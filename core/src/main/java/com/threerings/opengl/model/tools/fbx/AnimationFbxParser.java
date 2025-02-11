package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.opengl.model.tools.AnimationDef;

import static com.threerings.opengl.Log.log;

public class AnimationFbxParser extends AbstractFbxParser
{
    /**
     * Parse the animation.
     */
    public static AnimationDef parseAnimation (InputStream in)
        throws IOException
    {
        return new AnimationFbxParser().parse(in);
    }

    /**
     * Parse the animation.
     */
    protected AnimationDef parse (InputStream in)
        throws IOException
    {
        AnimationDef anim = new AnimationDef();
        FBXFile fbx = FBXLoader.loadFBXFile("anim", in);
        FbxDumper.Dump(fbx);

        root = fbx.getRootNode();
        objects = root.getChildByName("Objects");

        // STUFF

        // TODO parse FrameDefs, call addFrame()

        // --> inside each frame we parse transforms, call addTransform()

        // that's it

        log.warning("TODO: Parsing animations from FBX files!");

        return anim;
    }

    protected AnimationFbxParser () { /* instantiate via the static method. Bleah. */ }
}
