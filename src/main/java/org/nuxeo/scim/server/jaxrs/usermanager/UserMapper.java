package org.nuxeo.scim.server.jaxrs.usermanager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.SCIMConstants;

public class UserMapper {

    protected UserManager um;

    protected final String baseUrl;

    public UserMapper(String baseUrl) {
        um = Framework.getLocalService(UserManager.class);
        this.baseUrl = baseUrl;
    }

    public GroupResource getGroupResourceFromGroupModel(DocumentModel groupModel) {
        return null;
    }

    public UserResource getUserResourcefromUserModel(DocumentModel userModel)
            throws Exception {

        UserResource userResource = new UserResource(CoreSchema.USER_DESCRIPTOR);

        String userId = (String) userModel.getProperty(um.getUserSchemaName(),
                um.getUserIdField());
        userResource.setUserName(userId);
        userResource.setId(userId);
        userResource.setExternalId(userId);

        String fname = (String) userModel.getProperty(um.getUserSchemaName(),
                "firstName");
        String lname = (String) userModel.getProperty(um.getUserSchemaName(),
                "lastName");
        String email = (String) userModel.getProperty(um.getUserSchemaName(),
                "email");
        String company = (String) userModel.getProperty(um.getUserSchemaName(),
                "company");

        String displayName = fname + " " + lname;
        userResource.setDisplayName(displayName);
        Collection<Entry<String>> emails = new ArrayList<>();
        emails.add(new Entry<String>(email, "string"));
        userResource.setEmails(emails);

        Name fullName = new Name(displayName, lname, "", fname, "", "");
        userResource.setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
                "name", Name.NAME_RESOLVER, fullName);
        URI location = new URI(baseUrl + "/" + userId);
        Meta meta = new Meta(null, null, location, null);
        userResource.setMeta(meta);

        // manage groups
        List<String> groupIds = um.getPrincipal(userId).getAllGroups();
        Collection<Entry<String>> groups = new ArrayList<>();
        for (String groupId : groupIds) {
            groups.add(new Entry<String>(groupId, "string"));
        }
        userResource.setGroups(groups);
        
        userResource.setActive(true);
        
        return userResource;
    }

    public DocumentModel createUserModelFromUserResource(UserResource user)
            throws ClientException {

        DocumentModel newUser = um.getBareUserModel();

        String userId = user.getId();
        if (userId == null || userId.isEmpty()) {
            userId = user.getUserName();
        }
        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(), userId);
        updateUserModel(newUser, user);
        return um.createUser(newUser);
    }

    public DocumentModel updateUserModelFromUserResource(UserResource user)
            throws ClientException {

        DocumentModel userModel = um.getUserModel(user.getId());
        if (userModel == null) {
            return null;
        }
        updateUserModel(userModel, user);
        um.updateUser(userModel);
        return userModel;
    }

    protected void updateUserModel(DocumentModel userModel,
            UserResource userResouce) throws ClientException {
        if (userResouce.getEmails() != null
                && userResouce.getEmails().size() > 0) {
            userModel.setProperty(um.getUserSchemaName(), "email",
                    userResouce.getEmails().iterator().next().getValue());
        }
        // XXX
    }

    public DocumentModel createGroupModelFromGroupResource(GroupResource group)
            throws ClientException {

        if (um.getGroup(group.getId())==null) {
            DocumentModel newGroup = um.getBareGroupModel();
    
            String groupId = group.getId();
            if (groupId == null || groupId.isEmpty()) {
                groupId = group.getDisplayName();
            }
            newGroup.setProperty(um.getGroupSchemaName(), um.getGroupIdField(),
                    groupId);
            updateGroupModel(newGroup, group);
            return um.createGroup(newGroup);
        } else {
            return updateGroupModelFromGroupResource(group);
        }
    }

    public DocumentModel updateGroupModelFromGroupResource(GroupResource group)
            throws ClientException {

        DocumentModel groupModel = um.getGroupModel(group.getId());
        if (groupModel == null) {
            return null;
        }
        updateGroupModel(groupModel, group);
        um.updateGroup(groupModel);
        return groupModel;
    }

    protected void updateGroupModel(DocumentModel userModel,
            GroupResource groupResouce) throws ClientException {
        // XXX

    }

    public GroupResource getGroupResourcefromGroupModel(DocumentModel groupModel)
            throws Exception {

        GroupResource groupResource = new GroupResource(
                CoreSchema.GROUP_DESCRIPTOR);

        String groupId = (String) groupModel.getProperty(
                um.getGroupSchemaName(), um.getGroupIdField());

        String groupLabel = (String) groupModel.getProperty(
                um.getGroupSchemaName(), um.getGroupLabelField());

        groupResource.setDisplayName(groupLabel);
        groupResource.setId(groupId);
        groupResource.setExternalId(groupId);

        // XXX

        return groupResource;
    }

}
