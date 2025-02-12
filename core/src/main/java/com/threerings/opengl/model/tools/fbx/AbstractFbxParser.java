package com.threerings.opengl.model.tools.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.samskivert.util.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;

import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public class AbstractFbxParser
{
    protected AbstractFbxParser () { /* do not instantiate directly */ }

    protected final Map<Long, Object> objectsById = Maps.newHashMap();

    protected final ListMultimap<Long, Connection> connsBySrc = ArrayListMultimap.create();
    protected final ListMultimap<Long, Connection> connsByDest = ArrayListMultimap.create();

    protected FBXNode root, objects;

    /**
     * Map an object by fbx node.
     * return false if there was some sort of error which will already be logged
     */
    protected boolean mapObject (FBXNode node, Object object)
    {
        Long id;
        try {
            id = node.getData();
        } catch (Exception e) {
            log.warning("Unable to extract id property!", "node", node.getFullName());
            return false;
        }
        return mapObject(id, object);
    }

    protected boolean mapObject (Long id, Object object)
    {
        Object oval = objectsById.put(id, object);
        if (oval == null) return true;
        log.warning("Two objects of same id?", "id", id, "new", object, "old", oval);
        return false;
    }

    /**
     * Detached code that had been part of my explorations.
     * I'll clean it up eventually.
     */
    protected void checkMore () {
        // see if we have any attributes
        for (FBXNode attr : objects.getChildrenByName("NodeAttribute")) {
            if (mapObject(attr, attr)) {
                log.info("Stored attribute.. " + attr.getProperty(0).getData());
            }
        }

        // Poses
        for (FBXNode pose : objects.getChildrenByName("Pose")) {
            if (mapObject(pose, pose)) {
                for (FBXNode poseNode : pose.getChildrenByName("PostNode")) {
                    mapObject(poseNode, poseNode);
                }
            }
        }

        // document root node
        for (FBXNode doc : root.getChildByName("Documents").getChildrenByName("Document")) {
            mapObject(doc, doc);
            FBXNode rootNodeId = doc.getChildByName("RootNode");
            if (rootNodeId != null) mapObject(rootNodeId, rootNodeId);
        }

        // more
        for (String name : new String[] {
            "Material", "Deformer", "AnimationStack", "AnimationCurveNode", "AnimationLayer",
            "Texture",
            "CollectionExclusive" }) {
            for (FBXNode node : objects.getChildrenByName(name)) mapObject(node, node);
        }

        // https://download.autodesk.com/us/fbx/20112/fbx_sdk_help/index.html?url=WS73099cc142f487551fea285e1221e4f9ff8-7fda.htm,topicNumber=d0e6388
//        FBXNode connections = root.getChildByName("Connections");
//        for (FBXNode conn : connections.getChildrenByName("C")) {
//            String type = conn.getData(0);
//            if (true || "OO".equals(type)) {
//                Long srcId = conn.getData(1);
//                Long destId = conn.getData(2);
//                // child is "source", parent is "destination"
//                Object src = objectsById.get(srcId);
//                Object dest = objectsById.get(destId);
//                if (dest instanceof ModelDef.NodeDef) {
//                    ModelDef.NodeDef parent = (ModelDef.NodeDef)dest;
//                    if (src instanceof ModelDef.NodeDef) {
//                        ModelDef.NodeDef child = (ModelDef.NodeDef)src;
//                        if (child.parent == null) {
//                            child.parent = parent.name;
//                            log.info("Added parent!", "child", child.name, "parent", parent.name);
//                        } else {
//                            log.warning("Oh noes! Child already has a parent defined?",
//                                    "child", child.name,
//                                    "parent", child.parent,
//                                    "newparent", parent.name);
//                        }
//                    } else if (src instanceof FBXNode) {
//                        FBXNode srcNode = (FBXNode)src;
//                        log.info("Found conn", "type", type, "dest", dest,
//                                "attrs", srcNode.getFullName());
//                        FbxDumper.Dump(srcNode, "            ");
//                    }
//                } else {
//                    log.info((dest == null && src == null) ? "Unfound XXXX" : "Unknown conn",
//                        "type", type,
//                        "src", formatObj(src, srcId),
//                        "dest", formatObj(dest, destId));
//                }
//            }
//        }
    }

    protected float[] getFloatTriplet (FBXNode propertyNode)
    {
        return new float[] {
            propertyNode.<Double>getData(4).floatValue(),
            propertyNode.<Double>getData(5).floatValue(),
            propertyNode.<Double>getData(6).floatValue() };
    }

    /**
     * Get the rotation out of a property node.
     */
    protected float[] getRotation (FBXNode propertyNode)
    {
        if (propertyNode.getNumProperties() > 7) {
            throw new RuntimeException(Logger.format(
                "TODO! We don't yet support a 4-element rotation in the FBX!",
                "w?", propertyNode.getData(7)));
        }
        return getRotation(
            FloatMath.HALF_PI + FloatMath.toRadians(propertyNode.<Double>getData(4).floatValue()),
            FloatMath.toRadians(propertyNode.<Double>getData(5).floatValue()),
            FloatMath.toRadians(propertyNode.<Double>getData(6).floatValue()));
    }

    /**
     * Convert a 3-element rotation into the quaternion values needed.
     */
    protected float[] getRotation (float x, float y, float z)
    {
        float[] values = new float[4];
        new Quaternion().fromAngles(x, y, z).get(values);
        return values;
    }

    /**
     * For debug logging.
     */
    protected Object formatObj (Object obj, Long id) {
        if (obj == null) return "Unknown{" + id + "}";
        if (obj instanceof ModelDef.SpatialDef) {
            ModelDef.SpatialDef spat = (ModelDef.SpatialDef)obj;
            return obj.getClass().getName() + "{" + spat.name + "}";
        }
        if (obj instanceof FBXNode) {
            FBXNode node = (FBXNode)obj;
            if ("Deformer".equals(node.getName())) {
                List<Object> logArgs = Lists.newArrayList();
                for (int ii = 0, nn = node.getNumChildren(); ii < nn; ++ii) {
                    FBXNode child = node.getChild(ii);
                    if (child.getNumProperties() == 1) {
                        logArgs.add(child.getName());
                        logArgs.add(child.getProperty(0).getData());
                    }
                }
                return Logger.format("Deformer", logArgs.toArray());
            }
            return node.getFullName();
        }
        return obj;
    }


    protected void populateConnections () {
        FBXNode connections = root.getChildByName("Connections");
        for (FBXNode cNode : connections.getChildrenByName("C")) {
            Connection conn = new Connection(cNode);
            connsBySrc.put(conn.srcId, conn);
            connsByDest.put(conn.destId, conn);
        }
    }

    protected static class Connection
    {
        public final String type;
        public final Long srcId;
        public final Long destId;

        public Connection (FBXNode cNode) {
            this(cNode.<String>getData(0), cNode.<Long>getData(1), cNode.<Long>getData(2));
        }

        public Connection (String type, Long srcId, Long destId) {
            this.type = type;
            this.srcId = srcId;
            this.destId = destId;
        }

        @Override
        public String toString () {
            return "C:" + type + " " + srcId + " " + destId;
        }
    }
}
