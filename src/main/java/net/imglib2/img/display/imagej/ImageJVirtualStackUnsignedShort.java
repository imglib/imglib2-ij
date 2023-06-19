/*
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

package net.imglib2.img.display.imagej;

import java.util.concurrent.ExecutorService;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.readwrite.SamplerConverter;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

/**
 * TODO
 *
 */
public class ImageJVirtualStackUnsignedShort extends ImageJVirtualStack< UnsignedShortType >
{
	public static < T extends RealType< ? > > ImageJVirtualStackUnsignedShort wrap( final RandomAccessibleInterval< T > source )
	{
		final ImageJVirtualStackUnsignedShort result = new ImageJVirtualStackUnsignedShort( toUnsignedShort( source ) );
		result.initMinMax( Util.getTypeFromInterval( source ) );
		return result;
	}

	private static < T extends RealType< ? > > RandomAccessibleInterval< UnsignedShortType > toUnsignedShort( RandomAccessibleInterval< T > source )
	{
		if( Util.getTypeFromInterval( source ) instanceof UnsignedShortType )
			return ( RandomAccessibleInterval< UnsignedShortType > ) source;
		return Converters.convert( source, new ShortConverter( Util.getTypeFromInterval( source ) ) );
	}

	public < S > ImageJVirtualStackUnsignedShort( final RandomAccessibleInterval< S > source, final Converter< ? super S, UnsignedShortType > converter )
	{
		this( source, converter, null );
	}

	public < S > ImageJVirtualStackUnsignedShort( final RandomAccessibleInterval< S > source, final Converter< ? super S, UnsignedShortType > converter, final ExecutorService service )
	{
		super( source, converter, new UnsignedShortType(), 16, service );
		initMinMax( Util.getTypeFromInterval( source ) );
	}

	private ImageJVirtualStackUnsignedShort( final RandomAccessibleInterval< UnsignedShortType > source )
	{
		super( source, 16 );
	}

	private void initMinMax( Object s )
	{
		int maxDisplay = ( 1 << 16 ) - 1;

		if ( BitType.class.isInstance( s ) )
			maxDisplay = 1;
		else if ( Unsigned12BitType.class.isInstance( s ) )
			maxDisplay = 4095;

		setMinAndMax( 0, maxDisplay );
	}

	private static class ShortConverter implements SamplerConverter< RealType< ? >, UnsignedShortType >
	{

		private final double min;

		private final double max;

		ShortConverter(RealType<?> type) {
			this.min = type.getMinValue();
			this.max = type.getMaxValue();
		}

		@Override
		public UnsignedShortType convert( Sampler< ? extends RealType< ? > > sampler )
		{
			return new UnsignedShortType( new ShortAccess()
			{
				@Override
				public short getValue( int index )
				{
					double val = sampler.get().getRealDouble() + 0.5;
					if ( val < 0 )
						val = 0;
					else if ( val > 65535 )
						val = 65535;
					return (short) ((int) val);
				}

				@Override
				public void setValue( int index, short value )
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
}
