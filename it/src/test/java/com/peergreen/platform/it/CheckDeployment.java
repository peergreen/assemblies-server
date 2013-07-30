/**
 * Copyright 2013 Peergreen S.A.S.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.platform.it;


import static com.peergreen.deployment.DeploymentMode.DEPLOY;
import static com.peergreen.deployment.DeploymentMode.UNDEPLOY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.peergreen.deployment.Artifact;
import com.peergreen.deployment.ArtifactBuilder;
import com.peergreen.deployment.ArtifactProcessRequest;
import com.peergreen.deployment.DeploymentMode;
import com.peergreen.deployment.DeploymentService;
import com.peergreen.deployment.facet.content.XMLContent;
import com.peergreen.deployment.model.ArtifactModel;
import com.peergreen.deployment.model.ArtifactModelManager;
import com.peergreen.deployment.model.Wire;
import com.peergreen.deployment.model.WireScope;
import com.peergreen.deployment.report.DeploymentStatusReport;

/**
 * Test basic operations of deployment
 * Check injection, and then check that a bundle can be deployed and undeployed
 * @author Florent Benoit
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckDeployment {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private DeploymentService deploymentService;

    @Inject
    private ArtifactBuilder artifactBuilder;

    @Inject
    private ArtifactModelManager artifactModelManager;

    private URI fileURIBundle;

    private Artifact artifact;


    @Before
    public void init() throws Exception {
        // M2 URI of ow2 util file
        this.fileURIBundle = new URI("mvn:org.ow2.util.file/file/2.0.0");
        this.artifact = artifactBuilder.build("file-bundle.jar", fileURIBundle);
    }

    @Test
    public void checkInject() {
        assertNotNull(bundleContext);
        assertNotNull(deploymentService);
        assertNotNull(artifactBuilder);
        assertNotNull(artifactModelManager);
    }

    @Test
    public void test1DeployBundle() throws Exception {

        // Check not here
        Collection<URI> uris = artifactModelManager.getDeployedRootURIs();
        assertFalse(uris.contains(fileURIBundle));

        int beforeBundlesLength = bundleContext.getBundles().length;

        ArtifactProcessRequest artifactProcessRequest = new ArtifactProcessRequest(artifact);
        artifactProcessRequest.setDeploymentMode(DeploymentMode.DEPLOY);
        deploymentService.process(Collections.singleton(artifactProcessRequest));

        // Check here
        uris = artifactModelManager.getDeployedRootURIs();
        assertTrue(uris.contains(fileURIBundle));

        // new bundle on the platform
        int afterBundlesLength = bundleContext.getBundles().length;
        assertEquals(beforeBundlesLength + 1, afterBundlesLength);

    }


    @Test
    public void test2UndeployBundle() throws Exception {

        // Check here
        Collection<URI> uris = artifactModelManager.getDeployedRootURIs();
        assertTrue(uris.contains(fileURIBundle));

        int beforeBundlesLength = bundleContext.getBundles().length;

        // Undeploy order
        ArtifactProcessRequest artifactProcessRequest = new ArtifactProcessRequest(artifact);
        artifactProcessRequest.setDeploymentMode(DeploymentMode.UNDEPLOY);
        deploymentService.process(Collections.singleton(artifactProcessRequest));

        // Check not here anymore
        uris = artifactModelManager.getDeployedRootURIs();
        assertFalse(uris.contains(fileURIBundle));

        // removed bundle on the platform
        int afterBundlesLength = bundleContext.getBundles().length;
        assertEquals(beforeBundlesLength - 1, afterBundlesLength);

    }


    @Test
    public void testResourceDeploymentPGK100() throws Exception {
        URL url = CheckDeployment.class.getResource("/resource-maven.xml");
        assertNotNull(url);

        // Get URI
        URI uri = url.toURI();

        Artifact resourceArtifact = artifactBuilder.build("resource",  uri);

        // Deploy
        ArtifactProcessRequest artifactProcessRequest = new ArtifactProcessRequest(resourceArtifact);
        artifactProcessRequest.setDeploymentMode(DeploymentMode.DEPLOY);
        deploymentService.process(Collections.singleton(artifactProcessRequest));

        // Check here
        Collection<URI> uris = artifactModelManager.getDeployedRootURIs();
        uris = artifactModelManager.getDeployedRootURIs();
        assertTrue(uris.contains(uri));

        ArtifactModel artifactModel = artifactModelManager.getArtifactModel(uri);
        assertNotNull(artifactModel);

        Artifact artifactFound = artifactModel.getArtifact();
        assertNotNull(artifactFound);

        XMLContent xmlContent = artifactFound.as(XMLContent.class);
        assertNotNull(xmlContent);

        // Test that this is the right deployment plan
        assertEquals("http://jonas.ow2.org/ns/deployment-plan/1.0", xmlContent.namespace());

        // Test that the bundle has been deployed (two depths of model)
        Iterable<? extends Wire> wires = artifactModel.getWires(WireScope.FROM);
        assertNotNull(wires);

        Iterator<? extends Wire> itWires = wires.iterator();
        assertTrue(itWires.hasNext());

        Wire wire = itWires.next();
        ArtifactModel to = wire.getTo();
        wires = to.getWires(WireScope.FROM);
        assertNotNull(wires);
        wire = wires.iterator().next();
        to = wire.getTo();

        Bundle bundle = to.getArtifact().as(Bundle.class);
        assertNotNull(bundle);
        assertEquals("org.ow2.util.base64", bundle.getSymbolicName());


        // Now undeploy it
        artifactProcessRequest = new ArtifactProcessRequest(resourceArtifact);
        artifactProcessRequest.setDeploymentMode(UNDEPLOY);
        DeploymentStatusReport report = deploymentService.process(Collections.singleton(artifactProcessRequest));
        assertFalse(report.isFailed());

    }


    @Test
    public void testPGK110() throws Exception {
        URI fileURI = new URI("mvn:org.ow2.util.base64/base64/2.0.0");
        Artifact webConsoleCommunity = artifactBuilder.build("base-64-with-no-extension-file", fileURI);
        ArtifactProcessRequest artifactProcessRequest = new ArtifactProcessRequest(webConsoleCommunity);
        artifactProcessRequest.setDeploymentMode(DEPLOY);
        DeploymentStatusReport report = deploymentService.process(Collections.singleton(artifactProcessRequest));
        assertFalse(report.isFailed());
        ArtifactModel artifactModel = artifactModelManager.getArtifactModel(fileURI);
        assertNotNull(artifactModel);

        // Test that the bundle has been deployed (One depth of model)
        Iterable<? extends Wire> wires = artifactModel.getWires(WireScope.FROM);
        assertNotNull(wires);

        Iterator<? extends Wire> itWires = wires.iterator();
        assertTrue(itWires.hasNext());

        Wire wire = itWires.next();
        ArtifactModel to = wire.getTo();

        Artifact artifactFound = to.getArtifact();
        assertNotNull(artifactFound);



        // Check we have a bundle
        Bundle bundle = artifactFound.as(Bundle.class);
        assertNotNull(bundle);
        assertEquals("org.ow2.util.base64", bundle.getSymbolicName());


      // Now undeploy it
      artifactProcessRequest = new ArtifactProcessRequest(webConsoleCommunity);
      artifactProcessRequest.setDeploymentMode(UNDEPLOY);
      report = deploymentService.process(Collections.singleton(artifactProcessRequest));
      assertFalse(report.isFailed());

    }


}
