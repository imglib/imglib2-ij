/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2022 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.display.projector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.FlatIterationOrder;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.view.RandomAccessibleIntervalCursor;
import net.imglib2.view.Views;

/**
 * Multithreaded version of {@link IterableIntervalProjector2D} by 
 * 
 * @author Michael Zinsmaier
 * @author Martin Horn
 * @author Christian Dietz
 * 
 * The output
 * {@link IterableInterval} will be divided into approximately equally sized
 * portions that are filled by separate threads.
 *
 * @author David Hoerl
 *
 * @param <A>
 *            pixel type of the input
 * @param <B>
 *            pixel type of the output
 */
public class MultithreadedIterableIntervalProjector2D<A, B> extends IterableIntervalProjector2D< A, B >
{

	final ExecutorService service;

	private final int dimX;

	private final int dimY;

	private final int nTasks;

	public MultithreadedIterableIntervalProjector2D(int dimX, int dimY, RandomAccessible< A > source,
			IterableInterval< B > target, Converter< ? super A, B > converter, ExecutorService service, int nTasks)
	{
		super( dimX, dimY, source, target, converter );

		this.service = service;
		this.dimX = dimX;
		this.dimY = dimY;
		this.nTasks = nTasks;
	}

	public MultithreadedIterableIntervalProjector2D(int dimX, int dimY, RandomAccessible< A > source,
			IterableInterval< B > target, Converter< ? super A, B > converter, ExecutorService service)
	{
		this( dimX, dimY, source, target, converter, service, Runtime.getRuntime().availableProcessors() );
	}

	@Override
	public void map()
	{
		// fix interval for all dimensions
		for ( int d = 0; d < position.length; ++d )
			min[d] = max[d] = position[d];

		min[dimX] = target.min( 0 );
		min[dimY] = target.min( 1 );
		max[dimX] = target.max( 0 );
		max[dimY] = target.max( 1 );

		final IterableInterval< A > ii = Views.iterable( Views.interval( source, new FinalInterval( min, max ) ) );

		final long portionSize = target.size() / nTasks;

		final List< Callable< Void > > tasks = new ArrayList<>();
		final AtomicInteger ai = new AtomicInteger();

		for ( int t = 0; t < nTasks; ++t )
		{
			tasks.add( new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					int i = ai.getAndIncrement();

					final Cursor< B > targetCursor = target.localizingCursor();

					// we might need either a cursor or a RandomAccess
					final RandomAccess< A > sourceRandomAccess = source.randomAccess();
					sourceRandomAccess.setPosition( position );

					final Cursor< A > sourceCursor = ii.cursor();

					// jump to correct starting point
					targetCursor.jumpFwd( i * portionSize );
					sourceCursor.jumpFwd( i * portionSize );
					long stepsTaken = 0;

					if ( target.iterationOrder().equals( ii.iterationOrder() )
							&& !( sourceCursor instanceof RandomAccessibleIntervalCursor ) )
					{
						// either map a portion or (for the last portion) go
						// until the end
						while ( ( i != nTasks - 1 && stepsTaken < portionSize )
								|| ( i == nTasks - 1 && targetCursor.hasNext() ) )
						{
							stepsTaken++;
							converter.convert( sourceCursor.next(), targetCursor.next() );
						}
					}

					else if ( target.iterationOrder() instanceof FlatIterationOrder )
					{

						final long cr = -target.dimension( 0 );
						final long width = target.dimension( 0 );
						final long height = target.dimension( 1 );

						final long initX = ( i * portionSize ) % width;
						final long initY = ( i * portionSize ) / width;
						// either map a portion or (for the last portion) go
						// until the end
						final long endX = ( i == nTasks - 1 ) ? width : ( initX + ( i + 1 ) * portionSize ) % width;
						final long endY = ( i == nTasks - 1 ) ? height - 1
								: ( initX + ( i + 1 ) * portionSize ) / width;

						sourceRandomAccess.setPosition( initX, dimX );
						sourceRandomAccess.setPosition( initY, dimY );

						for ( long y = initY; y <= endY; ++y )
						{
							for ( long x = ( y == initY ? initX : 0 ); x < ( y == endY ? endX : width ); ++x )
							{
								targetCursor.fwd();
								converter.convert( sourceRandomAccess.get(), targetCursor.get() );
								sourceRandomAccess.fwd( dimX );

							}
							sourceRandomAccess.move( cr, dimX );
							sourceRandomAccess.fwd( dimY );
						}
					}

					else
					{
						// either map a portion or (for the last portion) go
						// until the end
						while ( ( i != nTasks - 1 && stepsTaken < portionSize )
								|| ( i == nTasks - 1 && targetCursor.hasNext() ) )
						{
							stepsTaken++;

							final B b = targetCursor.next();
							sourceRandomAccess.setPosition( targetCursor.getLongPosition( 0 ), dimX );
							sourceRandomAccess.setPosition( targetCursor.getLongPosition( 1 ), dimY );

							converter.convert( sourceRandomAccess.get(), b );
						}
					}

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
			e.printStackTrace();
		}

	}

}
