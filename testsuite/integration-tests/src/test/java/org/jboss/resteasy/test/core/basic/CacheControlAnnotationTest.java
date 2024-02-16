package org.jboss.resteasy.test.core.basic;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.test.annotations.FollowUpRequired;
import org.jboss.resteasy.test.core.basic.resource.CacheAnnotationInheritance;
import org.jboss.resteasy.test.core.basic.resource.CacheControlAnnotationResource;
import org.jboss.resteasy.test.core.basic.resource.CacheControlAnnotationResourceInheritance;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for org.jboss.resteasy.annotations.cache.Cache class
 * @tpSince RESTEasy 3.0.16
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class CacheControlAnnotationTest {

    private static ResteasyClient client;

    @Deployment
    public static Archive<?> deploySimpleResource() {
        WebArchive war = TestUtil.prepareArchive(CacheControlAnnotationTest.class.getSimpleName());
        war.addClasses(CacheControlAnnotationResourceInheritance.class);

        return TestUtil.finishContainerPrepare(war, null, CacheControlAnnotationResource.class,
                CacheAnnotationInheritance.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CacheControlAnnotationTest.class.getSimpleName());
    }

    @BeforeEach
    public void setup() {
        client = (ResteasyClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test for correct value of max-age of cache annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResourceValid() {
        WebTarget base = client.target(generateURL("/maxage"));

        try (Response response = base.request().get()) {
            Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            CacheControl cc = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class)
                    .fromString(response.getHeaderString("cache-control"));
            Assertions.assertFalse(cc.isPrivate(), "Cache should not be private");
            Assertions.assertEquals(3600, cc.getMaxAge(), "Wrong age of cache");
        }
    }

    /**
     * @tpTestDetails Test for no-cache settings
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResourceNoCach() {
        WebTarget base = client.target(generateURL("/nocache"));

        try (Response response = base.request().get()) {
            Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            String value = response.getHeaderString("cache-control");
            Assertions.assertEquals("no-cache", value, "Wrong value of cache header");
            CacheControl cc = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class).fromString(value);
            Assertions.assertTrue(cc.isNoCache(), "Wrong value of cache header");
        }
    }

    /**
     * @tpTestDetails Test for no-cache settings mixed with other directives
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testResourceCompositeNoCache() {
        WebTarget base = client.target(generateURL("/composite"));

        try (Response response = base.request().get()) {
            Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            CacheControl cc = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class)
                    .fromString(response.getHeaderString("cache-control"));
            Assertions.assertTrue(cc.isNoStore(), "There must be no-store");
            Assertions.assertTrue(cc.isMustRevalidate(), "There must be must-revalidate");
            Assertions.assertTrue(cc.isPrivate(), "Cache must be private");
            Assertions.assertEquals(0, cc.getMaxAge(), "Wrong age of cache");
            Assertions.assertEquals(0, cc.getSMaxAge(), "Wrong age of shared cache");
            Assertions.assertTrue(cc.isNoCache(), "There must be no-cache");
        }
    }

    /**
     * @tpTestDetails Test for correct value of max-age of inherited cache annotation
     * @tpSince RESTEasy 7.0.0
     */
    @Test
    @Tag("AwaitingUpgradeInWildFly.class")
    @FollowUpRequired("Caused by RESTEASY-3111. Once upgraded in WildFly, we can re-enable this test")
    public void testInheritedResourceValid() {
        WebTarget base = client.target(generateURL("/inheritance"));

        try (Response response = base.request().get()) {
            Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            CacheControl cc = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class)
                    .fromString(response.getHeaderString("cache-control"));
            Assertions.assertFalse(cc.isPrivate(), "Cache should not be private");
            Assertions.assertEquals(3600, cc.getMaxAge(), "Wrong age of cache");
        }
    }
}
