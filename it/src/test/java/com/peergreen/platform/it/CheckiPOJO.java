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


import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Test that factories and instances of iPOJO are all valid
 * @author Florent Benoit
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckiPOJO {

    @Inject
    private BundleContext bundleContext;

    @Test
    public void checkInject() {
        Assert.assertNotNull(bundleContext);
    }

    @Test
    public void testFactories() throws Exception {
        List<Factory> factories = new ArrayList<Factory>();
        // get all factories
        Collection<ServiceReference<Factory>> serviceReferenceFactories = bundleContext.getServiceReferences(Factory.class, null);
        for (ServiceReference<Factory> serviceReferenceFactory : serviceReferenceFactories) {
            Factory factory = bundleContext.getService(serviceReferenceFactory);
            factories.add(factory);
        }

        // check we have factories
        Assert.assertTrue(factories.size() > 5);

        StringBuilder sb = new StringBuilder();
        // Now check state
        for (Factory factory : factories) {
            if (Factory.VALID != factory.getState()) {
                sb.append(format("Factory %s is invalid%n", factory.getName()));
                sb.append(format("  missing handlers: %s%n", factory.getMissingHandlers()));
                sb.append("----------------------------------------------------------------%n");
            }
        }

        Assert.assertEquals(sb.toString(), 0, sb.length());
    }


    @Test
    public void testInstances() throws Exception {
        List<Architecture> instances = new ArrayList<Architecture>();
        // get all instances
        Collection<ServiceReference<Architecture>> architectureReferences = bundleContext.getServiceReferences(Architecture.class, null);
        for (ServiceReference<Architecture> reference : architectureReferences) {
            Architecture instance = bundleContext.getService(reference);
            instances.add(instance);
        }

        // check we have instances
        Assert.assertTrue(instances.size() > 5);

        StringBuilder sb = new StringBuilder();
        // Now check state
        for (Architecture instance : instances) {
            if (ComponentInstance.VALID != instance.getInstanceDescription().getState()) {
                sb.append(format("Instance %s is invalid%n", instance.getInstanceDescription().getName()));
                sb.append(format("%s%n", instance.getInstanceDescription().getDescription()));
                sb.append("----------------------------------------------------------------%n");
            }
        }

        Assert.assertEquals(sb.toString(), 0, sb.length());
    }

}
