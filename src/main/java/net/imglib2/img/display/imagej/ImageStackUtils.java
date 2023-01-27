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

import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Utility functions for the implementation of {@link AbstractVirtualStack}.
 *
 * @author Matthias Arzt
 */
class ImageStackUtils
{
	private ImageStackUtils()
	{
		// prevent from instantiation
	}

	/**
	 * Creates a copy of a given {@link ImageStack}.
	 */
	public static ImageStack duplicate( ImageStack original )
	{
		return crop( original, 0, 0, 0, original.getWidth(), original.getHeight(), original.getSize() );
	}

	/**
	 * Creates a new {@link ImageStack} by cropping the given stack
	 */
	public static ImageStack crop( ImageStack stack, int x, int y, int z, int width, int height, int depth )
	{
		if ( x < 0 || y < 0 || z < 0 || x + width > stack.getWidth() || y + height > stack.getHeight() || z + depth > stack.getSize() )
			throw new IllegalArgumentException( "Argument out of range" );
		ImageStack result = new ImageStack( width, height, stack.getColorModel() );
		for ( int i = z; i < z + depth; i++ )
		{
			ImageProcessor ip = stack.getProcessor( i + 1 );
			ip.setRoi( x, y, width, height );
			result.addSlice( stack.getSliceLabel( i + 1 ), ip.crop() );
		}
		return result;
	}

	/**
	 * Create a new {@link ImageStack} with same content but featuring {@link ij.process.FloatProcessor}s.
	 */
	public static ImageStack convertToFloat( ImageStack stack )
	{
		ImageStack result = new ImageStack(stack.getWidth(), stack.getHeight(), stack.getColorModel());
		for (int i=1; i<= stack.getSize(); i++) {
			ImageProcessor ip = stack.getProcessor(i);
			result.addSlice(stack.getSliceLabel(i), ip.convertToFloat() );
		}
		return result;
	}
}
