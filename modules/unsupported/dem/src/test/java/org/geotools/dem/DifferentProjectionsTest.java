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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.CatalogManager;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.test.TestData;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Created by devon on Jun/13/2016.
 */
public class DifferentProjectionsTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder crsMosaicFolder = new TemporaryFolder();

    protected ImageMosaicReader imReader;

    private File testDirectory;

    @Before
    public void createTestData() throws IOException, URISyntaxException {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        URL testDataURL = TestData.url(this, "diffprojections");
        File testDataFolder = new File(testDataURL.toURI());
        testDirectory = testFolder.newFolder("diffprojectionstest");
        FileUtils.copyDirectory(testDataFolder, testDirectory);
        CatalogManager catalogManager = new DEMCatalogManager();
        Hints creationHints = new Hints();
        imReader = new ImageMosaicReader(testDirectory, creationHints, catalogManager);
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.forceDelete(testDirectory);
    }
}
