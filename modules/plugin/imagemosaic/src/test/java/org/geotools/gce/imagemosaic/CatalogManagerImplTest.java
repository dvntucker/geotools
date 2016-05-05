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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geotools.TestData;
import org.geotools.factory.Hints;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test the catalog manager
 */
public class CatalogManagerImplTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testReprojectingMosaicIndex() throws IOException, URISyntaxException {
        URL storeUrl = TestData.url(this, "heterogeneous_crs");

        File testDataFolder = new File(storeUrl.toURI());
        File testDirectory = testFolder.newFolder("diffprojectionstest");
        FileUtils.copyDirectory(testDataFolder, testDirectory);
        CatalogManagerImpl catalogManager = new CatalogManagerImpl();
        Hints creationHints = new Hints();
        creationHints.put(Utils.ALLOW_HETEROGENOUS_CRS, true);
        ImageMosaicReader imReader = new ImageMosaicReader(testDirectory, creationHints, catalogManager);
        assertNotNull(imReader);
        FileUtils.forceDelete(testDirectory);


    }

}