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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Ignore;
import org.junit.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class ImageJVirtualStackTest
{
	@Test
	public void testColorProcessor()
	{
		// NB: ColorModel can cause troubles when using a ColorProcessor so try to create one
		final Img< ARGBType > img = ArrayImgs.argbs( 1, 1 );
		final ImageStack stackARGB = ImageJVirtualStackARGB.wrap( img );
		final ImagePlus imagePlus = new ImagePlus( "title", stackARGB );
		final ImageStack stack = imagePlus.getStack();
		assertTrue( stack.getProcessor( 1 ) instanceof ColorProcessor );
	}

	@Test
	public void test()
	{
		final Img< UnsignedByteType > img = RandomImgs.seed(12345).nextImage( new UnsignedByteType(), 1000, 1000, 10 );
		final VirtualStack vs = ImageJVirtualStackUnsignedByte.wrap( img );
		final RandomAccess< UnsignedByteType > randomAccess = img.randomAccess();
		for ( int z = 0; z < 10; z++ )
		{
			final ImageProcessor processor = vs.getProcessor( z + 1 );
			for ( int y = 0; y < 1000; y++ )
				for ( int x = 0; x < 1000; x++ )
				{
					randomAccess.setPosition( x, 0 );
					randomAccess.setPosition( y, 1 );
					randomAccess.setPosition( z, 2 );
					assertEquals( randomAccess.get().get(), processor.get( x, y ) );
				}
		}
	}

	@Test
	public void testGetProcessor()
	{
		final VirtualStack vs = example();
		assertEquals( 2, vs.getProcessor( 1 ).get( 1, 0 ) );
		assertEquals( 6, vs.getProcessor( 2 ).get( 2, 0 ) );
	}

	private VirtualStack example()
	{
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( new byte[] { 1, 2, 3, 4, 5, 6 }, 3, 1, 2 );
		return ImageJVirtualStackUnsignedByte.wrap( img );
	}

	@Test
	public void testGetHeight()
	{
		final VirtualStack vs = example();
		assertEquals( 1, vs.getHeight() );
	}

	@Test
	public void testGetWidth()
	{
		final VirtualStack vs = example();
		assertEquals( 3, vs.getWidth() );
	}

	@Test
	public void testGetSize()
	{
		final VirtualStack vs = example();
		assertEquals( 2, vs.getSize() );
	}

	@Test
	public void testBitDepth()
	{
		final VirtualStack vs = example();
		assertEquals( 8, vs.getBitDepth() );
	}

	@Test
	public void testGetPixels()
	{
		final VirtualStack vs = example();
		assertArrayEquals( new byte[] { 4, 5, 6 }, ( byte[] ) vs.getPixels( 2 ) );
	}

	@Test
	public void testByteProcessor()
	{
		final VirtualStack vs = example();
		assertTrue( vs.getProcessor( 1 ) instanceof ByteProcessor );
	}

	@Test
	public void testFloatProcessor()
	{
		final float value = 42f;
		final VirtualStack vs = ImageJVirtualStackFloat.wrap( ArrayImgs.floats( new float[] { value }, 1, 1, 1 ) );
		final ImageProcessor processor = vs.getProcessor( 1 );
		assertTrue( processor instanceof FloatProcessor );
		assertEquals( value, processor.getf( 0, 0 ), 0f );
	}

	@Test
	public void testShortProcessor()
	{
		final short value = 13;
		final VirtualStack vs = ImageJVirtualStackUnsignedShort.wrap( ArrayImgs.unsignedShorts( new short[] { value }, 1, 1, 1 ) );
		final ImageProcessor processor = vs.getProcessor( 1 );
		assertTrue( processor instanceof ShortProcessor );
		assertEquals( value, processor.get( 0, 0 ) );
	}

	@Test
	public void testProcessor()
	{
		final int value = 43;
		final VirtualStack vs = ImageJVirtualStackARGB.wrap( ArrayImgs.argbs( new int[] { value }, 1, 1, 1 ) );
		final ImageProcessor processor = vs.getProcessor( 1 );
		assertTrue( processor instanceof ColorProcessor );
		assertEquals( value, processor.get( 0, 0 ) & 0x00ffffff );
	}

	@Test
	public void testSetPixelsBytes() {
		final Img< UnsignedByteType > image = ArrayImgs.unsignedBytes( 1, 1, 1 );
		final ImageJVirtualStack vs = ImageJVirtualStackUnsignedByte.wrap( image );
		byte value = 42;
		vs.setWritable( true );
		vs.setPixels( new byte[] { value }, 1 );
		assertEquals( value, image.firstElement().get() );
	}

	@Test
	public void testSetPixelsShort() {
		final Img< UnsignedShortType > image = ArrayImgs.unsignedShorts( 1, 1, 1 );
		final ImageJVirtualStack vs = ImageJVirtualStackUnsignedShort.wrap( image );
		short value = 42;
		vs.setWritable( true );
		vs.setPixels( new short[] { value }, 1 );
		assertEquals( value, image.firstElement().get() );
	}

	@Test
	public void testSetPixelsARGB() {
		final Img< ARGBType > image = ArrayImgs.argbs( 1, 1, 1 );
		final ImageJVirtualStack vs = ImageJVirtualStackARGB.wrap( image );
		int value = 42;
		vs.setWritable( true );
		vs.setPixels( new int[] { value }, 1 );
		assertEquals( value, image.firstElement().get() );
	}

	@Test
	public void testSetPixelsFloat() {
		final Img< FloatType > image = ArrayImgs.floats( 1, 1, 1 );
		final ImageJVirtualStack vs = ImageJVirtualStackFloat.wrap( image );
		float value = 42;
		vs.setWritable( true );
		vs.setPixels( new float[] { value }, 1 );
		assertEquals( value, image.firstElement().get(), 0 );
	}

	@Test
	public void testGetVoxels5DStack() {
		// NB: this tests ImageJVirtualStack getSliceZeroBasedIndex
		Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 }, 1, 1, 2, 2, 2 );
		final ImageStack stack = ImageJVirtualStackUnsignedByte.wrap( img );
		float[] voxels = new float[8];
		stack.getVoxels( 0, 0, 0, 1, 1, 8, voxels );
		assertArrayEquals( new float[] { 0, 1, 2, 3, 4, 5, 6, 7 }, voxels, 0);
	}

	@Test
	public void testSetVoxels() {
		// NB: this tests ImageJVirtualStack getSliceZeroBasedIndex
		Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( 1, 1 );
		final ImageJVirtualStack<?> stack = ImageJVirtualStackUnsignedByte.wrap( img );
		stack.setWritable( true );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 42 } );
		assertEquals( 42, img.firstElement().get() );
	}

	@Test
	public void testProcessorPerPlane()
	{
		final Img< UnsignedByteType > expected = RandomImgs.seed(54321).nextImage( new UnsignedByteType(), 100, 100, 100 );
		final ImagePlus imagePlus = ImageJFunctions.wrap( expected, "title" );
		final ImageStack stack = imagePlus.getStack();
		final ImageProcessor p1 = stack.getProcessor( 1 );
		final ImageProcessor p2 = stack.getProcessor( 2 );
		assertNotSame( p1, p2 );
	}
}
