package net.imglib2.kdtree;

import java.util.Arrays;

import static net.imglib2.kdtree.KDTreeUtils.quicksort;
import static net.imglib2.kdtree.KDTreeUtils.reorder;

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



}
