package net.imglib2.img.io.proxyaccess;

import java.util.Arrays;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;

abstract public class AbstractAccessProxy< T >
{
	protected final RandomAccess< T > ra;
	protected final long[] dimensions,
						   position;
	protected final int size;
	
	public AbstractAccessProxy( final RandomAccessibleInterval< T > rai )
	{
		this.ra = rai.randomAccess();
		this.dimensions = new long[ rai.numDimensions() ];
		rai.dimensions( this.dimensions );
		this.size = ( int )Arrays.stream( this.dimensions ).reduce( (a, b) -> a * b ).getAsLong();
		this.position = new long[ rai.numDimensions() ];
	}
}