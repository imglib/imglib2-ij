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

package net.imglib2.img;

import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.test.AssertImgs;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import ij.ImagePlus;

/**
 * Tests {@link VirtualStackAdapter}.
 *
 * @author Matthias Arzt
 */
public class VirtualStackAdapterTest
{

	private static final long[] DIMENSIONS = { 2, 3, 4, 5, 6 };

	// TODO fix test
	@Test
	public void testUnsignedByte()
	{
		final ImagePlus image = randomImagePlus( new UnsignedByteType(), DIMENSIONS );
		final Img< UnsignedByteType > actual = VirtualStackAdapter.wrapByte( image );
		final Img< UnsignedByteType > expected = ImagePlusAdapter.wrapByte( image );
		AssertImgs.assertImageEquals( expected, actual );
	}

	@Test
	public void testUnsignedShort()
	{
		final ImagePlus image = randomImagePlus( new UnsignedShortType(), DIMENSIONS );
		final Img< UnsignedShortType > actual = VirtualStackAdapter.wrapShort( image );
		final Img< UnsignedShortType > expected = ImagePlusAdapter.wrapShort( image );
		AssertImgs.assertImageEquals( expected, actual );
	}

	@Test
	public void testRGB()
	{
		final ImagePlus image = randomImagePlus( new ARGBType(), DIMENSIONS );
		final Img< ARGBType > actual = VirtualStackAdapter.wrapRGBA( image );
		final Img< ARGBType > expected = ImagePlusAdapter.wrapRGBA( image );
		AssertImgs.assertImageEquals( expected, actual );
	}

	@Test
	public void testFloat()
	{
		final ImagePlus image = randomImagePlus( new FloatType(), DIMENSIONS );
		final Img< FloatType > actual = VirtualStackAdapter.wrapFloat( image );
		final Img< FloatType > expected = ImagePlusAdapter.wrapFloat( image );
		AssertImgs.assertImageEquals( expected, actual );
	}

	@Test
	public void testLowerNumDimensions()
	{
		final ImagePlus image = randomImagePlus( new UnsignedByteType(), 2, 3, 6 );
		final Img< UnsignedByteType > actual = VirtualStackAdapter.wrapByte( image );
		final Img< UnsignedByteType > expected = ImagePlusAdapter.wrapByte( image );
		AssertImgs.assertImageEquals( expected, actual );
	}

	@Test
	public void testSingletonDimensions()
	{
		final ImagePlus image = randomImagePlus( new UnsignedByteType(), 2, 1, 1, 6 );
		final Img< UnsignedByteType > actual = VirtualStackAdapter.wrapByte( image );
		final Img< UnsignedByteType > expected = ImagePlusAdapter.wrapByte( image );
		AssertImgs.assertImageEquals( expected, actual );
	}

	private < T extends NativeType< T > & NumericType< T > > ImagePlus randomImagePlus( final T type, final long... dimensions )
	{
		final Img< T > random = RandomImgs.randomImage( type, dimensions );
		return ImageJFunctions.wrap( random, "test" );
	}

}
