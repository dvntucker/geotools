package org.geotools.dem;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import org.junit.Test;

/**
 * Test creating an image mosaic with two files with a different projection
 */
public class EnsureDiffProjectionsIndexedTest extends DifferentProjectionsTest {

    @Test
    public void differentProjectionsGetIndexed() throws Exception {
        assertNotNull(imReader);
        assertEquals(imReader.getGranules(
                imReader.getGridCoverageNames()[0], true).getCount(null),
                2);
    }

//    @Test
//    public void testHeterogeneousCRS() throws IOException, URISyntaxException, TransformException,
//            NoninvertibleTransformException, FactoryException {
//        System.setProperty("org.geotools.referencing.forceXY", "true");
//        URL storeUrl = org.geotools.TestData.url(this, "heterogeneous_crs");
//
//        File testDataFolder = new File(storeUrl.toURI());
//        File testDirectory = crsMosaicFolder.newFolder("diffprojectionstest");
//        FileUtils.copyDirectory(testDataFolder, testDirectory);
//        CatalogManagerImpl catalogManager = new DEMCatalogManager();
//        Hints creationHints = new Hints();
//        creationHints.put(Utils.ALLOW_HETEROGENEOUS_CRS, true);
//        creationHints.put(Utils.FORCED_CRS, CRS.decode("EPSG:4326"));
//        ImageMosaicReader imReader = new ImageMosaicReader(testDirectory, creationHints,
//                catalogManager);
//        Assert.assertNotNull(imReader);
//        ReferencedEnvelope roi = new ReferencedEnvelope(
//                -104.01374816894531, -103.48640441894531, 44.43695068359375, 44.663543701171875,
//                imReader.getCoordinateReferenceSystem());
//
//        Rectangle rasterArea = new Rectangle(0, 0, 768, 330);
//
//        Parameter<GridGeometry2D> readGG = null;
//        readGG = (Parameter<GridGeometry2D>) AbstractGridFormat.READ_GRIDGEOMETRY2D
//                .createValue();
//        readGG.setValue(new GridGeometry2D(new GridEnvelope2D(rasterArea), roi));
//        GridCoverage2D gc2d = imReader.read(new GeneralParameterValue[]{readGG, DEMFormat.SORT_BY.createValue()});
//        RenderedImage renderImage = gc2d.getRenderedImage();
//
//        File resultsFile = org.geotools.TestData
//                .file(this, "heterogeneous_crs_results/red_blue_mosaic_results.tiff");
//        ImageIO.write(renderImage, "tiff", new File("/Users/devon/tmp/redBlueResults.tiff"));
//
//        //number 1000 was a bit arbitrary for differences, should account for small differences in
//        //interpolation and such, but not the reprojection of the blue tiff. Correct and incorrect
//        //images will be pretty similar anyway
//        ImageAssert.assertEquals(resultsFile, renderImage, 1000);
//    }
}
