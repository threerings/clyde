//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Extends {@link ActionEvent} to provide a generic argument.
 */
public class CommandEvent extends ActionEvent
{
    /**
     * Creates a new command event with the supplied parameters.
     */
    public CommandEvent (Object source, long when, int modifiers, String action, Object argument)
    {
        super(source, when, modifiers, action);
        _argument = argument;
    }

    /**
     * Returns the argument associated with this event.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        super.toString(buf);
        buf.append(", argument=").append(_argument);
    }

    /** The argument of the command. */
    protected Object _argument;
}
