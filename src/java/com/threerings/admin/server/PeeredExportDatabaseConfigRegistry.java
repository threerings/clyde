//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.admin.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.export.util.ExportUtil;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.peer.server.PeerManager;

/**
 * A peered database config registry that exports objects as opposed to streaming them.
 */
@Singleton
public class PeeredExportDatabaseConfigRegistry extends PeeredDatabaseConfigRegistry
{
    /**
     * Creates a configuration registry and prepares it for operation.
     *
     * @param ctx will provide access to our database.
     * @param invoker this will be used to perform all database activity (except first time
     * initialization) so as to avoid blocking the distributed object thread.
     * @param peermgr a reference to the peer manager.
     */
    @Inject public PeeredExportDatabaseConfigRegistry (
        PersistenceContext ctx, @MainInvoker Invoker invoker, PeerManager peermgr)
    {
        super(ctx, invoker, peermgr);
    }

    @Override // documentation inherited
    protected ObjectRecord createObjectRecord (String path, DObject object)
    {
        return new PeerDatabaseObjectRecord(path, object) {
            @Override protected void serialize (String name, String key, Object value) {
                setValue(key, StringUtil.hexlate(ExportUtil.toBytes(value)));
                fieldUpdated(name, value);
            }
            @Override protected Object deserialize (String value) {
                return ExportUtil.fromBytes(StringUtil.unhexlate(value));
            }
        };
    }
}
