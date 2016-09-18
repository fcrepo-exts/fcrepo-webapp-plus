/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.GraphStoreFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.fcrepo.http.commons.test.util.CloseableGraphStore;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.MAX_VALUE;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

/**
 * @author awoods
 * @since 2016-07-12
 */
public class AbstractResourceIT {
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

    private static Logger logger;
    private static int noAuthExpectedResponse;

    private static CloseableHttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE).setMaxConnTotal(MAX_VALUE).build();
    }

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

    private static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH + "rest/";

    private static final PoolingClientConnectionManager connectionManager =
            new PoolingClientConnectionManager();

    private static CloseableHttpClient client = createClient();

    static {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client = new DefaultHttpClient(connectionManager);
    }

    protected static HttpPost postObjMethod() {
        return postObjMethod("");
    }

    private static HttpPost postObjMethod(final String id) {
        return new HttpPost(serverAddress + id);
    }

    protected static HttpHead headObjMethod(final String id) {
        return new HttpHead(serverAddress + id);
    }

    protected static String getRandomUniqueId() {
        return randomUUID().toString();
    }

    protected void createDatastream(final String pid, final String dsid, final String content) throws IOException {
        logger.trace("Attempting to create datastream for object: {} at datastream ID: {}", pid, dsid);
        assertEquals(CREATED.getStatusCode(), getStatus(putDSMethod(pid, dsid, content)));
    }

    private static CloseableGraphStore parseTriples(final HttpEntity entity) throws IOException {
        return parseTriples(entity.getContent(), getRdfSerialization(entity));
    }

    private static CloseableGraphStore parseTriples(final InputStream content, final String contentType) {
        final Model model = ModelFactory.createDefaultModel();
        model.read(content, "", contentType);
        return new CloseableGraphStore(GraphStoreFactory.create(model));
    }

    private static String getRdfSerialization(final HttpEntity entity) {
        final MediaType mediaType = MediaType.valueOf(entity.getContentType().getValue());
        final Lang lang = RDFLanguages.contentTypeToLang(mediaType.toString());
        Assert.assertNotNull("Entity is not an RDF serialization", lang);
        return lang.getName();
    }

    private static HttpPut putDSMethod(final String pid, final String ds, final String content)
            throws UnsupportedEncodingException {
        final HttpPut put = new HttpPut(serverAddress + pid + "/" + ds);
        put.setEntity(new StringEntity(content == null ? "" : content));
        put.setHeader("Content-Type", TEXT_PLAIN);
        return put;
    }

    protected CloseableHttpResponse createObject(final String pid) {
        final HttpPost httpPost = postObjMethod("/");
        if (pid.length() > 0) {
            httpPost.addHeader("Slug", pid);
        }
        try {
            final CloseableHttpResponse response = execute(httpPost);
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return response;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CloseableGraphStore getGraphStore(final HttpUriRequest req) throws IOException {
        return getGraphStore(client, req);
    }

    private CloseableGraphStore getGraphStore(final CloseableHttpClient client, final HttpUriRequest req)
            throws IOException {
        if (!req.containsHeader("Accept")) {
            req.addHeader("Accept", "application/n-triples");
        }
        logger.debug("Retrieving RDF using mimeType: {}", req.getFirstHeader("Accept"));

        try (final CloseableHttpResponse response = client.execute(req)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            final CloseableGraphStore result = parseTriples(response.getEntity());
            logger.trace("Retrieved RDF: {}", result);
            return result;
        }

    }

    protected static int getStatus(final HttpUriRequest req) {
        try (final CloseableHttpResponse response = execute(req)) {
            final int result = getStatus(response);
            if (!(result > 199) || !(result < 400)) {
                logger.warn("Got status {}", result);
                if (response.getEntity() != null) {
                    logger.trace(EntityUtils.toString(response.getEntity()));
                }
            }
            EntityUtils.consume(response.getEntity());
            return result;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static int getStatus(final HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }


    protected static CloseableHttpResponse execute(final HttpUriRequest req) throws IOException {
        logger.debug("Executing: " + req.getMethod() + " to " + req.getURI());
        return client.execute(req);
    }

    protected static String getLocation(final HttpUriRequest req) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            EntityUtils.consume(response.getEntity());
            return getLocation(response);
        }
    }

    private static String getLocation(final HttpResponse response) {
        return response.getFirstHeader("Location").getValue();
    }


}
