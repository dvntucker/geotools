/*
 * GeoTools - The Open Source Java GIS Toolkit
 * http://geotools.org
 *
 * (C) 2016, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package org.geotools.gce.imagemosaic;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.geotools.gce.imagemosaic.acceptors.ColorCheckAcceptor;
import org.geotools.gce.imagemosaic.acceptors.DefaultGranuleAcceptorFactory;
import org.geotools.gce.imagemosaic.acceptors.GranuleAcceptor;
import org.geotools.gce.imagemosaic.acceptors.GranuleAcceptorFactorySPI;
import org.geotools.gce.imagemosaic.acceptors.GranuleAcceptorFactorySPIFinder;
import org.geotools.gce.imagemosaic.acceptors.HeterogeneousCRSAcceptorFactory;
import org.geotools.gce.imagemosaic.acceptors.HomogeneousCRSAcceptor;
import org.junit.Assert;
import org.junit.Test;


/**
 * Testing that granule collectors correctly get configured and initialized
 */
public class GranuleCollectorSPITest {

    @Test
    public void basicTest(){
        // get the SPIs
        Map<String, GranuleAcceptorFactorySPI> spiMap = GranuleAcceptorFactorySPIFinder.getGranuleAcceptorFactorySPI();
        
        // make sure it is not empty
        assertNotNull(spiMap);
        Assert.assertTrue(!spiMap.isEmpty());
        
        // check the default ones are there
        Assert.assertTrue(spiMap.containsKey(HeterogeneousCRSAcceptorFactory.class.getName()));
        Assert.assertTrue(spiMap.containsKey(DefaultGranuleAcceptorFactory.class.getName()));
        
        // check the content
        
        // HeterogeneousCRSAcceptorFactory
        assertNotNull(spiMap.get(HeterogeneousCRSAcceptorFactory.class.getName()));
        GranuleAcceptorFactorySPI spi = spiMap.get(HeterogeneousCRSAcceptorFactory.class.getName());
        List<GranuleAcceptor> acceptors = spi.create();
        assertNotNull(acceptors);
        Assert.assertTrue(acceptors.size()==1);
        GranuleAcceptor granuleAcceptor = acceptors.get(0);
        Assert.assertTrue(granuleAcceptor.getClass().equals(ColorCheckAcceptor.class));
        
        // DefaultGranuleAcceptorFactory
        assertNotNull(spiMap.get(DefaultGranuleAcceptorFactory.class.getName()));
        spi = spiMap.get(DefaultGranuleAcceptorFactory.class.getName());
        acceptors = spi.create();
        assertNotNull(acceptors);
        Assert.assertTrue(acceptors.size()==2);
        granuleAcceptor = acceptors.get(0);
        Assert.assertTrue(granuleAcceptor.getClass().equals(ColorCheckAcceptor.class)||granuleAcceptor.getClass().equals(HomogeneousCRSAcceptor.class));
        granuleAcceptor = acceptors.get(1);
        Assert.assertTrue(granuleAcceptor.getClass().equals(ColorCheckAcceptor.class)||granuleAcceptor.getClass().equals(HomogeneousCRSAcceptor.class));
        
        
    }

}