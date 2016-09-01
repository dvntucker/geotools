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

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;

/**
 * Streaming support for FeatureSources
 */
public class FeatureStream<T extends FeatureType, F extends Feature> implements Stream<F> {

    private FeatureCollection<T, F> wrappedFeatureCollection;

    public FeatureStream(FeatureCollection<T ,F> inputSource) {
        this.wrappedFeatureCollection = inputSource;
    }

    @Override public Stream<F> filter(Predicate<? super F> predicate) {
        Filter featureFilter;

        if (!(predicate instanceof Filter)) {
            featureFilter = new PredicateWrappingFilter((Predicate<Object>) predicate);
        } else {
            featureFilter = (Filter)predicate;
        }

        FeatureCollection<T,F> filteredCollection = wrappedFeatureCollection.subCollection(
            featureFilter);

        return new FeatureStream<>(filteredCollection);
    }

    @Override
    public <R> Stream<R> map(Function<? super F, ? extends R> mapper) {
        Stream.Builder<R> builder = Stream.builder();
        this.forEach(f -> builder.accept(mapper.apply(f)));
        return builder.build();
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super F> mapper) {
        return this.defaultStreamFromSpliterator().mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super F> mapper) {
        return this.defaultStreamFromSpliterator().mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super F> mapper) {
        return this.defaultStreamFromSpliterator().mapToDouble(mapper);
    }

    @Override
    public <R> Stream<R> flatMap(
        Function<? super F, ? extends Stream<? extends R>> mapper) {
        return this.defaultStreamFromSpliterator().flatMap(mapper);
    }

    @Override
    public IntStream flatMapToInt(Function<? super F, ? extends IntStream> mapper) {
        return this.defaultStreamFromSpliterator().flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super F, ? extends LongStream> mapper) {
        return this.defaultStreamFromSpliterator().flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(
        Function<? super F, ? extends DoubleStream> mapper) {
        return this.defaultStreamFromSpliterator().flatMapToDouble(mapper);
    }

    @Override
    public Stream<F> distinct() {
        return this.defaultStreamFromSpliterator().distinct();
    }

    @Override
    public Stream<F> sorted() {
        return this.defaultStreamFromSpliterator().sorted();
    }

    @Override
    public Stream<F> sorted(Comparator<? super F> comparator) {
        return this.defaultStreamFromSpliterator().sorted(comparator);
    }

    @Override
    public Stream<F> peek(Consumer<? super F> action) {
        return this.defaultStreamFromSpliterator().peek(action);
    }

    @Override
    public Stream<F> limit(long maxSize) {
        return this.defaultStreamFromSpliterator().limit(maxSize);
    }

    @Override
    public Stream<F> skip(long n) {
        return this.defaultStreamFromSpliterator().skip(n);
    }

    @Override
    public void forEach(Consumer<? super F> act) {
        try {
            wrappedFeatureCollection.accepts(act::accept, null);
        } catch (IOException e) {
            //TODO Fix this
            e.printStackTrace();
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super F> action) {
        this.defaultStreamFromSpliterator().forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return this.defaultStreamFromSpliterator().toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return this.defaultStreamFromSpliterator().toArray(generator);
    }

    @Override
    public F reduce(F identity, BinaryOperator<F> accumulator) {
        return this.defaultStreamFromSpliterator().reduce(identity, accumulator);
    }

    @Override
    public Optional<F> reduce(BinaryOperator<F> accumulator) {
        return this.defaultStreamFromSpliterator().reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super F, U> accumulator,
        BinaryOperator<U> combiner) {
        return this.defaultStreamFromSpliterator().reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super F> accumulator,
        BiConsumer<R, R> combiner) {
        return this.defaultStreamFromSpliterator().collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super F, A, R> collector) {
        return this.defaultStreamFromSpliterator().collect(collector);
    }

    @Override
    public Optional<F> min(Comparator<? super F> comparator) {
        return this.defaultStreamFromSpliterator().min(comparator);
    }

    @Override
    public Optional<F> max(Comparator<? super F> comparator) {
        return this.defaultStreamFromSpliterator().max(comparator);
    }

    @Override
    public long count() {
        return this.defaultStreamFromSpliterator().count();
    }

    @Override
    public boolean anyMatch(Predicate<? super F> predicate) {
        return this.defaultStreamFromSpliterator().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super F> predicate) {
        return this.defaultStreamFromSpliterator().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super F> predicate) {
        return this.defaultStreamFromSpliterator().noneMatch(predicate);
    }

    @Override
    public Optional<F> findFirst() {
        return this.defaultStreamFromSpliterator().findFirst();
    }

    @Override
    public Optional<F> findAny() {
        return this.defaultStreamFromSpliterator().findAny();
    }

    @Override
    public Iterator<F> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<F> spliterator() {
        return wrappedFeatureCollection.spliterator();
    }

    @Override
    public boolean isParallel() {
        return this.defaultStreamFromSpliterator().isParallel();
    }

    @Override
    public Stream<F> sequential() {
        return this.defaultStreamFromSpliterator().sequential();
    }

    @Override
    public Stream<F> parallel() {
        return null;
    }

    @Override
    public Stream<F> unordered() {
        return this.defaultStreamFromSpliterator().unordered();
    }

    @Override
    public Stream<F> onClose(Runnable closeHandler) {
        return this.defaultStreamFromSpliterator();
    }

    @Override
    public void close() {
        this.defaultStreamFromSpliterator().close();
    }

    private Stream<F> defaultStreamFromSpliterator() {
        Spliterator<F> collectionSpliterator = wrappedFeatureCollection.spliterator();
        return StreamSupport.stream(collectionSpliterator, false);
    }

    private static class PredicateWrappingFilter implements Filter {

        private final Predicate<Object> wrappedPredicate;

        private PredicateWrappingFilter(Predicate<Object> wrappedPredicate) {
            this.wrappedPredicate = wrappedPredicate;
        }

        @Override public boolean evaluate(Object object) {
            return wrappedPredicate.test(object);
        }

        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            throw new UnsupportedOperationException("Accept isn't implemented for predicates");
        }
    }
}
