package net.imglib2.kdtree;

import java.util.Arrays;

class Candidates
{
	double[] distances;

	int[] indices;

	int size;

	Candidates()
	{
		final int capacity = 10;
		distances = new double[ capacity ];
		indices = new int[ capacity ];
		size = 0;
	}

	void clear()
	{
		size = 0;
	}

	void add( final double distance, final int index )
	{
		if ( distances.length <= size )
		{
			// reallocate
			final int newLength = distances.length * 2;
			distances = Arrays.copyOf( distances, newLength );
			indices = Arrays.copyOf( indices, newLength );
		}
		distances[ size ] = distance;
		indices[ size ] = index;
		++size;
	}

	void sort() {
		final int[] order = new int[ size ];
		Arrays.setAll( order, i -> i );
		quicksort( 0, size - 1, distances, order );
		System.arraycopy( reorder( distances, order ), 0, distances, 0, size );
		System.arraycopy( reorder( indices, order ), 0, indices, 0, size );
	}

	void makeCopyOf( final Candidates other )
	{
		distances = other.distances.clone();
		indices = other.indices.clone();
		size = other.size;
	}



	// TODO --> move to KDTreeUtil and reuse
	private static double[] reorder( final double[] values, final int[] order )
	{
		final int size = order.length;
		final double[] reordered = new double[ size ];
		Arrays.setAll( reordered, i -> values[ order[ i ] ] );
		return reordered;
	}

	// TODO --> move to KDTreeUtil
	private static int[] reorder( final int[] values, final int[] order )
	{
		final int size = order.length;
		final int[] reordered = new int[ size ];
		Arrays.setAll( reordered, i -> values[ order[ i ] ] );
		return reordered;
//		System.arraycopy( reordered, 0, values, 0, size );
	}


	// TODO --> move to KDTreeUtil
	private static void quicksort( final int i, final int j, final double[] values, final int[] order )
	{
		if ( 0 <= i && i < j )
		{
			final int p = partition( i, j, values, order );
			quicksort( i, p - 1, values, order );
			quicksort( p + 1, j, values, order );
		}
	}

	// TODO --> move to KDTreeUtil and reuse
	private static void swap( final int i, final int j, final int[] order )
	{
		final int tmp = order[ i ];
		order[ i ] = order[ j ];
		order[ j ] = tmp;
	}

	/**
	 * Partition a sublist of distances.
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
	 * @return index of pivot element
	 */
	// TODO --> move to KDTreeUtil and reuse
	private static int partition( int i, int j, final double[] values, final int[] order )
	{
		final int len = j - i + 1;
		if ( len <= 2 )
		{
			if ( len <= 0 )
				throw new IllegalArgumentException();
			if ( values[ order[ i ] ] > values[ order[ j ] ] )
				swap( i, j, order );
			return i;
		}
		else
		{
			final int m = ( i + j ) / 2;
			if ( values[ order[ i ] ] > values[ order[ m ] ] )
				swap( i, m, order );
			if ( values[ order[ i ] ] > values[ order[ j ] ] )
				swap( i, j, order );
			if ( values[ order[ m ] ] > values[ order[ j ] ] )
				swap( m, j, order );
			swap( m, i + 1, order );
			final int p = ++i;
			final double pivot = values[ order[ p ] ];
			while ( true )
			{
				while ( values[ order[ ++i ] ] < pivot )
					;
				while ( values[ order[ --j ] ] > pivot )
					;
				if ( j < i )
					break;
				swap( i, j, order );
			}
			swap( p, j, order );
			return j;
		}
	}
}
