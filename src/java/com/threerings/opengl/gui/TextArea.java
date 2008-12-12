//
// $Id$

package com.threerings.opengl.gui;

import java.util.ArrayList;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.FontConfig;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.text.TextFactory;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays one or more lines of text which may contain basic formatting (changing of color,
 * toggling bold, italic and underline). Newline characters in the appended text will result in
 * line breaks in the on-screen layout.
 */
public class TextArea extends Container
{
    /** A font style constant. */
    public static final int PLAIN = 0;

    /** A font style constant. */
    public static final int BOLD = 1;

    /** A font style constant. */
    public static final int ITALIC = 2;

    /** A font style constant. */
    public static final int UNDERLINE = 3;

    public TextArea (GlContext ctx)
    {
        this(ctx, null);
    }

    public TextArea (GlContext ctx, String text)
    {
        super(ctx);
        _model.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                modelDidChange();
            }
        });
        if (text != null) {
            setText(text);
        }
    }

    /**
     * Returns the horizontal alignment for this component's text.
     */
    public int getHorizontalAlignment ()
    {
        if (_haligns != null) {
            int halign = _haligns[getState()];
            return (halign != -1) ? halign : _haligns[DEFAULT];
        }
        return UIConstants.LEFT;
    }

    /**
     * Returns the vertical alignment for this component's text.
     */
    public int getVerticalAlignment ()
    {
        if (_valigns != null) {
            int valign = _valigns[getState()];
            return (valign != -1) ? valign : _valigns[DEFAULT];
        }
        return UIConstants.TOP;
    }

    /**
     * Configures the preferred width of this text area (the preferred height will be calculated
     * from the font).
     */
    public void setPreferredWidth (int width)
    {
        _prefWidth = width;
    }

    /**
     * Returns a model that can be wired to a scroll bar to allow scrolling up and down through the
     * lines in this text area.
     */
    public BoundedRangeModel getScrollModel ()
    {
        return _model;
    }

    /**
     * Clears any text in this text area and appends the supplied text.
     */
    public void setText (String text)
    {
        clearText();
        appendText(text);
    }

    /**
     * Appends text with the foreground color in the plain style.
     */
    public void appendText (String text)
    {
        appendText(text, null);
    }

    /**
     * Appends text with the specified color in the plain style.
     */
    public void appendText (String text, Color4f color)
    {
        appendText(text, color, PLAIN);
    }

    /**
     * Appends text with the foreground color in the specified style.
     */
    public void appendText (String text, int style)
    {
        appendText(text, null, style);
    }

    /**
     * Appends text with the specified color and style.
     */
    public void appendText (String text, Color4f color, int style)
    {
        int offset = 0, nlidx;
        while ((nlidx = text.indexOf("\n", offset)) != -1) {
            String line = text.substring(offset, nlidx);
            _runs.add(new Run(line, color, style, true));
            offset = nlidx+1;
        }
        if (offset < text.length()) {
            _runs.add(new Run(text.substring(offset), color, style, false));
        }
        // TODO: optimize appending
        invalidate();
    }

    /**
     * Clears out the text displayed in this area.
     */
    public void clearText ()
    {
        _runs.clear();
        invalidate();
    }

    /**
     * Scrolls our display such that the sepecified line is visible.
     */
    public void scrollToLine (int line)
    {
        // TODO
    }

    /**
     * Returns the number of lines of text contained in this area.
     */
    public int getLineCount ()
    {
        return _lines.size();
    }

    /**
     * Returns a text factory suitable for creating text in the style defined by the component's
     * current state.
     */
    public TextFactory getTextFactory ()
    {
        TextFactory textfact = _textfacts[getState()];
        return (textfact != null) ? textfact : _textfacts[DEFAULT];
    }

    /**
     * Returns the effect for this component's text.
     */
    public int getTextEffect ()
    {
        if (_teffects != null) {
            int teffect = _teffects[getState()];
            return (teffect != -1) ? teffect : _teffects[DEFAULT];
        }
        return UIConstants.NORMAL;
    }

    /**
     * Returns the effect size for this component's text.
     */
    public int getEffectSize ()
    {
        if (_effsizes != null) {
            int effsize = _effsizes[getState()];
            return (effsize > 0) ? effsize : _effsizes[DEFAULT];
        }
        return UIConstants.DEFAULT_SIZE;
    }

    /**
     * Returns the color to use for our text effect.
     */
    public Color4f getEffectColor ()
    {
        if (_effcols != null) {
            Color4f effcol = _effcols[getState()];
            return (effcol != null) ? effcol : _effcols[DEFAULT];
        }
        return Color4f.WHITE;
    }

    @Override // from TextArea
    public void setEnabled (boolean enabled)
    {
        boolean wasEnabled = isEnabled();
        super.setEnabled(enabled);
        if (isAdded() && wasEnabled != isEnabled()) {
            refigureContents(getWidth());
        }
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/TextArea";
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        _haligns[state] = config.textAlignment.getConstant();
        _valigns[state] = config.verticalAlignment.getConstant();
        _teffects[state] = config.textEffect.getConstant();
        _effsizes[state] = config.effectSize;
        _effcols[state] = config.effectColor;

        FontConfig fconfig = _ctx.getConfigManager().getConfig(
            FontConfig.class, config.font);
        _textfacts[state] = (fconfig == null) ?
            null : fconfig.getTextFactory(_ctx, config.fontStyle, config.fontSize);
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();

        refigureContents(getWidth());
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        int halign = getHorizontalAlignment(), valign = getVerticalAlignment();

        // compute the total height of the lines
        int start = _model.getValue(), stop = start + _model.getExtent(), lheight = 0;
        for (int ii = start; ii < stop; ii++) {
            lheight += _lines.get(ii).height;
        }

        int x = getInsets().left, y;
        Insets insets = getInsets();
        if (valign == UIConstants.TOP) {
            y = _height - insets.top;
        } else if (valign == UIConstants.BOTTOM) {
            y = lheight + insets.bottom;
        } else { // valign == UIConstants.CENTER
            y = lheight + insets.bottom + (_height - insets.getVertical() - lheight) / 2;
        }

        // render the lines
        for (int ii = start; ii < stop; ii++) {
            Line line = _lines.get(ii);
            y -= line.height;
            if (halign == UIConstants.RIGHT) {
                x = _width - line.getWidth() - insets.right;
            } else if (halign == UIConstants.CENTER) {
                x = insets.left + (_width - insets.getHorizontal() - line.getWidth()) / 2;
            }
            line.render(renderer, x, y, _alpha);
        }
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        // lay out our text if we have not yet done so
        if (_lines.size() == 0) {
            if (_prefWidth > 0) {
                // our preferred width overrides any hint
                whint = _prefWidth;
            } else if (whint == -1) {
                // if we have no hints and no preferred width, allow arbitrarily wide lines
                whint = Short.MAX_VALUE;
            }
            refigureContents(whint);
        }

        // compute our dimensions based on the dimensions of our text
        Dimension d = new Dimension();
        for (int ii = 0, ll = _lines.size(); ii < ll; ii++) {
            Line line = _lines.get(ii);
            d.width = Math.max(line.getWidth(), d.width);
            d.height += line.height;
        }

        return d;
    }

    /**
     * Reflows the entirety of our text.
     */
    protected void refigureContents (int width)
    {
        // if we're not yet added to the heirarchy, we can stop now
        if (!isAdded()) {
            return;
        }

        // remove and recreate our existing lines
        _lines.clear();

        int insets = getInsets().getHorizontal();
        int maxWidth = (width - insets);

        // wrap our text into lines
        Line current = null;
        for (int ii = 0, ll = _runs.size(); ii < ll; ii++) {
            Run run = _runs.get(ii);
            if (current == null) {
                _lines.add(current = new Line());
            }
            int offset = 0;
            Color4f color = (run.color == null) ? getColor() : run.color;
            while ((offset = current.addRun(
                        getTextFactory(), run, color, getTextEffect(),
                        getEffectSize(), getEffectColor(), maxWidth, offset)) > 0) {
                _lines.add(current = new Line());
            }
            if (run.endsLine) {
                current = null;
            }
        }

        // determine how many lines we can display in total
        insets = getInsets().getVertical();

        // start at the last line and see how many we can fit
        int lines = 0, lheight = 0;
        for (int ll = _lines.size()-1; ll >= 0; ll--) {
            lheight += _lines.get(ll).height;
            if (lheight > _height-insets) {
                break;
            }
            lines++;
        }

        // update our model (which will cause the text to be repositioned)
        int sline = Math.max(0, _lines.size() - lines);
        if (!_model.setRange(0, sline, lines, _lines.size())) {
            // we need to force adjustment of the text even if we didn't change anything because we
            // wiped out and recreated all of our lines
            modelDidChange();
        }
    }

    /**
     * Called when our model has changed (due to scrolling by a scroll bar or a call to {@link
     * #scrollToLine}, etc.).
     */
    protected void modelDidChange ()
    {
    }

    /** Used to associate a style with a run of text. */
    protected static class Run
    {
        public String text;
        public Color4f color;
        public int style;
        public boolean endsLine;

        public Run (String text, Color4f color, int style, boolean endsLine) {
            this.text = text;
            this.color = color;
            this.style = style;
            this.endsLine = endsLine;
        }
    }

    /** Contains the segments of text on a single line. */
    protected static class Line
    {
        /** The run that starts this line. */
        public Run start;

        /** The run that ends this line. */
        public Run end;

        /** The current x position at which new text will be appended. */
        public int dx;

        /** The height of this line. */
        public int height;

        /** A list of {@link Text} instances for the text on this line. */
        public ArrayList<Text> segments = new ArrayList<Text>();

        /**
         * Adds the supplied run to the line using the supplied text factory, returns the offset
         * into the run that must be appeneded to a new line or -1 if the entire run was appended.
         */
        public int addRun (TextFactory tfact, Run run, Color4f color, int effect,
                           int effectSize, Color4f effectColor, int maxWidth, int offset)
        {
            if (dx == 0) {
                start = run;
            }
            String rtext = run.text.substring(offset);
            // TODO: this could perhaps be done more efficiently now that the text factory breaks
            // things down into multiple lines for us
            Text[] text = tfact.wrapText(
                rtext, color, effect, effectSize, effectColor, maxWidth-dx);
            segments.add(text[0]);
            // we only ever add runs when we're added
            int remainder = rtext.length() - text[0].getLength();
            height = Math.max(height, text[0].getSize().height);
            dx += text[0].getSize().width;
            return (remainder == 0) ? -1 : run.text.length() - remainder;
        }

        /**
         * Renders this line of text.
         */
        public void render (Renderer renderer, int x, int y, float alpha)
        {
            int dx = x;
            for (int ii = 0, ll = segments.size(); ii < ll; ii++) {
                Text text = segments.get(ii);
                text.render(renderer, dx, y, alpha);
                dx += text.getSize().width;
            }
        }

        /**
         * Returns the width of this line.
         */
        public int getWidth ()
        {
            int width = 0;
            for (int ii = 0, ll = segments.size(); ii < ll; ii++) {
                width += segments.get(ii).getSize().width;
            }
            return width;
        }
    }

    protected int[] _haligns = new int[getStateCount()];
    protected int[] _valigns = new int[getStateCount()];
    protected int[] _teffects = new int[getStateCount()];
    protected int[] _effsizes = new int[getStateCount()];
    protected Color4f[] _effcols = new Color4f[getStateCount()];
    protected TextFactory[] _textfacts = new TextFactory[getStateCount()];

    protected BoundedRangeModel _model = new BoundedRangeModel(0, 0, 0, 0);
    protected int _prefWidth = -1;

    protected ArrayList<Run> _runs = new ArrayList<Run>();
    protected ArrayList<Line> _lines = new ArrayList<Line>();
}
