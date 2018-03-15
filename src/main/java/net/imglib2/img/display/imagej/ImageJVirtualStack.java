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

import ij.ImagePlus;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.display.projector.MultithreadedIterableIntervalProjector2D;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.view.Views;

/**
 * TODO
 * 
 */
public abstract class ImageJVirtualStack<S, T extends NativeType< T >> extends VirtualStack
{
	final private IterableIntervalProjector2D< S, T > projector;

	final private int size;
	final private int numDimensions;
	final private long[] higherSourceDimensions;

	final private int bitDepth;

	final private RandomAccessibleInterval< S > source;
	final private ArrayImg< T, ? > img;

	final protected ImageProcessor imageProcessor;

	private boolean isWritable = false;

	final protected ExecutorService service;

	/* old constructor -> non-multithreaded projector */
	protected ImageJVirtualStack(final RandomAccessibleInterval< S > source, final Converter< S, T > converter,
			final T type, final int ijtype)
	{
		this( source, converter, type, ijtype, null );
	}

	protected ImageJVirtualStack(final RandomAccessibleInterval< S > source, final Converter< S, T > converter,
			final T type, final int ijtype, ExecutorService service)
	{
		super( (int) source.dimension( 0 ), (int) source.dimension( 1 ), null, null );

		this.source = source;
		assert source.numDimensions() > 1;

		int tmpsize = 1;
		for ( int d = 2; d < source.numDimensions(); ++d )
			tmpsize *= (int) source.dimension( d );
		this.size = tmpsize;

		final int sizeX = (int) source.dimension( 0 );
		final int sizeY = (int) source.dimension( 1 );

		img = new ArrayImgFactory<>( type ).create( new long[] { sizeX, sizeY } );

		higherSourceDimensions = new long[3];
		higherSourceDimensions[0] = ( source.numDimensions() > 2 ) ? source.dimension( 2 ) : 1;
		higherSourceDimensions[1] = ( source.numDimensions() > 3 ) ? source.dimension( 3 ) : 1;
		higherSourceDimensions[2] = ( source.numDimensions() > 4 ) ? source.dimension( 4 ) : 1;
		this.numDimensions = source.numDimensions();

		// if the source interval is not zero-min, we wrap it into a view that
		// translates it to the origin
		// if we were given an ExecutorService, use a multithreaded projector
		RandomAccessible< S > zeroMin = zeroMin( source );
		this.projector = ( service == null )
				? new IterableIntervalProjector2D<>( 0, 1, zeroMin, img, converter )
				: new MultithreadedIterableIntervalProjector2D<>( 0, 1, zeroMin, img, converter, service );

		this.service = service;

		this.bitDepth = initBitDepth( ijtype );
		this.imageProcessor = initImageProcessor( ijtype, sizeX, sizeY );
	}

	private RandomAccessible< S > zeroMin( RandomAccessibleInterval< S > source )
	{
		return Views.isZeroMin( source ) ? source : Views.zeroMin( source );
	}

	private ImageProcessor initImageProcessor( int ijtype, int sizeX, int sizeY )
	{
		Object storageArray = ( ( ArrayDataAccess< ? > ) img.update( null ) ).getCurrentStorageArray();
		switch ( ijtype ) {
		case ImagePlus.GRAY8:
			return new ByteProcessor( sizeX, sizeY, (byte[]) storageArray, null );
		case ImagePlus.GRAY16:
			return new ShortProcessor( sizeX, sizeY, (short[]) storageArray, null );
		case ImagePlus.COLOR_RGB:
			return new ColorProcessor( sizeX, sizeY, (int[]) storageArray );
		case ImagePlus.GRAY32:
			return new FloatProcessor( sizeX, sizeY, (float[]) storageArray, null );
		}
		throw new IllegalArgumentException( "unsupported color type " + ijtype );
	}

	private int initBitDepth( int ijtype )
	{
		switch ( ijtype ) {
		case ImagePlus.GRAY8:
			return 8;
		case ImagePlus.GRAY16:
			return 16;
		case ImagePlus.COLOR_RGB:
			return 24;
		case ImagePlus.GRAY32:
			return 32;
		}
		throw new IllegalArgumentException( "unsupported color type " + ijtype );
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
		if ( numDimensions > 2 )
		{
			final int[] position = new int[3];
			IntervalIndexer.indexToPosition( n - 1, higherSourceDimensions, position );
			projector.setPosition( position[0], 2 );
			if ( numDimensions > 3 )
				projector.setPosition( position[1], 3 );
			if ( numDimensions > 4 )
				projector.setPosition( position[2], 4 );
		}

		projector.map();
		return imageProcessor;
	}

	@Override
	public int getBitDepth()
	{
		return bitDepth;
	}

	/** Obsolete. Short images are always unsigned. */
	@Override
	public void addUnsignedShortSlice(final String sliceLabel, final Object pixels)
	{
	}

	/** Adds the image in 'ip' to the end of the stack. */
	@Override
	public void addSlice(final String sliceLabel, final ImageProcessor ip)
	{
	}

	/**
	 * Adds the image in 'ip' to the stack following slice 'n'. Adds the slice
	 * to the beginning of the stack if 'n' is zero.
	 */
	@Override
	public void addSlice(final String sliceLabel, final ImageProcessor ip, final int n)
	{
	}

	/** Deletes the specified slice, where {@code 1<=n<=nslices}. */
	@Override
	public void deleteSlice(final int n)
	{
	}

	/** Deletes the last slice in the stack. */
	@Override
	public void deleteLastSlice()
	{
	}

	/**
	 * Updates this stack so its attributes, such as min, max, calibration table
	 * and color model, are the same as 'ip'.
	 */
	@Override
	public void update(final ImageProcessor ip)
	{
	}

	/**
	 * Returns the pixel array for the specified slice, where
	 * {@code 1<=n<=nslices}.
	 */
	@Override
	public Object getPixels(final int n)
	{
		return getProcessor( n ).getPixels();
	}

	/**
	 * Assigns a pixel array to the specified slice, where
	 * {@code 1<=n<=nslices}.
	 */
	@Override
	public void setPixels(final Object pixels, final int n)
	{
		if ( isWritable() )
		{

			// Input and output need to be RealTypes
			if ( !( source.randomAccess().get() instanceof RealType ) || !( img.firstElement() instanceof RealType ) )
				return;

			RandomAccessibleInterval< S > origin = source;

			// Get the 2D plane represented by the virtual array
			if ( numDimensions > 2 )
			{
				final int[] position = new int[3];
				IntervalIndexer.indexToPosition( n - 1, higherSourceDimensions, position );
				origin = Views.hyperSlice( source, 2, position[0] );
				if ( numDimensions > 3 )
					origin = Views.hyperSlice( origin, 2, position[1] );
				if ( numDimensions > 4 )
					origin = Views.hyperSlice( origin, 2, position[2] );
			}

			final Cursor< S > originCursor = Views.iterable( origin ).cursor();
			final Cursor< T > cursor = img.cursor();

			// Replace the origin values with the current state of the virtual
			// array
			while ( originCursor.hasNext() )
			{
				( (RealType) originCursor.next() ).setReal( ( (RealType) cursor.next() ).getRealDouble() );
			}
		}
	}

	/**
	 * Returns the stack as an array of 1D pixel arrays. Note that the size of
	 * the returned array may be greater than the number of slices currently in
	 * the stack, with unused elements set to null.
	 */
	@Override
	public Object[] getImageArray()
	{
		return null;
	}

	/**
	 * Returns the slice labels as an array of Strings. Note that the size of
	 * the returned array may be greater than the number of slices currently in
	 * the stack. Returns null if the stack is empty or the label of the first
	 * slice is null.
	 */
	@Override
	public String[] getSliceLabels()
	{
		return null;
	}

	/**
	 * Returns the label of the specified slice, where {@code 1<=n<=nslices}.
	 * Returns null if the slice does not have a label. For DICOM and FITS
	 * stacks, labels may contain header information.
	 */
	@Override
	public String getSliceLabel(final int n)
	{
		return "" + n;
	}

	/**
	 * Returns a shortened version (up to the first 60 characters or first
	 * newline and suffix removed) of the label of the specified slice. Returns
	 * null if the slice does not have a label.
	 */
	@Override
	public String getShortSliceLabel(final int n)
	{
		return getSliceLabel( n );
	}

	/** Sets the label of the specified slice, where {@code 1<=n<=nslices}. */
	@Override
	public void setSliceLabel(final String label, final int n)
	{
	}

	/** Returns true if this is a 3-slice RGB stack. */
	@Override
	public boolean isRGB()
	{
		return false;
	}

	/** Returns true if this is a 3-slice HSB stack. */
	@Override
	public boolean isHSB()
	{
		return false;
	}

	/**
	 * Returns true if this is a virtual (disk resident) stack. This method is
	 * overridden by the VirtualStack subclass.
	 */
	@Override
	public boolean isVirtual()
	{
		return true;
	}

	/** Frees memory by deleting a few slices from the end of the stack. */
	@Override
	public void trim()
	{
	}

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
	public void setBitDepth(final int bitDepth)
	{
	}

	@Override
	public String getDirectory()
	{
		return null;
	}

	@Override
	public String getFileName(final int n)
	{
		return null;
	}
}
