

package org.geotools.dem;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geotools.gce.imagemosaic.CatalogManagerImpl;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.test.TestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test creating an image mosaic with two files with a different projection
 */
public class DifferentProjectionsTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testDifferentProjections() throws Exception {

        URL diffprojections = TestData.url(this, "diffprojections");
        File testDataURL = new File(diffprojections.toURI());
        File testDirectory = testFolder.getRoot();
        FileUtils.copyDirectory(testDataURL, testDirectory);

        ImageMosaicReader imReader = new ImageMosaicReader(testDirectory, null, new CatalogManagerImpl());
        assertNotNull(imReader);

        assertEquals(imReader.getGranules(imReader.getGridCoverageNames()[0], true).getCount(null), 2);

        FileUtils.forceDelete(testDirectory);
    }
}
