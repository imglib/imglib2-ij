package net.imglib2.kdtree;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.NativeType;

// TODO javadoc
// TODO revise visibility of fields and methods

public class KDTreeData< T >
{
	private final int numDimensions;
	private final int numPoints;

	private final PositionsLayout layout;
	private final double[][] positions;
	private final double[] flatPositions;

	private final List< T > valuesList;
	private final RandomAccessibleInterval< T > valuesImg;
	private final Supplier< IntFunction< T > > valuesSupplier;

	public KDTreeData( double[][] positions, List< T > values )
	{
		numPoints = values.size();
		numDimensions = positions.length;

		layout = PositionsLayout.NESTED;
		this.positions = positions;
		flatPositions = null;

		valuesList = values;
		valuesImg = null;
		final IntFunction< T > v = values::get;
		valuesSupplier = () -> v;
	}

	public KDTreeData( double[][] positions, RandomAccessibleInterval< T > values )
	{
		numPoints = ( int ) values.dimension( 0 );
		numDimensions = positions.length;

		layout = PositionsLayout.NESTED;
		this.positions = positions;
		flatPositions = null;

		valuesList = null;
		valuesImg = values;
		valuesSupplier = () -> {
			final RandomAccess<T> ra = valuesImg.randomAccess();
			return i -> ra.setPositionAndGet(i);
		};
	}

	public KDTreeData( double[] positions, List< T > values )
	{
		numPoints = values.size();
		numDimensions = positions.length / numPoints;

		layout = PositionsLayout.FLAT;
		this.positions = null;
		flatPositions = positions;

		valuesList = values;
		valuesImg = null;
		final IntFunction< T > v = values::get;
		valuesSupplier = () -> v;
	}

	public KDTreeData( double[] positions, RandomAccessibleInterval< T > values )
	{
		numPoints = ( int ) values.dimension( 0 );
		numDimensions = positions.length / numPoints;

		layout = PositionsLayout.FLAT;
		this.positions = null;
		flatPositions = positions;

		valuesList = null;
		valuesImg = values;
		valuesSupplier = () -> {
			final RandomAccess<T> ra = valuesImg.randomAccess();
			return i -> ra.setPositionAndGet(i);
		};
	}

	public T type() // TODO could also be Class<T> instead? What is more useful?
	{
		throw new UnsupportedOperationException(); // TODO
	}

	// for serialisation and usage by the tree
	// (TODO) Internal storage may be flattened into single double[] array, which is translated here
	public double[][] positions()
	{
		return positions;
	}

	// for serialization
	// 1D image of values, ArrayImg<T>  if T extends NativeType< T >
	// Even if the underlying storage is a List<T>, it will be packaged as an Img<T>
	public RandomAccessibleInterval< T > values()
	{
		return valuesImg != null
				? valuesImg
				: ListImg.wrap( valuesList, size() );
	}

	// for usage by the KDTree
	public Supplier< IntFunction< T > > valuesSupplier()
	{
		return valuesSupplier;
	}

	// for usage by the KDTree
	// may be null if internal layout is not flat
	public double[] flatPositions()
	{
		return flatPositions;
	}

	public enum PositionsLayout
	{
		FLAT,
		NESTED
	}

	public PositionsLayout layout()
	{
		return layout;
	}

	public int numDimensions()
	{
		return numDimensions;
	}

	public int size()
	{
		return numPoints;
	}


	/**
	 * @param storeValuesAsNativeImg
	 * 		If {@code true} and {@code T} is a {@code NativeType},
	 * 		store values into {@code NativeImg}.
	 * 		Otherwise, store values as a {@code List<T>}.
	 */
	public static < L extends RealLocalizable, T > KDTreeData< T > create(
			final int numDimensions,
			final int numPoints,
			final Iterable< T > values,
			final Iterable< L > positions,
			final boolean storeValuesAsNativeImg )
	{
		final double[][] points = buildCoordinates( numDimensions, numPoints, positions );
		final int[] tree = KDTreeBuilder.tree( points );
		final int[] invtree = KDTreeBuilder.invert( tree );

		// TODO: Alternatively, this could also flatten out the dimensions if
		// 		 everything fits into one array
		//       See KDTreeBuilder.MAX_ARRAY_SIZE
		//		 and KDTreeBuilder.reorderToFlatLayout(...)
		final double[][] treePoints = KDTreeBuilder.reorder( points, tree );

		if ( storeValuesAsNativeImg && getType( values ) instanceof NativeType )
		{
			@SuppressWarnings( "unchecked" )
			final Img< T > treeValues = ( Img< T > ) orderValuesImg( invtree, ( Iterable ) values );
			return new KDTreeData< T >( treePoints, treeValues );
		}
		else
		{
			final List< T > treeValues = orderValuesList( invtree, values );
			return new KDTreeData< T >( treePoints, treeValues );
		}
	}

	// TODO: move to KDTreeBuilder?
	private static double[][] buildCoordinates(
			final int numDimensions,
			final int numPoints,
			final Iterable< ? extends RealLocalizable > positions )
	{
		final double[][] coordinates = new double[ numDimensions ][ numPoints ];
		final Iterator< ? extends RealLocalizable > ipos = positions.iterator();
		for ( int i = 0; i < numPoints; ++i )
		{
			if ( !ipos.hasNext() )
				throw new IllegalArgumentException( "provided positions Iterable has fewer elements than required" );
			final RealLocalizable pos = ipos.next();
			for ( int d = 0; d < numDimensions; d++ )
				coordinates[ d ][ i ] = pos.getDoublePosition( d );
		}
		return coordinates;
	}

	private static < T > T getType( Iterable< T > values )
	{
		final Iterator< T > ival = values.iterator();
		if ( !ival.hasNext() )
			throw new IllegalArgumentException( "provided values Iterable has fewer elements than required" );
		return ival.next();
	}

	private static < T > List< T > orderValuesList(
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

	private static < T extends NativeType< T > > Img< T > orderValuesImg(
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
}
