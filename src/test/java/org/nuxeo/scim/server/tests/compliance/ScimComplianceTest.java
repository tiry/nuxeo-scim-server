package org.nuxeo.scim.server.tests.compliance;

import java.util.ArrayList;

import info.simplecloud.core.Group;
import info.simplecloud.core.User;
import info.simplecloud.scimproxy.compliance.CSP;
import info.simplecloud.scimproxy.compliance.enteties.TestResult;
import info.simplecloud.scimproxy.compliance.test.AttributeTest;
import info.simplecloud.scimproxy.compliance.test.ConfigTest;
import info.simplecloud.scimproxy.compliance.test.DeleteTest;
import info.simplecloud.scimproxy.compliance.test.FilterTest;
import info.simplecloud.scimproxy.compliance.test.PostTest;
import info.simplecloud.scimproxy.compliance.test.PutTest;
import info.simplecloud.scimproxy.compliance.test.ResourceCache;
import info.simplecloud.scimproxy.compliance.test.SortTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.scim.server.tests.ScimFeature;
import org.nuxeo.scim.server.tests.ScimServerInit;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ ScimFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = ScimServerInit.class)
public class ScimComplianceTest {
   
    @Inject
    CoreSession session;

    @Test
    public void runComplianceSuite() throws Exception {
        
        CSP csp = new CSP();
        csp.setUrl("http://localhost:18090/scim");
        csp.setVersion("/v1");
        csp.setAuthentication("basicAuth");
        csp.setUsername("Administrator");
        csp.setPassword("Administrator");
        
        ArrayList<TestResult> results = new ArrayList<TestResult>();
                
        // Config 
        ConfigTest configTest = new ConfigTest();
        results.add(configTest.getConfiguration(csp));

        // Schemas
        results.add(configTest.getSchema("Users", csp));
        results.add(configTest.getSchema("Groups", csp));
        
        ResourceCache<User> userCache = new ResourceCache<User>();
        ResourceCache<Group> groupCache = new ResourceCache<Group>();

        results.addAll(new PostTest(csp, userCache, groupCache).run());
        
        /*
        results.addAll(new FilterTest(csp, userCache, groupCache).run());
        results.addAll(new PutTest(csp, userCache, groupCache).run());
        results.addAll(new SortTest(csp, userCache, groupCache).run());
        results.addAll(new AttributeTest(csp, userCache, groupCache).run());
        results.addAll(new DeleteTest(csp, userCache, groupCache).run());
        */

        for (TestResult res : results) {
            System.out.println(res.getMessage());
        }
        
    }

}
