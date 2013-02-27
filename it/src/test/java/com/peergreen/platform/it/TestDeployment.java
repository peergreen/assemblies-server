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


import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

import com.peergreen.deployment.Artifact;
import com.peergreen.deployment.ArtifactBuilder;
import com.peergreen.deployment.ArtifactProcessRequest;
import com.peergreen.deployment.DeploymentMode;
import com.peergreen.deployment.DeploymentService;
import com.peergreen.deployment.model.ArtifactModelManager;

/**
 * Test basic operations of deployment
 * Check injection, and then check that a bundle can be deployed and undeployed
 * @author Florent Benoit
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDeployment {

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


    @Configuration
    public Option[] config() {
        return options(junitBundles());
    }

    @Before
    public void init() throws URISyntaxException {
        // M2 URI of ow2 util file
        this.fileURIBundle = new URI("mvn:org.ow2.util.file/file/2.0.0");
        this.artifact = artifactBuilder.build("file-bundle.jar", fileURIBundle);
    }


    @Test
    public void checkInject() {
        Assert.assertNotNull(bundleContext);
        Assert.assertNotNull(deploymentService);
        Assert.assertNotNull(artifactBuilder);
        Assert.assertNotNull(artifactModelManager);
    }

    @Test
    public void test1DeployBundle() {

        // Check not here
        Collection<URI> uris = artifactModelManager.getDeployedRootURIs();
        Assert.assertFalse(uris.contains(fileURIBundle));

        int beforeBundlesLength = bundleContext.getBundles().length;

        ArtifactProcessRequest artifactProcessRequest = new ArtifactProcessRequest(artifact);
        artifactProcessRequest.setDeploymentMode(DeploymentMode.DEPLOY);
        deploymentService.process(Collections.singleton(artifactProcessRequest));

        // Check here
        uris = artifactModelManager.getDeployedRootURIs();
        Assert.assertTrue(uris.contains(fileURIBundle));

        // new bundle on the platform
        int afterBundlesLength = bundleContext.getBundles().length;
        Assert.assertEquals(beforeBundlesLength + 1, afterBundlesLength);

    }


    @Test
    public void test2UndeployBundle() throws URISyntaxException {
        // Check here
        Collection<URI> uris = artifactModelManager.getDeployedRootURIs();
        Assert.assertTrue(uris.contains(fileURIBundle));

        int beforeBundlesLength = bundleContext.getBundles().length;

        // Undeploy order
        ArtifactProcessRequest artifactProcessRequest = new ArtifactProcessRequest(artifact);
        artifactProcessRequest.setDeploymentMode(DeploymentMode.UNDEPLOY);
        deploymentService.process(Collections.singleton(artifactProcessRequest));

        // Check not here anymore
        uris = artifactModelManager.getDeployedRootURIs();
        Assert.assertFalse(uris.contains(fileURIBundle));

        // removed bundle on the platform
        int afterBundlesLength = bundleContext.getBundles().length;
        Assert.assertEquals(beforeBundlesLength - 1, afterBundlesLength);

    }

}
