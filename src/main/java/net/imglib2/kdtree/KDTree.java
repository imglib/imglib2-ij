package net.imglib2.kdtree;

import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.imglib2.IterableRealInterval;
import net.imglib2.RealLocalizable;

public class KDTree< T >
{
	final KDTreeImpl impl;

	final Supplier< IntFunction< T > > valuesSupplier;

	public KDTree( final IterableRealInterval< T > interval )
	{
//		--> This is implemented as a special case with a List<RealCursor<T>>
//		I will change this to instead a List<T> (or better ArrayImg<T>, ListImg<T>?)

		// TODO: assert size > 0
		// TODO: test

		this( ( int ) interval.size(), interval, new CursorConvertedIterableRealInterval<>( interval, () -> c -> c ) );
	}

	public < L extends RealLocalizable > KDTree( final List< T > values, final List< L > positions )
	{
//		--> the constructor can be called with the same list twice, values == positions, if T extends RealLocalizable
//		For the new implementation, this distinction shouldn't matter

		// TODO: revise: encapsulate check in size(values, positions) private static method
//		assert values.size() == positions.size();
//		assert !positions.isEmpty();
		this( values.size(), values, positions );
	}

	public < L extends RealLocalizable > KDTree( final int numPoints, final Iterable< T > values, final Iterable< L > positions )
	{
		Iterator< L > ipos = positions.iterator();
		if ( !ipos.hasNext() )
			throw new IllegalArgumentException( "provided positions Iterable has fewer elements than required" );
		final int numDimensions = ipos.next().numDimensions();

		final double[][] points = new double[ numDimensions ][ numPoints ];
		ipos = positions.iterator();
		for ( int i = 0; i < numPoints; ++i )
		{
			if ( !ipos.hasNext() )
				throw new IllegalArgumentException( "provided positions Iterable has fewer elements than required" );
			final L pos = ipos.next();
			for ( int d = 0; d < numDimensions; d++ )
				points[ d ][ i ] = pos.getDoublePosition( d );
		}
		final int[] tree = KDTreeBuilder.tree(points);
		final int[] invtree = KDTreeBuilder.invert( tree );

		// TODO: Alternatively, this could also flatten out the dimensions if
		// 		 everything fits into one array
		final double[][] treeCoordinates = KDTreeBuilder.reorder( points, tree );

		// TODO: Encapsulate this into a function
		//       Then make a variant that uses an ArrayImg<T> if T is NativeType
		//       (This should be opt-in, if desired by client code)
		@SuppressWarnings( "unchecked" )
		final T[] reorderedValues = ( T[] ) new Object[ numPoints ];
		final Iterator< T > ival = values.iterator();
		for ( int i = 0; i < numPoints; ++i )
		{
			if ( !ival.hasNext() )
				throw new IllegalArgumentException( "provided values Iterable has fewer elements than required" );
			reorderedValues[ invtree[ i ] ] = ival.next();
		}
		final IntFunction<T> getValue = k -> reorderedValues[ k ];

		impl = new KDTreeImpl( treeCoordinates );
		valuesSupplier = () -> getValue;
	}


	KDTree( final KDTreeImpl impl, final Supplier< IntFunction< T > > valuesSupplier )
	{
		this.impl = impl;
		this.valuesSupplier = valuesSupplier;
	}

	public KDTreeNode< T > getRoot()
	{
		return new KDTreeNode<>( this ).setNodeIndex( impl.root() );
	}

	KDTreeNode< T > left( final KDTreeNode< T > parent )
	{
		final int c = impl.left( parent.nodeIndex() );
		return c < 0 ? null : new KDTreeNode<>( this ).setNodeIndex( c );
	}

	KDTreeNode< T > right( final KDTreeNode< T > parent )
	{
		final int c = impl.right( parent.nodeIndex() );
		return c < 0 ? null : new KDTreeNode<>( this ).setNodeIndex( c );
	}

	KDTreeNode< T > getRoot( final KDTreeNode< T > ref )
	{
		return ref.setNodeIndex( impl.root() );
	}

	KDTreeNode< T > left( final KDTreeNode< T > parent, final KDTreeNode< T > ref )
	{
		final int c = impl.left( parent.nodeIndex() );
		return c < 0 ? null : ref.setNodeIndex( c );
	}

	KDTreeNode< T > right( final KDTreeNode< T > parent, final KDTreeNode< T > ref )
	{
		final int c = impl.right( parent.nodeIndex() );
		return c < 0 ? null : ref.setNodeIndex( c );
	}

	public int numDimensions()
	{
		return impl.numDimensions();
	}
}
