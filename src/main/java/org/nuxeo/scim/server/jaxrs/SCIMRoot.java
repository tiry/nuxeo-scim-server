/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */

package org.nuxeo.scim.server.jaxrs;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * The root entry for the WebEngine module.
 *
 * @since 5.7.2
 */
@Path("/scim/v1")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "SCIMRoot")
public class SCIMRoot extends ModuleRoot {
    
    @Path("/Users")
    public Object doGetUsersResource() {
        return newObject("users");
    }

    @Path("/Groups")
    public Object doGetGroups() {
        return newObject("groups");
    }

    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity("not authorized").type(
                    "text/plain").build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(e.getMessage()).type(
                    "text/plain").build();
        } else {
            return super.handleError(e);
        }
    }
}
