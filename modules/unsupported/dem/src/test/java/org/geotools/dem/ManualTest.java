/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2016, Open Source Geospatial Foundation (OSGeo)
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
 *
 */

package org.geotools.dem;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.MosaicType;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.transform.WarpBuilder;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

/**
 * Manually test creating a mosaic
 */
public class ManualTest {

    private String testFileOne = "dem_large-clipped.tif";
    private String testFileTwo = "sfdem.tif";
    String targetCrs = "EPSG:4326";

    private GridCoverage2D coverageOne;
    private GridCoverage2D coverageTwo;

    private GridCoverage2D reprojectedSFDem;

    private RenderedOp mosaicRenderedOp;

    private CoordinateReferenceSystem crsCoverageOne;

    private CoordinateReferenceSystem crsCoverageTwo;

    private ImageWorker sfDemWorker;

    private GridEnvelope2D sourceBB;

    private ImageWorker demLargeImageWorker;

    private ImageWorker mosaicWorker;

    public static void main(String[] args) throws IOException, FactoryException,
            TransformException {
        ManualTest manualTest = new ManualTest();
        manualTest.loadFiles();
        manualTest.reproject();
        manualTest.colorExpansion();
        manualTest.produceMosaic();
        manualTest.writeMosaic();
    }

    private void colorExpansion() {
        //dem needs to be expanded
        demLargeImageWorker.forceColorSpaceGRAYScale();
        sfDemWorker.forceColorSpaceGRAYScale();
    }

    private void writeMosaic() throws IOException {
        ImageIO.write(this.mosaicWorker.getRenderedOperation(), "tiff", new File("mosaicresult.tiff"));
    }

    private void produceMosaic() throws IOException {

        ImageIO.write(sfDemWorker.getRenderedOperation(), "tiff", new File("sfdemintermediate.tiff"));
        ImageIO.write(demLargeImageWorker.getRenderedOperation(), "tiff", new File("demlargeinter.tiff"));
        RenderedImage[] sources = new RenderedImage[] {
                demLargeImageWorker.getRenderedOperation(),
                sfDemWorker.getRenderedOperation()};

        mosaicWorker = new ImageWorker();
        mosaicWorker = mosaicWorker.mosaic(
                sources,
                MosaicDescriptor.MOSAIC_TYPE_BLEND,
                null,
                null,
                null,
                null);
    }

    private void reproject() throws FactoryException, TransformException, IOException {
        //reproject sfdem
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");

        sfDemWorker = new ImageWorker(this.coverageTwo.getRenderedImage());
        demLargeImageWorker = new ImageWorker(this.coverageOne.getRenderedImage());

        //Most of this was copied from the Resampler2D class which does reprojection
        MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);

        GridGeometry sourceGG = coverageTwo.getGridGeometry();
        CoordinateOperationFactory coordOpFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);

        CoordinateOperation operation = coordOpFactory.createOperation(coverageTwo.getCoordinateReferenceSystem(), targetCRS);
        MathTransform targetToSource = coordOpFactory.createOperation(targetCRS, crsCoverageTwo).getMathTransform(); //step 2
        MathTransform c2CrsToGrid = coverageTwo.getGridGeometry().getGridToCRS(PixelOrientation.UPPER_LEFT).inverse(); //step 3
        GridEnvelope targetGR = sourceGG.getGridRange();

        Envelope sourceEnvelope = coverageTwo.getEnvelope(); // Don't force this one to 2D.
        GeneralEnvelope targetEnvelope = CRS.transform(operation, sourceEnvelope);
        targetEnvelope.setCoordinateReferenceSystem(targetCRS);
        GridGeometry2D targetGG = new GridGeometry2D(targetGR, targetEnvelope);
        MathTransform targetGridToCRS = targetGG.getGridToCRS(PixelOrientation.UPPER_LEFT); //step 1


        MathTransform finalTransform = mtFactory.createConcatenatedTransform(
                mtFactory.createConcatenatedTransform(targetGridToCRS, targetToSource), c2CrsToGrid);

        Warp warp = new WarpBuilder(0.333)
                .buildWarp((MathTransform2D) finalTransform, this.sourceBB);

        sfDemWorker = sfDemWorker.warp(warp, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
    }

    private void loadFiles() throws IOException {
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2DReader readerOne = format.getReader(new File(testFileOne), null);
        GridCoverage2DReader readerTwo = format.getReader(new File(testFileTwo), null);

        crsCoverageOne = readerOne.getCoordinateReferenceSystem();
        crsCoverageTwo = readerTwo.getCoordinateReferenceSystem();

        this.coverageOne = readerOne.read(null);
        this.coverageTwo = readerTwo.read(null);

        sourceBB = coverageTwo.getGridGeometry().getGridRange2D();

    }
}



