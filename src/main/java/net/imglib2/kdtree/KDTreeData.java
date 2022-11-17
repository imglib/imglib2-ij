package net.imglib2.kdtree;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.util.Util;

// TODO javadoc
// TODO revise visibility of fields and methods

public class KDTreeData< T >
{
	public enum PositionsLayout
	{
		FLAT,
		NESTED
	}

	private final int numDimensions;
	private final int numPoints;

	private final PositionsLayout layout;
	private final double[][] positions;
	private final double[] flatPositions;

	private final List< T > valuesList;
	private final RandomAccessibleInterval< T > valuesImg;
	private final Supplier< IntFunction< T > > valuesSupplier;

	private final T type;

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

		type = KDTreeUtils.getType( values );
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

		type = Util.getTypeFromInterval( values );
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

		type = KDTreeUtils.getType( values );
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

		type = Util.getTypeFromInterval( values );
	}

	public T type() // TODO could also be Class<T> instead? What is more useful?
	{
		return type;
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
}
