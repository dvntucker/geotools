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

package org.geotools.dem;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geotools.data.Query;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Test that footprints get reprojected with the catalog manager
 */
public class FootprintReprojectTest extends DifferentProjectionsTest {

    @Test
    public void testDifferentProjections() throws Exception {
        assertNotNull(imReader);

        GranuleCatalog gc = imReader.getRasterManager("diffprojectionstest").getGranuleCatalog();
        assertNotNull(gc);
        Query q = new Query(gc.getTypeNames()[0], CQL.toFilter("BBOX(the_geom, -180, -90, 180, 90)"));
        assertEquals(2, gc.getGranules(q).size());
    }
}
