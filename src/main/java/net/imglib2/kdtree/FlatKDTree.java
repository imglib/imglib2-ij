package net.imglib2.kdtree;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.kdtree.FlatKDTree.NodeData.Node;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class FlatKDTree
{
	public static class KDTree< T >
	{
		final double[][] positions;

		final int[] tree;

		final List< T > values;

		private final int numDimensions;

		private final int numPoints;

		KDTree( final double[][] positions, final int[] tree, final List< T > values )
		{
			this.positions = positions;
			this.tree = tree;
			this.values = values;

			numDimensions = positions.length;
			numPoints = positions[ 0 ].length;
		}

		public KDTreeNode< T > getRoot()
		{
			return new KDTreeNode<>( this ).setNodeIndex( 0 );
		}

		KDTreeNode< T > left( final KDTreeNode< T > parent )
		{
			final int c = 2 * parent.nodeIndex + 1;
			if ( c >= numPoints )
				return null;
			return new KDTreeNode<>( this ).setNodeIndex( c );
		}

		KDTreeNode< T > right( final KDTreeNode< T > parent )
		{
			final int c = 2 * parent.nodeIndex + 2;
			if ( c >= numPoints )
				return null;
			return new KDTreeNode<>( this ).setNodeIndex( c );
		}

		KDTreeNode< T > getRoot( final KDTreeNode< T > ref )
		{
			return ref.setNodeIndex( 0 );
		}

		KDTreeNode< T > left( final KDTreeNode< T > parent, final KDTreeNode< T > ref )
		{
			final int c = 2 * parent.nodeIndex + 1;
			if ( c >= numPoints )
				return null;
			return ref.setNodeIndex( c );
		}

		KDTreeNode< T > right( final KDTreeNode< T > parent, final KDTreeNode< T > ref )
		{
			final int c = 2 * parent.nodeIndex + 2;
			if ( c >= numPoints )
				return null;
			return ref.setNodeIndex( c );
		}

		//		@Override
		public int numDimensions()
		{
			return numDimensions;
		}
	}

	public static class KDTreeNode< T > implements RealLocalizable, Sampler< T >
	{
		private final KDTree< T > tree;

		private final int n;

		private int nodeIndex;

		private int k;

		KDTreeNode( final KDTree< T > tree )
		{
			this.tree = tree;
			n = tree.numDimensions();
		}

		/**
		 * Left child of this node. All nodes x in the left subtree have
		 * {@code x.pos[splitDimension] <= this.pos[splitDimension]}.
		 */
		public KDTreeNode< T > left()
		{
			return tree.left( this );
		}

		public KDTreeNode< T > left(final KDTreeNode< T > ref)
		{
			return tree.left( this, ref );
		}

		/**
		 * Right child of this node. All nodes x in the right subtree have
		 * {@code x.pos[splitDimension] >= this.pos[splitDimension]}.
		 */
		public KDTreeNode< T > right()
		{
			return tree.right( this );
		}

		public KDTreeNode< T > right(final KDTreeNode< T > ref)
		{
			return tree.right( this, ref );
		}

		/**
		 * Get the dimension along which this node divides the space.
		 *
		 * @return splitting dimension.
		 */
		public final int getSplitDimension()
		{
			return ( 31 - Integer.numberOfLeadingZeros( nodeIndex + 1 ) ) % n;
		}

		/**
		 * Get the position along {@link net.imglib2.KDTreeNode#getSplitDimension()} where this
		 * node divides the space.
		 *
		 * @return splitting position.
		 */
		public final double getSplitCoordinate()
		{
			return getDoublePosition( getSplitDimension() );
		}

		KDTreeNode< T > setNodeIndex( final int nodeIndex )
		{
			this.nodeIndex = nodeIndex;
			k = tree.tree[ nodeIndex ];
			return this;
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return tree.positions[ d ][ k ];
		}

		@Override
		public int numDimensions()
		{
			return n;
		}

		@Override
		public T get()
		{
			return tree.values.get( k );
		}

		@Override
		public KDTreeNode< T > copy()
		{
			final KDTreeNode< T > copy = new KDTreeNode<>( tree );
			copy.setNodeIndex( nodeIndex );
			return copy;
		}

		/**
		 * Compute the squared distance from p to this node.
		 */
		public double squDistanceTo( final double[] p )
		{
			double sum = 0;
			for ( int d = 0; d < n; ++d )
			{
				final double x = getDoublePosition( d ) - p[ d ];
				sum += x * x;
			}
			return sum;
		}
	}

	/**
	 * A list of points.
	 * <p>
	 * {@code k = indices[i]} is the index in positions for the {@code i}th point.
	 * The coordinates for the {@code i}th point are stored at {@code positions[d][k]} where {@code d} is the dimension.
	 */
	public static class NodeData
	{
		private final int numDimensions;

		private final int numPoints;

		private final double[][] positions;

		private final int[] indices;

		private final int[] tree;

		public NodeData( final int numDimensions, final int numPoints )
		{
			this.numDimensions = numDimensions;
			this.numPoints = numPoints;
			positions = new double[ numDimensions ][ numPoints ];

			// point order, initialize to {0,1,2,...}
			indices = new int[ numPoints ];
			Arrays.setAll( indices, j -> j );

			tree = new int[ numPoints ];
		}

		public Node get( final int i, final Node ref )
		{
			checkIndexBounds( i );
			ref.k = indices[ i ];
			return ref;
		}

		public Node get( final int i )
		{
			return get( i, new Node() );
		}

		public void swap( final int i, final int j )
		{
			// TODO unchecked version
			checkIndexBounds( i );
			checkIndexBounds( j );
			final int tmp = indices[ i ];
			indices[ i ] = indices[ j ];
			indices[ j ] = tmp;
		}

		public class Node
		{
			private int k;

			/**
			 * Place the node at {@code position}.
			 */
			public void setPosition( final RealLocalizable position )
			{
				for ( int d = 0; d < numDimensions; d++ )
				{
					positions[ d ][ k ] = position.getDoublePosition( d );
				}
			}
		}

		private void checkIndexBounds(final int i)
		{
			if ( i < 0 || i > numPoints )
				throw new IndexOutOfBoundsException();
		}

		private static int leftChild(final int i) {
			return 2 * i + 1;
		}

		private static int rightChild(final int i) {
			return 2 * i + 2;
		}

		public void makeTree()
		{
			makeNode( 0, numPoints - 1, 0, 0 );
		}

		private void makeNode( final int i, final int j, final int d, final int nodeIndex )
		{
			System.out.println( "i = " + i + ", j = " + j + ", d = " + d + ", nodeIndex = " + nodeIndex );
			if ( j > i )
			{
				final int k = i + pivot( j - i + 1 );
				System.out.println( "k = " + k );
				kthElement( i, j, k, d );
				tree[ nodeIndex ] = indices[ k ];
				final int dChild = ( d + 1 ) % numDimensions;
//				final int dChild = d + 1 == numDimensions ? 0 : d + 1;
				System.out.println( "leftChild( " + nodeIndex + " ) = " + leftChild( nodeIndex ) );
				System.out.println( "rightChild( " + nodeIndex + " ) = " + rightChild( nodeIndex ) );
				makeNode( i, k - 1, dChild, leftChild( nodeIndex ) );
				makeNode( k + 1, j, dChild, rightChild( nodeIndex ) );
			}
			else if ( j == i )
			{
				tree[ nodeIndex ] = indices[ i ];
			}
		}

		/**
		 * Calculate pivot index such that the tree will be arranged in a way that
		 * "leaf layers" are filled from the left.
		 * For example 10 nodes will always be arranged like this:
		 * <pre>
		 *            0
		 *         /     \
		 *       1         2
		 *     /   \     /   \
 		 *    3     4   5     6
		 *   / \   /
		 *  7   8 9
		 * </pre>
		 *
		 * never like this:
		 * <pre>
		 *            0
		 *         /     \
		 *       1         2
		 *     /   \     /   \
		 *    3     4   5     6
		 *   /         /     /
		 *  7         8     9
		 * </pre>
		 *
		 * By choosing pivots in this way, the tree structure is fully
		 * determined. For every node index, the child indices can be calculated
		 * without dependent reads. And iff the calculated child index is less
		 * than the number of nodes, the child exists.
		 */
		private static int pivot( final int len )
		{
			final int h = Integer.highestOneBit( len );
			final int h2 = h >> 1;
			return ( len - h >= h2 )
					? h - 1
					: len - h2;
		}

		/**
		 * Partition a sublist of Nodes by their coordinate in the specified
		 * dimension, such that the k-th smallest value is at position {@code
		 * k}, elements before the k-th are smaller or equal and elements after
		 * the k-th are larger or equal.
		 *
		 * @param i
		 *            index of first element of the sublist
		 * @param j
		 *            index of last element of the sublist
		 * @param k
		 *            index for k-th smallest value. {@code i <= k <= j}.
		 * @param compare_d
		 *            dimension by which to order the sublist
		 */
		private void kthElement( int i, int j, final int k, final int compare_d )
		{
			while ( true )
			{
				final int pivotpos = partition( i, j, compare_d );
				if ( pivotpos > k )
				{
					// partition lower half
					j = pivotpos - 1;
				}
				else if ( pivotpos < k )
				{
					// partition upper half
					i = pivotpos + 1;
				}
				else
					return;
			}
		}

		/**
		 * Partition a sublist of Nodes by their coordinate in the specified
		 * dimension.
		 *
		 * A pivot element is chosen by median-of-three method. Then {@code
		 * [i,j]} is reordered, such that all elements before the pivot are
		 * smaller-equal and all elements after the pivot are larger-equal the
		 * pivot. The index of the pivot element is returned.
		 *
		 * @param i
		 *            index of first element of the sublist
		 * @param j
		 *            index of last element of the sublist
		 * @param compare_d
		 *            dimension by which to order the sublist
		 * @return index of pivot element
		 */
		private int partition( int i, int j, final int compare_d )
		{
			final double[] values = positions[compare_d];
			final int len = j - i + 1;
			if ( len <= 2 )
			{
				if ( len <= 0 )
					throw new IllegalArgumentException();
				if ( values[ indices[ i ] ] > values[ indices[ j ] ] )
					swap( i, j );
				return i;
			}
			else
			{
				final int m = ( i + j ) / 2;
				if ( values[ indices[ i ] ] > values[ indices[ m ] ] )
					swap( i, m );
				if ( values[ indices[ i ] ] > values[ indices[ j ] ] )
					swap( i, j );
				if ( values[ indices[ m ] ] > values[ indices[ j ] ] )
					swap( m, j );
				swap( m, i + 1 );
				final int p = ++i;
				final double pivot = values[ indices[ p ] ];
				while ( true )
				{
					while ( values[ indices[ ++i ] ] < pivot )
						;
					while ( values[ indices[ --j ] ] > pivot )
						;
					if ( j < i )
						break;
					swap( i, j );
				}
				swap( p, j );
				return j;
			}
		}
	}

	public static class KDTree0< T >
	{
		private final int numDimensions;

		private final int numPoints;

		private final List< T > values;

		private final NodeData nodes;

		public KDTree0(
				final Collection< ? extends RealLocalizable > points,
				final List< T > values )
		{
			numDimensions = points.iterator().next().numDimensions();
			numPoints = points.size();
			this.values = values;
			nodes = new NodeData( numDimensions, numPoints );

			final Node ref = nodes.get( 0 );

			// init positions
			int i = 0;
			for ( RealLocalizable point : points )
				nodes.get( i++, ref ).setPosition( point );

			nodes.makeTree();
		}

		public KDTree< T > getKDTree()
		{
			return new KDTree<>( nodes.positions, nodes.tree, values );
		}
	}

	public static void main( String[] args )
	{
		List< ARGBType > colors = Arrays.asList( SparseExample1.colorsArray );
		List< RealPoint > coordinates = Arrays.asList( SparseExample1.coordinatesArray );

		// the interval we want to display
		Interval interval = Intervals.createMinSize( 0, 0, 320, 200 );

		KDTree< ARGBType > kdtree = new KDTree0<>( coordinates, colors ).getKDTree();
		NearestNeighborSearch< ARGBType > search = new NearestNeighborSearchOnKDTree<>( kdtree );
		RealRandomAccessible< ARGBType > interpolated = Views.interpolate( search, new NearestNeighborSearchInterpolatorFactory<>() );
		RandomAccessibleInterval< ARGBType > view = Views.interval( Views.raster( interpolated ), interval );
		ImageJFunctions.show( view );
	}
}
