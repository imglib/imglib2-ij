package net.imglib2.img.io.proxyaccess;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.util.IntervalIndexer;

/**
 * Proxy access over any {@link GenericIntType} container.
 * 
 * @author Albert Cardona
 */
public class IntAccessProxy< T extends GenericIntType< T > > extends AbstractAccessProxy< T > implements IntAccess, ArrayDataAccess< int[] >
{
	private static final long serialVersionUID = 1L;

	public IntAccessProxy( final RandomAccessibleInterval< T > rai )
	{
		super( rai );
	}
	
	@Override
	synchronized public int getValue( final int index )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		return this.ra.get().getInteger();
	}

	@Override
	synchronized public void setValue( final int index, final int value )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		this.ra.get().setInteger( value );
	}

	@Override
	public int[] createArray( final int numEntities ) {
		return new int[ numEntities ];
	}

	@Override
	public Object getCurrentStorageArray()
	{
		return null;
	}

	@Override
	public int getArrayLength()
	{
		return this.size;
	}
}
