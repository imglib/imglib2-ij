package net.imglib2.kdtree;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.imglib2.IterableRealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;

import static net.imglib2.kdtree.KDTreeData.PositionsLayout.FLAT;

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
		// TODO make storeValuesAsNativeImg a parameter
		this( createKDTree( numPoints, values, positions, true ) );
	}

	// cinstruct with pre-built data, e.g., from deserialization
	public KDTree( final KDTreeData< T > data )
	{
		treeData = data;
		if ( data.layout() == FLAT )
			impl = new KDTreeImpl.Flat( data.flatPositions(), data.numDimensions() );
		else
			impl = new KDTreeImpl.Nested( data.positions() );
		valuesSupplier = data.valuesSupplier();
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

	/**
	 * @param storeValuesAsNativeImg
	 * 		If {@code true} and {@code T} is a {@code NativeType},
	 * 		store values into {@code NativeImg}.
	 * 		Otherwise, store values as a {@code List<T>}.
	 */
	private static < L extends RealLocalizable, T > KDTreeData< T > createKDTree(
			final int numPoints,
			final Iterable< T > values,
			final Iterable< L > positions,
			final boolean storeValuesAsNativeImg )
	{
		final int numDimensions = KDTreeUtils.getNumDimensions( positions );
		final double[][] points = KDTreeUtils.initPositions( numDimensions, numPoints, positions );
		final int[] tree = KDTreeUtils.makeTree( points );
		final int[] invtree = KDTreeUtils.invert( tree );

		// TODO: Alternatively, this could also flatten out the dimensions if
		// 		 everything fits into one array
		//       See KDTreeBuilder.MAX_ARRAY_SIZE
		//		 and KDTreeBuilder.reorderToFlatLayout(...)
//		final double[][] treePoints = KDTreeUtils.reorder( points, tree );
		final double[] treePoints = KDTreeUtils.reorderToFlatLayout( points, tree );

		if ( storeValuesAsNativeImg && KDTreeUtils.getType( values ) instanceof NativeType )
		{
			@SuppressWarnings( "unchecked" )
			final Img< T > treeValues = ( Img< T > ) KDTreeUtils.orderValuesImg( invtree, ( Iterable ) values );
			return new KDTreeData< T >( treePoints, treeValues );
		}
		else
		{
			final List< T > treeValues = KDTreeUtils.orderValuesList( invtree, values );
			return new KDTreeData< T >( treePoints, treeValues );
		}
	}
}
