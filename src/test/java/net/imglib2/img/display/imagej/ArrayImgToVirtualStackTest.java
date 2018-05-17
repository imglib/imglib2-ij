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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import ij.ImagePlus;

public class ArrayImgToVirtualStackTest
{
	@Test
	public void testSharedBuffer()
	{
		final int width = 2;
		final int height = 3;
		final byte[] buffer = new byte[ width * height ];
		final ImgPlus< UnsignedByteType > img = new ImgPlus<>( ArrayImgs.unsignedBytes( buffer, width, height ) );
		final ImagePlus imagePlus = ArrayImgToVirtualStack.wrap( img );
		assertEquals( width, imagePlus.getWidth() );
		assertEquals( height, imagePlus.getHeight() );
		assertSame( buffer, imagePlus.getProcessor().getPixels() );
	}

	@Test
	public void testIsSupported()
	{
		final ImgPlus< UnsignedByteType > supported = new ImgPlus<>( ArrayImgs.unsignedBytes( 2, 2 ), "image", new AxisType[] { Axes.X, Axes.Y } );
		final ImgPlus< UnsignedByteType > unsupported1 = new ImgPlus<>( ArrayImgs.unsignedBytes( 2, 2, 3 ), "image", new AxisType[] { Axes.X, Axes.Y, Axes.Z } );
		final CellImg< UnsignedByteType, ? > cellImg = new CellImgFactory<>( new UnsignedByteType() ).create( 2, 2 );
		final ImgPlus< UnsignedByteType > unsupported2 = new ImgPlus<>( cellImg, "image", new AxisType[] { Axes.X, Axes.Y } );
		assertTrue( ArrayImgToVirtualStack.isSupported( supported ) );
		assertFalse( ArrayImgToVirtualStack.isSupported( unsupported1 ) );
		assertFalse( ArrayImgToVirtualStack.isSupported( unsupported2 ) );
	}

	@Test
	public void testPersistence()
	{
		// setup
		final float expected = 42.0f;
		final Img< FloatType > img = ArrayImgs.floats( 1, 1 );
		final ImgPlus< FloatType > imgPlus = new ImgPlus<>( img, "title", new AxisType[] { Axes.X, Axes.Y } );
		final ImagePlus imagePlus = ArrayImgToVirtualStack.wrap( imgPlus );
		// process
		imagePlus.getProcessor().setf( 0, 0, expected );
		// test
		assertEquals( expected, img.cursor().next().getRealFloat(), 0.0f );
	}
}
