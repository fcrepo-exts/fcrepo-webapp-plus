/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.AbstractHttpMessage;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author awoods
 */
public class SanityIT {

    /**
     * The server port of the application, set as system property by
     * maven-failsafe-plugin.
     */
    private static final String SERVER_PORT = System.getProperty("fcrepo.dynamic.test.port");

    /**
     * The context path of the application (including the leading "/"), set as
     * system property by maven-failsafe-plugin.
     */
    private static final String CONTEXT_PATH = System.getProperty("fcrepo.test.context.path");

    protected Logger logger;
    private static int noAuthExpectedResponse;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());

        logger.debug("auth.enabled: {}", System.getProperty("auth.enabled"));
        if ("true".equals(System.getProperty("auth.enabled"))) {
            noAuthExpectedResponse = UNAUTHORIZED.getStatusCode();
        } else {
            noAuthExpectedResponse = OK.getStatusCode();
        }
    }

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH;

    protected static final PoolingClientConnectionManager connectionManager =
            new PoolingClientConnectionManager();

    protected static HttpClient client;

    static {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client = new DefaultHttpClient(connectionManager);
    }

    @Test
    public void doASanityCheck() throws IOException {
        final HttpGet get = new HttpGet(serverAddress + "rest/");
        setAdminAuth(get);
        assertEquals(OK.getStatusCode(), getStatus(get));
    }

    @Test
    public void doASanityCheckNoAuth() throws IOException {
        final HttpGet get = new HttpGet(serverAddress + "rest/");
        assertEquals(noAuthExpectedResponse, getStatus(get));
    }

    @Test
    public void doSanityTranform() throws IOException {
        final HttpPost post = new HttpPost(serverAddress + "rest/");
        setAdminAuth(post);
        final HttpResponse responsePost = client.execute(post);
        assertEquals(CREATED.getStatusCode(), responsePost.getStatusLine().getStatusCode());

        final Header locationHeader = responsePost.getFirstHeader("Location");
        assertNotNull("Location header was null!", locationHeader);

        final String location = locationHeader.getValue();
        final HttpGet get = new HttpGet(location + "/fcr:transform/default");
        setAdminAuth(get);
        assertEquals(OK.getStatusCode(), getStatus(get));
    }

    protected int getStatus(final HttpUriRequest method) throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                             method.getURI());
        return client.execute(method).getStatusLine().getStatusCode();
    }

    private static void setAdminAuth(final AbstractHttpMessage method) {
        setAuth(method, "admin1", "password3");
    }

    private static void setAuth(final AbstractHttpMessage method, final String username, final String password) {
        final String creds = username + ":" + password;
        // in test configuration we don't need real passwords
        final String encCreds =
                new String(Base64.encodeBase64(creds.getBytes()));
        final String basic = "Basic " + encCreds;
        method.setHeader("Authorization", basic);
    }
}
