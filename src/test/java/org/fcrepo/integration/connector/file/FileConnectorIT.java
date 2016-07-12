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
package org.fcrepo.integration.connector.file;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;
import org.fcrepo.integration.http.api.AbstractResourceIT;

import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;

/**
 * Tests around using the fcrepo-connector-file
 *
 * @author awoods
 * @author ajs6f
 */
public class FileConnectorIT extends AbstractResourceIT {

    private static SimpleDateFormat headerFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    /**
     * I should be able to link to content on a federated filesystem.
     *
     * @throws IOException in case of IOException
     **/
    @Test
    public void testFederatedDatastream() throws IOException {
        final String federationAddress = serverAddress + "files/FileSystem1/ds1";
        final String linkingAddress = getLocation(postObjMethod());

        // link from the object to the content of the file on the federated filesystem
        final HttpPatch patch = new HttpPatch(linkingAddress);
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new ByteArrayEntity(
                ("INSERT DATA { <> <http://some-vocabulary#hasExternalContent> " + "<" + federationAddress + "> . }")
                .getBytes()));
        assertEquals("Couldn't link to external datastream!", NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    /**
     * Given a directory at: test-FileSystem1/ /ds1 /ds2 /TestSubdir/ and a projection of test-objects as fedora:/files,
     * then I should be able to retrieve an object from fedora:/files/FileSystem1 that lists a child object at
     * fedora:/files/FileSystem1/TestSubdir and lists datastreams ds and ds2
     *
     * @throws IOException thrown during this function
     */
    @Test
    public void testGetProjectedNode() throws IOException {
        final HttpGet method = new HttpGet(serverAddress + "files/FileSystem1");
        try (final CloseableGraphStore result = getGraphStore(method)) {
            final Node subjectURI = createURI(serverAddress + "files/FileSystem1");
            assertTrue("Didn't find the first datastream! ",
                    result.contains(ANY, subjectURI, ANY, createURI(subjectURI + "/ds1")));
            assertTrue("Didn't find the second datastream! ",
                    result.contains(ANY, subjectURI, ANY, createURI(subjectURI + "/ds2")));
            assertTrue("Didn't find the first object! ",
                    result.contains(ANY, subjectURI, ANY, createURI(subjectURI + "/TestSubdir")));
        }
    }

    /**
     * When I make changes to a resource in a federated filesystem, the parent folder's Last-Modified header should be
     * updated.
     *
     * @throws IOException in case of IOException
     **/
    @Test
    public void testLastModifiedUpdatedAfterUpdates() throws IOException {

        // create directory containing a file in filesystem
        final File fed = new File("target/test-classes/test-objects");
        final String id = getRandomUniqueId();
        final File dir = new File(fed, id);
        final File child = new File(dir, "child");
        final long timestamp1 = currentTimeMillis();
        dir.mkdir();
        child.mkdir();
        // TODO this seems really brittle
        try {
            sleep(2000);
        } catch (final InterruptedException e) {
        }

        // check Last-Modified header is current
        final long lastmod1;
        try (final CloseableHttpResponse resp1 = execute(headObjMethod("files/" + id))) {
            assertEquals(OK.getStatusCode(), getStatus(resp1));
            lastmod1 = headerFormat.parse(resp1.getFirstHeader("Last-Modified").getValue()).getTime();
            assertTrue((timestamp1 - lastmod1) < 1000); // because rounding

            // remove the file and wait for the TTL to expire
            final long timestamp2 = currentTimeMillis();
            child.delete();
            try {
                sleep(2000);
            } catch (final InterruptedException e) {
            }

            // check Last-Modified header is updated
            try (final CloseableHttpResponse resp2 = execute(headObjMethod("files/" + id))) {
                assertEquals(OK.getStatusCode(), getStatus(resp2));
                final long lastmod2 = headerFormat.parse(resp2.getFirstHeader("Last-Modified").getValue()).getTime();
                assertTrue((timestamp2 - lastmod2) < 1000); // because rounding
                assertFalse("Last-Modified headers should have changed", lastmod1 == lastmod2);
            } catch (final ParseException e) {
                fail();
            }
        } catch (final ParseException e) {
            fail();
        }
    }

    /**
     * I should be able to copy objects from a federated filesystem to the repository.
     **/
    @Test
    public void testCopyFromProjection() {
        final String destination = serverAddress + "copy-" + getRandomUniqueId() + "-ds1";
        final String source = serverAddress + "files/FileSystem1/ds1";

        // ensure the source is present
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(source)));

        // copy to repository
        final HttpCopy request = new HttpCopy(source);
        request.addHeader("Destination", destination);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        // repository copy should now exist
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(destination)));
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(source)));
    }

    /**
     * I should be able to copy objects from the repository to a federated filesystem.
     *
     * @throws IOException exception thrown during this function
     **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testCopyToProjection() throws IOException {
        // create object in the repository
        final String pid = getRandomUniqueId();
        createDatastream(pid, "ds1", "abc123");

        // copy to federated filesystem
        final HttpCopy request = new HttpCopy(serverAddress + pid);
        request.addHeader("Destination", serverAddress + "files/copy-" + pid);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        // federated copy should now exist
        final HttpGet copyGet = new HttpGet(serverAddress + "files/copy-" + pid);
        assertEquals(OK.getStatusCode(), getStatus(copyGet));

        // repository copy should still exist
        final HttpGet originalGet = new HttpGet(serverAddress + pid);
        assertEquals(OK.getStatusCode(), getStatus(originalGet));
    }

    /**
     * I should be able to move a node within a federated filesystem with properties preserved.
     *
     * @throws IOException exception thrown during this function
     **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testFederatedMoveWithProperties() throws IOException {
        // create object on federation
        final String pid = getRandomUniqueId();
        final String source = serverAddress + "files/" + pid + "/src";
        createObject("files/" + pid + "/src");

        // add properties
        final HttpPatch patch = new HttpPatch(source);
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(
                new StringEntity("insert { <> <http://purl.org/dc/elements/1.1/identifier> \"identifier.123\" . " +
                        "<> <http://purl.org/dc/elements/1.1/title> \"title.123\" } where {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        // move object
        final String destination = serverAddress + "files/" + pid + "/dst";
        final HttpMove request = new HttpMove(source);
        request.addHeader("Destination", destination);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        // check properties
        final HttpGet get = new HttpGet(destination);
        get.addHeader("Accept", "application/n-triples");
        try (final CloseableGraphStore graphStore = getGraphStore(get)) {
            assertTrue(graphStore.contains(ANY, createURI(destination),
                    createURI("http://purl.org/dc/elements/1.1/identifier"), createLiteral("identifier.123")));
            assertTrue(graphStore.contains(ANY, createURI(destination),
                    createURI("http://purl.org/dc/elements/1.1/title"), createLiteral("title.123")));
        }
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpCopy extends HttpRequestBase {

        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpCopy(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "COPY";
        }
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpMove(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "MOVE";
        }
    }
}
