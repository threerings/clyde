//
// $Id$

package com.threerings.editor.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MouseInputAdapter;

/**
 * A spinner that allows the user to rapidly change the value by clicking on the arrows and
 * dragging the mouse cursor right or left.
 */
public class DraggableSpinner extends JSpinner
{
    public DraggableSpinner (double value, double minimum, double maximum, double stepSize)
    {
        this(Double.valueOf(value), Double.valueOf(minimum),
            Double.valueOf(maximum), Double.valueOf(stepSize));
    }

    public DraggableSpinner (float value, float minimum, float maximum, float stepSize)
    {
        this(Float.valueOf(value), Float.valueOf(minimum),
            Float.valueOf(maximum), Float.valueOf(stepSize));
    }

    public DraggableSpinner (int value, int minimum, int maximum, int stepSize)
    {
        this(Integer.valueOf(value), Integer.valueOf(minimum),
            Integer.valueOf(maximum), Integer.valueOf(stepSize));
    }

    public DraggableSpinner (Number value, Comparable minimum, Comparable maximum, Number stepSize)
    {
        this(new NumberModel(value, minimum, maximum, stepSize));
    }

    public DraggableSpinner (final SpinnerNumberModel model)
    {
        super(model);
        MouseInputAdapter adapter = new MouseInputAdapter() {
            public void mousePressed (MouseEvent event) {
                _lx = event.getX();
            }
            public void mouseDragged (MouseEvent event) {
                int dx = event.getX() - _lx;
                _lx = event.getX();
                Object ovalue = getValue();
                _suppressStateChanged = true;
                try {
                    for (int dir = (dx < 0) ? +1 : -1; dx != 0; dx += dir) {
                        Object nvalue = (dx < 0) ? getPreviousValue() : getNextValue();
                        if (nvalue == null) {
                            setValue(dx < 0 ? model.getMinimum() : model.getMaximum());
                            break;
                        }
                        setValue(nvalue);
                    }
                } finally {
                    _suppressStateChanged = false;
                }
                if (!getValue().equals(ovalue)) {
                    fireStateChanged();
                }
            }
            protected int _lx;
        };
        for (int ii = 0, nn = getComponentCount(); ii < nn; ii++) {
            Component child = getComponent(ii);
            if (child instanceof JButton) {
                child.addMouseListener(adapter);
                child.addMouseMotionListener(adapter);
            }
        }

        // set the minimum fraction digits in the display based on the step size
        double step = model.getStepSize().doubleValue();
        int digits = (int)Math.round(-Math.log(step) / Math.log(10.0));
        ((NumberEditor)getEditor()).getFormat().setMinimumFractionDigits(
            Math.max(digits, 0));
        ((NumberEditor)getEditor()).getTextField().setValue(getValue());

        // set the preferred width to allow some room to grow
        Dimension dim = getPreferredSize();
        setPreferredSize(new Dimension(65, dim.height));
    }

    /**
     * Convenience method to get the value as a double.
     */
    public double getDoubleValue ()
    {
        return ((Number)getValue()).doubleValue();
    }

    /**
     * Convenience method to get the value as a float.
     */
    public float getFloatValue ()
    {
        return ((Number)getValue()).floatValue();
    }

    /**
     * Convenience method to get the value as an integer.
     */
    public int getIntValue ()
    {
        return ((Number)getValue()).intValue();
    }

    @Override // documentation inherited
    protected void fireStateChanged ()
    {
        if (!_suppressStateChanged) {
            super.fireStateChanged();
        }
    }

    /**
     * Gets rid of pesky "negative zero" values.
     */
    protected static class NumberModel extends SpinnerNumberModel
    {
        public NumberModel (Number value, Comparable minimum, Comparable maximum, Number stepSize)
        {
            super(value, minimum, maximum, stepSize);
            setValue(value);
        }

        @Override // documentation inherited
        public void setValue (Object value)
        {
            if (value instanceof Float && ((Float)value) == 0f) {
                super.setValue(0f);
            } else if (value instanceof Double && ((Double)value) == 0.0) {
                super.setValue(0.0);
            } else {
                super.setValue(value);
            }
        }
    }

    /** When set, suppresses the state-changed event. */
    protected boolean _suppressStateChanged;
}
