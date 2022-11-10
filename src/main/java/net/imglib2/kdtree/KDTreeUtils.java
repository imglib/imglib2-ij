package net.imglib2.kdtree;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;

final class KDTreeUtils
{
	static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	/**
	 * If the tree is flattened into an array the left child of node at
	 * index {@code i} has index {@code 2 * i + 1}.
	 */
	static int leftChildIndex( final int i )
	{
		return 2 * i + 1;
	}

	/**
	 * If the tree is flattened into an array the right child of node at
	 * index {@code i} has index {@code 2 * i + 2}.
	 */
	static int rightChildIndex( final int i )
	{
		return 2 * i + 2;
	}

	/**
	 * If the tree is flattened into an array the parent of node at
	 * index {@code i} has index {@code (i - 1) / 2} (except for the
	 * root node {@code i==0}).
	 */
	static int parentIndex( final int i )
	{
		return ( i - 1 ) / 2;
	}

	/**
	 * Copy the coordinates of the given {@code points} to a new {@code double[][] positions} array.
	 * The coordinate in dimension {@code d} of the {@code i}th point is stored at {@code positions[d][i]}.
	 * That is, {@code positions[0]} has all X coordinates, {@code positions[0]} has all Y coordinates, and so on.
	 */
	static double[][] initPositions(
			final int numDimensions,
			final int numPoints,
			final Iterable< ? extends RealLocalizable > points )
	{
		final double[][] positions = new double[ numDimensions ][ numPoints ];
		final Iterator< ? extends RealLocalizable > ipos =  points.iterator();
		for ( int i = 0; i < numPoints; ++i )
		{
			if ( !ipos.hasNext() )
				throw new IllegalArgumentException( "positions Iterable is empty" );
			final RealLocalizable pos = ipos.next();
			for ( int d = 0; d < numDimensions; d++ )
				positions[ d ][ i ] = pos.getDoublePosition( d );
		}
		return positions;
	}

	/**
	 * Sort the given points into a k-d tree.
	 * <p>
	 * The tree is given as a flat (heap-like) array of point indices, {@code int[] tree}.
	 * The index of the point chosen as the root node is {@code tree[0]}.
	 * The coordinates of the root node are at {@code positions[d][tree[0]]}.
	 * <p>
	 * The indices of the children (less-or-equal and greater-or-equal, respectively) of root are at {@code tree[1]} and {@code tree[2]}, and so on.
	 *
	 * @param positions
	 * 		The coordinates for the {@code i}th point are stored at {@code positions[d][i]} where {@code d} is the dimension.
	 * 	    See {@link #initPositions(int, int, Iterable)}.
	 *
	 * @return flattened tree of point indices
	 */
	static int[] makeTree( double[][] positions )
	{
		return new MakeTree( positions ).tree;
	}

	/**
	 * Re-order the node {@code positions} to form a tree corresponding to the index array {@code tree'={0,1,2,...}}.
	 *
	 * @param positions
	 * @param tree
	 *
	 * @return
	 */
	static double[][] reorder( double[][] positions, int[] tree )
	{
		final int numDimensions = positions.length;
		final int numPoints = positions[ 0 ].length;
		assert tree.length == numPoints;
		final double[][] reordered = new double[ numDimensions ][ numPoints ];
		for ( int d = 0; d < numDimensions; ++d )
		{
			final double[] p = positions[ d ];
			Arrays.setAll( reordered[ d ], i -> p[ tree[ i ] ] );
		}
		return reordered;
	}

	/**
	 * Re-order the node {@code positions} to form a tree corresponding to the index array {@code tree'={0,1,2,...}}.
	 * Then flatten the result into a 1-D array, interleaving coordinates in all dimensions.
	 *
	 * @param positions
	 * @param tree
	 *
	 * @return
	 */
	static double[] reorderToFlatLayout( double[][] positions, int[] tree )
	{
		final int numDimensions = positions.length;
		final int numPoints = positions[ 0 ].length;
		assert tree.length == numPoints;
		if ( ( long ) numDimensions * numPoints > MAX_ARRAY_SIZE )
			throw new IllegalArgumentException( "positions[][] is too large to be stored in a flat array" );
		final double[] reordered = new double[ numDimensions * numPoints ];
		for ( int i = 0; i < numPoints; ++i )
			for ( int d = 0; d < numDimensions; ++d )
				reordered[ numDimensions * i + d ] = positions[ d ][ tree[ i ] ];
		return reordered;
	}

	/**
	 * Invert the given permutation {@code tree}.
	 * <p>
	 * For example, {@code tree = {3, 4, 1, 0, 5, 2}} indicates that coordinates
	 * and value for the node at heap index {@code i} can be found at index
	 * {@code tree[i]} in the respective input list.
	 * <p>
	 * The inverse, {@code inv = {3, 4, 1, 0, 5, 2}} indicates that coordinates
	 * and value at index {@code i} in the respective input list belong to the
	 * node at heap index {@code inv[i]}.
	 *
	 * @param tree a permutation
	 * @return the inverse permutation
	 */
	static int[] invert( int[] tree )
	{
		// For example:
		// i =          0  1  2  3  4  5
		// tree =      {3, 4, 1, 0, 5, 2}
		// output =    {3, 2, 5, 0, 1, 4}

		final int[] inv = new int[ tree.length ];
		for ( int i = 0; i < tree.length; i++ )
			inv[tree[i]] = i;
		return inv;
	}

	/**
	 * Re-order the node {@code values} to form a tree corresponding to the index array {@code tree'={0,1,2,...}}.
	 * The tree is given as an {@link #invert(int[]) inverted permutation}, so that we can iterate through the {@code values} in order, putting each at the right index in the returned {@code List}.
	 *
	 * @param invtree
	 * @param values
	 * @param <T>
	 *
	 * @return
	 */
	static < T > List< T > orderValuesList(
			final int[] invtree,
			final Iterable< T > values )
	{
		final int size = invtree.length;
		@SuppressWarnings( "unchecked" )
		final T[] orderedValues = ( T[] ) new Object[ size ];
		final Iterator< T > ival = values.iterator();
		for ( final int i : invtree )
		{
			if ( !ival.hasNext() )
				throw new IllegalArgumentException( "provided values Iterable has fewer elements than required" );
			orderedValues[ i ] = ival.next();
		}
		return Arrays.asList( orderedValues );
	}

	/**
	 * Re-order the node {@code values} to form a tree corresponding to the index array {@code tree'={0,1,2,...}}.
	 * The tree is given as an {@link #invert(int[]) inverted permutation}, so that we can iterate through the {@code values} in order, putting each at the right index in the returned 1D {@code Img}.
	 *
	 * @param invtree
	 * @param values
	 * @param <T>
	 *
	 * @return
	 */
	static < T extends NativeType< T > > Img< T > orderValuesImg(
			final int[] invtree,
			final Iterable< T > values )
	{
		final int size = invtree.length;
		final Img< T > img = new ArrayImgFactory<>( getType( values ) ).create( size );
		final RandomAccess< T > orderedValues = img.randomAccess();
		final Iterator< T > ival = values.iterator();
		for ( final int i : invtree )
		{
			if ( !ival.hasNext() )
				throw new IllegalArgumentException( "provided values Iterable has fewer elements than required" );
			orderedValues.setPositionAndGet( i ).set( ival.next() );
		}
		return img;
	}

	/**
	 * Returns the first element of {@code values}.
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code values} has no elements.
	 */
	static < T > T getType( Iterable< T > values )
	{
		final Iterator< T > ival = values.iterator();
		if ( !ival.hasNext() )
			throw new IllegalArgumentException( "values Iterable is empty" );
		return ival.next();
	}

	/**
	 * Returns the number of dimensions of the first element of {@code positions}.
	 * If {@code positions} has no elements, returns {@code 0}.
	 *
	 * @param positions
	 * 		list of points
	 *
	 * @return number of dimensions of the first point
	 */
	static int getNumDimensions( Iterable< ? extends RealLocalizable > positions )
	{
		final Iterator< ? extends RealLocalizable > ipos = positions.iterator();
		return ipos.hasNext() ? ipos.next().numDimensions() : 0;
	}

	private static final class MakeTree
	{
		private final int numDimensions;

		private final int numPoints;

		/**
		 * The coordinates for the {@code i}th point are stored at {@code positions[d][k]} where {@code d} is the dimension.
		 */
		private final double[][] positions;

		/**
		 * Temporary array to keep track of elements.
		 * Initialized to {@code 0, 1, ... } and then permuted when sorting the elements into a tree.
		 */
		private final int[] indices;

		/**
		 * Node indices in a flattened (heap-like) array.
		 * For example: the index of the root node is {@code tree[0]}.
		 * The coordinates of the root node are at {@code positions[d][tree[0]]} where {@code d} is the dimension.
		 * The children of the root node are at {@code tree[1]} and {@code tree[2]}, and so on.
		 */
		private final int[] tree;

		private MakeTree( final double[][] positions )
		{
			this.positions = positions;
			numDimensions = positions.length;
			numPoints = positions[ 0 ].length;
			indices = new int[ numPoints ];
			tree = new int[ numPoints ];
			Arrays.setAll( indices, j -> j );
			makeNode( 0, numPoints - 1, 0, 0 );
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

		private void swap( final int i, final int j )
		{
//			checkIndexBounds( i );
//			checkIndexBounds( j );
			final int tmp = indices[ i ];
			indices[ i ] = indices[ j ];
			indices[ j ] = tmp;
		}

		private void checkIndexBounds(final int i)
		{
			if ( i < 0 || i > numPoints )
				throw new IndexOutOfBoundsException();
		}

		private void makeNode( final int i, final int j, final int d, final int nodeIndex )
		{
			if ( j > i )
			{
				final int k = i + pivot( j - i + 1 );
				kthElement( i, j, k, d );
				tree[ nodeIndex ] = indices[ k ];
				final int dChild = ( d + 1 ) % numDimensions;
//				final int dChild = d + 1 == numDimensions ? 0 : d + 1;
				makeNode( i, k - 1, dChild, leftChildIndex( nodeIndex ) );
				makeNode( k + 1, j, dChild, rightChildIndex( nodeIndex ) );
			}
			else if ( j == i )
			{
				tree[ nodeIndex ] = indices[ i ];
			}
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
			final double[] values = positions[ compare_d ];
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

	private KDTreeUtils() {}
}
