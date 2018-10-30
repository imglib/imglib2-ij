package net.imglib2.io.proxyaccess;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.util.IntervalIndexer;

/**
 * Proxy access over any {@link GenericShortType} container.
 * 
 * @author Albert Cardona
 */
public class ShortAccessProxy< T extends GenericShortType< T > > extends AbstractAccessProxy< T > implements ShortAccess, ArrayDataAccess< short[] >
{
	private static final long serialVersionUID = 1L;

	public ShortAccessProxy( final RandomAccessibleInterval< T > rai )
	{
		super( rai );
	}
	
	@Override
	synchronized public short getValue( final int index )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		return this.ra.get().getShort();
	}

	@Override
	synchronized public void setValue( final int index, final short value )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		this.ra.get().setShort( value );
	}

	@Override
	public short[] createArray( final int numEntities ) {
		return new short[ numEntities ];
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
