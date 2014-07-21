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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.sdk.Resources;

/**
 * Simple Resource class used to expose the SCIM API on Users endpoint
 * 
 * @author tiry
 * 
 */
@WebObject(type = "groups")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class SCIMGroupWebObject extends BaseUMObject {

    
    protected String getPrefix() {
        return "/Groups";
    }
    
    protected GroupResource resolveGroupRessource(String uid) {

        try {
            DocumentModel groupModel = um.getGroupModel(uid);
            if (groupModel != null) {
                return mapper.getGroupResourceFromGroupModel(groupModel);
            }
        } catch (Exception e) {
            log.error("Error while resolving User", e);
        }
        return null;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + "; qs=0.9" })
    public Resources<GroupResource> getGroups(@Context
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
            String directoryName = um.getGroupDirectoryName();

            DirectoryService ds = Framework.getLocalService(DirectoryService.class);
            
            Session dSession = null;
            DocumentModelList groupModels = null;
            try {
                dSession= ds.open(directoryName);           
                groupModels = dSession.query(filter, null, orderBy, true, count, startIndex-1);
            } finally {
                dSession.close();
            }
            
            List<GroupResource> groupResources = new ArrayList<>();
            for (DocumentModel groupModel : groupModels) {
                groupResources.add(mapper.getGroupResourceFromGroupModel(groupModel));
            }            
            return new Resources<>(groupResources, groupResources.size(), startIndex);                        
        } catch (Exception e) {
            log.error("Error while getting Groups", e);        }                
        return null;
    }
    
    @Path("{uid}")
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public GroupResource getGroupResource(@Context
    UriInfo uriInfo, @PathParam("uid")
    String uid) {
        return resolveGroupRessource(uid);

    }

    @Path("{uid}.xml")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public GroupResource getGroupResourceAsXml(@Context
    UriInfo uriInfo, @PathParam("uid")
    String uid) {
        return getGroupResource(uriInfo, uid);
    }

    @Path("{uid}.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GroupResource getUserResourceAsJSON(@Context
    UriInfo uriInfo, @PathParam("uid")
    String uid) {
        return getGroupResource(uriInfo, uid);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public GroupResource createGroup(@Context
    UriInfo uriInfo, GroupResource group, @Context final HttpServletResponse response) {
        try {
            checkUpdateGuardPreconditions();
            response.setStatus(Response.Status.CREATED.getStatusCode());
            try {
                response.flushBuffer();
            }catch(Exception e){}            
            return doCreateGroup(group);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path(".xml")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_XML)
    public GroupResource createGroupAsXml(@Context
    UriInfo uriInfo, GroupResource group, @Context final HttpServletResponse response) {
        return createGroup(uriInfo, group, response);
    }

    @POST
    @Path(".json")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    public GroupResource createGroupAsJSON(@Context
    UriInfo uriInfo, GroupResource group, @Context final HttpServletResponse response) {
        return createGroup(uriInfo, group, response);
    }

    @PUT
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public GroupResource updateGroup(@Context
    UriInfo uriInfo, GroupResource user) {
        try {
            checkUpdateGuardPreconditions();
            return doUpdateGroup(user);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @PUT
    @Path(".xml")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_XML)
    public GroupResource updateUserAsXml(@Context
    UriInfo uriInfo, GroupResource group) {
        return updateGroup(uriInfo, group);
    }

    @PUT
    @Path(".json")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    public GroupResource updateUserAsJSON(@Context
    UriInfo uriInfo, GroupResource group) {
        return updateGroup(uriInfo, group);
    }

    protected GroupResource doUpdateGroup(GroupResource group) {

        try {
            DocumentModel groupModel = mapper.updateGroupModelFromGroupResource(group);
            if (groupModel!=null) {
                return mapper.getGroupResourcefromGroupModel(groupModel);
            }
        } catch (Exception e) {
            log.error("Unable to update Group", e);
        }
        return null;
    }

    protected GroupResource doCreateGroup(GroupResource group) {

        try {
            DocumentModel newGroup = mapper.createGroupModelFromGroupResource(group);
            return mapper.getGroupResourcefromGroupModel(newGroup);
        } catch (Exception e) {
            log.error("Unable to create Group", e);
        }
        return null;
    }


}
