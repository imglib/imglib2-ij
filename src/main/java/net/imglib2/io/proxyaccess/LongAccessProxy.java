package net.imglib2.io.proxyaccess;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.util.IntervalIndexer;

/**
 * Proxy access over any {@link GenericShortType} container.
 * 
 * @author Albert Cardona
 */
public class LongAccessProxy< T extends GenericLongType< T > > extends AbstractAccessProxy< T > implements LongAccess, ArrayDataAccess< long[] >
{
	private static final long serialVersionUID = 1L;

	public LongAccessProxy( final RandomAccessibleInterval< T > rai )
	{
		super( rai );
	}
	
	@Override
	synchronized public long getValue( final int index )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		return this.ra.get().getIntegerLong();
	}

	@Override
	synchronized public void setValue( final int index, final long value )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		this.ra.get().setInteger( value );
	}

	@Override
	public long[] createArray( final int numEntities ) {
		return new long[ numEntities ];
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
