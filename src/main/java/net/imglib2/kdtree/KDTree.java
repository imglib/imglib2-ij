package net.imglib2.kdtree;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.imglib2.IterableRealInterval;
import net.imglib2.RealLocalizable;

public class KDTree< T >
{
	final KDTreeData< T > treeData;

	final KDTreeImpl impl;

	final Supplier< IntFunction< T > > valuesSupplier;

	public KDTree( final IterableRealInterval< T > interval )
	{
		//	Previously, this was implemented as a special case with a List<RealCursor<T>>

		// TODO: assert size > 0
		// TODO: test

		this( ( int ) interval.size(), interval, new CursorConvertedIterableRealInterval<>( interval, () -> c -> c ) );
	}

	public < L extends RealLocalizable > KDTree( final List< T > values, final List< L > positions )
	{
		// TODO: revise: encapsulate check in size(values, positions) private static method
//		assert values.size() == positions.size();
//		assert !positions.isEmpty();
		this( values.size(), values, positions );
	}

	public < L extends RealLocalizable > KDTree( final int numPoints, final Iterable< T > values, final Iterable< L > positions )
	{
		final boolean storeValuesAsNativeImg = true; // TODO make this a parameter
		treeData = KDTreeData.create( numPoints, values, positions, storeValuesAsNativeImg );
		impl = new KDTreeImpl( treeData.positions() ); // TODO or treeData.flatPositions() depending on treeData.layout()
		valuesSupplier = treeData.valuesSupplier();
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
