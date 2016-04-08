

package org.geotools.dem;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.CatalogManagerImpl;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.test.TestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test creating an image mosaic with two files with a different projection
 */
public class DifferentProjectionsTest {

    @Rule public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testDifferentProjections() throws Exception {

        URL testDataURL = TestData.url(this, "diffprojections");
        File testDataFolder = new File(testDataURL.toURI());
        File testDirectory = testFolder.newFolder("diffprojectionstest");
        FileUtils.copyDirectory(testDataFolder, testDirectory);
        CatalogManagerImpl catalogManager = new CatalogManagerImpl();
        Hints creationHints = new Hints();
        creationHints.put(Utils.ALLOW_HETEROGENOUS_CRS, true);
        ImageMosaicReader imReader = new ImageMosaicReader(testDirectory, creationHints, catalogManager);
        assertNotNull(imReader);

        assertEquals(imReader.getGranules(imReader.getGridCoverageNames()[0], true).getCount(null),
                2);

        FileUtils.forceDelete(testDirectory);
    }
}
