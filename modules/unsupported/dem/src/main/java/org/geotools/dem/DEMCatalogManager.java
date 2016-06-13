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

import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.imagemosaic.CatalogManager;
import org.geotools.gce.imagemosaic.MosaicConfigurationBean;
import org.geotools.gce.imagemosaic.RasterManager;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.gce.imagemosaic.catalog.index.Indexer;
import org.geotools.gce.imagemosaic.catalogbuilder.CatalogBuilderConfiguration;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Catalog manager used by the DEM format
 */
public class DEMCatalogManager extends CatalogManager {

    private static final Logger LOGGER = Logging.getLogger(DEMFormat.class);

    private static final String MASK_TIFF = ".mask.tiff";

    @Override
    public SimpleFeatureType createDefaultSchema(CatalogBuilderConfiguration runConfiguration,
            CoordinateReferenceSystem actualCRS) {
        final SimpleFeatureTypeBuilder featureBuilder = new SimpleFeatureTypeBuilder();
        featureBuilder.setName(runConfiguration.getParameter(Utils.Prop.INDEX_NAME));
        featureBuilder.setNamespaceURI("http://boundlessgeo.com//");
        featureBuilder.add(runConfiguration.getParameter(Utils.Prop.LOCATION_ATTRIBUTE).trim(),
                String.class);
        featureBuilder.add("the_geom", Polygon.class, actualCRS);
        featureBuilder.setDefaultGeometry("the_geom");
        addAttributes("date", featureBuilder, Date.class);
        addAttributes("fsDate", featureBuilder, Date.class);
        addAttributes("resolution", featureBuilder, Double.class);
        addAttributes("crs", featureBuilder, String.class);

        return featureBuilder.buildFeatureType();
    }

    @Override
    public List<Indexer.Collectors.Collector> customCollectors() {
        List<Indexer.Collectors.Collector> list = new ArrayList<>();

        Indexer.Collectors.Collector collectorDate = Utils.OBJECT_FACTORY
                .createIndexerCollectorsCollector();
        collectorDate.setSpi("DateExtractorSPI");
        collectorDate.setMapped("date");
        collectorDate.setValue("");
        list.add(collectorDate);

        Indexer.Collectors.Collector collectorFSDate = Utils.OBJECT_FACTORY
                .createIndexerCollectorsCollector();
        collectorFSDate.setSpi("FSDateExtractorSPI");
        collectorFSDate.setMapped("fsDate");
        collectorFSDate.setValue("");
        list.add(collectorFSDate);

        Indexer.Collectors.Collector collectorX = Utils.OBJECT_FACTORY
                .createIndexerCollectorsCollector();
        collectorX.setSpi("ResolutionExtractorSPI");
        collectorX.setMapped("resolution");
        collectorX.setValue("");
        list.add(collectorX);

        Indexer.Collectors.Collector collectorCrs = Utils.OBJECT_FACTORY
                .createIndexerCollectorsCollector();
        collectorCrs.setSpi("CRSExtractorSPI");
        collectorCrs.setMapped("crs");
        collectorCrs.setValue("");
        list.add(collectorCrs);

        return list;
    }

    private File getMaskedFile(File fileBeingProcessed) {
        return new File(fileBeingProcessed.getParent(),
                FilenameUtils.getBaseName(fileBeingProcessed.getName()) + MASK_TIFF);
    }

    @Override
    public boolean accepts(GridCoverage2DReader coverageReader,
            MosaicConfigurationBean mosaicConfiguration, RasterManager rasterManager)
            throws IOException {
        ColorModel colorModel = mosaicConfiguration.getColorModel();
        ColorModel actualCM = coverageReader.getImageLayout().getColorModel(null);
        if (colorModel == null) {
            colorModel = rasterManager.defaultCM;
        }
        return !Utils.checkColorModels(colorModel, actualCM);
    }

    @Override
    protected Object processGranuleGeometry(Object propValue, GeometryDescriptor propName,
            MosaicConfigurationBean mosaicConfig, GridCoverage2DReader inputReader) {

        if (propValue instanceof Envelope) {
            Envelope envelope = (Envelope) propValue;
            //default CRS of the reader
            CoordinateReferenceSystem mosaicCRS = mosaicConfig.getCrs();

            //CRS of the grid coverage reader
            CoordinateReferenceSystem gridReaderCRS = inputReader.getCoordinateReferenceSystem();
            ReferencedEnvelope targetIndexEnvelope = new ReferencedEnvelope(envelope);
            if (!CRS.equalsIgnoreMetadata(mosaicCRS, gridReaderCRS)) {

                //now need to reproject the geometry in order to add it to the mosaic.
                try {
                    targetIndexEnvelope = new ReferencedEnvelope(
                            CRS.transform(envelope, mosaicCRS));
                } catch (TransformException e) {
                    LOGGER.log(Level.INFO, "Unable to transform source GridReader envelope to "
                            + "target mosaic CRS", e);
                }
            }

            return targetIndexEnvelope;
        }
        else {
            return propValue;
        }
    }
}
