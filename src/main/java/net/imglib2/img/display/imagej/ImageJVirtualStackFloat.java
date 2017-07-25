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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.RandomAccessibleIntervalCursor;
import net.imglib2.view.Views;

/**
 * TODO
 *
 */
public class ImageJVirtualStackFloat<S> extends ImageJVirtualStack< S, FloatType >
{
	public ImageJVirtualStackFloat(final RandomAccessibleInterval< S > source,
			final Converter< S, FloatType > converter)
	{
		this( source, converter, null );
	}

	public ImageJVirtualStackFloat(final RandomAccessibleInterval< S > source,
			final Converter< S, FloatType > converter, ExecutorService service)
	{
		super( source, converter, new FloatType(), ImagePlus.GRAY32, service );
		imageProcessor.setMinAndMax( 0, 1 );
	}

	public void setMinMax(final RandomAccessibleInterval< S > source, final Converter< S, FloatType > converter)
	{
		if ( service != null )
		{
			setMinMaxMT( source, converter );
			return;
		}

		final RandomAccessibleIntervalCursor< S > cursor = new RandomAccessibleIntervalCursor<>(
				Views.isZeroMin( source ) ? source : Views.zeroMin( source ) );
		final FloatType t = new FloatType();

		if ( cursor.hasNext() )
		{
			converter.convert( cursor.next(), t );

			float min = t.get();
			float max = min;

			while ( cursor.hasNext() )
			{
				converter.convert( cursor.next(), t );
				final float value = t.get();

				if ( value < min )
					min = value;

				if ( value > max )
					max = value;
			}

			System.out.println( "fmax = " + max );
			System.out.println( "fmin = " + min );
			imageProcessor.setMinAndMax( min, max );
		}
	}

	private void setMinMaxMT(final RandomAccessibleInterval< S > source, final Converter< S, FloatType > converter)
	{
		final long nTasks = Runtime.getRuntime().availableProcessors();
		long size = 1;
		for ( int d = 0; d < source.numDimensions(); ++d )
			size *= source.dimension( d );

		final long portionSize = size / nTasks;

		final List< Callable< Void > > tasks = new ArrayList<>();
		final AtomicInteger ai = new AtomicInteger();

		final ArrayList< Float > mins = new ArrayList<>( (int) nTasks );
		final ArrayList< Float > maxs = new ArrayList<>( (int) nTasks );

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
					final FloatType t = new FloatType();
					long stepsTaken = 0;

					cursor.jumpFwd( i * portionSize );

					float min = Float.MAX_VALUE;
					float max = -Float.MAX_VALUE;

					// either map a portion or (for the last portion) go
					// until the end
					while ( ( i != nTasks - 1 && stepsTaken < portionSize ) || ( i == nTasks - 1 && cursor.hasNext() ) )
					{
						stepsTaken++;
						converter.convert( cursor.next(), t );

						final float value = t.get();

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
			List< Future< Void > > futures = service.invokeAll( tasks );
			for ( Future< Void > f : futures )
				f.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		for ( int t = 0; t < nTasks; t++ )
		{
			if ( min > mins.get( t ) )
				min = mins.get( t );
			if ( max < maxs.get( t ) )
				max = maxs.get( t );
		}

		System.out.println( "fmax = " + max );
		System.out.println( "fmin = " + min );
		imageProcessor.setMinAndMax( min, max );

	}
}
