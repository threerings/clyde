package com.threerings.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.samskivert.util.StringUtil;

/**
 * Kinda simple kinda hacky XmlFormatter.
 */
public abstract class XmlFormatter
{
  public final String format (Object obj)
  {
    StringBuilder builder = new StringBuilder("<?xml version=\"1.0\" standalone=\"yes\"?>\n");
    format(builder, obj, 0);
    return builder.toString();
  }

  protected final void format (StringBuilder builder, Object obj, int indent)
  {
    String name = getName(obj);
    if (name == null) throw new NullPointerException("Obj has unknown name: " + obj);
    indent(builder, indent);
    builder.append('<').append(name);
    List<Object> properties = getProperties(obj);
    for (int ii = 0, nn = properties.size(); ii < nn; ii += 2) {
      Object value = properties.get(ii + 1);
      if (value != null) {
        builder.append(' ').append(String.valueOf(properties.get(ii))).append("=\"");
        StringUtil.toString(builder, value, "", "");
        builder.append('"');
      }
    }
    Collection<? extends Object> nodes = getNodes(obj);
    if (nodes.isEmpty()) builder.append("/>\n");
    else {
      builder.append(">\n");
      int nextdent = indent + 2;
      for (Object subNode : nodes) {
        format(builder, subNode, nextdent);
      }
      indent(builder, indent);
      builder.append("</").append(name).append(">\n");
    }
  }

  /**
   * Get the node name of the object.
   */
  protected String getName (Object node)
  {
    return null;
  }

  /**
   * Get any subnodes.
   */
  protected Collection<? extends Object> getNodes (Object node)
  {
    return Collections.emptyList();
  }

  /**
   * Get a list of properties, which must be name / value, name / value. Values of null
   * will be omitted.
   */
  protected List<Object> getProperties (Object node)
  {
    return Collections.emptyList();
  }

  protected final void indent (StringBuilder builder, int indent)
  {
    for (int cc = 0; cc < indent; ++cc) builder.append(' ');
  }
}
