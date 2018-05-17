/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

package net.imglib2.img.display.imagej;

import java.util.concurrent.ExecutorService;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 * TODO
 *
 */
public class ImageJVirtualStackUnsignedByte< S > extends ImageJVirtualStack< S, UnsignedByteType >
{
	public static < T extends RealType< ? > > ImageJVirtualStackUnsignedByte< T > wrap( final RandomAccessibleInterval< T > source )
	{
		return new ImageJVirtualStackUnsignedByte<>( source, new ByteConverter() );
	}

	public static ImageJVirtualStackUnsignedByte< BitType > wrapAndScaleBitType( final RandomAccessibleInterval< BitType > source )
	{
		return new ImageJVirtualStackUnsignedByte<>( source, new BitToByteConverter() );
	}

	public ImageJVirtualStackUnsignedByte( final RandomAccessibleInterval< S > source, final Converter< ? super S, UnsignedByteType > converter )
	{
		this( source, converter, null );
	}

	public ImageJVirtualStackUnsignedByte( final RandomAccessibleInterval< S > source, final Converter< ? super S, UnsignedByteType > converter, final ExecutorService service )
	{
		super( source, converter, new UnsignedByteType(), 8, service );
		setMinAndMax( 0, 255 );
	}

	private static class ByteConverter implements
			Converter< RealType< ? >, UnsignedByteType >
	{

		@Override
		public void convert( final RealType< ? > input, final UnsignedByteType output )
		{
			double val = input.getRealDouble();
			if ( val < 0 )
				val = 0;
			else if ( val > 255 )
				val = 255;
			output.setReal( val );
		}
	}

	private static class BitToByteConverter implements
			Converter< BitType, UnsignedByteType >
	{

		@Override
		public void convert( final BitType input, final UnsignedByteType output )
		{
			output.set( input.get() ? 255 : 0 );
		}
	}
}
