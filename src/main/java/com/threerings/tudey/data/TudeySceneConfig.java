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

package com.threerings.tudey.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.export.Exportable;
import com.threerings.util.Copyable;
import com.threerings.util.DeepUtil;

import com.threerings.tudey.client.TudeySceneController;

/**
 * Place configuration for Tudey scenes.
 */
public class TudeySceneConfig extends PlaceConfig
    implements Exportable, Cloneable, Copyable
{
    /**
     * Returns the interval ahead of the smoothed server time (which estimates the server time
     * minus one-way latency) at which clients schedule input events.  This should be at least the
     * transmit interval (which represents the maximum amount of time that events may be delayed)
     * plus the two-way latency.
     *
     * @param pingAverage the two-way latency estimate to use in the calculation.
     */
    public int getInputAdvance (int pingAverage)
    {
        return Math.round((getTransmitInterval() + pingAverage) * 1.25f);
    }

    /**
     * Returns the interval at which clients transmit their input frames.
     */
    public int getTransmitInterval ()
    {
        return 100;
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        return copy(dest, null);
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest, Object outer)
    {
        return DeepUtil.copy(this, dest, outer);
    }

    @Override
    public PlaceController createController ()
    {
        return new TudeySceneController();
    }

    @Override
    public String getManagerClassName ()
    {
        return "com.threerings.tudey.server.TudeySceneManager";
    }

    @Override
    public TudeySceneConfig clone ()
    {
        return (TudeySceneConfig) copy(null, null);
    }

    @Override
    public boolean equals (Object other)
    {
        return DeepUtil.equals(this, other);
    }

    @Override
    public int hashCode ()
    {
        return DeepUtil.hashCode(this);
    }
}
