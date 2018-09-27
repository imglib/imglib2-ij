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
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

/**
 * TODO
 *
 */
public class ImageJVirtualStackUnsignedByte< S > extends ImageJVirtualStack< S, UnsignedByteType >
{
	public static < T extends RealType< ? > > ImageJVirtualStackUnsignedByte< T > wrap( final RandomAccessibleInterval< T > source )
	{
		final Converter< ? super T, UnsignedByteType > converter =
				initConverter( Util.getTypeFromInterval( source ) );
		return new ImageJVirtualStackUnsignedByte<>( source, converter );
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

	static < T extends RealType< ? > > Converter< ? super T, UnsignedByteType > initConverter( T type )
	{
		if ( type instanceof UnsignedByteType )
			return (Converter) new ByteToByteConverter();
		if ( type instanceof IntegerType )
			return (Converter) new IntegerToByteConverter();
		return new RealToByteConverter();
	}

	private static class ByteToByteConverter implements
			Converter< UnsignedByteType, UnsignedByteType >
	{

		@Override
		public void convert( final UnsignedByteType input, final UnsignedByteType output )
		{
			output.set( input );
		}
	}

	private static class IntegerToByteConverter implements
			Converter< IntegerType, UnsignedByteType >
	{

		@Override
		public void convert( IntegerType input, UnsignedByteType output )
		{
			int value = input.getInteger();
			if ( value < 0 )
				value = 0;
			else if ( value > 255 )
				value = 255;
			output.set( value );
		}
	}

	private static class RealToByteConverter implements
			Converter< RealType< ? >, UnsignedByteType >
	{

		@Override
		public void convert( final RealType< ? > input, final UnsignedByteType output )
		{
			double value = input.getRealDouble();
			if ( value < 0 )
				value = 0;
			else if ( value > 255 )
				value = 255;
			output.setReal( value );
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
