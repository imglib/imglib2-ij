/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2023 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.img.io.proxyaccess;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.util.IntervalIndexer;

/**
 * Proxy access over any {@link GenericLongType} container.
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
