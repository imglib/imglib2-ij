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

import java.awt.*;
import java.awt.image.ColorModel;
import java.util.stream.IntStream;

public abstract class AbstractVirtualStack extends VirtualStack
{
	private final int width;

	private final int height;

	private final int size;

	private final int bitDepth;

	private ColorModel colorModel;

	private Rectangle roi;
	private double min = 0.0, max = 1.0;

	public AbstractVirtualStack( int width, int height, int size, int bitDepth )
	{
		super( 10, 10, null, "" );
		this.width = width;
		this.height = height;
		this.size = size;
		this.bitDepth = bitDepth;
		this.colorModel = null;
		this.roi = new Rectangle( 0, 0, width, height );
	}

	protected void setMinAndMax( double min, double max ) {
		this.min = min;
		this.max = max;
	}

	@Override public abstract Object getPixels( int n );

	@Override
	public ImageProcessor getProcessor(final int n)
	{

		Object pixels = getPixels( n );
		ImageProcessor processor = ImageProcessorUtils.initProcessor( width, height, pixels, colorModel );
		processor.setMinAndMax( min, max );
		return processor;
	}

	@Override public void addSlice( String name )
	{
		// ignore
	}

	@Override public void addSlice( String sliceLabel, Object pixels )
	{
		// ignore
	}

	@Override public void addSlice( String sliceLabel, ImageProcessor ip )
	{
		// ignore
	}

	@Override public void addSlice( String sliceLabel, ImageProcessor ip, int n )
	{
		// ignore
	}

	@Override public void deleteSlice( int n )
	{
		// ignore
	}

	@Override public void deleteLastSlice()
	{
		// ignore
	}

	@Override public void setPixels( Object pixels, int n )
	{
		// ignore for now
	}

	/**
	 * Currently not implemented
	 */
	@Override public int saveChanges( int n )
	{
		return -1;
	}

	@Override public int getSize()
	{
		return size;
	}

	@Override public String getSliceLabel( int n )
	{
		return Integer.toString( n );
	}

	@Override public Object[] getImageArray()
	{
		return null; // this is the same as VirtualStack
	}

	@Override public void setSliceLabel( String label, int n )
	{
		// slice labels are not supported -> do nothing
	}

	@Override public boolean isVirtual()
	{
		return true;
	}

	@Override public void trim()
	{
		// ignore
	}

	@Override public String getDirectory()
	{
		return ""; // there is no directory that can be returned
	}

	@Override public String getFileName( int n )
	{
		return ""; // there is no filename, but I don't want to return null
	}

	@Override public void setBitDepth( int bitDepth )
	{
		// ignore
	}

	@Override public int getBitDepth()
	{
		return bitDepth;
	}

	@Override public ImageStack sortDicom( String[] strings, String[] info, int maxDigits )
	{
		// ignore
		return this;
	}

	@Override public void addUnsignedShortSlice( String sliceLabel, Object pixels )
	{
		super.addUnsignedShortSlice( sliceLabel, pixels );
	}

	@Override public void addSlice( ImageProcessor ip )
	{
		// ignore
	}

	@Override public int getWidth()
	{
		return width;
	}

	@Override public int getHeight()
	{
		return height;
	}

	@Override public void setRoi( Rectangle roi )
	{
		this.roi = roi;
	}

	@Override public Rectangle getRoi()
	{
		return roi;
	}

	@Override public void update( ImageProcessor ip )
	{
		colorModel = ip.getColorModel();
	}

	@Override public int size()
	{
		return size;
	}

	@Override public String[] getSliceLabels()
	{
		return IntStream.range( 0, size ).mapToObj( this::getSliceLabel ).toArray( String[]::new );
	}

	@Override public String getShortSliceLabel( int n )
	{
		return getSliceLabel( n );
	}

	@Override public void setProcessor( ImageProcessor ip, int n )
	{
		// ignore
	}

	@Override public void setColorModel( ColorModel cm )
	{
		this.colorModel = cm;
	}

	@Override public ColorModel getColorModel()
	{
		return colorModel;
	}

	@Override public boolean isRGB()
	{
		return false;
	}

	@Override public boolean isHSB()
	{
		return false;
	}

	@Override public boolean isLab()
	{
		return false;
	}

	@Override public String toString()
	{
		return super.toString();
	}

	@Override public float[] getVoxels( int x0, int y0, int z0, int w, int h, int d, float[] voxels )
	{
		throw new UnsupportedOperationException();
	}

	@Override public float[] getVoxels( int x0, int y0, int z0, int w, int h, int d, float[] voxels, int channel )
	{
		throw new UnsupportedOperationException();
	}

	@Override public void setVoxels( int x0, int y0, int z0, int w, int h, int d, float[] voxels )
	{
		throw new UnsupportedOperationException();
	}

	@Override public void setVoxels( int x0, int y0, int z0, int w, int h, int d, float[] voxels, int channel )
	{
		throw new UnsupportedOperationException();
	}

	@Override public void drawSphere( double radius, int xc, int yc, int zc )
	{
		super.drawSphere( radius, xc, yc, zc );
	}

	@Override public ImageStack duplicate()
	{
		throw new UnsupportedOperationException();
	}

	@Override public ImageStack crop( int x, int y, int z, int width, int height, int depth )
	{
		throw new UnsupportedOperationException();
	}

	@Override public ImageStack convertToFloat()
	{
		throw new UnsupportedOperationException();
	}
}
