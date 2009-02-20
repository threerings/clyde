//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.tudey.dobj;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.delta.ReflectiveDelta;

import com.threerings.tudey.data.actor.Actor;

/**
 * Extends {@link ReflectiveDelta} to include the id of the affected actor.  Declared final for
 * streaming efficiency.
 */
public final class ActorDelta extends ReflectiveDelta
{
    /**
     * Creates a new actor delta.
     */
    public ActorDelta (Actor original, Actor revised)
    {
        super(original, revised);
        _id = original.getId();
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ActorDelta ()
    {
    }

    /**
     * Returns the id of the affected actor.
     */
    public int getId ()
    {
        return _id;
    }

    @Override // documentation inherited
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeInt(_id);
        super.writeObject(out);
    }

    @Override // documentation inherited
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        _id = in.readInt();
        super.readObject(in);
    }

    /** The id of the affected actor. */
    protected int _id;
}
