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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.junit.Test;

import ij.ImagePlus;
import ij.VirtualStack;

public class PlanarImgToVirtualStackTest
{
	@Test
	public void testStorageArray()
	{
		final PlanarImg< UnsignedByteType, ? > img = example();
		final VirtualStack stack = PlanarImgToVirtualStack.wrap( img );
		assertSame( img.getPlane( 1 ).getCurrentStorageArray(), stack.getPixels( 2 ) );
	}

	@Test
	public void testPixelValues()
	{
		final PlanarImg< UnsignedByteType, ? > img = example();
		final VirtualStack stack = PlanarImgToVirtualStack.wrap( img );
		assertArrayEquals( new byte[] { 1, 2, 3 }, ( byte[] ) stack.getPixels( 1 ) );
		assertArrayEquals( new byte[] { 4, 5, 6 }, ( byte[] ) stack.getPixels( 2 ) );
	}

	@Test
	public void testImgPlus()
	{
		// setup
		final PlanarImg< UnsignedByteType, ? > img = example();
		final String title = "test image";
		final ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( img, title, new AxisType[] { Axes.X, Axes.Y, Axes.TIME } );
		// process
		final ImagePlus imagePlus = PlanarImgToVirtualStack.wrap( imgPlus );
		// test
		assertEquals( title, imagePlus.getTitle() );
		assertEquals( 3, imagePlus.getWidth() );
		assertEquals( 1, imagePlus.getHeight() );
		assertEquals( 1, imagePlus.getNChannels() );
		assertEquals( 1, imagePlus.getNSlices() );
		assertEquals( 2, imagePlus.getNFrames() );
	}

	@Test
	public void testAxisOrder()
	{
		final PlanarImg< UnsignedByteType, ? > img = new PlanarImgFactory< UnsignedByteType >().create( new long[] { 1, 1, 2, 3, 4 }, new UnsignedByteType() );
		fill( img );
		final ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( img, "title", new AxisType[] { Axes.X, Axes.Y, Axes.TIME, Axes.CHANNEL, Axes.Z } );
		final ImagePlus imagePlus = PlanarImgToVirtualStack.wrap( imgPlus );
		assertEquals( 2, imagePlus.getStack().getProcessor( imagePlus.getStackIndex( 1, 1, 2 ) ).get( 0, 0 ) );
		assertEquals( 7, imagePlus.getStack().getProcessor( imagePlus.getStackIndex( 1, 2, 1 ) ).get( 0, 0 ) );
		assertEquals( 3, imagePlus.getStack().getProcessor( imagePlus.getStackIndex( 2, 1, 1 ) ).get( 0, 0 ) );
	}

	private void fill( final RandomAccessibleInterval< ? extends IntegerType > img )
	{
		final AtomicInteger i = new AtomicInteger();
		Views.flatIterable( img ).forEach( pixel -> pixel.setInteger( i.incrementAndGet() ) );
	}

	private PlanarImg< UnsignedByteType, ? > example()
	{
		final PlanarImg< UnsignedByteType, ? > img = new PlanarImgFactory< UnsignedByteType >().create( new long[] { 3, 1, 2 }, new UnsignedByteType() );
		fill( img );
		return img;
	}

	@Test
	public void testPersistence()
	{
		// setup
		final PlanarImg< FloatType, FloatArray > img = PlanarImgs.floats( 1, 1 );
		final ImgPlus< FloatType > imgPlus = new ImgPlus< FloatType >( img, "title", new AxisType[] { Axes.X, Axes.Y } );
		final ImagePlus imagePlus = PlanarImgToVirtualStack.wrap( imgPlus );
		final float expected = 42.0f;
		// process
		imagePlus.getProcessor().setf( 0, 0, expected );
		// test
		assertEquals( expected, img.cursor().next().get(), 0.0f );
	}

	@Test
	public void testSetPixels() {
		// setup
		final PlanarImg< FloatType, FloatArray > img = PlanarImgs.floats( 1, 1 );
		final ImagePlus imagePlus = PlanarImgToVirtualStack.wrap( new ImgPlus<>( img, "title" ) );
		final float expected = 42.0f;
		// process
		imagePlus.getStack().setPixels( new float[] { expected }, 1 );
		// test
		assertEquals( expected, img.cursor().next().get(), 0.0f );
	}
}
