package net.imglib2.io.proxyaccess;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;

/**
 * Proxy access over any {@link RealType} container.
 * 
 * @author Albert Cardona
 */
public class FloatAccessProxy< T extends RealType< T > > extends AbstractAccessProxy< T > implements FloatAccess, ArrayDataAccess< float[] >
{
	private static final long serialVersionUID = 1L;

	public FloatAccessProxy( final RandomAccessibleInterval< T > rai )
	{
		super( rai );
	}
	
	@Override
	synchronized public float getValue( final int index )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		return this.ra.get().getRealFloat();
	}

	@Override
	synchronized public void setValue( final int index, final float value )
	{
		IntervalIndexer.indexToPosition( index, this.dimensions, this.position );
		this.ra.setPosition( this.position );
		this.ra.get().setReal( value );
	}

	@Override
	public float[] createArray( final int numEntities )
	{
		return new float[ numEntities ];
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
