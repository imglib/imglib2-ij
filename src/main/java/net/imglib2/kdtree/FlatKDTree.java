package net.imglib2.kdtree;

import ij.ImageJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class FlatKDTree {


	private static <T extends NativeType<T>> Img<T> toArrayImg(final T type, final int size, final Iterator<T> values) {
        final ArrayImg<T, ?> img = new ArrayImgFactory<>(type).create(size);
        img.forEach(t -> t.set(values.next()));
        return img;
    }

    public static <T> KDTree<T> kdtree(
            final Collection<? extends RealLocalizable> points,
            final List<T> values) {
        final int numDimensions = points.iterator().next().numDimensions();
        final int numPoints = points.size();

        double[][] positions = new double[numDimensions][numPoints];
        {
            int i = 0;
            for (RealLocalizable point : points) {
                for (int d = 0; d < numDimensions; d++)
                    positions[d][i] = point.getDoublePosition(d);
                ++i;
            }
        }

        final int[] tree = KDTreeBuilder.tree(positions);
        positions = KDTreeBuilder.reorder(positions, tree);
        final List<T> reorderedValues = new ArrayList<>();
        KDTreeBuilder.reorder(values::get, tree).forEachRemaining(reorderedValues::add);

        final T type = values.get(0);
        if (type instanceof NativeType) {
            Img<T> img = (Img<T>) toArrayImg((NativeType) type, reorderedValues.size(), (Iterator) reorderedValues.iterator());
            ImageJFunctions.show((RandomAccessibleInterval) Views.addDimension(img, 0, 0));
            final Supplier<IntFunction<T>> valuesSupplier = () -> {
                final RandomAccess<T> ra = img.randomAccess();
                return i -> ra.setPositionAndGet(i);
            };
			System.out.println( "valuesSupplier = " + valuesSupplier );
            return new KDTree<>(new KDTreeImpl(positions), valuesSupplier);
        }

        final IntFunction<T> getValue = reorderedValues::get;
        final Supplier<IntFunction<T>> valuesSupplier = () -> getValue;
        return new KDTree<>(new KDTreeImpl(positions), valuesSupplier);
    }

    public static void main(String[] args) {
        new ImageJ();

        List<ARGBType> colors = Arrays.asList(SparseExample1.colorsArray);
        List<RealPoint> coordinates = Arrays.asList(SparseExample1.coordinatesArray);

        // the interval we want to display
        Interval interval = Intervals.createMinSize(0, 0, 320, 200);

        final KDTree<ARGBType> kdtree = new KDTree<>(colors, coordinates);
        NearestNeighborSearch<ARGBType> search = new NearestNeighborSearchOnKDTree<>(kdtree);
        RealRandomAccessible<ARGBType> interpolated = Views.interpolate(search, new NearestNeighborSearchInterpolatorFactory<>());
        RandomAccessibleInterval<ARGBType> view = Views.interval(Views.raster(interpolated), interval);
        ImageJFunctions.show(view);
    }
}
