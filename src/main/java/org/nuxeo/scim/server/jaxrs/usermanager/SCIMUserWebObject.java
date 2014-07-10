/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.scim.server.jaxrs.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.sdk.Resources;

/**
 * Simple Resource class used to expose the SCIM API on Users endpoint
 * 
 * @author tiry
 * 
 */
@WebObject(type = "users")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class SCIMUserWebObject extends DefaultObject {

    protected static Log log = LogFactory.getLog(SCIMUserWebObject.class);

    protected UserManager um;

    protected UserMapper mapper;
    
    @Override
    protected void initialize(Object... args) {
        um = Framework.getLocalService(UserManager.class);
        mapper = new UserMapper();
    }

    protected UserResource resolveUserRessource(String uid) {

        try {
            DocumentModel userModel = um.getUserModel(uid);
            if (userModel != null) {
                return mapper.getUserResourcefromUserModel(userModel);
            }
        } catch (Exception e) {
            log.error("Error while resolving User", e);
        }
        return null;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + "; qs=0.9" })
    public Resources<UserResource> getUsers(@Context
            UriInfo uriInfo) {
        
        Map<String, List<String>> params = uriInfo.getQueryParameters();
        
        // filter
        Map<String, Serializable> filter = new HashMap<>(); 
        List<String> filters = params.get("filter");
        if (filters!=null && filters.size()>0) {            
            String[] filterParts = filters.get(0).split(" ");
            if (filterParts[1].equals("eq")) {
                String key = filterParts[0];
                if (key.equals("userName")) {
                    key = "username";
                }
                String value = filterParts[2];
                if (value.startsWith("\"")) {
                    value = value.substring(1,value.length()-2);
                }
                filter.put(key, value);
            }            
        }
        
        // sort
        List<String> sortCol = params.get("sortBy");
        List<String> sortType = params.get("sortOrder");
        // XXX mapping
        Map<String, String> orderBy = new HashMap<>();
        if (sortCol!=null && sortCol.size()>0) {
            String order = "asc";
            if (sortType!=null && sortType.size()>0) {
                if (sortType.get(0).equalsIgnoreCase("descending")) {
                    order = "desc";
                }
                orderBy.put(sortCol.get(0), order);                
            }
        }
        int startIndex = 1;
        if (params.get("startIndex")!=null) {
            startIndex = Integer.parseInt(params.get("startIndex").get(0));
        }
        int count = 10;
        if (params.get("count")!=null) {
            count = Integer.parseInt(params.get("count").get(0));
        }
        
        try {
            String directoryName = um.getUserDirectoryName();

            DirectoryService ds = Framework.getLocalService(DirectoryService.class);
            
            Session dSession = null;
            DocumentModelList userModels = null;
            try {
                dSession= ds.open(directoryName);           
                userModels = dSession.query(filter, null, orderBy, true, count, startIndex-1);
            } finally {
                dSession.close();
            }
            
            List<UserResource> userResources = new ArrayList<>();
            for (DocumentModel userModel : userModels) {
                userResources.add(mapper.getUserResourcefromUserModel(userModel));
            }            
            return new Resources<>(userResources, userResources.size(), startIndex);                        
        } catch (Exception e) {
            log.error("Error while getting Users", e);        }                
        return null;
    }
    
    @Path("{uid}")
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public UserResource getUserResource(@Context
    UriInfo uriInfo, @PathParam("uid")
    String uid) {
        return resolveUserRessource(uid);

    }

    @Path("{uid}.xml")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public UserResource getUserResourceAsXml(@Context
    UriInfo uriInfo, @PathParam("uid")
    String uid) {
        return getUserResource(uriInfo, uid);
    }

    @Path("{uid}.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserResource getUserResourceAsJSON(@Context
    UriInfo uriInfo, @PathParam("uid")
    String uid) {
        return getUserResource(uriInfo, uid);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public UserResource createUser(@Context
    UriInfo uriInfo, UserResource user) {
        try {
            checkUpdateGuardPreconditions();
            return doCreateUser(user);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path(".xml")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_XML)
    public UserResource createUserAsXml(@Context
    UriInfo uriInfo, UserResource user) {
        return createUser(uriInfo, user);
    }

    @POST
    @Path(".json")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    public UserResource createUserAsJSON(@Context
    UriInfo uriInfo, UserResource user) {
        return createUser(uriInfo, user);
    }

    protected UserResource doCreateUser(UserResource user) {

        try {
            DocumentModel newUser = mapper.createUserModelFromUserResource(user);
            return mapper.getUserResourcefromUserModel(newUser);
        } catch (Exception e) {
            log.error("Unable to create User", e);
        }
        return null;
    }

    protected void checkUpdateGuardPreconditions() throws ClientException {
        NuxeoPrincipal principal = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (!principal.isAdministrator()) {
            if ((!principal.isMemberOf("powerusers"))
                    || !isAPowerUserEditableArtifact()) {

                throw new WebSecurityException(
                        "User is not allowed to edit users");
            }
        }
    }

    /**
     * Check that the current artifact is editable by a power user. Basically
     * this means not an admin user or not an admin group.
     * 
     * @return
     * 
     */
    protected boolean isAPowerUserEditableArtifact() {
        return false;
    }

}
