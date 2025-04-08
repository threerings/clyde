package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.List;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXDataType;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;
import com.lukaseichberg.fbxloader.FBXProperty;

import com.samskivert.util.StringUtil;

// TODO: replace with java.util.function.Consumer someday
import static com.threerings.config.ConfigManager.Consumer;

import static com.threerings.opengl.Log.log;

public class FbxDumper {

    public static void main (String[] args) throws IOException {
        DumpToOut(FBXLoader.loadFBXFile(args[0]));
    }

    public static void DumpToOut (FBXFile file) {
        Consumer<String> outConsumer = new Consumer<String>() {
            public void accept (String s) {
                System.out.println(s);
            }
        };
        Dump(file, outConsumer);
    }

    public static void DumpToLog (FBXFile file) {
        Consumer<String> logConsumer = new Consumer<String>() {
            public void accept (String s) {
                log.info(s);
            }
        };
        Dump(file, logConsumer);
    }

    public static List<String> DumpToList (FBXFile file) {
        final List<String> list = new ArrayList<String>();
        Consumer<String> listConsumer = new Consumer<String>() {
            public void accept (String s) {
                list.add(s);
            }
        };
        Dump(file, listConsumer);
        return list;
    }

    public static void Dump (FBXFile file, Consumer<String> into) {
        Dump(file.getRootNode(), into);
    }

    public static void Dump (FBXNode node, Consumer<String> into) {
        Dump(node, "", into);
    }

    public static void Dump (FBXNode node, String indent, Consumer<String> into) {
        int children = node.getNumChildren();
        int props = node.getNumProperties();
        String name = node.getName();
        String ourdent = indent.substring(0, Math.max(0, indent.length() - 2)) + "+-";
        if (props == 1) {
            Dump(node.getProperty(0), ourdent + name + " ", into);
        } else if (props >= 3 && "C".equals(name)) {
            StringBuilder cc = new StringBuilder(ourdent + name + ":" + node.getData(0));
            for (int ii = 1; ii < props; ++ii) {
                cc.append(' ').append(String.valueOf(node.<Object>getData(ii)));
            }
            into.accept(cc.toString());
        } else {
            into.accept(ourdent + name + (props == 0 ? "" : (": " + props)));
            String propdent = indent + "  ";
            for (int ii = 0; ii < props; ++ii) {
                Dump(node.getProperty(ii), propdent, into);
            }
        }
        if ("Properties70".equals(name)) {
            indent += "  ";
            for (int ii = 0; ii < children; ++ii) {
                into.accept(DumpProperty70(node.getChild(ii), indent));
            }
        } else {
            indent += "| ";
            for (int ii = 0; ii < children; ++ii) {
                if (ii == children - 1) {
                    indent = indent.substring(0, indent.length() - 2) + "  ";
                }
                Dump(node.getChild(ii), indent, into);
            }
        }
    }

    public static void Dump (FBXProperty prop, String indent, Consumer<String> into) {
        FBXDataType type = prop.getDataType();
        switch (type) {
        case DOUBLE_ARRAY:
            into.accept(indent + ":double array: " + DumpArray(prop));
            break;

        case FLOAT_ARRAY:
            into.accept(indent + ":float array: " + DumpArray(prop));
            break;

        case LONG_ARRAY:
            into.accept(indent + ":long array: " + DumpArray(prop));
            break;

        case INT_ARRAY:
            into.accept(indent + ":int array: " + DumpArray(prop));
            break;

        case BOOLEAN_ARRAY:
            into.accept(indent + ":boolean array: " + DumpArray(prop));
            break;

        case STRING:
            into.accept(indent + ":string: " + prop.getData());
            break;

        case RAW:
            into.accept(indent + ":raw: " + ((byte[])prop.getData()).length);
            break;

        case SHORT:
        case BOOLEAN:
        case INT:
        case FLOAT:
        case DOUBLE:
        case LONG:
            into.accept(indent + ":" + type + ": " + prop.getData());
            break;
        }
    }

    protected static String DumpArray (FBXProperty prop)
    {
        return DumpArray(prop.getData());
    }
    protected static String DumpArray (Object array)
    {
        int len = Array.getLength(array);
        if (len > 4) return String.valueOf(len);
        return StringUtil.toString(array, "[", "]");
    }

    protected static String DumpProperty70 (FBXNode node, String indent) {
        StringBuilder buf = new StringBuilder(node.getName());
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
        return indent + buf + "(" + val + ")";
    }
}
