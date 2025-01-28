package com.threerings.opengl.model.tools.fbx;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXDataType;
import com.lukaseichberg.fbxloader.FBXNode;
import com.lukaseichberg.fbxloader.FBXProperty;

import static com.threerings.opengl.Log.log;

public class FbxDumper {

    public static void Dump (FBXFile file) {
        Dump(file.getRootNode());
    }

    public static void Dump (FBXNode node) {
        Dump(node, "");
    }

    public static void Dump (FBXNode node, String indent) {
        int children = node.getNumChildren();
        int props = node.getNumProperties();
        String name = node.getName();
        String ourdent = indent.substring(0, Math.max(0, indent.length() - 2)) + "+-";
        if (props == 1) {
            Dump(node.getProperty(0), ourdent + name + " ");
        } else {
            log.info(ourdent + name + (props == 0 ? "" : (": " + props)));
            String propdent = indent + "  ";
            for (int ii = 0; ii < props; ++ii) {
                Dump(node.getProperty(ii), propdent);
            }
        }
        if ("Properties70".equals(name)) {
            indent += "  ";
            for (int ii = 0; ii < children; ++ii) {
                DumpProperty70(node.getChild(ii), indent);
            }
        } else {
            indent += "| ";
            for (int ii = 0; ii < children; ++ii) {
                if (ii == children - 1) {
                    indent = indent.substring(0, indent.length() - 2) + "  ";
                }
                Dump(node.getChild(ii), indent);
            }
        }
    }

    public static void Dump (FBXProperty prop, String indent) {
        FBXDataType type = prop.getDataType();
        switch (type) {
        case DOUBLE_ARRAY:
            log.info(indent + ":double array: " + ((double[])prop.getData()).length);
            break;

        case FLOAT_ARRAY:
            log.info(indent + ":float array: " + ((float[])prop.getData()).length);
            break;

        case LONG_ARRAY:
            log.info(indent + ":long array: " + ((long[])prop.getData()).length);
            break;

        case INT_ARRAY:
            log.info(indent + ":int array: " + ((int[])prop.getData()).length);
            break;

        case BOOLEAN_ARRAY:
            log.info(indent + ":boolean array: " + ((boolean[])prop.getData()).length);
            break;

        case STRING:
            log.info(indent + ":string: " + prop.getData());
            break;

        case RAW:
            log.info(indent + ":raw: " + ((byte[])prop.getData()).length);
            break;

        case SHORT:
        case BOOLEAN:
        case INT:
        case FLOAT:
        case DOUBLE:
        case LONG:
            log.info(indent + ":" + type + ": " + prop.getData());
            break;
        }
    }

    protected static void DumpProperty70 (FBXNode node, String indent) {
        StringBuilder buf = new StringBuilder();
        StringBuilder val = new StringBuilder();
        for (int ii = 0, nn = node.getNumProperties(); ii < nn; ++ii) {
            FBXProperty prop = node.getProperty(ii);
            if (ii < 4 && prop.getDataType() == FBXDataType.STRING) {
                String str = (String)prop.getData();
                if (str != null && !"".equals(str)) {
                    if (buf.length() > 0 ) buf.append(" : ");
                    buf.append(str);
                }
            } else {
                if (val.length() > 0) val.append(", ");
                val.append(prop.getData());
            }
        }
        log.info(indent + buf + "(" + val + ")");
    }
}
