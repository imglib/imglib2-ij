package net.imglib2.kdtree;

import java.util.ArrayList;
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
		throw new UnsupportedOperationException(); // TODO
	}

	public < L extends RealLocalizable > KDTree( final List< T > values, final List< L > positions )
	{
//		--> the constructor can be called with the same list twice, values == positions, if T extends RealLocalizable
//		For the new implementation, this distinction shouldn't matter

		assert values.size() == positions.size();
		assert !positions.isEmpty();

		final int numPoints = positions.size();
		final int numDimensions = positions.iterator().next().numDimensions();

		final double[][] points = new double[ numDimensions ][ numPoints ];
		int i = 0;
		for ( RealLocalizable pos : positions )
		{
			for ( int d = 0; d < numDimensions; d++ )
				points[ d ][ i ] = pos.getDoublePosition( d );
			++i;
		}
		final int[] tree = KDTreeBuilder.tree(points);

		final double[][] treeCoordinates = KDTreeBuilder.reorder( points, tree );
		T[] reorderedValues = ( T[] ) new Object[ numPoints ];
		final int[] invtree = KDTreeBuilder.invert( tree );
		for ( int j = 0; j < numPoints; j++ )
			reorderedValues[ invtree[ j ] ] = values.get( j );
		final IntFunction<T> getValue = k -> reorderedValues[ k ];

		impl = new KDTreeImpl( treeCoordinates );
		valuesSupplier = () -> getValue;
	}

	public < L extends RealLocalizable > KDTree( final Iterable< T > values, final Iterable< L > positions )
	{
		throw new UnsupportedOperationException(); // TODO
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
