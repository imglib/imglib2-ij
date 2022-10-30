package net.imglib2.kdtree;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.imglib2.KDTreeNode;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.Sampler;
import net.imglib2.kdtree.FlatKDTree.NodeData.Node;

import static net.imglib2.util.Partition.partitionSubList;

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

		public KDTreeNode< T > root()
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

		KDTreeNode< T > root( final KDTreeNode< T > ref )
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
			if ( j > i )
			{
				final int k = i + ( j - i + 1 ) / 2;
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
			else
			{
				throw new IllegalArgumentException();
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

	public static class KDTree0
	{
		private final int numDimensions;

		private final int numPoints;

		private final NodeData nodes;

		public KDTree0( final Collection< ? extends RealLocalizable > points )
		{
			numDimensions = points.iterator().next().numDimensions();
			numPoints = points.size();
			nodes = new NodeData( numDimensions, numPoints );

			final Node ref = nodes.get( 0 );

			// init positions
			int i = 0;
			for ( RealLocalizable point : points )
				nodes.get( i++, ref ).setPosition( point );

			nodes.makeTree();
		}
	}

	public static void main( String[] args )
	{
		List< RealPoint > coordinates = Arrays.asList( coordinatesArray );
		new KDTree0( coordinates );
	}

	static final RealPoint[] coordinatesArray = new RealPoint[] {
			new RealPoint( 136.1, 56.6 ), new RealPoint( 243.3, 88.5 ), new RealPoint( 0.6, 48.5 ), new RealPoint( 53.8, 178.3 ), new RealPoint( 155.7, 159.1 ), new RealPoint( 17.6, 126.9 ),
			new RealPoint( 51.5, 43.3 ), new RealPoint( 257.5, 187.8 ), new RealPoint( 178.5, 5.7 ), new RealPoint( 196.7, 116.5 ), new RealPoint( 274.0, 110.2 ), new RealPoint( 235.9, 127.3 ),
			new RealPoint( 281.3, 182.5 ), new RealPoint( 96.6, 109.4 ), new RealPoint( 231.1, 188.7 ), new RealPoint( 222.9, 15.1 ), new RealPoint( 125.6, 37.8 ), new RealPoint( 72.5, 187.1 ),
			new RealPoint( 291.7, 0.2 ), new RealPoint( 127.2, 109.3 ), new RealPoint( 21.4, 80.0 ), new RealPoint( 75.6, 37.4 ), new RealPoint( 98.2, 173.4 ), new RealPoint( 315.4, 87.3 ),
			new RealPoint( 136.4, 156.6 ), new RealPoint( 282.7, 84.4 ), new RealPoint( 69.2, 113.4 ), new RealPoint( 315.2, 126.6 ), new RealPoint( 230.1, 178.3 ), new RealPoint( 216.3, 152.8 ),
			new RealPoint( 219.1, 100.6 ), new RealPoint( 28.7, 189.7 ), new RealPoint( 138.5, 53.5 ), new RealPoint( 189.3, 53.9 ), new RealPoint( 230.6, 29.5 ), new RealPoint( 153.6, 36.1 ),
			new RealPoint( 38.5, 123.0 ), new RealPoint( 55.7, 163.9 ), new RealPoint( 7.6, 183.7 ), new RealPoint( 182.9, 145.1 ), new RealPoint( 238.9, 54.0 ), new RealPoint( 219.0, 113.3 ),
			new RealPoint( 16.5, 138.6 ), new RealPoint( 90.2, 2.8 ), new RealPoint( 6.0, 170.1 ), new RealPoint( 39.3, 125.5 ), new RealPoint( 167.7, 92.8 ), new RealPoint( 86.0, 7.5 ),
			new RealPoint( 115.1, 91.3 ), new RealPoint( 25.4, 39.1 ),
			new RealPoint( 212.5, 25.1 ), new RealPoint( 242.3, 161.0 ), new RealPoint( 182.9, 154.4 ), new RealPoint( 241.2, 54.3 ), new RealPoint( 247.1, 81.4 ), new RealPoint( 274.9, 111.0 ),
			new RealPoint( 96.6, 187.8 ), new RealPoint( 197.7, 128.3 ), new RealPoint( 312.5, 75.6 ), new RealPoint( 164.6, 179.1 ), new RealPoint( 263.6, 17.3 ), new RealPoint( 155.4, 170.0 ),
			new RealPoint( 74.6, 16.8 ), new RealPoint( 148.5, 77.2 ), new RealPoint( 268.9, 157.8 ), new RealPoint( 16.6, 48.3 ), new RealPoint( 283.3, 44.8 ), new RealPoint( 289.0, 23.7 ),
			new RealPoint( 73.8, 2.3 ), new RealPoint( 39.7, 70.9 ), new RealPoint( 137.8, 192.3 ), new RealPoint( 222.0, 65.7 ), new RealPoint( 311.8, 22.8 ), new RealPoint( 32.7, 54.1 ),
			new RealPoint( 72.8, 49.5 ), new RealPoint( 89.7, 29.9 ), new RealPoint( 234.6, 21.3 ), new RealPoint( 142.1, 126.8 ), new RealPoint( 102.5, 41.0 ), new RealPoint( 230.8, 187.5 ),
			new RealPoint( 230.6, 44.7 ), new RealPoint( 54.3, 95.2 ), new RealPoint( 210.9, 112.4 ), new RealPoint( 152.9, 180.0 ), new RealPoint( 290.3, 81.4 ), new RealPoint( 202.5, 15.8 ),
			new RealPoint( 99.3, 25.8 ), new RealPoint( 261.9, 171.2 ), new RealPoint( 226.9, 79.2 ), new RealPoint( 43.4, 94.7 ), new RealPoint( 154.3, 143.8 ), new RealPoint( 16.1, 82.3 ),
			new RealPoint( 201.7, 82.4 ), new RealPoint( 27.1, 75.0 ), new RealPoint( 152.7, 37.5 ), new RealPoint( 285.0, 177.1 ), new RealPoint( 186.2, 139.6 ), new RealPoint( 249.6, 108.9 ),
			new RealPoint( 274.7, 65.1 ), new RealPoint( 230.3, 148.4 ),
			new RealPoint( 260.6, 76.9 ), new RealPoint( 20.0, 193.5 ), new RealPoint( 145.2, 173.3 ), new RealPoint( 160.0, 73.1 ), new RealPoint( 219.0, 81.3 ), new RealPoint( 104.4, 67.1 ),
			new RealPoint( 316.3, 172.2 ), new RealPoint( 191.8, 87.1 ), new RealPoint( 223.7, 100.4 ), new RealPoint( 234.4, 126.2 ), new RealPoint( 155.3, 13.6 ), new RealPoint( 277.5, 58.1 ),
			new RealPoint( 180.8, 98.2 ), new RealPoint( 182.2, 187.5 ), new RealPoint( 220.1, 47.0 ), new RealPoint( 196.2, 188.7 ), new RealPoint( 56.8, 66.5 ), new RealPoint( 19.8, 135.6 ),
			new RealPoint( 55.2, 79.2 ), new RealPoint( 122.8, 144.1 ), new RealPoint( 245.7, 141.9 ), new RealPoint( 50.4, 178.0 ), new RealPoint( 288.2, 98.3 ), new RealPoint( 220.8, 125.5 ),
			new RealPoint( 217.3, 26.9 ), new RealPoint( 16.3, 69.9 ), new RealPoint( 120.7, 195.7 ), new RealPoint( 26.0, 190.3 ), new RealPoint( 139.9, 152.2 ), new RealPoint( 143.2, 106.9 ),
			new RealPoint( 207.0, 57.9 ), new RealPoint( 56.8, 144.3 ), new RealPoint( 38.5, 99.8 ), new RealPoint( 278.7, 161.4 ), new RealPoint( 262.8, 38.7 ), new RealPoint( 196.5, 189.4 ),
			new RealPoint( 66.3, 6.1 ), new RealPoint( 161.9, 59.6 ), new RealPoint( 279.8, 106.3 ), new RealPoint( 316.3, 173.1 ), new RealPoint( 118.0, 165.8 ), new RealPoint( 111.1, 62.9 ),
			new RealPoint( 270.8, 166.9 ), new RealPoint( 73.5, 110.6 ), new RealPoint( 289.8, 79.8 ), new RealPoint( 224.3, 72.2 ), new RealPoint( 243.6, 148.2 ), new RealPoint( 6.5, 126.0 ),
			new RealPoint( 255.8, 27.2 ), new RealPoint( 90.2, 2.4 ),
			new RealPoint( 272.6, 62.9 ), new RealPoint( 64.1, 193.1 ), new RealPoint( 180.2, 135.8 ), new RealPoint( 134.3, 43.9 ), new RealPoint( 294.7, 124.7 ), new RealPoint( 80.4, 111.9 ),
			new RealPoint( 120.8, 132.0 ), new RealPoint( 58.6, 174.8 ), new RealPoint( 292.9, 45.0 ), new RealPoint( 89.4, 1.0 ), new RealPoint( 62.8, 16.9 ), new RealPoint( 125.1, 126.7 ),
			new RealPoint( 17.9, 67.7 ), new RealPoint( 46.4, 51.0 ), new RealPoint( 251.5, 173.7 ), new RealPoint( 263.7, 64.3 ), new RealPoint( 27.5, 185.8 ), new RealPoint( 198.3, 118.0 ),
			new RealPoint( 293.5, 58.3 ), new RealPoint( 41.9, 98.2 ), new RealPoint( 162.1, 196.0 ), new RealPoint( 235.8, 189.6 ), new RealPoint( 43.7, 47.1 ), new RealPoint( 60.6, 182.1 ),
			new RealPoint( 268.4, 118.2 ), new RealPoint( 222.4, 15.5 ), new RealPoint( 271.3, 123.2 ), new RealPoint( 316.2, 7.9 ), new RealPoint( 86.2, 156.7 ), new RealPoint( 318.9, 27.9 ),
			new RealPoint( 219.1, 72.8 ), new RealPoint( 80.8, 152.1 ), new RealPoint( 79.1, 25.5 ), new RealPoint( 223.3, 153.8 ), new RealPoint( 97.7, 146.8 ), new RealPoint( 105.0, 5.5 ),
			new RealPoint( 285.6, 99.2 ), new RealPoint( 289.2, 136.2 ), new RealPoint( 47.4, 194.2 ), new RealPoint( 50.3, 9.4 ), new RealPoint( 86.0, 132.4 ), new RealPoint( 299.2, 56.3 ),
			new RealPoint( 259.1, 100.2 ), new RealPoint( 91.4, 49.2 ), new RealPoint( 268.9, 98.7 ), new RealPoint( 247.6, 194.7 ), new RealPoint( 7.4, 36.1 ), new RealPoint( 73.2, 34.7 ),
			new RealPoint( 244.2, 185.6 ), new RealPoint( 227.2, 20.5 ),
			new RealPoint( 129.0, 69.1 ), new RealPoint( 217.9, 190.4 ), new RealPoint( 239.5, 18.6 ), new RealPoint( 103.5, 121.2 ), new RealPoint( 157.9, 124.5 ), new RealPoint( 117.6, 20.8 ),
			new RealPoint( 211.1, 21.6 ), new RealPoint( 60.3, 195.6 ), new RealPoint( 234.9, 151.2 ), new RealPoint( 166.6, 140.1 ), new RealPoint( 41.6, 111.1 ), new RealPoint( 308.1, 8.9 ),
			new RealPoint( 119.3, 186.2 ), new RealPoint( 95.2, 60.3 ), new RealPoint( 254.1, 125.1 ), new RealPoint( 240.3, 111.6 ), new RealPoint( 154.8, 70.3 ), new RealPoint( 195.0, 8.9 ),
			new RealPoint( 315.7, 66.8 ), new RealPoint( 272.6, 167.9 ), new RealPoint( 188.6, 3.6 ), new RealPoint( 57.2, 167.7 ), new RealPoint( 301.5, 90.1 ), new RealPoint( 113.8, 153.0 ),
			new RealPoint( 178.0, 1.3 ), new RealPoint( 253.5, 130.3 ), new RealPoint( 280.8, 194.9 ), new RealPoint( 50.7, 174.9 ), new RealPoint( 237.3, 135.2 ), new RealPoint( 27.8, 30.4 ),
			new RealPoint( 164.6, 57.1 ), new RealPoint( 111.7, 100.6 ), new RealPoint( 191.9, 117.7 ), new RealPoint( 95.8, 67.1 ), new RealPoint( 278.4, 57.6 ), new RealPoint( 25.1, 16.7 ),
			new RealPoint( 60.8, 16.5 ), new RealPoint( 272.0, 180.7 ), new RealPoint( 136.2, 51.9 ), new RealPoint( 97.7, 16.2 ), new RealPoint( 98.5, 156.0 ), new RealPoint( 223.1, 84.2 ),
			new RealPoint( 304.7, 31.5 ), new RealPoint( 30.9, 44.0 ), new RealPoint( 63.1, 106.8 ), new RealPoint( 244.4, 112.9 ), new RealPoint( 208.4, 112.5 ), new RealPoint( 104.7, 184.5 ),
			new RealPoint( 9.8, 90.0 ), new RealPoint( 311.9, 13.2 ),
			new RealPoint( 114.2, 123.7 ), new RealPoint( 310.6, 32.2 ), new RealPoint( 203.8, 22.2 ), new RealPoint( 205.2, 10.5 ), new RealPoint( 304.7, 108.6 ), new RealPoint( 111.7, 16.4 ),
			new RealPoint( 59.0, 26.1 ), new RealPoint( 71.7, 186.3 ), new RealPoint( 97.8, 123.3 ), new RealPoint( 310.8, 39.1 ), new RealPoint( 235.9, 118.2 ), new RealPoint( 241.2, 14.9 ),
			new RealPoint( 88.7, 149.3 ), new RealPoint( 294.3, 187.3 ), new RealPoint( 308.6, 15.4 ), new RealPoint( 137.3, 159.5 ), new RealPoint( 222.4, 191.2 ), new RealPoint( 267.8, 170.1 ),
			new RealPoint( 121.2, 53.6 ), new RealPoint( 40.6, 120.0 ), new RealPoint( 105.3, 87.5 ), new RealPoint( 315.3, 154.8 ), new RealPoint( 275.8, 81.1 ), new RealPoint( 286.9, 101.1 ),
			new RealPoint( 63.7, 198.3 ), new RealPoint( 316.0, 149.3 ), new RealPoint( 141.0, 7.3 ), new RealPoint( 218.9, 194.7 ), new RealPoint( 266.7, 10.0 ), new RealPoint( 46.3, 106.7 ),
			new RealPoint( 148.4, 162.1 ), new RealPoint( 247.7, 170.3 ), new RealPoint( 91.2, 99.8 ), new RealPoint( 151.8, 188.1 ), new RealPoint( 226.8, 71.3 ), new RealPoint( 251.6, 34.4 ),
			new RealPoint( 307.8, 22.8 ), new RealPoint( 286.4, 77.5 ), new RealPoint( 23.9, 74.0 ), new RealPoint( 59.5, 172.0 ), new RealPoint( 218.3, 51.7 ), new RealPoint( 48.5, 66.0 ),
			new RealPoint( 263.7, 106.9 ), new RealPoint( 192.0, 138.0 ), new RealPoint( 138.9, 28.1 ), new RealPoint( 126.7, 113.8 ), new RealPoint( 200.9, 182.0 ), new RealPoint( 154.9, 186.6 ),
			new RealPoint( 242.6, 171.5 ), new RealPoint( 193.9, 106.0 ),
			new RealPoint( 250.7, 165.3 ), new RealPoint( 271.1, 125.4 ), new RealPoint( 254.1, 188.9 ), new RealPoint( 54.3, 194.3 ), new RealPoint( 131.0, 100.5 ), new RealPoint( 41.2, 83.8 ),
			new RealPoint( 102.6, 40.1 ), new RealPoint( 102.9, 164.6 ), new RealPoint( 157.8, 177.5 ), new RealPoint( 135.3, 68.6 ), new RealPoint( 122.3, 191.2 ), new RealPoint( 106.5, 5.0 ),
			new RealPoint( 19.4, 95.9 ), new RealPoint( 207.1, 10.7 ), new RealPoint( 111.3, 37.6 ), new RealPoint( 180.0, 117.2 ), new RealPoint( 278.2, 9.8 ), new RealPoint( 113.6, 62.3 ),
			new RealPoint( 143.2, 189.0 ), new RealPoint( 256.1, 61.3 ), new RealPoint( 127.6, 116.8 ), new RealPoint( 78.9, 162.1 ), new RealPoint( 49.5, 32.6 ), new RealPoint( 168.1, 148.9 ),
			new RealPoint( 1.7, 181.2 ), new RealPoint( 209.0, 119.7 ), new RealPoint( 310.0, 149.3 ), new RealPoint( 312.4, 97.3 ), new RealPoint( 173.6, 134.8 ), new RealPoint( 159.8, 132.0 ),
			new RealPoint( 149.2, 103.0 ), new RealPoint( 64.9, 153.6 ), new RealPoint( 272.3, 159.9 ), new RealPoint( 161.8, 98.7 ), new RealPoint( 204.3, 83.1 ), new RealPoint( 139.3, 196.2 ),
			new RealPoint( 262.3, 103.4 ), new RealPoint( 217.4, 134.9 ), new RealPoint( 298.8, 4.7 ), new RealPoint( 159.8, 132.0 ), new RealPoint( 2.0, 109.6 ), new RealPoint( 108.1, 153.4 ),
			new RealPoint( 163.5, 69.7 ), new RealPoint( 28.4, 182.3 ), new RealPoint( 317.6, 32.7 ), new RealPoint( 59.8, 16.3 ), new RealPoint( 182.1, 74.3 ), new RealPoint( 28.7, 96.0 ),
			new RealPoint( 315.0, 13.7 ), new RealPoint( 77.3, 20.8 ),
			new RealPoint( 270.8, 172.7 ), new RealPoint( 110.8, 162.2 ), new RealPoint( 88.2, 80.4 ), new RealPoint( 188.0, 133.6 ), new RealPoint( 190.6, 83.8 ), new RealPoint( 157.9, 175.9 ),
			new RealPoint( 196.0, 61.5 ), new RealPoint( 85.2, 27.5 ), new RealPoint( 199.6, 148.1 ), new RealPoint( 244.6, 123.4 ), new RealPoint( 207.6, 161.7 ), new RealPoint( 251.6, 26.5 ),
			new RealPoint( 79.4, 125.9 ), new RealPoint( 294.0, 34.9 ), new RealPoint( 289.3, 4.4 ), new RealPoint( 22.7, 135.7 ), new RealPoint( 70.2, 39.8 ), new RealPoint( 217.4, 85.1 ),
			new RealPoint( 318.1, 172.2 ), new RealPoint( 252.3, 151.0 ), new RealPoint( 310.9, 48.5 ), new RealPoint( 103.3, 79.1 ), new RealPoint( 14.3, 170.8 ), new RealPoint( 139.9, 111.3 ),
			new RealPoint( 246.8, 169.5 ), new RealPoint( 104.1, 103.2 ), new RealPoint( 162.7, 128.5 ), new RealPoint( 187.9, 10.5 ), new RealPoint( 184.4, 124.4 ), new RealPoint( 65.9, 182.8 ),
			new RealPoint( 280.3, 60.1 ), new RealPoint( 50.4, 120.4 ), new RealPoint( 232.8, 23.1 ), new RealPoint( 302.7, 30.4 ), new RealPoint( 294.3, 124.9 ), new RealPoint( 211.9, 146.4 ),
			new RealPoint( 249.4, 70.6 ), new RealPoint( 213.7, 10.7 ), new RealPoint( 156.4, 185.6 ), new RealPoint( 244.5, 59.5 ), new RealPoint( 15.5, 114.2 ), new RealPoint( 130.1, 126.0 ),
			new RealPoint( 9.4, 8.7 ), new RealPoint( 190.3, 127.8 ), new RealPoint( 228.6, 98.4 ), new RealPoint( 60.4, 40.1 ), new RealPoint( 97.2, 104.1 ), new RealPoint( 208.8, 109.4 ),
			new RealPoint( 189.9, 156.9 ), new RealPoint( 242.7, 27.9 ),
			new RealPoint( 3.9, 67.2 ), new RealPoint( 92.7, 2.1 ), new RealPoint( 266.6, 115.0 ), new RealPoint( 202.4, 198.7 ), new RealPoint( 80.4, 93.0 ), new RealPoint( 11.7, 24.3 ),
			new RealPoint( 66.1, 16.9 ), new RealPoint( 108.5, 166.3 ), new RealPoint( 20.3, 38.2 ), new RealPoint( 100.3, 133.7 ), new RealPoint( 241.3, 159.5 ), new RealPoint( 8.9, 198.3 ),
			new RealPoint( 153.3, 7.9 ), new RealPoint( 90.3, 41.1 ), new RealPoint( 298.3, 155.4 ), new RealPoint( 260.9, 145.9 ), new RealPoint( 259.6, 128.0 ), new RealPoint( 271.3, 125.3 ),
			new RealPoint( 168.2, 184.3 ), new RealPoint( 172.1, 71.1 ), new RealPoint( 38.2, 196.4 ), new RealPoint( 36.9, 126.6 ), new RealPoint( 305.9, 75.4 ), new RealPoint( 241.4, 168.1 ),
			new RealPoint( 18.4, 155.2 ), new RealPoint( 242.9, 106.3 ), new RealPoint( 261.5, 76.0 ), new RealPoint( 70.5, 13.5 ), new RealPoint( 190.4, 189.9 ), new RealPoint( 53.8, 49.2 ),
			new RealPoint( 217.7, 42.8 ), new RealPoint( 48.3, 17.5 ), new RealPoint( 89.6, 102.7 ), new RealPoint( 85.7, 110.7 ), new RealPoint( 174.6, 16.8 ), new RealPoint( 312.3, 34.4 ),
			new RealPoint( 0.2, 47.9 ), new RealPoint( 130.4, 175.5 ), new RealPoint( 159.5, 12.4 ), new RealPoint( 146.2, 94.1 ), new RealPoint( 156.3, 189.9 ), new RealPoint( 236.9, 29.4 ),
			new RealPoint( 130.3, 90.2 ), new RealPoint( 90.6, 105.6 ), new RealPoint( 318.2, 76.8 ), new RealPoint( 294.5, 107.4 ), new RealPoint( 237.2, 38.3 ), new RealPoint( 195.0, 9.5 ),
			new RealPoint( 267.9, 164.5 ), new RealPoint( 35.0, 40.1 ),
			new RealPoint( 273.8, 28.2 ), new RealPoint( 38.7, 116.5 ), new RealPoint( 255.0, 18.4 ), new RealPoint( 180.9, 34.0 ), new RealPoint( 96.0, 79.0 ), new RealPoint( 168.7, 24.8 ),
			new RealPoint( 180.2, 103.1 ), new RealPoint( 117.7, 21.5 ), new RealPoint( 0.7, 193.1 ), new RealPoint( 215.7, 131.5 ), new RealPoint( 217.3, 194.6 ), new RealPoint( 311.9, 122.3 ),
			new RealPoint( 131.4, 91.6 ), new RealPoint( 193.4, 52.7 ), new RealPoint( 225.9, 34.2 ), new RealPoint( 230.9, 39.7 ), new RealPoint( 293.3, 113.6 ), new RealPoint( 83.2, 54.0 ),
			new RealPoint( 175.1, 131.0 ), new RealPoint( 142.8, 36.1 ), new RealPoint( 45.1, 118.5 ), new RealPoint( 222.9, 107.9 ), new RealPoint( 49.2, 75.5 ), new RealPoint( 264.2, 18.8 ),
			new RealPoint( 289.6, 46.2 ), new RealPoint( 311.7, 190.1 ), new RealPoint( 18.9, 132.2 ), new RealPoint( 53.0, 75.9 ), new RealPoint( 8.4, 66.2 ), new RealPoint( 29.2, 113.5 ),
			new RealPoint( 146.9, 188.5 ), new RealPoint( 204.9, 86.8 ), new RealPoint( 50.9, 91.6 ), new RealPoint( 197.6, 25.0 ), new RealPoint( 251.8, 137.6 ), new RealPoint( 101.8, 58.7 ),
			new RealPoint( 127.1, 72.8 ), new RealPoint( 305.9, 159.9 ), new RealPoint( 315.9, 50.4 ), new RealPoint( 129.5, 103.8 ), new RealPoint( 114.1, 146.9 ), new RealPoint( 100.1, 87.6 ),
			new RealPoint( 305.8, 135.2 ), new RealPoint( 177.7, 18.3 ), new RealPoint( 282.0, 170.7 ), new RealPoint( 258.5, 119.6 ), new RealPoint( 270.3, 134.3 ), new RealPoint( 71.0, 65.7 ),
			new RealPoint( 223.8, 198.5 ), new RealPoint( 127.0, 38.9 ),
	};

}
