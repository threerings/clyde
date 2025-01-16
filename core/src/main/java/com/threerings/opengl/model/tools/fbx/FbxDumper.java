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
        log.info(indent + node.getName() + ": " + node.getNumProperties());
        indent += "  ";
        for (int ii = 0, nn = node.getNumProperties(); ii < nn; ++ii) {
            Dump(node.getProperty(ii), indent);
        }
        for (int ii = 0, nn = node.getNumChildren(); ii < nn; ++ii) {
            Dump(node.getChild(ii), indent);
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
}
