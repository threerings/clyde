//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.util;

import com.samskivert.util.Config;

//import com.threerings.presents.client.Client;
//import com.threerings.presents.dobj.DObjectManager;

/**
 * Provides access to standard services needed by code that is part of or uses the Presents
 * package.
 */
public interface PresentsContext
{
    /**
     * Provides a configuration object from which various services can obtain configuration values
     * and via the properties file that forms the basis of the configuration object, those services
     * can be customized.
     */
    Config getConfig ();

//    /**
//     * Returns a reference to the client. This reference should be valid for the life of the
//     * application.
//     */
//    Client getClient ();
//
//    /**
//     * Returns a reference to the distributed object manager. This reference is only valid for the
//     * duration of a session.
//     */
//    DObjectManager getDObjectManager ();
}
