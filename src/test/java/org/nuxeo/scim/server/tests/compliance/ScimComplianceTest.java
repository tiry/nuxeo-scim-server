package org.nuxeo.scim.server.tests.compliance;

import info.simplecloud.core.Group;
import info.simplecloud.core.User;
import info.simplecloud.scimproxy.compliance.CSP;
import info.simplecloud.scimproxy.compliance.enteties.ReadableTestResult;
import info.simplecloud.scimproxy.compliance.enteties.TestResult;
import info.simplecloud.scimproxy.compliance.test.ConfigTest;
import info.simplecloud.scimproxy.compliance.test.DeleteTest;
import info.simplecloud.scimproxy.compliance.test.PostTest;
import info.simplecloud.scimproxy.compliance.test.PutTest;
import info.simplecloud.scimproxy.compliance.test.ResourceCache;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.scim.server.tests.ScimFeature;
import org.nuxeo.scim.server.tests.ScimServerInit;

import com.google.inject.Inject;

@RunWith(OrderedFeaturesRunner.class)
@Features({ ScimFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.CLASS, init = ScimServerInit.class)
public class ScimComplianceTest {

    @Inject
    CoreSession session;

    protected static CSP csp = null;

    protected static int textIdx = 0;

    protected static ArrayList<TestResult> results;

    protected static ResourceCache<User> userCache;

    protected static ResourceCache<Group> groupCache;

    protected static ConfigTest configTest;

    @BeforeClass
    public static void initComplianceSuite() {
        csp = new CSP();
        csp.setUrl("http://localhost:18090/scim");
        csp.setVersion("/v1");
        csp.setAuthentication("basicAuth");
        csp.setUsername("Administrator");
        csp.setPassword("Administrator");
        results = new ArrayList<TestResult>();
        userCache = new ResourceCache<User>();
        groupCache = new ResourceCache<Group>();
        configTest = new ConfigTest();
    }

    protected void verifyTests() {
        for (int i = textIdx; i < results.size(); i++) {
            ReadableTestResult result = new ReadableTestResult(results.get(i));
            System.out.println(result.getDisplay());
            Assert.assertTrue(result.getErrorMessage(), !result.isFailed());
        }
    }

    @Test
    public void test1Config() throws Exception {
        results.add(configTest.getConfiguration(csp));
        // Schemas
        results.add(configTest.getSchema("Users", csp));
        results.add(configTest.getSchema("Groups", csp));

        verifyTests();
    }

    @Test
    public void test2Create() throws Exception {
        results.addAll(new PostTest(csp, userCache, groupCache).run());
        verifyTests();
    }

    @Test
    public void test3Update() throws Exception {
        results.addAll(new PutTest(csp, userCache, groupCache).run());
        verifyTests();
    }

    @Test
    public void test4Delete() throws Exception {
        results.addAll(new DeleteTest(csp, userCache, groupCache).run());
        verifyTests();
    }

    @AfterClass
    public static void finish() {

        int nbRun = results.size();
        int nbSuccess = 0;
        int nbSkipped = 0;
        int nbFailed = 0;
        for (TestResult res : results) {
            if (res.getStatus() == TestResult.SUCCESS) {
                nbSuccess++;
            } else if (res.getStatus() == TestResult.SKIPPED) {
                nbSkipped++;
            } else if (res.getStatus() == TestResult.ERROR) {
                nbFailed++;
            }
        }

        System.out.println("Ran " + nbRun + " compliancy tests : ");
        System.out.println("   " + nbSuccess + " success ");
        System.out.println("   " + nbSkipped + " skipped ");
        System.out.println("   " + nbFailed + " failed ");

    }

}
