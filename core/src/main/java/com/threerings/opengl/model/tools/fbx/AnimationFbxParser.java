package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

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
        FBXFile fbx = FBXLoader.loadFBXFile("anim", in);
        //FbxDumper.Dump(fbx);

        AnimationDef anim = new AnimationDef();
        anim.frameRate = readFrameRate(fbx);

        populateConnections(fbx);

        FBXNode objects = fbx.getRootNode().getChildByName("Objects");
        populateObjects(objects, "AnimationCurveNode", "AnimationLayer");

        // read in our default prototype limb transforms
        Map<Long, AnimationDef.TransformDef> limbs = Maps.newHashMap();

        for (FBXNode model : objects.getChildrenByName("Model")) {
            String type = model.getData(2);
            if ("Mesh".equals(type)) continue;

            AnimationDef.TransformDef xform = new AnimationDef.TransformDef();
            xform.name = sanitizeName(model.<String>getData(1));
            xform.translation = new float[3];
            xform.rotation = new float[] { 0f, 0f, 0f, 1f };
            xform.scale = new float[] { 1f, 1f, 1f };

            FBXNode props = model.getChildByName("Properties70");
            if (props == null) {
                log.warning("No props for node?", "name", xform.name);
                continue;
            }
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = prop.getData(0);
                if ("Lcl Translation".equals(pname)) {
                    xform.translation = getFloatTriplet(prop);
                } else if ("Lcl Rotation".equals(pname)) {
                    xform.rotation = getRotation(prop);
                } else if ("Lcl Scaling".equals(pname)) {
                    xform.scale = getFloatTriplet(prop);
                }
            }
            limbs.put(model.<Long>getData(), xform);
        }

        for (FBXNode stack : objects.getChildrenByName("AnimationStack")) {
            // I think?
            AnimationDef.FrameDef frame = new AnimationDef.FrameDef();
            anim.addFrame(frame);
            FBXNode layer = findNodeToDest(stack.<Long>getData(), "AnimationLayer");
            if (layer == null) {
                log.warning("Can't find layer?");
                continue;
            }
            for (Connection cc : connsByDest.get(layer.<Long>getData())) {
                Object src = objectsById.get(cc.srcId);
                if (!(src instanceof FBXNode)) {
                    log.info("layer connects to what: " + formatObj(src, cc.srcId));
                    continue;
                }
                FBXNode srcNode = (FBXNode)src;
                if (!"AnimationCurveNode".equals(srcNode.getName())) {
                    log.info("layer connects to whaaaat? " + formatObj(srcNode, null));
                    continue;
                }
                for (Connection curveCon : connsBySrc.get(cc.srcId)) {
                    if (!curveCon.type.equals("OP")) continue;
                    AnimationDef.TransformDef limbProto = limbs.get(curveCon.destId);
                    if (limbProto == null) {
                        // TODO: MaxHandle?
                        log.warning("Unable to find limb prototype for curve node?",
                                "src", cc.srcId, "prop", curveCon.property);
                        continue;
                    }
                    AnimationDef.TransformDef limb = frame.transforms.get(limbProto.name);
                    if (limb == null) {
                        limb = new AnimationDef.TransformDef();
                        limb.name = limbProto.name;
                        limb.translation = limbProto.translation.clone();
                        limb.rotation = limbProto.rotation.clone();
                        limb.scale = limbProto.scale.clone();
                        frame.addTransform(limb);
                    }
                    if ("Lcl Translation".equals(curveCon.property)) {
                        limb.translation = parseCurveTriplet(srcNode);
//                        log.info("Updated nodes translation",
//                                "node", limb.name, "trans", limb.translation);

                    } else if ("Lcl Scaling".equals(curveCon.property)) {
                        limb.scale = parseCurveTriplet(srcNode);
//                        log.info("Updated nodes scale", "node", limb.name, "scale", limb.scale);

                    } else if ("Lcl Rotation".equals(curveCon.property)) {
                        float[] trip = parseCurveTriplet(srcNode);
                        limb.rotation = getRotation(trip[0], trip[1], trip[2]);
//                        log.info("Updated nodes rot", "node", limb.name, "rot", limb.rotation);
                    }
                }
            }
        }

        return anim;
    }

    protected float[] parseCurveTriplet (FBXNode curveNode) {
        FBXNode props = curveNode.getChildByName("Properties70");
        if (props != null && props.getNumChildren() > 2) {
            return new float[] {
                props.getChild(0).<Double>getData(4).floatValue(),
                props.getChild(1).<Double>getData(4).floatValue(),
                props.getChild(2).<Double>getData(4).floatValue() };
        }
        log.warning("Oh shit!", "curveNode", formatObj(curveNode, null));
        return new float[3];
    }

    /**
     * Read the frame rate or return 0f if it can't be determined.
     */
    protected float readFrameRate (FBXFile fbx)
    {
        FBXNode settings = fbx.getRootNode().getChildByName("GlobalSettings");
        if (settings != null) {
            FBXNode props = settings.getChildByName("Properties70");
            FBXNode timeMode = null;
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = prop.getData();
                if ("TimeMode".equals(pname)) timeMode = prop;
                else if ("CustomFrameRate".equals(pname)) {
                    double rate = prop.<Double>getData(4);
                    if (rate > -1) return (float)rate;
                }
            }
            if (timeMode != null) {
                int enumVal = timeMode.<Integer>getData(4);
                final float[] standardValues = new float [] {
                    0f,   //FbxTime.eDefaultMode
                    120f, //FbxTime.eFrames120
                    100f, //FbxTime.eFrames100
                     60f, //FbxTime.eFrames60
                     50f, //FbxTime.eFrames50
                     48f, //FbxTime.eFrames48
                     30f, //FbxTime.eFrames30
                     29.97f, //FbxTime.eFrames30Drop
                     29.97f, //FbxTime.eNTSCDropFrame
                     30f, //FbxTime.eNTSCFullFrame
                     25f, //FbxTime.eFrames25
                     25f, //FbxTime.ePAL
                     24f, //FbxTime.eFrames24
                   1000f, //FbxTime.eFrames1000
                     24f, //FbxTime.eFilmFullFrame
                     96f, //FbxTime.eFrames96
                     72f, //FbxTime.eFrames72
                     59.94f, //FbxTime.eFrames59dot94
                    119.88f //FbxTime.eFrames119dot88
                            //FbxTime.eCustom (read the custom frame rate)...
                };
                if (enumVal >= 0 && enumVal < standardValues.length) return standardValues[enumVal];
            }
        }
        return 0f;
    }

    protected AnimationFbxParser () { /* instantiate via the static method. Bleah. */ }
}
