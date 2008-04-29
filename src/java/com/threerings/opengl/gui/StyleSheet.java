//
// $Id$

package com.threerings.opengl.gui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.background.BlankBackground;
import com.threerings.opengl.gui.background.ImageBackground;
import com.threerings.opengl.gui.background.TintedBackground;
import com.threerings.opengl.gui.border.Border;
import com.threerings.opengl.gui.border.EmptyBorder;
import com.threerings.opengl.gui.border.LineBorder;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.icon.BlankIcon;
import com.threerings.opengl.gui.icon.ImageIcon;
import com.threerings.opengl.gui.text.KeyMap;
import com.threerings.opengl.gui.text.TextFactory;
import com.threerings.opengl.gui.text.DefaultKeyMap;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Defines a stylesheet which is used to configure the style (font family, font size, foreground
 * and background color, etc.) of components in the BUI library. The BUI stylesheets syntax is a
 * subset of the Cascading Style Sheet sytnax and follows its semantic conventions as well where
 * possible.
 *
 * <p> A basic stylesheet enumerating most usable values is as follows:
 * <pre>
 * style_class {
 *   // foreground and background properties
 *   color: 0 0 0;
 *   background: solid #00000088; // note the 50% alpha
 *   background: image monkey.png XX; // XX = centerx|centery|centerxy|
 *                                    //      scalex|scaley|scalexy|
 *                                    //      tilex|tiley|tilexy|
 *                                    //      framex|framey|framexy
 *   background: image monkey.png framexy top right bottom left;
 *   cursor: name;
 *
 *   // text properties
 *   font: Helvetica XX 12; // XX = normal|bold|italic|bolditalic
 *   text-align: XX; // XX = left|center|right
 *   vertical-align: XX; // XX = top|center|bottom
 *   text-effect: XX; // XX = none|outline|shadow|glow
 *   line-spacing: -2; // XX = amount of space to add/remove between lines
 *
 *   // box properties
 *   padding: top; // right=top, bottom=top, left=top
 *   padding: top, right; // bottom=top, left=right
 *   padding: top, right, bottom, left;
 *   border: 1 solid #FFCC99;
 *   border: 1 blank;
 *   size: 250 100; // overrrides component preferred size
 *
 *   // explicit inheritance
 *   parent: other_class; // other_class must be defined *before* this one
 *
 *   tooltip: other_class; // used to define the style class for the tool_tip
 * }
 * </pre>
 *
 * Each component is identified by its default stylesheet class, which are derived from the
 * component's Java class name: <code>window, label, textfield, component, popupmenu, etc.</code>
 * The component's stylesheet class can be overridden with a call to {@link
 * Component#setStyleClass}.
 *
 * <p> A component's style is resolved in the following manner:
 * <ul>
 * <li> First by looking up the property using the component's stylesheet class.

 * <li> <em>For certain properties</em>, the interface hierarchy is then climbed and each parents'
 * stylesheet class is checked for the property in question. The properties for which that applies
 * are: <code>color, font, text-align, vertical-align</code>.
 *
 * <li> Lastly the <code>root</code> stylesheet class is checked (for all properties, not just
 * those for which we climb the interface hierarchy).
 * </ul>
 *
 * <p> This resolution process is followed at the time the component is added to the interface
 * hierarchy and the result is used to configure the component. We tradeoff the relative expense of
 * doing the lookup every time the component is rendered (every frame) with the memory expense of
 * storing the style of every component in memory.
 */
public class StyleSheet
{
    /** An interface used by the stylesheet to obtain font and image resources. */
    public interface ResourceProvider
    {
        /**
         * Creates a factory that will render text using the specified font.
         */
        public TextFactory createTextFactory (
            String family, String style, int size);

        /**
         * Loads the image with the specified path.
         */
        public Image loadImage (String path) throws IOException;

        /**
         * Loads the cursor with the specified name.
         */
        public Cursor loadCursor (String name) throws IOException;
    }

    /** A font style constant. */
    public static final String PLAIN = "plain";

    /** A font style constant. */
    public static final String BOLD = "bold";

    /** A font style constant. */
    public static final String ITALIC = "italic";

    /** A font style constant. */
    public static final String BOLD_ITALIC = "bolditalic";

    /**
     * Creates a stylesheet from the specified textual source.
     */
    public StyleSheet (Reader reader, ResourceProvider rsrcprov)
        throws IOException
    {
        _rsrcprov = rsrcprov;
        StreamTokenizer tok = new StreamTokenizer(new BufferedReader(reader));
        tok.lowerCaseMode(true);
        tok.slashSlashComments(true);
        tok.slashStarComments(true);
        tok.eolIsSignificant(false);
        tok.wordChars('#', '#');
        tok.wordChars('_', '_');
        parse(tok);
    }

    public Color4f getColor (Component component, String pseudoClass)
    {
        return (Color4f)findProperty(component, pseudoClass, "color", true);
    }

    public Background getBackground (Component component, String pseudoClass)
    {
        return (Background)findProperty(component, pseudoClass, "background", false);
    }

    public Icon getIcon (Component component, String pseudoClass)
    {
        return (Icon)findProperty(component, pseudoClass, "icon", false);
    }

    public Cursor getCursor (Component component, String pseudoClass)
    {
        return (Cursor)findProperty(component, pseudoClass, "cursor", true);
    }

    public TextFactory getTextFactory (
        Component component, String pseudoClass)
    {
        return (TextFactory)findProperty(component, pseudoClass, "font", true);
    }

    public int getTextAlignment (Component component, String pseudoClass)
    {
        Integer value = (Integer)findProperty(component, pseudoClass, "text-align", true);
        return (value == null) ? UIConstants.LEFT : value.intValue();
    }

    public int getVerticalAlignment (Component component, String pseudoClass)
    {
        Integer value = (Integer)findProperty(component, pseudoClass, "vertical-align", true);
        return (value == null) ? UIConstants.CENTER : value.intValue();
    }

    public int getTextEffect (Component component, String pseudoClass)
    {
        Integer value = (Integer)findProperty(component, pseudoClass, "text-effect", true);
        return (value == null) ? UIConstants.NORMAL : value.intValue();
    }

    public int getLineSpacing (Component component, String pseudoClass)
    {
        Integer value = (Integer)findProperty(component, pseudoClass, "line-spacing", true);
        return (value == null) ? UIConstants.DEFAULT_SPACING : value.intValue();
    }

    public int getEffectSize (Component component, String pseudoClass)
    {
        Integer value = (Integer)findProperty(component, pseudoClass, "effect-size", true);
        return (value == null) ? UIConstants.DEFAULT_SIZE : value.intValue();
    }

    public Color4f getEffectColor (Component component, String pseudoClass)
    {
        return (Color4f)findProperty(component, pseudoClass, "effect-color", true);
    }

    public Insets getInsets (Component component, String pseudoClass)
    {
        Insets insets = (Insets)findProperty(component, pseudoClass, "padding", false);
        return (insets == null) ? Insets.ZERO_INSETS : insets;
    }

    public Border getBorder (Component component, String pseudoClass)
    {
        return (Border)findProperty(component, pseudoClass, "border", false);
    }

    public Dimension getSize (Component component, String pseudoClass)
    {
        return (Dimension)findProperty(component, pseudoClass, "size", false);
    }

    public String getTooltipStyle (Component component, String pseudoClass)
    {
        return (String)findProperty(component, pseudoClass, "tooltip", true);
    }

    public KeyMap getKeyMap (Component component, String pseudoClass)
    {
        return new DefaultKeyMap();
    }

    public Background getSelectionBackground (Component component, String pseudoClass)
    {
        return (Background)findProperty(component, pseudoClass, "selection-background", false);
    }

    protected Object findProperty (
        Component component, String pseudoClass, String property, boolean climb)
    {
        Object value;

        // first try this component's configured style class
        String styleClass = component.getStyleClass();
        String fqClass = makeFQClass(styleClass, pseudoClass);
        if ((value = getProperty(fqClass, property)) != null) {
            return value;
        }

        // next fall back to its un-qualified configured style
        if (pseudoClass != null) {
            if ((value = getProperty(styleClass, property)) != null) {
                return value;
            }
        }

        // if applicable climb up the hierarch to its parent and try there
        if (climb) {
            Component parent = component.getParent();
            if (parent != null) {
                return findProperty(parent, pseudoClass, property, climb);
            }
        }

        // finally check the "root" class
        fqClass = makeFQClass("root", pseudoClass);
        if ((value = getProperty(fqClass, property)) != null) {
            return value;
        }
        if (pseudoClass != null) {
            return getProperty("root", property);
        }

        return null;
    }

    protected Object getProperty (String fqClass, String property)
    {
        Rule rule = _rules.get(fqClass);
        if (rule == null) {
            return null;
        }

        // we need to lazily resolve certain properties at this time
        Object prop = rule.get(_rules, property);
        if (prop instanceof Property) {
            prop = ((Property)prop).resolve(_rsrcprov);
            rule.properties.put(property, prop);
        }
        return prop;
    }

    protected void parse (StreamTokenizer tok)
        throws IOException
    {
        while (tok.nextToken() != StreamTokenizer.TT_EOF) {
            Rule rule = startRule(tok);
            while (parseProperty(tok, rule)) {
            }
            _rules.put(makeFQClass(rule.styleClass, rule.pseudoClass), rule);
        }
    }

    protected Rule startRule (StreamTokenizer tok)
        throws IOException
    {
        if (tok.ttype != StreamTokenizer.TT_WORD) {
            fail(tok, "style-class");
        }

        Rule rule = new Rule();
        rule.styleClass = tok.sval;

        switch (tok.nextToken()) {
        case '{':
            return rule;

        case ':':
            if (tok.nextToken() != StreamTokenizer.TT_WORD) {
                fail(tok, "pseudo-class");
            }
            rule.pseudoClass = tok.sval;
            if (tok.nextToken() != '{') {
                fail(tok, "{");
            }
            return rule;

        default:
            fail(tok, "{ or :");
            return null; // not reachable
        }
    }

    protected boolean parseProperty (StreamTokenizer tok, Rule rule)
        throws IOException
    {
        if (tok.nextToken() == '}') {
            return false;
        } else if (tok.ttype != StreamTokenizer.TT_WORD) {
            fail(tok, "property-name");
        }

        int sline = tok.lineno();
        String name = tok.sval;

        if (tok.nextToken() != ':') {
            fail(tok, ":");
        }

        ArrayList<Comparable> args = new ArrayList<Comparable>();
        while (tok.nextToken() != ';' && tok.ttype != '}') {
            switch (tok.ttype) {
            case '\'':
            case '"':
            case StreamTokenizer.TT_WORD:
                args.add(tok.sval);
                break;
            case StreamTokenizer.TT_NUMBER:
                args.add(new Double(tok.nval));
                break;
            default:
                System.err.println(
                    "Unexpected token: '" + (char)tok.ttype + "'. Line " + tok.lineno() + ".");
                break;
            }
        }

        try {
            rule.properties.put(name, createProperty(name, args));
//             System.out.println("  " + name + " -> " + rule.get(name));
        } catch (Exception e) {
            System.err.println(
                "Failure parsing property '" + name + "' line " + sline + ": " + e.getMessage());
            if (!(e instanceof IllegalArgumentException)) {
                e.printStackTrace(System.err);
            }
        }
        return true;
    }

    protected Object createProperty (String name, ArrayList args)
    {
        if (name.equals("color") || name.equals("effect-color")) {
            return parseColor((String)args.get(0));

        } else if (name.equals("background") || name.equals("selection-background")) {
            BackgroundProperty bprop = new BackgroundProperty();
            bprop.type = (String)args.get(0);
            if (bprop.type.equals("solid")) {
                bprop.color = parseColor((String)args.get(1));

            } else if (bprop.type.equals("image")) {
                bprop.ipath = (String)args.get(1);
                if (args.size() > 2) {
                    String scale = (String)args.get(2);
                    Integer scval = _ibconsts.get(scale);
                    if (scval == null) {
                        throw new IllegalArgumentException(
                            "Unknown background scaling type: '" + scale + "'");
                    }
                    bprop.scale = scval.intValue();
                    if (bprop.scale == ImageBackground.FRAME_XY && args.size() > 3) {
                        bprop.frame = new Insets();
                        bprop.frame.top = parseInt(args.get(3));
                        bprop.frame.right = (args.size() > 4) ?
                            parseInt(args.get(4)) : bprop.frame.top;
                        bprop.frame.bottom = (args.size() > 5) ?
                            parseInt(args.get(5)) : bprop.frame.top;
                        bprop.frame.left = (args.size() > 6) ?
                            parseInt(args.get(6)) : bprop.frame.right;
                    }
                }

            } else if (bprop.type.equals("blank")) {
                // nothing to do

            } else {
                throw new IllegalArgumentException(
                    "Unknown background type: '" + bprop.type + "'");
            }
            return bprop;

        } else if (name.equals("icon")) {
            IconProperty iprop = new IconProperty();
            iprop.type = (String)args.get(0);
            if (iprop.type.equals("image")) {
                iprop.ipath = (String)args.get(1);

            } else if (iprop.type.equals("blank")) {
                iprop.width = parseInt(args.get(1));
                iprop.height = parseInt(args.get(2));

            } else {
                throw new IllegalArgumentException("Unknown icon type: '" + iprop.type + "'");
            }
            return iprop;

        } else if (name.equals("cursor")) {
            CursorProperty cprop = new CursorProperty();
            cprop.name = (String)args.get(0);
            return cprop;

        } else if (name.equals("font")) {
            try {
                FontProperty fprop = new FontProperty();
                fprop.family = (String)args.get(0);
                fprop.style = (String)args.get(1);
                if (!fprop.style.equals(PLAIN) && !fprop.style.equals(BOLD) &&
                    !fprop.style.equals(ITALIC) && !fprop.style.equals(BOLD_ITALIC)) {
                    throw new IllegalArgumentException("Unknown font style: '" + fprop.style + "'");
                }
                fprop.size = parseInt(args.get(2));
                return fprop;

            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw new IllegalArgumentException(
                    "Fonts must be specified as: " +
                    "\"Font name\" plain|bold|italic|bolditalic point-size");
            }

        } else if (name.equals("text-align")) {
            String type = (String)args.get(0);
            Object value = _taconsts.get(type);
            if (value == null) {
                throw new IllegalArgumentException("Unknown text-align type '" + type + "'");
            }
            return value;

        } else if (name.equals("vertical-align")) {
            String type = (String)args.get(0);
            Object value = _vaconsts.get(type);
            if (value == null) {
                throw new IllegalArgumentException("Unknown vertical-align type '" + type + "'");
            }
            return value;

        } else if (name.equals("text-effect")) {
            String type = (String)args.get(0);
            Object value = _teconsts.get(type);
            if (value == null) {
                throw new IllegalArgumentException("Unknown text-effect type '" + type + "'");
            }
            return value;

        } else if (name.equals("effect-size")) {
            Integer value = new Integer(parseInt(args.get(0)));
            return value;

        } else if (name.equals("line-spacing")) {
            Integer value = new Integer(parseInt(args.get(0)));
            return value;

        } else if (name.equals("padding")) {
            Insets insets = new Insets();
            insets.top = parseInt(args.get(0));
            insets.right = (args.size() > 1) ? parseInt(args.get(1)) : insets.top;
            insets.bottom = (args.size() > 2) ? parseInt(args.get(2)) : insets.top;
            insets.left = (args.size() > 3) ? parseInt(args.get(3)) : insets.right;
            return insets;

        } else if (name.equals("border")) {
            int thickness = parseInt(args.get(0));
            String type = (String)args.get(1);
            if (type.equals("blank")) {
                return new EmptyBorder(thickness, thickness, thickness, thickness);

            } else if (type.equals("solid")) {
                return new LineBorder(parseColor((String)args.get(2)), thickness);

            } else {
                throw new IllegalArgumentException("Unknown border type '" + type + "'");
            }

        } else if (name.equals("size")) {
            Dimension size = new Dimension();
            size.width = parseInt(args.get(0));
            size.height = parseInt(args.get(1));
            return size;

        } else if (name.equals("parent")) {
            Rule parent = _rules.get(args.get(0));
            if (parent == null) {
                throw new IllegalArgumentException("Unknown parent class '" + args.get(0) + "'");
            }
            return parent;

        } else if (name.equals("tooltip")) {
            String style = (String)args.get(0);
            return style;

        } else {
            throw new IllegalArgumentException("Unknown property '" + name + "'");
        }
    }

    protected void fail (StreamTokenizer tok, String expected)
        throws IOException
    {
        String err = "Parse failure line: " + tok.lineno() +
            " expected: '" + expected + "' found: '";
        switch (tok.ttype) {
        case StreamTokenizer.TT_WORD: err += tok.sval; break;
        case StreamTokenizer.TT_NUMBER: err += tok.nval; break;
        case StreamTokenizer.TT_EOF: err += "EOF"; break;
        default: err += (char)tok.ttype;
        }
        throw new IOException(err + "'");
    }

    protected Color4f parseColor (String hex)
    {
        if (!hex.startsWith("#") || (hex.length() != 7 && hex.length() != 9)) {
            String errmsg = "Color must be #RRGGBB or #RRGGBBAA: " + hex;
            throw new IllegalArgumentException(errmsg);
        }
        float r = Integer.parseInt(hex.substring(1, 3), 16) / 255f;
        float g = Integer.parseInt(hex.substring(3, 5), 16) / 255f;
        float b = Integer.parseInt(hex.substring(5, 7), 16) / 255f;
        float a = 1f;
        if (hex.length() == 9) {
            a = Integer.parseInt(hex.substring(7, 9), 16) / 255f;
        }
        return new Color4f(r, g, b, a);
    }

    protected int parseInt (Object arg)
    {
        return (int)((Double)arg).doubleValue();
    }

    protected static String makeFQClass (String styleClass, String pseudoClass)
    {
        return (pseudoClass == null) ? styleClass : (styleClass + ":" + pseudoClass);
    }

    protected static class Rule
    {
        public String styleClass;

        public String pseudoClass;

        public HashMap<String, Object> properties = new HashMap<String, Object>();

        public Object get (HashMap rules, String key)
        {
            Object value = properties.get(key);
            if (value != null) {
                return value;
            }
            Rule prule = (Rule)properties.get("parent");
            return (prule != null) ? prule.get(rules, key) : null;
        }

        @Override // from Object
        public String toString () {
            return "[class=" + styleClass + ", pclass=" + pseudoClass + "]";
        }
    }

    protected static abstract class Property
    {
        public abstract Object resolve (ResourceProvider rsrcprov);
    }

    protected static class FontProperty extends Property
    {
        String family;
        String style;
        int size;

        @Override // from Property
        public Object resolve (ResourceProvider rsrcprov) {
//             System.out.println("Resolving text factory [family=" + family +
//                                ", style=" + style + ", size=" + size + "].");
            return rsrcprov.createTextFactory(family, style, size);
        }
    }

    protected static class BackgroundProperty extends Property
    {
        String type;
        Color4f color;
        String ipath;
        int scale = ImageBackground.SCALE_XY;
        Insets frame;

        @Override // from Property
        public Object resolve (ResourceProvider rsrcprov) {
            if (type.equals("solid")) {
                return new TintedBackground(color);

            } else if (type.equals("image")) {
                Image image;
                try {
                    image = rsrcprov.loadImage(ipath);
                } catch (IOException ioe) {
                    System.err.println("Failed to load background image '" + ipath + "': " + ioe);
                    return new BlankBackground();
                }
                return new ImageBackground(scale, image, frame);

            } else {
                return new BlankBackground();
            }
        }
    }

    protected static class IconProperty extends Property
    {
        public String type;
        public String ipath;
        public int width, height;

        @Override // from Property
        public Object resolve (ResourceProvider rsrcprov) {
            if (type.equals("image")) {
                Image image;
                try {
                    image = rsrcprov.loadImage(ipath);
                } catch (IOException ioe) {
                    System.err.println("Failed to load icon image '" + ipath + "': " + ioe);
                    return new BlankIcon(10, 10);
                }
                return new ImageIcon(image);

            } else if (type.equals("blank")) {
                return new BlankIcon(width, height);

            } else {
                return new BlankIcon(10, 10);
            }
        }
    }

    protected static class CursorProperty extends Property
    {
        public String name;

        @Override // from Property
        public Object resolve (ResourceProvider rsrcprov) {
            try {
                return rsrcprov.loadCursor(name);
            } catch (IOException ioe) {
                System.err.println("Failed to load cursor '" + name + "': " + ioe);
                return null;
            }
        }
    }

    protected ResourceProvider _rsrcprov;
    protected HashMap<String, Rule> _rules = new HashMap<String, Rule>();

    protected static HashMap<String, Integer> _taconsts = new HashMap<String, Integer>();
    protected static HashMap<String, Integer> _vaconsts = new HashMap<String, Integer>();
    protected static HashMap<String, Integer> _teconsts = new HashMap<String, Integer>();
    protected static HashMap<String, Integer> _ibconsts = new HashMap<String, Integer>();

    static {
        // alignment constants
        _taconsts.put("left", new Integer(UIConstants.LEFT));
        _taconsts.put("right", new Integer(UIConstants.RIGHT));
        _taconsts.put("center", new Integer(UIConstants.CENTER));

        _vaconsts.put("center", new Integer(UIConstants.CENTER));
        _vaconsts.put("top", new Integer(UIConstants.TOP));
        _vaconsts.put("bottom", new Integer(UIConstants.BOTTOM));

        // effect constants
        _teconsts.put("none", new Integer(UIConstants.NORMAL));
        _teconsts.put("shadow", new Integer(UIConstants.SHADOW));
        _teconsts.put("outline", new Integer(UIConstants.OUTLINE));
        _teconsts.put("plain", new Integer(UIConstants.PLAIN));
        _teconsts.put("glow", new Integer(UIConstants.GLOW));

        // background image constants
        _ibconsts.put("centerxy", new Integer(ImageBackground.CENTER_XY));
        _ibconsts.put("centerx", new Integer(ImageBackground.CENTER_X));
        _ibconsts.put("centery", new Integer(ImageBackground.CENTER_Y));
        _ibconsts.put("scalexy", new Integer(ImageBackground.SCALE_XY));
        _ibconsts.put("scalex", new Integer(ImageBackground.SCALE_X));
        _ibconsts.put("scaley", new Integer(ImageBackground.SCALE_Y));
        _ibconsts.put("tilexy", new Integer(ImageBackground.TILE_XY));
        _ibconsts.put("tilex", new Integer(ImageBackground.TILE_X));
        _ibconsts.put("tiley", new Integer(ImageBackground.TILE_Y));
        _ibconsts.put("framexy", new Integer(ImageBackground.FRAME_XY));
        _ibconsts.put("framex", new Integer(ImageBackground.FRAME_X));
        _ibconsts.put("framey", new Integer(ImageBackground.FRAME_Y));
    }
}
