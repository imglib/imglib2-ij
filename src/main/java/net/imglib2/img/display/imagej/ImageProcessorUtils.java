/*
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

package net.imglib2.img.display.imagej;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import java.awt.image.ColorModel;

public class ImageProcessorUtils
{
	private ImageProcessorUtils()
	{
		// prevent from instantiation
	}

	/**
	 * Creates an {@link ImageProcessor}.
	 *
	 * @param pixels     The parameter must be an array: byte[], short[], int[] or float[].
	 *                   The array length must equal width * height.
	 *                   The array will be used by the {@link ImageProcessor} to read and store the pixel values.
	 * @param width      Width of the returned {@link ImageProcessor}.
	 * @param height     Height of the returned {@link ImageProcessor}.
	 * @param colorModel {@link ColorModel} used by the returned {@link ImageProcessor}.
	 * @return Will return a {@link ByteProcessor}, {@link ShortProcessor}, {@link ColorProcessor} or {@link FloatProcessor}.
	 * The concrete type is determined by the type of the array given as pixels.
	 */
	public static ImageProcessor createImageProcessor( final Object pixels, final int width, final int height, final ColorModel colorModel )
	{
		if ( pixels instanceof byte[] )
			return new ByteProcessor( width, height, ( byte[] ) pixels, colorModel );
		if ( pixels instanceof short[] )
			return new ShortProcessor( width, height, ( short[] ) pixels, colorModel );
		if ( pixels instanceof int[] )
			return new ColorProcessor( width, height, ( int[] ) pixels );
		if ( pixels instanceof float[] )
			return new FloatProcessor( width, height, ( float[] ) pixels, colorModel );
		throw new IllegalArgumentException( "unsupported color type" );
	}

	/**
	 * Returns true if the given ImgLib2 pixel type is also supported by ImageJ1.
	 */
	public static boolean isSupported( final Type< ? > type )
	{
		return ( type instanceof UnsignedByteType ) || ( type instanceof UnsignedShortType ) ||
				( type instanceof ARGBType ) || ( type instanceof FloatType );
	}

	/**
	 * Creates a two dimensional {@link Img} that wraps around the array given as pixels.
	 * Only supports UnsignedByteType, UnsignedShortType, FloatType and ARGBType.
	 * This method is like {@link #createImageProcessor(Object, int, int, ColorModel)} but
	 * creates an imglib2 {@link Img} instead of an {@link ImageProcessor}.
	 *
	 * @param pixels The parameter must be an array: byte[], short[], int[] or float[].
	 *               The array length must equal width * height.
	 *               The array will be used by the {@link Img} to read and store the pixel values.
	 * @param width  Width of the returned {@link Img}.
	 * @param height Height of the returned {@link Img}.
	 */
	public static Img< ? > createImg( final Object pixels, final int width, final int height )
	{
		if ( pixels instanceof int[] )
			return ArrayImgs.argbs( ( int[] ) pixels, width, height );
		if ( pixels instanceof byte[] )
			return ArrayImgs.unsignedBytes( ( byte[] ) pixels, width, height );
		if ( pixels instanceof short[] )
			return ArrayImgs.unsignedShorts( ( short[] ) pixels, width, height );
		if ( pixels instanceof float[] )
			return ArrayImgs.floats( ( float[] ) pixels, width, height );
		throw new IllegalArgumentException( "unsupported pixel type" );
	}
}
