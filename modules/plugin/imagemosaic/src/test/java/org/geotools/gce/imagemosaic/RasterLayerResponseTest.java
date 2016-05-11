/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.imagemosaic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.test.TestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

public class RasterLayerResponseTest {

    private final static double DELTA = 1E-5;

    @Rule public TemporaryFolder crsMosaicFolder = new TemporaryFolder();

    @Test
    public void testHeterogeneous() throws Exception {

        final URL testMosaic = TestData.url(this, "heterogeneous");
        ImageMosaicReader reader = null;
        try {

            reader = new ImageMosaicFormat().getReader(testMosaic, null);

            final ParameterValue<GridGeometry2D> gg = AbstractGridFormat.READ_GRIDGEOMETRY2D
                    .createValue();
            final GeneralEnvelope envelope = reader.getOriginalEnvelope();
            final Dimension dim = new Dimension();
            dim.setSize(10, 20);
            final Rectangle rasterArea = ((GridEnvelope2D) reader.getOriginalGridRange());
            rasterArea.setSize(dim);
            final GridEnvelope2D range = new GridEnvelope2D(rasterArea);
            gg.setValue(new GridGeometry2D(range, envelope));

            final RasterManager manager = reader.getRasterManager(reader.getGridCoverageNames()[0]);
            final RasterLayerRequest request = new RasterLayerRequest(
                    new GeneralParameterValue[] { gg }, manager);
            final RasterLayerResponse response = new RasterLayerResponse(request, manager);
            final Class<?> c = response.getClass();

            // Trigger the grid to world computations
            Method method = c.getDeclaredMethod("prepareResponse");
            method.setAccessible(true);
            method.invoke(response);

            Field finalGridToWorldCorner = c.getDeclaredField("finalGridToWorldCorner");
            finalGridToWorldCorner.setAccessible(true);
            MathTransform2D transform = (MathTransform2D) finalGridToWorldCorner.get(response);
            AffineTransform2D affineTransform = (AffineTransform2D) transform;
            assertEquals(18, XAffineTransform.getScaleX0(affineTransform), DELTA);
            assertEquals(18, XAffineTransform.getScaleY0(affineTransform), DELTA);
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {

                }
            }
        }
    }

    @Test
    public void testHeterogeneousCRS() throws IOException, URISyntaxException, TransformException,
            NoninvertibleTransformException, FactoryException {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        URL storeUrl = org.geotools.TestData.url(this, "heterogeneous_crs");

        File testDataFolder = new File(storeUrl.toURI());
        File testDirectory = crsMosaicFolder.newFolder("diffprojectionstest");
        FileUtils.copyDirectory(testDataFolder, testDirectory);
        CatalogManagerImpl catalogManager = new CatalogManagerImpl();
        Hints creationHints = new Hints();
        creationHints.put(Utils.ALLOW_HETEROGENOUS_CRS, true);
        ImageMosaicReader imReader = new ImageMosaicReader(testDirectory, creationHints,
                catalogManager);
        assertNotNull(imReader);
        ReferencedEnvelope roi = new ReferencedEnvelope(
                -104.01374816894531, -103.48640441894531, 44.43695068359375, 44.663543701171875,
                imReader.getCoordinateReferenceSystem());

        Rectangle rasterArea = new Rectangle(0, 0, 768, 330);

        Parameter<GridGeometry2D> readGG = null;
        readGG = (Parameter<GridGeometry2D>) AbstractGridFormat.READ_GRIDGEOMETRY2D
                .createValue();
        readGG.setValue(new GridGeometry2D(new GridEnvelope2D(rasterArea), roi));
        GridCoverage2D gc2d = imReader.read(new GeneralParameterValue[]{readGG});
        RenderedImage renderImage = gc2d.getRenderedImage();

        File resultsFile = org.geotools.TestData
                .file(this, "heterogeneous_crs_results/red_blue_mosaic_results.tiff");

        //number 1000 was a bit arbitrary for differences, should account for small differences in
        //interpolation and such, but not the reprojection of the blue tiff. Correct and incorrect
        //images will be pretty similar anyway
        ImageAssert.assertEquals(resultsFile, renderImage, 1000);
    }
}
