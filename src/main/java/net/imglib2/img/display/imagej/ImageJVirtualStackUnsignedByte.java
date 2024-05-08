/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2024 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.readwrite.SamplerConverter;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

import java.util.concurrent.ExecutorService;

/**
 * TODO
 *
 */
public class ImageJVirtualStackUnsignedByte extends ImageJVirtualStack< UnsignedByteType >
{
	public static < T extends RealType< ? > > ImageJVirtualStackUnsignedByte wrap( final RandomAccessibleInterval< T > source )
	{
		return new ImageJVirtualStackUnsignedByte( toUnsignedByteType( source ) );
	}

	private static < T extends RealType< ? > > RandomAccessibleInterval< UnsignedByteType > toUnsignedByteType( RandomAccessibleInterval< T > source )
	{
		if( Util.getTypeFromInterval( source ) instanceof UnsignedByteType )
			return ( RandomAccessibleInterval< UnsignedByteType > ) source;
		return Converters.convert(source, new ToUnsignedByteSamplerConverter( Util.getTypeFromInterval( source ) ) );
	}

	public static ImageJVirtualStackUnsignedByte wrapAndScaleBitType( final RandomAccessibleInterval< BitType > source )
	{
		return new ImageJVirtualStackUnsignedByte( Converters.convert(source, new ToBitByteSamplerConverter()) );
	}

	public < S > ImageJVirtualStackUnsignedByte( final RandomAccessibleInterval< S > source, final Converter< ? super S, UnsignedByteType > converter )
	{
		this( source, converter, null );
	}

	public < S > ImageJVirtualStackUnsignedByte( final RandomAccessibleInterval< S > source, final Converter< ? super S, UnsignedByteType > converter, final ExecutorService service )
	{
		super( source, converter, new UnsignedByteType(), 8, service );
		setMinAndMax( 0, 255 );
	}

	private ImageJVirtualStackUnsignedByte( final RandomAccessibleInterval< UnsignedByteType > source )
	{
		super( source, 8 );
		setMinAndMax( 0, 255 );
	}

	private static class ToUnsignedByteSamplerConverter implements SamplerConverter< RealType, UnsignedByteType >
	{

		private final double min;
		private final double max;

		ToUnsignedByteSamplerConverter( RealType<?> type ) {
			min = type.getMinValue();
			max = type.getMaxValue();
		}

		@Override
		public UnsignedByteType convert( Sampler< ? extends RealType > sampler )
		{
			return new UnsignedByteType( new ByteAccess()
			{
				@Override
				public byte getValue( int index )
				{
					double val = sampler.get().getRealDouble() + 0.5;
					if ( val < 0 )
						val = 0;
					else if ( val > 255 )
						val = 255;
					return (byte) val;
				}

				@Override
				public void setValue( int index, byte value )
				{
					double val = value;
					if ( val < min )
						val = min;
					else if ( val > max )
						val = max;
					sampler.get().setReal( val );
				}
			} );
		}
	}

	private static class ToBitByteSamplerConverter implements SamplerConverter< BooleanType<?>, UnsignedByteType >
	{

		@Override
		public UnsignedByteType convert( Sampler< ? extends BooleanType<?> > sampler )
		{
			return new UnsignedByteType( new ByteAccess()
			{
				@Override
				public byte getValue( int index )
				{
					return sampler.get().get() ? (byte) 255 : 0;
				}

				@Override
				public void setValue( int index, byte value )
				{
					sampler.get().set( value != 0 );
				}
			} );
		}
	}
}
