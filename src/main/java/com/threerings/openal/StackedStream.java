//
// $Id$

package com.threerings.openal;

import java.io.IOException;

import static com.threerings.openal.Log.log;

public class StackedStream extends ResourceStream
{
    /**
     * Creates a new stacked stream.
     */
    public StackedStream (SoundManager soundmgr, String file, boolean loop, String stack)
        throws IOException
    {
        super(soundmgr, file, loop);
        _stack = stack;
    }

    /**
     * Returns the name of the stack.
     */
    public String getStack ()
    {
        return _stack;
    }

    /**
     * Returns the level of the stack.
     */
    public int getLevel ()
    {
        return _level;
    }

    /**
     * Pushes the stream onto the stack, stopping any streams on top of the stack.
     */
    public void push (float interval, boolean current)
    {
        if (_level > -1) {
            log.warning("Can't push stream already on the stack", "level", _level);
            return;
        }
        int top = getTop();
        if (top > -1 && !current) {
            for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                if (sameStack(stream) && ((StackedStream)stream).getLevel() == top) {
                    if (interval > 0f) {
                        stream.fadeOut(interval, false);
                    } else {
                        stream.stop();
                    }
                }
            }
        }
        _level = top;
        if (top == -1 || !current) {
            _level++;
        }
        if (interval > 0f) {
            fadeIn(interval);
        } else {
            play();
        }
    }

    /**
     * Pops the stream off the stack, resuming any streams that were below it.
     */
    public void pop (float interval)
    {
        if (_level == -1) {
            log.warning("Can't pop stream not on the stack");
            return;
        }
        if (interval > 0f) {
            fadeOut(interval, true);
        } else {
            dispose();
        }
        int oldLevel = _level;
        _level = -1;
        int top = getTop();
        if (top > -1 && top < oldLevel) {
            for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                if (sameStack(stream) && ((StackedStream)stream).getLevel() == top &&
                        stream._fadeMode != FadeMode.OUT_DISPOSE) {
                    if (interval > 0f) {
                        stream.fadeIn(interval);
                    } else {
                        stream.play();
                    }
                }
            }
        }
    }

    /**
     * Returns true if this stream is on the same stack.
     */
    protected boolean sameStack (com.threerings.openal.Stream other)
    {
        if (other instanceof StackedStream) {
            String stack = ((StackedStream)other).getStack();
            return _stack == null ? stack == null : _stack.equals(stack);
        }
        return false;
    }

    /**
     * Returns the top level of the stack.
     */
    protected int getTop ()
    {
        int top = -1;
        for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
            if (sameStack(stream)) {
                top = Math.max(top, ((StackedStream)stream).getLevel());
            }
        }
        return top;
    }

    /** The stack this stream is on. */
    protected String _stack;

    /** The level of the stack this stream is on. */
    protected int _level = -1;
}
