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

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.readwrite.SamplerConverter;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.RandomAccessibleIntervalCursor;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO
 *
 */
public class ImageJVirtualStackInt extends ImageJVirtualStack< IntType >
{
	public static < T extends RealType< ? > > ImageJVirtualStackInt wrap(final RandomAccessibleInterval< T > source )
	{
		return new ImageJVirtualStackInt( toInt(source) );
	}

	private static < T extends RealType< ? > > RandomAccessibleInterval<IntType> toInt( RandomAccessibleInterval<T> source )
	{
		if( Util.getTypeFromInterval(source) instanceof IntType )
			return ( RandomAccessibleInterval< IntType > ) source;
		return Converters.convert( source, new ToIntSamplerConverter( Util.getTypeFromInterval( source )));
	}

	public < S > ImageJVirtualStackInt(final RandomAccessibleInterval< S > source,
																		 final Converter< ? super S, IntType > converter )
	{
		this( source, converter, null );
	}

	public < S > ImageJVirtualStackInt(final RandomAccessibleInterval< S > source,
																		 final Converter< ? super S, IntType > converter, final ExecutorService service )
	{
		super( source, converter, new IntType(), 32, service );
		setMinAndMax( 0, 1 );
	}

	private ImageJVirtualStackInt(final RandomAccessibleInterval< IntType > source )
	{
		super( source, 32 );
		setMinAndMax( 0, 1 );
	}

	public < S > void setMinMax( final RandomAccessibleInterval< S > source, final Converter< S, IntType > converter )
	{
		if ( service != null )
		{
			setMinMaxMT( source, converter );
			return;
		}

		final RandomAccessibleIntervalCursor< S > cursor = new RandomAccessibleIntervalCursor<>(
				Views.isZeroMin( source ) ? source : Views.zeroMin( source ) );
		final IntType t = new IntType();

		if ( cursor.hasNext() )
		{
			converter.convert( cursor.next(), t );

			int min = t.get();
			int max = min;

			while ( cursor.hasNext() )
			{
				converter.convert( cursor.next(), t );
				final int value = t.get();

				if ( value < min )
					min = value;

				if ( value > max )
					max = value;
			}

			setMinAndMax( min, max );
		}
	}

	private < S > void setMinMaxMT( final RandomAccessibleInterval< S > source, final Converter< S, IntType > converter )
	{
		final long nTasks = Runtime.getRuntime().availableProcessors();
		long size = 1;
		for ( int d = 0; d < source.numDimensions(); ++d )
			size *= source.dimension( d );

		final long portionSize = size / nTasks;

		final List< Callable< Void > > tasks = new ArrayList<>();
		final AtomicInteger ai = new AtomicInteger();

		final ArrayList< Integer > mins = new ArrayList<>( ( int ) nTasks );
		final ArrayList< Integer > maxs = new ArrayList<>( ( int ) nTasks );

		for ( int t = 0; t < nTasks; ++t )
		{
			tasks.add( new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					final int i = ai.getAndIncrement();
					final RandomAccessibleIntervalCursor< S > cursor = new RandomAccessibleIntervalCursor<>(
							Views.isZeroMin( source ) ? source : Views.zeroMin( source ) );
					final IntType t = new IntType();
					long stepsTaken = 0;

					cursor.jumpFwd( i * portionSize );

					int min = Integer.MAX_VALUE;
					int max = -Integer.MAX_VALUE;

					// either map a portion or (for the last portion) go
					// until the end
					while ( ( i != nTasks - 1 && stepsTaken < portionSize ) || ( i == nTasks - 1 && cursor.hasNext() ) )
					{
						stepsTaken++;
						converter.convert( cursor.next(), t );

						final int value = t.get();

						if ( value < min )
							min = value;

						if ( value > max )
							max = value;

					}

					mins.add( min );
					maxs.add( max );

					return null;
				}

			} );
		}

		try
		{
			final List< Future< Void > > futures = service.invokeAll( tasks );
			for ( final Future< Void > f : futures )
				f.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int min = Integer.MAX_VALUE;
		int max = -Integer.MAX_VALUE;

		for ( int t = 0; t < nTasks; t++ )
		{
			if ( min > mins.get( t ) )
				min = mins.get( t );
			if ( max < maxs.get( t ) )
				max = maxs.get( t );
		}

		setMinAndMax( min, max );

	}

	private static class ToIntSamplerConverter<S extends RealType<S>> implements SamplerConverter<S, IntType>
	{
		private final int min;
		private final int max;

		ToIntSamplerConverter(S type) {
			this.min = (int) type.getMinValue();
			this.max = (int) type.getMaxValue();
		}

		@Override
		public IntType convert( Sampler< ? extends S > sampler )
		{
			return new IntType( new IntAccess()
			{
				@Override
				public int getValue( int index )
				{
					double val = sampler.get().getRealDouble();
					if ( val < -Integer.MAX_VALUE )
						val = -Integer.MAX_VALUE;
					else if ( val > Integer.MAX_VALUE )
						val = Integer.MAX_VALUE;
					return ( int ) val;
				}

				@Override
				public void setValue( int index, int value )
				{
					int val = value;
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
