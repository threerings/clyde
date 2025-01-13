package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;
import java.io.InputStream;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;

import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public class ModelFbxParser
{
    public ModelDef parseModel (InputStream in)
        throws IOException
    {
        ModelDef model = new ModelDef();
        FBXFile fbx = FBXLoader.loadFBXFile("model", in);
        log.info("We do appear to have read an fbx file",
                "version", fbx.getVersion());
        // TODO: stuff!
        return model;
    }
}
