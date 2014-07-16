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
        userResource.setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "name", Name.NAME_RESOLVER, fullName);
        URI location = new URI(baseUrl + userId);
        Meta meta = new Meta(null, null, location, null);
        userResource.setMeta(meta);
        
        // manage groups
        List<String> groupIds = um.getPrincipal(userId).getAllGroups();
        Collection<Entry<String>> groups = new ArrayList<>();
        for (String groupId : groupIds) {
            groups.add(new Entry<String>(groupId, "string"));    
        }
        userResource.setGroups(groups);
        return userResource;
    }
    
    public DocumentModel createUserModelFromUserResource(UserResource user) throws ClientException {

        DocumentModel newUser = um.getBareUserModel();
        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(),
                user.getId());
        //newUser.setProperty(um.getUserSchemaName(), "company",user.getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "organization", AttributeValueResolver.STRING_RESOLVER));
        if (user.getEmails().size() > 0) {
            newUser.setProperty(um.getUserSchemaName(), "email",
                    user.getEmails().iterator().next().getValue());
        }
    
        return um.createUser(newUser);
    }
}
