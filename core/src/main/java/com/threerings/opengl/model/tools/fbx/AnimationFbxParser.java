package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import com.google.common.io.Files;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.opengl.model.tools.AnimationDef;

import static com.threerings.opengl.Log.log;

public class AnimationFbxParser
{
    /**
     * Parse the animation.
     */
    public AnimationDef parseAnimation (InputStream in)
        throws IOException
    {
        AnimationDef anim = new AnimationDef();
        FBXFile fbx = FBXLoader.loadFBXFile("anim", in);
        FbxDumper.Dump(fbx);

        FBXNode root = fbx.getRootNode();

        // TODO parse FrameDefs, call addFrame()

        // --> inside each frame we parse transforms, call addTransform()

        // that's it

        log.warning("TODO: Parsing animations from FBX files!");

        return anim;
    }
}
