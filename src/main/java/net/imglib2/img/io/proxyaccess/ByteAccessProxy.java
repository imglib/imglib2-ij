package net.imglib2.img.io.proxyaccess;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.util.IntervalIndexer;

/**
 * Proxy access over any {@link GenericByteType} container.
 * 
 * @author Albert Cardona
 */
public class ByteAccessProxy< T extends GenericByteType< T > > extends AbstractAccessProxy< T > implements ByteAccess, ArrayDataAccess< byte[] >
{
	private static final long serialVersionUID = 1L;

	public ByteAccessProxy( final RandomAccessibleInterval< T > rai )
	{
		super( rai );
	}
	
	@Override
	synchronized public byte getValue( final int index )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		return this.ra.get().getByte();
	}

	@Override
	synchronized public void setValue( final int index, final byte value )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		this.ra.get().setByte( value );
	}

	@Override
	public byte[] createArray( final int numEntities )
	{
		return new byte[ numEntities ];
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
