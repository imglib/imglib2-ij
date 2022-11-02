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


	public static class KDTree<T> {

        final KDTreeImpl impl;

        final Supplier<IntFunction<T>> valuesSupplier;

        KDTree(final KDTreeImpl impl, final Supplier<IntFunction<T>> valuesSupplier) {
            this.impl = impl;
            this.valuesSupplier = valuesSupplier;
        }

        public KDTreeNode<T> getRoot() {
            return new KDTreeNode<>(this).setNodeIndex(impl.root());
        }

        KDTreeNode<T> left(final KDTreeNode<T> parent) {
			final int c = impl.left( parent.nodeIndex );
            return c < 0 ? null : new KDTreeNode<>(this).setNodeIndex(c);
        }

        KDTreeNode<T> right(final KDTreeNode<T> parent) {
			final int c = impl.right( parent.nodeIndex );
			return c < 0 ? null : new KDTreeNode<>(this).setNodeIndex(c);
        }

        KDTreeNode<T> getRoot(final KDTreeNode<T> ref) {
            return ref.setNodeIndex(impl.root());
        }

        KDTreeNode<T> left(final KDTreeNode<T> parent, final KDTreeNode<T> ref) {
			final int c = impl.left( parent.nodeIndex );
			return c < 0 ? null : ref.setNodeIndex(c);
        }

        KDTreeNode<T> right(final KDTreeNode<T> parent, final KDTreeNode<T> ref) {
			final int c = impl.right( parent.nodeIndex );
			return c < 0 ? null : ref.setNodeIndex(c);
        }

        public int numDimensions() {
            return impl.numDimensions();
        }
    }

    public static class KDTreeNode<T> implements RealLocalizable, Sampler<T> {
        private final KDTree<T> tree;

        private int nodeIndex;

        private IntFunction<T> values;

        KDTreeNode(final KDTree<T> tree) {
            this.tree = tree;
        }

        /**
         * Left child of this node. All nodes x in the left subtree have
         * {@code x.pos[splitDimension] <= this.pos[splitDimension]}.
         */
        public KDTreeNode<T> left() {
            return tree.left(this);
        }

        public KDTreeNode<T> left(final KDTreeNode<T> ref) {
            return tree.left(this, ref);
        }

        /**
         * Right child of this node. All nodes x in the right subtree have
         * {@code x.pos[splitDimension] >= this.pos[splitDimension]}.
         */
        public KDTreeNode<T> right() {
            return tree.right(this);
        }

        public KDTreeNode<T> right(final KDTreeNode<T> ref) {
            return tree.right(this, ref);
        }

        /**
         * Get the dimension along which this node divides the space.
         *
         * @return splitting dimension.
         */
        public final int getSplitDimension() {
            return tree.impl.splitDimension( nodeIndex );
        }

        /**
         * Get the position along {@link net.imglib2.KDTreeNode#getSplitDimension()} where this
         * node divides the space.
         *
         * @return splitting position.
         */
        public final double getSplitCoordinate() {
            return getDoublePosition(getSplitDimension());
        }

        KDTreeNode<T> setNodeIndex(final int nodeIndex) {
            this.nodeIndex = nodeIndex;
            return this;
        }

        @Override
        public double getDoublePosition(final int d) {
            return tree.impl.getDoublePosition( nodeIndex, d );
        }

        @Override
        public int numDimensions() {
            return tree.numDimensions();
        }

        @Override
        public T get() {
            if (values == null)
                values = tree.valuesSupplier.get();
            return values.apply(nodeIndex);
        }

        @Override
        public KDTreeNode<T> copy() {
            final KDTreeNode<T> copy = new KDTreeNode<>(tree);
            copy.setNodeIndex(nodeIndex);
            return copy;
        }

        /**
         * Compute the squared distance from p to this node.
         */
        public double squDistanceTo(final double[] p) {
			return tree.impl.squDistance( nodeIndex, p );
        }
    }

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

        KDTree<ARGBType> kdtree = kdtree(coordinates, colors);
        NearestNeighborSearch<ARGBType> search = new NearestNeighborSearchOnKDTree<>(kdtree);
        RealRandomAccessible<ARGBType> interpolated = Views.interpolate(search, new NearestNeighborSearchInterpolatorFactory<>());
        RandomAccessibleInterval<ARGBType> view = Views.interval(Views.raster(interpolated), interval);
        ImageJFunctions.show(view);
    }
}
