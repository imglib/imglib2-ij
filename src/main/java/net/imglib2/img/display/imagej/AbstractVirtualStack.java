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

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import ij.ImageStack;
import ij.VirtualStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Abstract class to simplify the implementation of an {@link VirtualStack}.
 *
 * @author Matthias Arzt
 */
public abstract class AbstractVirtualStack extends VirtualStack
{
	private final int width;

	private final int height;

	private int size;

	private int offset;

	private final int bitDepth;

	private ColorModel colorModel;

	private Rectangle roi;

	private double min = 0.0, max = 1.0;

	public AbstractVirtualStack( final int width, final int height, final int size, final int bitDepth )
	{
		super( 10, 10, null, "" );
		this.width = width;
		this.height = height;
		this.offset = 0;
		this.size = size;
		this.bitDepth = bitDepth;
		this.colorModel = null;
		this.roi = new Rectangle( 0, 0, width, height );
	}

	protected void setMinAndMax( final double min, final double max )
	{
		this.min = min;
		this.max = max;
	}

	@Override
	public final Object getPixels( int n )
	{
		return getPixelsZeroBasedIndex( toZeroBasedIndex( n ) );
	}

	@Override
	public final void setPixels( final Object pixels, final int n )
	{
		if( isWritable() )
			setPixelsZeroBasedIndex( toZeroBasedIndex( n ), pixels );
	}

	private int toZeroBasedIndex( int n )
	{
		return ( n - 1 ) + offset;
	}

	protected boolean isWritable() {
		return true;
	}

	protected abstract Object getPixelsZeroBasedIndex( int index );

	protected abstract void setPixelsZeroBasedIndex( int index, Object pixels );

	protected RandomAccessibleInterval< ? > getSliceZeroBasedIndex( int index )
	{
		Object pixels = getPixelsZeroBasedIndex( index );
		return ImageProcessorUtils.initArrayImg( getWidth(), getHeight(), pixels );
	}

	@Override
	public ImageProcessor getProcessor( final int n )
	{

		final Object pixels = getPixels( n );
		final ImageProcessor processor = ImageProcessorUtils.initProcessor( width, height, pixels, colorModel );
		if ( min != Double.MAX_VALUE && !( processor instanceof ColorProcessor ) )
			processor.setMinAndMax( min, max );
		return processor;
	}

	@Override
	public void addSlice( final String name )
	{
		// ignore
	}

	@Override
	public void addSlice( final String sliceLabel, final Object pixels )
	{
		// ignore
	}

	@Override
	public void addSlice( final String sliceLabel, final ImageProcessor ip )
	{
		// ignore
	}

	@Override
	public void addSlice( final String sliceLabel, final ImageProcessor ip, final int n )
	{
		// ignore
	}

	@Override
	public void deleteSlice( final int n )
	{
		if ( n == 1 )
			deleteFirstSlice();
		else if ( n == size )
			deleteLastSlice();
		else
			throw new UnsupportedOperationException( "AbstractVirtualStack only supports to delete first or last slice." );
	}

	private void deleteFirstSlice()
	{
		size--;
		offset++;
	}

	@Override
	public void deleteLastSlice()
	{
		size--;
	}

	/**
	 * Currently not implemented
	 */
	@Override
	public int saveChanges( final int n )
	{
		return -1;
	}

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
	public String getSliceLabel( final int n )
	{
		return Integer.toString( toZeroBasedIndex( n ) + 1 );
	}

	@Override
	public Object[] getImageArray()
	{
		return null; // this is the same as VirtualStack
	}

	@Override
	public void setSliceLabel( final String label, final int n )
	{
		// slice labels are not supported -> do nothing
	}

	@Override
	public boolean isVirtual()
	{
		return true;
	}

	@Override
	public void trim()
	{
		// ignore
	}

	@Override
	public String getDirectory()
	{
		return ""; // there is no directory that can be returned
	}

	@Override
	public String getFileName( final int n )
	{
		return ""; // there is no filename, but I don't want to return null
	}

	@Override
	public void setBitDepth( final int bitDepth )
	{
		// ignore
	}

	@Override
	public int getBitDepth()
	{
		return bitDepth;
	}

	@Override
	public ImageStack sortDicom( final String[] strings, final String[] info, final int maxDigits )
	{
		// ignore
		return this;
	}

	@Override
	public void addUnsignedShortSlice( final String sliceLabel, final Object pixels )
	{
		super.addUnsignedShortSlice( sliceLabel, pixels );
	}

	@Override
	public void addSlice( final ImageProcessor ip )
	{
		// ignore
	}

	@Override
	public int getWidth()
	{
		return width;
	}

	@Override
	public int getHeight()
	{
		return height;
	}

	@Override
	public void setRoi( final Rectangle roi )
	{
		this.roi = roi;
	}

	@Override
	public Rectangle getRoi()
	{
		return roi;
	}

	@Override
	public void update( final ImageProcessor ip )
	{
		colorModel = ip.getColorModel();
	}

	@Override
	public int size()
	{
		return size;
	}

	@Override
	public String[] getSliceLabels()
	{
		return IntStream.range( 0, size ).mapToObj( this::getSliceLabel ).toArray( String[]::new );
	}

	@Override
	public String getShortSliceLabel( final int n )
	{
		return getSliceLabel( n );
	}

	@Override
	public void setProcessor( final ImageProcessor ip, final int n )
	{
		// ignore
	}

	@Override
	public void setColorModel( final ColorModel cm )
	{
		this.colorModel = cm;
	}

	@Override
	public ColorModel getColorModel()
	{
		return colorModel;
	}

	@Override
	public boolean isRGB()
	{
		return false;
	}

	@Override
	public boolean isHSB()
	{
		return false;
	}

	@Override
	public boolean isLab()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return super.toString();
	}

	@Override
	public float[] getVoxels( final int x0, final int y0, final int z0, final int w, final int h, final int d, float[] voxels )
	{
		return accessVoxels( x0, y0, z0, w, h, d, voxels, null, false );
	}

	@Override
	public float[] getVoxels( final int x0, final int y0, final int z0, final int w, final int h, final int d, float[] voxels, final int channel )
	{
		return accessVoxels( x0, y0, z0, w, h, d, voxels, channel, false );
	}

	@Override
	public void setVoxels( final int x0, final int y0, final int z0, final int w, final int h, final int d, final float[] voxels )
	{
		if ( isWritable() )
			accessVoxels( x0, y0, z0, w, h, d, voxels, null, true );
	}

	@Override
	public void setVoxels( final int x0, final int y0, final int z0, final int w, final int h, final int d, final float[] voxels, final int channel )
	{
		if ( isWritable() )
			accessVoxels( x0, y0, z0, w, h, d, voxels, channel, true );
	}

	private float[] accessVoxels( int x0, int y0, int z0, int w, int h, int d, float[] voxels, Integer optionalChannel, boolean setVoxel )
	{
		checkBounds( x0, y0, z0, w, h, d );
		if( ! setVoxel )
			voxels = checkResultArray( w, h, d, voxels );
		BiConsumer< Object, FloatType > action = ( BiConsumer< Object, FloatType > ) voxelAccessAction( optionalChannel, setVoxel );
		loopOverVoxels( x0, y0, z0, w, h, d, voxels, action );
		return voxels;
	}

	private void checkBounds( int x0, int y0, int z0, int w, int h, int d )
	{
		boolean inBounds = (x0 >= 0) && (x0 + w <= width) && (y0 >= 0) && (y0 + h <= height) && (z0 >= 0) && (z0 + d <= size);
		if(! inBounds)
			throw new IndexOutOfBoundsException();
	}

	private float[] checkResultArray( int w, int h, int d, float[] voxels )
	{
		if ( voxels == null || voxels.length != w * h * d )
			voxels = new float[ w * h * d ];
		return voxels;
	}

	private void loopOverVoxels( int x0, int y0, int z0, int w, int h, int d, float[] voxels, BiConsumer< Object, FloatType > action )
	{
		FinalInterval interval = Intervals.createMinSize( x0, y0, w, h );
		Cursor< FloatType > output = ArrayImgs.floats( voxels, w, h, d ).cursor();
		for ( int z = z0 + offset; z < z0 + offset + d; z++ )
		{
			Cursor< ? > cursor = Views.flatIterable( Views.interval( getSliceZeroBasedIndex( z ), interval ) ).cursor();
			while ( cursor.hasNext() ) {
				action.accept( cursor.next(), output.next() );
			}
		}
	}


	private BiConsumer<?, FloatType> voxelAccessAction( Integer channel, boolean isSetVoxels )
	{
		if ( bitDepth == 24 )
			return arbgVoxelAccessAction( channel, isSetVoxels );
		return isSetVoxels ?
				(BiConsumer< RealType, FloatType >) ( a, b ) -> a.setReal( b.getRealFloat() ) :
				(BiConsumer< RealType, FloatType >) ( a, b ) -> b.setReal( a.getRealFloat() );
	}

	private static BiConsumer<ARGBType, FloatType> arbgVoxelAccessAction( Integer channel, boolean isSetVoxel )
	{
		if( channel == null )
			return isSetVoxel ?
					( a, b ) -> a.set( (int) b.getRealFloat() ) :
					( a, b ) -> b.set( a.get() );
		int shift = 8 * (2 - channel);
		int mask = ~ (0xff << shift);
		return isSetVoxel ?
				( a, b ) -> a.set( a.get() & mask | ((((int) b.get()) & 0xff) << shift )) :
				( a, b ) -> b.set( ( a.get() >> shift) & 0xff );
	}

	@Override
	public void drawSphere( final double radius, final int xc, final int yc, final int zc )
	{
		super.drawSphere( radius, xc, yc, zc );
	}

	@Override
	public ImageStack duplicate()
	{
		return ImageStackUtils.duplicate( this );
	}

	@Override
	public ImageStack crop( final int x, final int y, final int z, final int width, final int height, final int depth )
	{
		return ImageStackUtils.crop( this, x, y, z, width, height, depth );
	}

	@Override
	public ImageStack convertToFloat()
	{
		return ImageStackUtils.convertToFloat( this );
	}
}
