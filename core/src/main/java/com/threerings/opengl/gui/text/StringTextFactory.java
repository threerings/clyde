//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.gui.text;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.Image;
import com.threerings.opengl.gui.util.Dimension;

import static com.threerings.opengl.gui.Log.log;

/**
 * Formats text by using the AWT to render runs of text into a bitmap and then texturing a quad
 * with the result.  This text factory handles a simple styled text syntax:
 *
 * <pre>
 * &#064;=b(this text would be bold)
 * &#064;=i(this text would be italic)
 * &#064;=s(this text would be striked-through)
 * &#064;=u(this text would be underlined)
 * &#064;=bi(this text would be bold and italic)
 * &#064;=bi#FFCC99(this text would be bold, italic and pink)
 * </pre>
 */
public class StringTextFactory extends TextFactory
{
    /**
     * Creates a string text factory with the supplied font.
     */
    public StringTextFactory (Font font, boolean antialias)
    {
        _antialias = antialias;
        _attrs.put(TextAttribute.FONT, font);

        // we need a graphics context to figure out how big our text is going to be, but we need an
        // image to get the graphics context, but we don't want to create our image until we know
        // how big our text needs to be. dooh!
        _stub = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

        // compute the height of our font by creating a sample text and storing its height
        _height = createText("J", Color4f.BLACK).getSize().height;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _height;
    }

    // documentation inherited
    public Text createText (String text, Color4f color, int effect, int effectSize,
                            Color4f effectColor, boolean useAdvance)
    {
        if (text.equals("")) {
            text = " ";
        }

        Graphics2D gfx = _stub.createGraphics();
        TextLayout layout;
        try {
            if (_antialias) {
                gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            layout = new TextLayout(
                parseStyledText(text, _attrs, null, effect != UIConstants.PLAIN).getIterator(),
                gfx.getFontRenderContext());
        } finally {
            gfx.dispose();
        }

        return createText(text, layout, color, effect, effectSize, effectColor,
                          text.length(), useAdvance);
    }

    // documentation inherited
    public Text[] wrapText (String text, Color4f color, int effect, int effectSize,
                            Color4f effectColor, int maxWidth)
    {
        // the empty string will break things; so use a single space instead
        if (text.isEmpty()) {
            text = " ";
        }

        ArrayList<Text> texts = new ArrayList<Text>();
        Graphics2D gfx = _stub.createGraphics();
        TextLayout layout;
        try {
            if (_antialias) {
                gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }

            String[] bare = new String[1];
            AttributedString atext = parseStyledText(text, _attrs, bare, effect != UIConstants.PLAIN);
            LineBreakMeasurer measurer = new LineBreakMeasurer(
                atext.getIterator(), gfx.getFontRenderContext());
            text = bare[0];

            int pos = 0;
            while (pos < text.length()) {
                // stop at the next newline or the end of the line if there are no newlines in the
                // text
                int nextret = text.indexOf('\n', pos);
                if (nextret == -1) {
                    nextret = text.length();
                }

                // measure out as much text as we can render in one line
                layout = measurer.nextLayout(maxWidth, nextret, false);
                String origText = text.substring(pos, measurer.getPosition());

                // skip past any newline that we used to terminate our wrap
                pos = measurer.getPosition();
                if (pos < text.length() && text.charAt(pos) == '\n') {
                    pos++;
                }

                texts.add(createText(origText, layout, color,
                                     effect, effectSize, effectColor, origText.length(), true));
            }

        } finally {
            gfx.dispose();
        }

        return texts.toArray(new Text[texts.size()]);
    }

    /** Helper function. */
    protected Text createText (String origText, final TextLayout layout, Color4f color,
                               int effect, int effectSize, Color4f effectColor,
                               final int length, boolean useAdvance)
    {
        // determine the size of our rendered text
        final Dimension size = new Dimension();
        Rectangle2D bounds = layout.getBounds();

        // MacOS font rendering is buggy, so we must compute the outline and use that for bounds
        // computation and rendering
        if (effect == OUTLINE || effect == GLOW || RunAnywhere.isMacOS()) {
            bounds = layout.getOutline(null).getBounds();
        }
        if (useAdvance) {
            size.width = (int)Math.round(Math.max(bounds.getX(), 0) + layout.getAdvance());
        } else {
            size.width = (int)Math.round(Math.max(bounds.getX(), 0) + bounds.getWidth());
        }
        size.height = (int)(layout.getLeading() + layout.getAscent() + layout.getDescent());

        // blank text results in a zero sized bounds, bump it up to 1x1 to avoid freakout by the
        // BufferedImage
        size.width = Math.max(size.width, 1);
        size.height = Math.max(size.height, 1);

        switch (effect) {
        case SHADOW:
            size.width += effectSize;
            size.height += effectSize;
            break;
        case OUTLINE:
            size.width += effectSize*2;
            size.height += effectSize*2;
            break;
        case GLOW:
            size.width += effectSize*2;
            size.height += effectSize*2;
            break;
        }

        // render the text into the image
        BufferedImage bimage = new BufferedImage(size.width, size.height,
                                                 BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gfx = bimage.createGraphics();
        try {
            if (effect == OUTLINE) {
                if (_antialias) {
                    gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                         RenderingHints.VALUE_ANTIALIAS_ON);
                }
                float tx = effectSize - 1;
                float ty = layout.getAscent() + effectSize;
                gfx.translate(tx, ty);
                if (effectSize > 1) {
                    gfx.setColor(new Color(effectColor.r, effectColor.g, effectColor.b,
                                           effectColor.a));
                    Stroke oldstroke = gfx.getStroke();
                    gfx.setStroke(new BasicStroke(effectSize, BasicStroke.CAP_ROUND,
                                                  BasicStroke.JOIN_ROUND));
                    gfx.draw(layout.getOutline(null));
                    gfx.setStroke(oldstroke);
                }
                gfx.setColor(new Color(color.r, color.g, color.b, color.a));
                gfx.fill(layout.getOutline(null));
                if (effectSize == 1) {
                    gfx.setColor(new Color(effectColor.r, effectColor.g,
                                           effectColor.b, effectColor.a));
                    gfx.draw(layout.getOutline(null));
                }

            } else if (effect == GLOW ) {
                // draw the background of the glow
                char[] chars = origText.toCharArray();
                int ox = 0;
                for (char c : chars) {
                    BufferedImage img = getGlowBackground(c, size.height, effectColor, effectSize);
                    gfx.drawImage(img, null, ox, 0);
                    ox += (img.getWidth() - effectSize*2);
                }

                // draw the foreground of the glow
                ox = effectSize;
                for (char c : chars) {
                    if (c != '\n' && c != '\r') {
                        BufferedImage img = getGlowForeground(c, size.height, color, effectSize);
                        gfx.drawImage(img, null, ox, 0);
                        ox += img.getWidth();
                    }
                }

            } else {
                // if we're antialiasing, we need to set a custom compositing rule to avoid
                // incorrectly blending with the blank background
                Composite ocomp = gfx.getComposite();
                if (_antialias) {
                    gfx.setComposite(AlphaComposite.SrcOut);
                    // on the MacOS we're not using the TextLayout to render, so we have to
                    // explicitly activate anti-aliasing
                    if (RunAnywhere.isMacOS()) {
                        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                             RenderingHints.VALUE_ANTIALIAS_ON);
                    }
                }

                int dx = 0;
                if (effect == SHADOW) {
                    gfx.setColor(new Color(effectColor.r, effectColor.g,
                                           effectColor.b, effectColor.a));
                    float tx = effectSize - 1;
                    float ty = layout.getAscent() + effectSize;
                    if (RunAnywhere.isMacOS()) {
                        gfx.translate(tx, ty);
                        gfx.fill(layout.getOutline(null));
                        gfx.translate(-tx, -ty);
                    } else {
                        layout.draw(gfx, tx, ty);
                    }
                    dx = 1;
                    gfx.setComposite(ocomp);
                }

                gfx.setColor(new Color(color.r, color.g, color.b, color.a));
                if (RunAnywhere.isMacOS()) {
                    gfx.translate(dx, layout.getAscent());
                    gfx.fill(layout.getOutline(null));
                } else {
                    layout.draw(gfx, dx, layout.getAscent());
                }
            }

        } finally {
            gfx.dispose();
        }

        // TODO: render into a properly sized image in the first place and create a JME Image
        // directly
        final Image image = new Image(bimage);

//         final ByteBuffer idata =
//             ByteBuffer.allocateDirect(4 * image.getWidth() * image.getHeight());
//         idata.order(ByteOrder.nativeOrder());
//         byte[] data = (byte[])image.getRaster().getDataElements(
//             0, 0, image.getWidth(), image.getHeight(), null);
//         idata.clear();
//         idata.put(data);
//         idata.flip();

        // wrap it all up in the right object
        return new Text() {
            public int getLength () {
                return length;
            }
            public Dimension getSize () {
                return size;
            }
            public int getHitPos (int x, int y) {
                TextHitInfo info = layout.hitTestChar(x, y);
                return info.getInsertionIndex();
            }
            public int getCursorPos (int index) {
                Shape[] carets = layout.getCaretShapes(index);
                Rectangle2D bounds = carets[0].getBounds2D();
                return (int)Math.round(bounds.getX() + bounds.getWidth()/2);
            }
            public void render (Renderer renderer, int x, int y, float alpha) {
                image.render(renderer, x, y, alpha);
            }
            public void render (Renderer renderer, int x, int y, int w, int h, float alpha) {
                image.render(renderer, x, y, w, h, alpha);
            }
        };
    }

    /** Helper function. */
    protected BufferedImage getGlowBackground (char c, int height, Color4f color, int effectSize)
    {
        BufferedImage image = _cachedGlowBGs.get(_gkey.init(c, color, effectSize));
        if (image != null) {
            return image;
        }

        image = new BufferedImage(
            computeWidth(c) + effectSize*2, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gfx = image.createGraphics();
        try {
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            TextLayout layout = new TextLayout(
                String.valueOf(c), _attrs.get(TextAttribute.FONT), gfx.getFontRenderContext());
            float alphaScale = Math.max(effectSize, 2f) / 2f;
            gfx.setColor(new Color(color.r, color.g, color.b, color.a / alphaScale));
            gfx.translate(effectSize, layout.getAscent() + effectSize);
            for (int ii = effectSize; ii > 0; ii--) {
                gfx.setStroke(new BasicStroke(effectSize * ((float) ii / effectSize),
                                              BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1));
                gfx.draw(layout.getOutline(null));
            }
        } finally {
            gfx.dispose();
        }
        _cachedGlowBGs.put(_gkey.cloneKey(), image);

        return image;
    }

    /** Helper function. */
    protected BufferedImage getGlowForeground (char c, int height, Color4f color, int effectSize)
    {
        BufferedImage image = _cachedGlowFGs.get(_gkey.init(c, color, effectSize));
        if (image != null) {
            return image;
        }

        image = new BufferedImage(computeWidth(c), height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gfx = image.createGraphics();
        try {
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            TextLayout layout = new TextLayout(
                String.valueOf(c), _attrs.get(TextAttribute.FONT), gfx.getFontRenderContext());
            gfx.setColor(new Color(color.r, color.g, color.b, color.a));
            gfx.translate(0, layout.getAscent() + effectSize);
            gfx.fill(layout.getOutline(null));
        } finally {
            gfx.dispose();
        }
        _cachedGlowFGs.put(_gkey.cloneKey(), image);

        return image;
    }

    /** Helper function. */
    protected int computeWidth (char c)
    {
        Graphics2D gfx = _stub.createGraphics();
        try {
            TextLayout layout = new TextLayout(
                String.valueOf(c), _attrs.get(TextAttribute.FONT), gfx.getFontRenderContext());
            return (int) Math.ceil(layout.getAdvance());
        } finally {
            gfx.dispose();
        }
    }

    /**
     * Parses our simple styled text formatting codes and creates an attributed string to render
     * them.
     */
    protected AttributedString parseStyledText (
        String text, Map<TextAttribute, Font> attrs, String[] bare, boolean style)
    {
        // if there are no style commands in the text, skip the complexity
        if (!style || !text.contains("@=")) {
            if (bare != null) {
                bare[0] = text;
            }
            return new AttributedString(text, attrs);
        }

        // parse the style commands into an array of runs and extract the raw text along the way
        ArrayList<StyleRun> stack = new ArrayList<StyleRun>();
        ArrayList<StyleRun> runs = new ArrayList<StyleRun>();
        StringBuilder raw = new StringBuilder();
        int rawpos = 0;
        for (int ii = 0, ll = text.length(); ii < ll; ii++) {
            char c = text.charAt(ii);

            if (c == ')') { // end of run
                if (stack.size() == 0) {
                    // not a problem, this is just a bare parenthesis
                    raw.append(c);
                    rawpos++;
                } else {
                    StyleRun run = stack.remove(0);
                    run.end = rawpos;
                    runs.add(run);
                }
                continue;

            } else if (c == '@') { // start of run
                // if we don't have enough characters left in the string for a complete run, skip
                // it; we need at least 5: @=X()
                if (ii >= ll-5) {
                    raw.append(c);
                    rawpos++;
                    continue;
                }

                // anything other than @= is a non-start-sequence
                if ((c = text.charAt(++ii)) != '=') {
                    // @ ( and ) are escaped as @@ @( and @) so we skip the @
                    if (c != '@' && c != '(' && c != ')') {
                        raw.append('@');
                        rawpos++;
                    }
                    raw.append(c);
                    rawpos++;
                    continue;
                }

                // otherwise fall through and parse the run

            } else { // plain old character
                raw.append(c);
                rawpos++;
                continue;
            }

            // otherwise this is the start of a style run
            StyleRun run = new StyleRun();
            run.start = rawpos;
            stack.add(0, run);

            int parenidx = text.indexOf('(', ii);
            if (parenidx == -1) {
                log.info("Invalid style specification, missing paren [text=" + text +
                         ", pos=" + ii + "].");
                continue;
            }

            String styles = text.substring(ii+1, parenidx);
            ii = parenidx;

            run.styles = new char[styles.length()];
            for (int ss = 0, ssl = styles.length(); ss < ssl; ss++) {
                run.styles[ss] = Character.toLowerCase(styles.charAt(ss));
                if (run.styles[ss] == '#') {
                    if (ss > ssl-7) {
                        log.warning("Invalid color definition [text=" + text +
                                    ", color=" + styles.substring(ss) + "].");
                        ss = ssl;
                    } else {
                        String hex = styles.substring(ss+1, ss+7);
                        ss += 6;
                        try {
                            run.color = new Color(Integer.parseInt(hex, 16));
                        } catch (Exception e) {
                            log.warning("Invalid color definition [text=" + text +
                                        ", color=#" + hex +"].");
                        }
                    }
                }
            }
        }

        String rawtext = raw.toString();
        if (bare != null) {
            bare[0] = rawtext;
        }

        // now create an attributed string and add our styles
        AttributedString string = new AttributedString(rawtext, attrs);
        for (int ii = 0; ii < runs.size(); ii++) {
            StyleRun run = runs.get(ii);
            if (run.styles == null) {
                continue; // ignore runs we failed to parse
            }
            for (char runStyle : run.styles) {
                switch (runStyle) {
                case '#':
                    if (run.color != null) {
                        string.addAttribute(TextAttribute.FOREGROUND, run.color,
                                            run.start, run.end);
                    }
                    break;

                case 'i':
                    string.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE,
                                        run.start, run.end);
                    break;

                case 'b':
                    // setting TextAttribute.WEIGHT doesn't seem to work
                    string.addAttribute(
                        TextAttribute.FONT, attrs.get(TextAttribute.FONT).deriveFont(Font.BOLD),
                        run.start, run.end);
                    break;

                case 's':
                    string.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON,
                                        run.start, run.end);
                    break;

                case 'u':
                    string.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON,
                                        run.start, run.end);
                    break;

                case 0: // ignore blank spots
                    break;

                default:
                    log.info("Invalid style command [text=" + text +
                             ", command=" + runStyle + ", run=" + run + "].");
                    break;
                }
            }
        }

        return string;
    }

    protected static class StyleRun
    {
        public char[] styles;
        public Color color;
        public int start;
        public int end;

        public String toString () {
            StringBuilder buf = new StringBuilder();
            for (char style : styles) {
                if (style > 0) {
                    buf.append(style);
                }
            }
            if (color != null) {
                buf.append(":").append(Integer.toHexString(color.getRGB()));
            }
            buf.append(":").append(start).append("-").append(end);
            return buf.toString();
        }
    }

    protected static class GlowKey implements Cloneable
    {
        public char c;
        public Color4f color;
        public int size;

        public GlowKey init (char c, Color4f color, int size) {
            this.c = c;
            this.color = color;
            this.size = size;
            return this;
        }

        public GlowKey cloneKey () {
            try {
                return (GlowKey)super.clone();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public boolean equals (Object other) {
            if (!(other instanceof GlowKey)) {
                return false;
            }
            GlowKey okey = (GlowKey)other;
            return (c == okey.c) && (size == okey.size) && color.equals(okey.color);
        }

        public int hashCode () {
            return c ^ size ^ color.hashCode();
        }
    }

    protected boolean _antialias;
    protected int _height;
    protected BufferedImage _stub;

    protected Map<TextAttribute, Font> _attrs = new HashMap<TextAttribute, Font>();

    // for caching glow fore- and backgrounds
    protected Map<GlowKey, BufferedImage> _cachedGlowBGs = new HashMap<GlowKey, BufferedImage>();
    protected Map<GlowKey, BufferedImage> _cachedGlowFGs = new HashMap<GlowKey, BufferedImage>();

    // to avoid exercising the garbage collector
    protected GlowKey _gkey = new GlowKey();

    protected static final char NONE = '!';
    protected static final char BOLD = 'b';
    protected static final char ITALIC = 'i';
    protected static final char UNDERLINE = 'u';
    protected static final char STRIKE = 's';
    protected static final char COLOR = '#';
}
