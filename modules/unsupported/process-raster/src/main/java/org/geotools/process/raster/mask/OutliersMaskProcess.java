/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.process.raster.mask;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.Histogram;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROIShape;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.raster.RasterProcess;
import org.geotools.resources.coverage.CoverageUtilities;
import org.jaitools.tiledimage.DiskMemImage;

/**
 * 
 * Create a mask to exclude outliers from a coverage using the Interquartile Range or Standard Deviation algorithm. 
 * 
 * @author Niels Charlier
 *
 */
@DescribeProcess(title = "OutliersMask", description = "Create a mask to exclude outliers from a coverage using the Interquartile Range algorithm. ")
public class OutliersMaskProcess implements RasterProcess {
    
    protected interface BoundsCalculator {
        public double[] getBounds(Histogram histogram, int band, double factor); 
    }
    
    public enum StatisticMethod {        
        StandardDeviation((Histogram histogram, int band, double factor) -> {
            double avg = histogram.getPTileThreshold(0.5)[band],
                    stdev = histogram.getStandardDeviation()[band];
            return new double[] {avg - factor * stdev, avg + factor * stdev};
        }),
        InterquartileRange((Histogram histogram, int band, double factor) -> {
            final double q1 = histogram.getPTileThreshold(0.25)[band],
                    q3 = histogram.getPTileThreshold(0.75)[band];
            return new double[] {q1 - factor * Math.max(1.0, q3 - q1), q3 + factor * Math.max(1.0, q3 - q1)};
        });
        
        private BoundsCalculator boundsCalculator;
       
        private StatisticMethod(BoundsCalculator stat) {
            this.boundsCalculator = stat;
        }
        
        BoundsCalculator getBoundsCalculator() {
            return boundsCalculator;
        }
    }
    

    public enum OutputMethod {
        OnlyMask, AlphaMask, NoDataMask
    }
    

    private final static double NO_DATA = Double.NaN;    
    private final static GridCoverageFactory coverageFac = new GridCoverageFactory();
    
    @DescribeResult(name = "result", description = "Outliers mask image")
    public GridCoverage2D execute(
            @DescribeParameter(name = "coverage", description = "Input raster") GridCoverage2D coverage,
            @DescribeParameter(name = "band", description = "Input raster band (default = 0)", min = 0, defaultValue = "0") int band,
            @DescribeParameter(name = "factor", description = "Permission distance factor (default = 1.5)", min = 0, defaultValue = "1.5") double factor,
            @DescribeParameter(name = "windowsize", description = "Window size (default = 500)", min = 0, defaultValue = "500") int windowSize,
            @DescribeParameter(name = "low", description = "Lower boundary value (pixels < will be ignored)", min = 0) Double low,
            @DescribeParameter(name = "high", description = "Upper boundary value (pixels > will be ignored)", min = 0) Double high,
            @DescribeParameter(name = "outputMethod", description = "Return only mask (default) or copy image and add alpha/nodata mask", min = 0, defaultValue = "OnlyMask") OutputMethod outputMethod,
            @DescribeParameter(name = "nodata value", description = "NODATA value (default = NaN)", min = 0) Double nodata,
            @DescribeParameter(name = "statisticMethod", description = "Statistic method for determining outliers (default = InterquartileRange)", 
                min = 0, defaultValue = "InterquartileRange") StatisticMethod statisticMethod) {
        
        if (low == null) {
            low = Double.NEGATIVE_INFINITY;
        }
        if (high == null) {
            high = Double.POSITIVE_INFINITY;
        }
        if (nodata == null) {
            nodata = NO_DATA;
        }
        
        final RenderedImage image = coverage.getRenderedImage();
        
        ColorModel cm;
        switch (outputMethod) {
        case AlphaMask: cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                true, false, Transparency.BITMASK, DataBuffer.TYPE_FLOAT); break;
        case NoDataMask: cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT); break;
        default: cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);         
        }
        SampleModel sm = cm.createCompatibleSampleModel(windowSize, windowSize);
        DiskMemImage resultImage = new DiskMemImage(0, 0, image.getWidth(), image.getHeight(),
                0, 0, sm, cm);
                
        //loop over windows
        for (int i = 0; i < (image.getWidth() + windowSize - 1) / windowSize; i++) {
            for (int j = 0; j < (image.getHeight() + windowSize - 1) / windowSize; j++) {
                
                //create window
                final Rectangle window = new Rectangle(i * windowSize + image.getMinX(),
                        j * windowSize + image.getMinY(), windowSize, windowSize);
                final ROIShape roi = new ROIShape(window);
                
                //retrieve the maximum and minimum pixel value
                ParameterBlockJAI pb = new ParameterBlockJAI("extrema");
                pb.setSource("source0", image);
                pb.setParameter("roi", roi);
                pb.setParameter("xPeriod", 1);
                pb.setParameter("yPeriod", 1);
                final double[][] extrema = (double[][]) JAI.create("extrema", pb).getProperty("extrema");
                final double min = extrema[0][band], max = extrema[1][band];
                
                //source raster
                final Raster data = image.getData(window);
                
                //create result raster
                WritableRaster resultData = resultImage.getWritableTile(i, j);
                if (outputMethod != OutputMethod.OnlyMask) {
                    //copy it all
                    for (int x = (int) window.getMinX(); x < window.getMaxX(); x++) {
                        for (int y = (int) window.getMinY(); y < window.getMaxY(); y++) {
                            final double pixel = data.getSampleDouble(x, y, band);
                            resultData.setSample(x - image.getMinX(), y - image.getMinY(), 0, pixel);
                        }
                    }            
                    if (outputMethod == OutputMethod.AlphaMask) {
                        resultData = cm.getAlphaRaster(resultData);
                    }
                }
                                
                if (max > min) {
                    //create histogram
                    pb = new ParameterBlockJAI("histogram");
                    pb.setSource("source0", image);
                    pb.setParameter("roi", roi);
                    pb.setParameter("lowValue", new double[] { Math.max(low, min) });
                    pb.setParameter("highValue", new double[] { Math.min(high, max) });

                    final Histogram histogram = (Histogram) JAI.create("histogram", pb).getProperty("histogram");

                    //calculate statistic
                    final double[] bounds = statisticMethod.getBoundsCalculator().getBounds(histogram, band, factor);
                    final double lowerBound = Math.max(low, bounds[0]), upperBound = Math.min(high, bounds[1]);

                    //create mask for this window
                    for (int x = (int) window.getMinX(); x < window.getMaxX(); x++) {
                        for (int y = (int) window.getMinY(); y < window.getMaxY(); y++) {
                            final double pixel = data.getSampleDouble(x, y, band);
                            if (outputMethod == OutputMethod.NoDataMask) {
                                if (pixel < lowerBound || pixel > upperBound) {
                                    resultData.setSample(x - image.getMinX(), y - image.getMinY(), 0, nodata);
                                }
                            } else {
                                resultData.setSample(x - image.getMinX(), y - image.getMinY(), 0,
                                    pixel >= lowerBound && pixel <= upperBound ? 0xFF : 0x00);
                            }
                        }
                    }
                } else {
                    final boolean letThrough = min >= low && min <= high;
              
                    //create mask for this window
                    if (outputMethod == OutputMethod.NoDataMask) {
                        if (!letThrough) {
                            for (int x = (int) window.getMinX(); x < window.getMaxX(); x++) {
                                for (int y = (int) window.getMinY(); y < window.getMaxY(); y++) {
                                    resultData.setSample(x - image.getMinX(), y - image.getMinY(), 0, nodata);
                                }
                            }
                        }                        
                    } else {
                        for (int x = (int) window.getMinX(); x < window.getMaxX(); x++) {
                            for (int y = (int) window.getMinY(); y < window.getMaxY(); y++) {
                                resultData.setSample(x - image.getMinX(), y - image.getMinY(), 0,
                                       letThrough ? 0xFF : 0x00);
                            }
                        }
                    }
                }
                
                resultImage.releaseWritableTile(i, j);
            }
        }
        
        Map<String, Object> properties = null;        
        if (outputMethod == OutputMethod.NoDataMask) {
            properties = new HashMap<String, Object>();
            CoverageUtilities.setNoDataProperty(properties, nodata);
        }
        return coverageFac.create("mask", resultImage, coverage.getEnvelope(), null, null, properties);
    }

}
