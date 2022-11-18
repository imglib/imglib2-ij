package net.imglib2.kdtree;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.imglib2.EuclideanSpace;
import net.imglib2.IterableRealInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.converter.AbstractConvertedIterableRealInterval;
import net.imglib2.converter.AbstractConvertedRealCursor;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;

import static net.imglib2.kdtree.KDTreeData.PositionsLayout.FLAT;

public class KDTree< T > implements EuclideanSpace, IterableRealInterval< T >
{
	// TODO visibility?
	final KDTreeData< T > treeData;

	// TODO visibility?
	final KDTreeImpl impl;

	// TODO visibility?
	final Supplier< IntFunction< T > > valuesSupplier;

	private final double[] min;

	private final double[] max;

	/**
	 * Construct a KDTree from the elements in the given list.
	 *
	 * <p>
	 * Note that the constructor can be called with the same list for both
	 * {@code values == positions} if {@code T extends RealLocalizable}.
	 * </p>
	 *
	 * @param values
	 * 		a list of values
	 * @param positions
	 * 		a list of positions corresponding to the values
	 */
	public < L extends RealLocalizable > KDTree( final List< T > values, final List< L > positions )
	{
		this( verifySize(values, positions), values, positions );
	}

	private static int verifySize( final List< ? > values, final List< ? > positions )
	{
		if ( values.size() != positions.size() )
			throw new IllegalArgumentException( "The list of values and the list of positions provided to KDTree should have the same size." );
		if ( positions.isEmpty() )
			throw new IllegalArgumentException( "List of positions is empty. At least one point is requires to construct a KDTree." );
		return values.size();
	}

	/**
	 * Construct a KDTree from the elements of the given
	 * {@link IterableRealInterval}.
	 *
	 * @param interval
	 *            elements in the tree are obtained by iterating this
	 */
	public KDTree( final IterableRealInterval< T > interval )
	{
		this( verifySize( interval ), interval, positionsIterable( interval ) );
	}

	private static int verifySize( final IterableRealInterval< ? > interval )
	{
		final long size = interval.size();
		if ( size > KDTreeUtils.MAX_ARRAY_SIZE )
			throw new IllegalArgumentException( "Interval contains too many points to store in KDTree" );
		else if ( size <= 0 )
			throw new IllegalArgumentException( "Interval is empty. At least one point is requires to construct a KDTree." );
		return ( int ) size;
	}

	private static < A > Iterable< RealLocalizable > positionsIterable( IterableRealInterval< A > sourceInterval )
	{
		return new AbstractConvertedIterableRealInterval< A, RealLocalizable >( sourceInterval )
		{

			class Cursor extends AbstractConvertedRealCursor< A, RealLocalizable >
			{
				Cursor( final RealCursor< A > source )
				{
					super( source );
				}

				@Override
				public RealLocalizable get()
				{
					return source;
				}

				@Override
				public Cursor copy()
				{
					return new Cursor( source.copyCursor() );
				}
			}

			@Override
			public AbstractConvertedRealCursor< A, RealLocalizable > cursor()
			{
				return new Cursor( sourceInterval.cursor() );
			}

			@Override
			public AbstractConvertedRealCursor< A, RealLocalizable > localizingCursor()
			{
				return new Cursor( sourceInterval.localizingCursor() );
			}
		};
	}

	public < L extends RealLocalizable > KDTree( final int numPoints, final Iterable< T > values, final Iterable< L > positions )
	{
		// TODO make storeValuesAsNativeImg a parameter
		this( createKDTree( numPoints, values, positions, true ) );
	}

	// construct with pre-built data, e.g., from deserialization
	public KDTree( final KDTreeData< T > data )
	{
		treeData = data;
		min = new double[ data.numDimensions() ];
		max = new double[ data.numDimensions() ];
		if ( data.layout() == FLAT )
		{
			impl = new KDTreeImpl.Flat( data.flatPositions(), data.numDimensions() );
			KDTreeUtils.computeMinMax( data.flatPositions(), min, max );
		}
		else
		{
			impl = new KDTreeImpl.Nested( data.positions() );
			KDTreeUtils.computeMinMax( data.positions(), min, max );
		}
		valuesSupplier = data.valuesSupplier();
	}

	/**
	 * Get the root node.
	 * <p>
	 * TODO: explain that users of this most likely should use KDTreeImpl and indices instead
	 *
	 * @return the root node.
	 */
	public KDTreeNode< T > getRoot()
	{
		return new KDTreeNode<>( this ).setNodeIndex( impl.root() );
	}

	@Override
	public int numDimensions()
	{
		return impl.numDimensions();
	}

	@Override
	public double realMin( final int d )
	{
		return min[ d ];
	}

	@Override
	public double realMax( final int d )
	{
		return max[ d ];
	}

	@Override
	public KDTreeCursor cursor()
	{
		return new KDTreeCursor();
	}

	public final class KDTreeCursor extends KDTreeNode< T > implements RealCursor< T >
	{
		KDTreeCursor()
		{
			super( KDTree.this );
			reset();
		}

		@Override
		public T next()
		{
			fwd();
			return get();
		}

		@Override
		public void fwd()
		{
			setNodeIndex( nodeIndex() + 1 );
		}

		@Override
		public void reset()
		{
			setNodeIndex( -1 );
		}

		@Override
		public boolean hasNext()
		{
			return nodeIndex() < impl.size() - 1;
		}

		@Override
		public void jumpFwd( final long steps )
		{
			for ( long i = 0; i < steps; ++i )
				fwd();
		}

		@Override
		public KDTreeCursor copyCursor()
		{
			return copy();
		}

		@Override
		public KDTreeCursor copy()
		{
			final KDTreeCursor copy = new KDTreeCursor();
			copy.setNodeIndex( nodeIndex() );
			return copy;
		}
	}

	@Override
	public KDTreeCursor localizingCursor()
	{
		return cursor();
	}

	@Override
	public KDTreeCursor iterator()
	{
		return cursor();
	}

	@Override
	public long size()
	{
		return impl.size();
	}

	@Override
	public Object iterationOrder()
	{
		return this; // iteration order is only compatible with ourselves
	}

	@Override
	public String toString()
	{
		return toString( impl.root(), "", createNode() );
	}

	private String toString( final int node, final String indent, final KDTreeNode< T > ref )
	{
		if ( node < 0 )
			return "";
		return indent + "- " + ref.setNodeIndex( node ).toString() + "\n"
				+ toString( impl.left( node ), indent + "  ", ref )
				+ toString( impl.right( node ), indent + "  ", ref );
	}

	// -----------------------------------------------------
	//   new API. TODO check what is actually used!
	//

	KDTreeNode< T > createNode() {
		return new KDTreeNode<>( this );
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

	KDTreeNode< T > root( final KDTreeNode< T > ref )
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
		if ( numPoints <= 0 )
			throw new IllegalArgumentException( "At least one point is required to construct a KDTree." );
		final int numDimensions = KDTreeUtils.getNumDimensions( positions );
		final double[][] points = KDTreeUtils.initPositions( numDimensions, numPoints, positions );
		final int[] tree = KDTreeUtils.makeTree( points );
		final int[] invtree = KDTreeUtils.invert( tree );

		final boolean useFlatLayout = ( long ) numDimensions * numPoints <= KDTreeUtils.MAX_ARRAY_SIZE;
		if ( storeValuesAsNativeImg && KDTreeUtils.getType( values ) instanceof NativeType )
		{
			@SuppressWarnings( "unchecked" )
			final Img< T > treeValues = ( Img< T > ) KDTreeUtils.orderValuesImg( invtree, ( Iterable ) values );
			if ( useFlatLayout )
				return new KDTreeData<>(KDTreeUtils.reorderToFlatLayout( points, tree ), treeValues);
			else
				return new KDTreeData<>(KDTreeUtils.reorder( points, tree ), treeValues);
		}
		else
		{
			final List< T > treeValues = KDTreeUtils.orderValuesList( invtree, values );
			if ( useFlatLayout )
				return new KDTreeData<>(KDTreeUtils.reorderToFlatLayout( points, tree ), treeValues);
			else
				return new KDTreeData<>(KDTreeUtils.reorder( points, tree ), treeValues);
		}
	}
}
