package com.threerings.opengl.model.tools.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.samskivert.util.Logger;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;

import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public abstract class AbstractFbxParser
{
    protected AbstractFbxParser () { /* do not instantiate directly */ }

    protected final Map<Long, Object> objectsById = Maps.newHashMap();

    protected final ListMultimap<Long, Connection> connsBySrc = ArrayListMultimap.create();
    protected final ListMultimap<Long, Connection> connsByDest = ArrayListMultimap.create();

    protected int xAxis = 0;
    protected int xAxisSign = 1;
    protected int yAxis = 1;
    protected int yAxisSign = 1;
    protected int zAxis = 2;
    protected int zAxisSign= 1;

    protected int[] reverseAxis = new int[3];
    protected int[] reverseAxisSign = new int[3];

    protected final Set<String> convertedNames = Sets.newHashSet();
    protected final Set<String> names = Sets.newHashSet();

    protected void init (FBXFile fbx)
    {
        readSettings(fbx);
        populateConnections(fbx);
    }

    protected void readSettings (FBXFile fbx)
    {
        FBXNode settings = fbx.getRootNode().getChildByName("GlobalSettings");
        if (settings == null) return;
        FBXNode props = settings.getChildByName("Properties70");
        if (props == null) return;
        for (FBXNode prop : props.getChildrenByName("P")) {
            String pname = prop.getData();
            if ("CoordAxis".equals(pname)) xAxis = prop.<Integer>getData(4);
            else if ("CoordAxisSign".equals(pname)) xAxisSign = prop.<Integer>getData(4);
            else if ("FrontAxis".equals(pname)) yAxis = prop.<Integer>getData(4);
            else if ("FrontAxisSign".equals(pname)) yAxisSign = -1 * prop.<Integer>getData(4);
            else if ("UpAxis".equals(pname)) zAxis = prop.<Integer>getData(4);
            else if ("UpAxisSign".equals(pname)) zAxisSign = prop.<Integer>getData(4);
        }
        reverseAxis[xAxis] = 0;
        reverseAxis[yAxis] = 1;
        reverseAxis[zAxis] = 2;
        // Wait, is this right? I always get confused with reversals
        reverseAxisSign[xAxis] = xAxisSign;
        reverseAxisSign[yAxis] = yAxisSign;
        reverseAxisSign[zAxis] = zAxisSign;
//        log.info("Nothing is certain but depth and axes",
//                "xAxis", axisStr(xAxis, xAxisSign),
//                "yAxis", axisStr(yAxis, yAxisSign),
//                "zAxis", axisStr(zAxis, zAxisSign));
//    }
//    private String axisStr (int axis, int sign)
//    {
//        return "(" + (sign > 0 ? "+" : "-") + ") " + axis;
    }

    protected void populateConnections (FBXFile fbx) {
        FBXNode connections = fbx.getRootNode().getChildByName("Connections");
        for (FBXNode cNode : connections.getChildrenByName("C")) {
            Connection conn = new Connection(cNode);
            connsBySrc.put(conn.srcId, conn);
            connsByDest.put(conn.destId, conn);
        }
    }

    /**
     * Map fbx nodes that are children of "Objects" by id.
     */
    protected void populateObjects (FBXNode objects, String... types)
    {
        Set<String> allTypes = ImmutableSet.copyOf(types);
        for (int ii = 0, nn = objects.getNumChildren(); ii < nn; ++ii) {
            FBXNode node = objects.getChild(ii);
            if (allTypes.contains(node.getName())) mapObject(node, node);
        }
    }

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
        log.warning("Two objects of same id?", "id", id,
                "new", formatObj(object, id), "old", formatObj(oval, id));
        return false;
    }

    protected FBXNode findNode (Long id)
    {
        return findNode(id, null);
    }

    protected FBXNode findNode (Long id, String name)
    {
        return findNode(id, name, null);
    }

    protected FBXNode findNode (Long id, String name, String type)
    {
        Object obj = objectsById.get(id);
        if (!(obj instanceof FBXNode)) return null;
        FBXNode node = (FBXNode)obj;
        if (name != null && !name.equals(node.getName())) return null;
        if (type != null && !type.equals(node.<String>getData(2))) return null;
        return node;
    }

    protected FBXNode findNodeToDest (Long destId, String name)
    {
        return findNodeToDest(destId, name, null);
    }

    protected FBXNode findNodeToDest (Long destId, String name, String type)
    {
        for (Connection conn : connsByDest.get(destId)) {
            FBXNode node = findNode(conn.srcId, name, type);
            if (node != null) return node;
        }
        return null;
    }

    protected float[] getXYZ (FBXNode propertyNode)
    {
        float[] triplet = getXYZUnsigned(propertyNode);
        triplet[0] *= xAxisSign;
        triplet[1] *= yAxisSign;
        triplet[2] *= zAxisSign;
        return triplet;
    }

    protected float[] getXYZUnsigned (FBXNode propertyNode)
    {
        return new float[] {
            propertyNode.<Double>getData(4 + xAxis).floatValue(),
            propertyNode.<Double>getData(4 + yAxis).floatValue(),
            propertyNode.<Double>getData(4 + zAxis).floatValue() };
    }

    /**
     * Get the rotation out of a property node.
     */
    protected float[] getRotation (FBXNode propertyNode)
    {
        return fromEuler(getXYZ(propertyNode));
    }

    /**
     * Convert a 3-element euler angles (degrees) rotation into the quaternion values needed.
     */
    protected float[] fromEuler (float[] rots)
    {
        float[] values = new float[4];
        new Quaternion().fromAngles(
            FloatMath.toRadians(rots[0]),
            FloatMath.toRadians(rots[1]),
            FloatMath.toRadians(rots[2]))
            .get(values);
        return values;
    }

    protected float[] newRotation ()
    {
        return new float[] { 0f, 0f, 0f, 1f };
    }

    protected float[] newTranslation ()
    {
        return new float[] { 0f, 0f, 0f };
    }

    protected float[] newScale ()
    {
        return new float[] { 1f, 1f, 1f };
    }

    protected float[] newRotationEuler ()
    {
        return new float[] { 0f, 0f, 0f };
    }

    /**
     * Sanitize a name for our purposes.
     * <ul>
     *   <li> Strip leading/trailing unprintables.
     *   <li> Convert underscores to spaces iff the first character is capital.
     * </ul>
     */
    protected String sanitizeName (String name)
    {
        int ii = 0, len = name.length();
        StringBuilder buf = new StringBuilder(len);
        boolean noScores = false;
        while (ii < len) {
            char cc = name.charAt(ii++);
            if (cc >= ' ') {
                buf.append(cc);
                noScores = Character.isUpperCase(cc);
                break;
            }
        }
        boolean didConvert = false;
        for (; ii < len; ++ii) {
            char cc = name.charAt(ii);
            if (cc < ' ') break;
            else if (noScores && cc == '_') {
                buf.append(' ');
                didConvert = true;
            } else buf.append(cc);
        }
        String result = buf.toString();
        if (didConvert) convertedNames.add(result);
        else names.add(result);
        return result;
    }

    protected void reportConvertedNames (@Nullable List<String> messages) {
        if (messages == null) return;

        Joiner jj = Joiner.on(", ");
        if (!convertedNames.isEmpty()) messages.add("Converted names: " + jj.join(convertedNames));
        if (!names.isEmpty()) messages.add("Other names: " + jj.join(names));
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

    protected Object formatObj (Long id) {
        return formatObj(objectsById.get(id), id);
    }

    protected static class Connection
    {
        public final String type;
        public final Long srcId;
        public final Long destId;
        public final String property;

        public Connection (FBXNode cNode) {
            this(cNode.<String>getData(0), cNode.<Long>getData(1), cNode.<Long>getData(2),
                 cNode.getNumProperties() > 3 ? cNode.<String>getData(3) : null);
        }

        public Connection (String type, Long srcId, Long destId, String property) {
            this.type = type;
            this.srcId = srcId;
            this.destId = destId;
            this.property = property;
        }

        @Override
        public String toString () {
            return "C:" + type + " " + srcId + " " + destId +
                (property == null ? "" : (" " + property));
        }
    }
}
