//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * A simple component for displaying a textual label.
 */
public class Label extends TextComponent
    implements UIConstants
{
    /** Configures the label's strategy when it does not fit into its allocated space. */
    public enum Fit { WRAP, TRUNCATE, SCALE };

    /**
     * Creates a label that will display the supplied text.
     */
    public Label (String text)
    {
        this(text, null);
    }

    /**
     * Creates a label that will display the supplied text using the specified style class.
     */
    public Label (String text, String styleClass)
    {
    	this(null, text, styleClass);
    }

    /**
     * Creates a label that will display the supplied icon.
     */
    public Label (Icon icon)
    {
        this(icon, null, null);
    }

    /**
     * Creates a label that will display the supplied icon using the specified style class.
     */
    public Label (Icon icon, String styleClass)
    {
    	this(icon, null, styleClass);
    }

    /**
     * Creates a label that will display the supplied text and icon using the specified style
     * class. All arguments can be null.
     */
    public Label (Icon icon, String text, String styleClass)
    {
        _label = new LabelRenderer(this);
        if (icon != null) {
            setIcon(icon);
        }
        if (text != null) {
            setText(text);
        }
        if (styleClass != null) {
            setStyleClass(styleClass);
        }
    }

    /**
     * Configures the label to display the specified icon.
     */
    public void setIcon (Icon icon)
    {
        _label.setIcon(icon);
    }

    /**
     * Returns the icon being displayed by this label.
     */
    public Icon getIcon ()
    {
        return _label.getIcon();
    }

    /**
     * Configures the gap between the icon and the text.
     */
    public void setIconTextGap (int gap)
    {
        _label.setIconTextGap(gap);
    }

    /**
     * Returns the gap between the icon and the text.
     */
    public int getIconTextGap ()
    {
        return _label.getIconTextGap();
    }

    /**
     * Sets the orientation of this label with respect to its icon. If the
     * horizontal (the default) the text is displayed to the right of the icon,
     * if vertical the text is displayed below it.
     */
    public void setOrientation (int orient)
    {
        _label.setOrientation(orient);
    }

    /**
     * Configures whether this label will wrap, truncate or scale if it cannot
     * fit text into its allotted width. The default is to wrap.
     */
    public void setFit (Fit mode)
    {
        _label.setFit(mode);
    }

    /**
     * Returns the current fit mode for this label.
     */
    public Fit getFit ()
    {
        return _label._fit;
    }

    // documentation inherited
    public void setText (String text)
    {
        _label.setText(text);
    }

    // documentation inherited
    public String getText ()
    {
        return _label.getText();
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "label";
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();
        _label.layout(getInsets(), getWidth(), getHeight());
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _label.render(renderer, 0, 0, getWidth(), getHeight(), _alpha);
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return _label.computePreferredSize(whint, hhint);
    }

    protected LabelRenderer _label;
}
