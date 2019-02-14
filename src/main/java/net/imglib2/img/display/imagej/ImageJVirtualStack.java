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

import ij.ImageStack;
import ij.VirtualStack;
import ij.process.ImageProcessor;
import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.display.projector.AbstractProjector2D;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.display.projector.MultithreadedIterableIntervalProjector2D;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * {@link VirtualStack} that wraps around a {@link RandomAccessibleInterval} of
 * type {@link UnsignedByteType}, {@link UnsignedShortType}, {@link FloatType}
 * or {@link ARGBType}.
 * <p>
 * By default {@link ImageJVirtualStack} is not writable. A call to
 * {@link #setPixels} or {@link #setVoxels} has no effect. Buf if
 * {@link #setWritable(boolean)} is set to true, the pixels set by this methods
 * are copied to the {@link RandomAccessibleInterval}.
 * <p>
 * A call to {@link #getPixels(int)} will return a copy of pixels of the specified image
 * plane. Writing to the returned array has no effect.
 * The {@link ImageProcessor} returned by {@link #getProcessor(int)}
 * wraps around a copy of the pixels of the image plane too. So methods like
 * {@link ImageProcessor#set(int, int, int)} will not change the content of
 * wrapped {@link RandomAccessibleInterval}.
 */
public class ImageJVirtualStack< T extends NativeType< T > > extends AbstractVirtualStack
{

	final private long[] higherSourceDimensions;

	final private RandomAccessibleInterval< T > source;

	private final T type;

	private boolean isWritable = false;

	protected ExecutorService service;

	/* old constructor -> non-multithreaded projector */
	protected < S > ImageJVirtualStack( final RandomAccessibleInterval< S > source, final Converter< ? super S, T > converter,
			final T type, final int bitDepth )
	{
		this( Converters.convert( source, converter, type ), bitDepth );
	}

	protected < S > ImageJVirtualStack( final RandomAccessibleInterval< S > source, final Converter< ? super S, T > converter,
			final T type, final int bitDepth, final ExecutorService service )
	{
		this( source, converter, type, bitDepth );
		setExecutorService(service);
	}

	protected ImageJVirtualStack( final RandomAccessibleInterval< T > source, final int bitDepth )
	{
		super( ( int ) source.dimension( 0 ), ( int ) source.dimension( 1 ), multiply( initHigherDimensions( source ) ), bitDepth );
		// if the source interval is not zero-min, we wrap it into a view that
		// translates it to the origin
		// if we were given an ExecutorService, use a multithreaded projector
		assert source.numDimensions() > 1;
		this.source = zeroMin( source );
		this.type = Util.getTypeFromInterval( source );
		this.higherSourceDimensions = initHigherDimensions( source );
	}

	private static int multiply( final long[] higherSourceDimensions )
	{
		return ( int ) LongStream.of( higherSourceDimensions ).reduce( 1, ( a, b ) -> a * b );
	}

	private static long[] initHigherDimensions( final Interval source )
	{
		return IntStream.range( 2, source.numDimensions() )
				.mapToLong( source::dimension )
				.toArray();
	}

	private static < S > RandomAccessibleInterval< S > zeroMin( final RandomAccessibleInterval< S > source )
	{
		return Views.isZeroMin( source ) ? source : Views.zeroMin( source );
	}

	public void setExecutorService( ExecutorService service )
	{
		this.service = service;
	}

	/**
	 * Set if the {@link ImageStack} is writable.
	 * <p>
	 * No call to any method of this class will change the wrapped {@link RandomAccessibleInterval},
	 * if this {@link ImageStack} is not writable.
	 * <p>
	 * If writable, the pixels written to the image by the methods {@link #setPixels(Object, int)}
	 * or {@link #setVoxels} will be copied to the wrapped {@link RandomAccessibleInterval}.
	 * Please note: The {@link ImageProcessor} cannot be used to persistently change the image
	 * content.
	 */
	public void setWritable( final boolean writable )
	{
		isWritable = writable;
	}

	/** True if the image is writable. */
	@Override
	public boolean isWritable()
	{
		return isWritable;
	}

	private ArrayImg< T, ? > getSlice( final int index )
	{
		final int sizeX = ( int ) source.dimension( 0 );
		final int sizeY = ( int ) source.dimension( 1 );
		final ArrayImg< T, ? > img = new ArrayImgFactory<>( type ).create( new long[] { sizeX, sizeY } );
		project( index, img, (i, o) -> o.set( i ) );
		return img;
	}

	private void project( int index, Img< T > img, Converter< T, T > converter )
	{
		final AbstractProjector2D projector = ( service == null )
				? new IterableIntervalProjector2D<>( 0, 1, source, img, converter )
				: new MultithreadedIterableIntervalProjector2D<>( 0, 1, source, img, converter, service );
		setPosition( index, projector );
		projector.map();
	}

	private void setPosition( int index, Positionable projector )
	{
		if ( higherSourceDimensions.length > 0 )
		{
			final int[] position = new int[ higherSourceDimensions.length ];
			IntervalIndexer.indexToPosition( index, higherSourceDimensions, position );
			for ( int i = 0; i < position.length; i++ )
				projector.setPosition( position[ i ], i + 2 );
		}
	}

	@Override
	protected Object getPixelsZeroBasedIndex( final int index )
	{
		final ArrayImg< T, ? > img = getSlice( index );
		return ( ( ArrayDataAccess< ? > ) img.update( null ) ).getCurrentStorageArray();
	}

	@Override
	protected void setPixelsZeroBasedIndex( final int index, final Object pixels )
	{
		Img< T > img = ( Img< T > ) ImageProcessorUtils.createImg( pixels, getWidth(), getHeight() );
		// NB: The use of Converter and Projector2D is a bit surprising.
		// As the converter intentionally uses the first parameter a output.
		project( index, img, (o, i) -> o.set( i ) );
	}

	@Override
	protected RandomAccessibleInterval< T > getSliceZeroBasedIndex( int index )
	{
		RandomAccessibleInterval< T > origin = source;
		// Get the 2D plane represented by the virtual array
		if ( higherSourceDimensions.length > 0 )
		{
			final int[] position = new int[ higherSourceDimensions.length ];
			IntervalIndexer.indexToPosition( index, higherSourceDimensions, position );
			for ( int i = 0; i < position.length; i++ )
				origin = Views.hyperSlice( origin, 2, position[ i ] );
		}
		return origin;
	}
}
