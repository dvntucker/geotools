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

package org.geotools.data;

import java.util.Spliterator;
import java.util.function.Consumer;

import org.opengis.feature.Feature;

/**
 * A default implementation of a feature spliterator. Feature sources should override this based on
 * available characteristics.
 */
public class DefaultFeatureSpliterator<F extends Feature> implements Spliterator<F> {
    @Override
    public boolean tryAdvance(Consumer<? super F> action) {
        return false;
    }

    @Override
    public Spliterator<F> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return 0;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
