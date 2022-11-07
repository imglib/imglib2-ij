package net.imglib2.kdtree;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

final class KDTreeBuilder // TODO: this stuff can move to KDTreeImpl as static methods?
{
	static int[] tree( double[][] positions )
	{
		return new Nodes( positions ).makeTree();
	}

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

	static < T > Iterator< T > reorder( final IntFunction< T > getAt, int[] tree )
	{
		return new Iterator< T >()
		{
			int i = 0;

			@Override
			public boolean hasNext()
			{
				return i < tree.length;
			}

			@Override
			public T next()
			{
				if ( !hasNext() )
					throw new NoSuchElementException();
				return getAt.apply( tree[ i++ ] );
			}
		};
	}

	private static final class Nodes
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

		/**
		 * If the tree is flattened into an array the left child of node at
		 * index {@code i} has index {@code 2 * i + 1}.
		 */
		private static int leftChild( final int i )
		{
			return 2 * i + 1;
		}

		/**
		 * If the tree is flattened into an array the right child of node at
		 * index {@code i} has index {@code 2 * i + 2}.
		 */
		private static int rightChild( final int i )
		{
			return 2 * i + 2;
		}

		Nodes( final double[][] positions )
		{
			this.positions = positions;

			numDimensions = positions.length;
			numPoints = positions[ 0 ].length;

			indices = new int[ numPoints ];
			tree = new int[ numPoints ];
		}

		int[] makeTree()
		{
			Arrays.setAll( indices, j -> j );
			makeNode( 0, numPoints - 1, 0, 0 );
			return tree;
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
				makeNode( i, k - 1, dChild, leftChild( nodeIndex ) );
				makeNode( k + 1, j, dChild, rightChild( nodeIndex ) );
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

	private KDTreeBuilder() {}
}
