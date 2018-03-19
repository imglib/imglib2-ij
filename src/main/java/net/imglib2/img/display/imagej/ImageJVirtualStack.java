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
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.display.projector.AbstractProjector2D;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.display.projector.MultithreadedIterableIntervalProjector2D;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.view.Views;

/**
 * TODO
 * 
 */
public class ImageJVirtualStack<S, T extends NativeType< T >> extends AbstractVirtualStack
{
	final private long[] higherSourceDimensions;
	final private RandomAccessibleInterval< S > source;
	private final T type;
	private final Converter< S, T > converter;
	private boolean isWritable = false;
	final protected ExecutorService service;
	private double min = 0.0;
	private double max = 1.0;

	/* old constructor -> non-multithreaded projector */
	protected ImageJVirtualStack(final RandomAccessibleInterval< S > source, final Converter< S, T > converter,
			final T type, final int bitDepth)
	{
		this( source, converter, type, bitDepth, null );
	}

	protected ImageJVirtualStack(final RandomAccessibleInterval< S > source, final Converter< S, T > converter,
			final T type, final int bitDepth, ExecutorService service)
	{
		super( (int) source.dimension( 0 ), (int) source.dimension( 1 ), multiply( initHigherDimensions( source ) ), bitDepth );
		// if the source interval is not zero-min, we wrap it into a view that
		// translates it to the origin
		// if we were given an ExecutorService, use a multithreaded projector
		assert source.numDimensions() > 1;
		this.source = zeroMin( source );
		this.higherSourceDimensions = initHigherDimensions( source );
		this.type = type;
		this.converter = converter;
		this.service = service;
	}

	private static int multiply( long[] higherSourceDimensions )
	{
		return ( int ) LongStream.of( higherSourceDimensions ).reduce( 1, (a, b) -> a * b );
	}

	private static long[] initHigherDimensions( Interval source )
	{
		return IntStream.range(2, source.numDimensions())
				.mapToLong( source::dimension )
				.toArray();
	}

	protected void setMinAndMax( double min, double max ) {
		this.min = min;
		this.max = max;
	}

	private static < S > RandomAccessibleInterval< S > zeroMin( RandomAccessibleInterval< S > source )
	{
		return Views.isZeroMin( source ) ? source : Views.zeroMin( source );
	}

	private ImageProcessor initImageProcessor( ArrayImg< T, ? > img )
	{
		int sizeX = ( int ) img.dimension( 0 );
		int sizeY = ( int ) img.dimension( 1 );
		Object storageArray = ( ( ArrayDataAccess< ? > ) img.update( null ) ).getCurrentStorageArray();
		if( storageArray instanceof byte[] )
			return new ByteProcessor( sizeX, sizeY, (byte[]) storageArray, null );
		if( storageArray instanceof short[] )
			return new ShortProcessor( sizeX, sizeY, (short[]) storageArray, null );
		if( storageArray instanceof int[] )
			return new ColorProcessor( sizeX, sizeY, (int[]) storageArray );
		if( storageArray instanceof float[] )
			return new FloatProcessor( sizeX, sizeY, (float[]) storageArray, null );
		throw new IllegalArgumentException( "unsupported color type" );
	}

	/**
	 * Sets whether or not this virtual stack is writable. The classic ImageJ
	 * VirtualStack was read-only; but if this stack is backed by a CellCache it
	 * may now be writable.
	 */
	public void setWritable(final boolean writable)
	{
		isWritable = writable;
	}

	/**
	 * @return True if this VirtualStack will attempt to persist changes
	 */
	public boolean isWritable()
	{
		return isWritable;
	}

	/**
	 * Returns an ImageProcessor for the specified slice, where
	 * {@code 1<=n<=nslices}. Returns null if the stack is empty.
	 */
	@Override
	public ImageProcessor getProcessor(final int n)
	{

		ImageProcessor processor = initImageProcessor( getSlice( n ) );
		processor.setMinAndMax( min, max );
		return processor;
	}

	private ArrayImg< T, ? > getSlice( int n )
	{
		final int sizeX = (int) source.dimension( 0 );
		final int sizeY = (int) source.dimension( 1 );
		ArrayImg< T, ? > img = new ArrayImgFactory<>( type ).create( new long[] { sizeX, sizeY } );
		AbstractProjector2D projector = ( service == null )
				? new IterableIntervalProjector2D<>( 0, 1, source, img, converter )
				: new MultithreadedIterableIntervalProjector2D<>( 0, 1, source, img, converter, service );
		if ( higherSourceDimensions.length > 0 )
		{
			final int[] position = new int[ higherSourceDimensions.length ];
			IntervalIndexer.indexToPosition( n - 1, higherSourceDimensions, position );
			for ( int i = 0; i < position.length; i++ )
				projector.setPosition( position[i], i + 2 );
		}
		projector.map();
		return img;
	}

	@Override public Object getPixels( int n )
	{
		return getProcessor( n ).getPixels();
	}

	@Override
	public void setPixels(final Object pixels, final int n)
	{
		if ( isWritable() )
		{

			// Input and output need to be RealTypes
			if ( !( source.randomAccess().get() instanceof RealType ) || !( type instanceof RealType ) )
				return;

			RandomAccessibleInterval< S > origin = source;
			// Get the 2D plane represented by the virtual array
			if ( higherSourceDimensions.length > 0 )
			{
				final int[] position = new int[higherSourceDimensions.length];
				IntervalIndexer.indexToPosition( n - 1, higherSourceDimensions, position );
				for ( int i = 0; i < position.length; i++ )
					origin = Views.hyperSlice( origin, 2, position[ i ] );
			}

			final Cursor< S > originCursor = Views.iterable( origin ).cursor();
			final Cursor< ? extends RealType<?> > cursor = createArrayImg( (int) source.dimension( 0 ), (int) source.dimension( 1 ), pixels).cursor();

			// Replace the origin values with the current state of the virtual
			// array
			while ( originCursor.hasNext() )
			{
				( (RealType) originCursor.next() ).setReal( cursor.next().getRealDouble() );
			}
		}
	}

	private static Img< ? extends RealType<?> > createArrayImg( int sizeX, int sizeY, Object pixels ) {
		if( pixels instanceof byte[] )
			return ArrayImgs.unsignedBytes( (byte[]) pixels, sizeX, sizeY);
		if( pixels instanceof short[] )
			return ArrayImgs.unsignedShorts( (short[]) pixels, sizeX, sizeY);
		if( pixels instanceof float[] )
			return ArrayImgs.floats( (float[]) pixels, sizeX, sizeY);
		throw new IllegalArgumentException( "unsupported pixel type" );
	}
}
