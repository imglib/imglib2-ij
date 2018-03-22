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

import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertNotSame;

public class ImageJVirtualStackTest
{
	@Test
	public void testColorProcessor() {
		// NB: ColorModel can cause troubles when using a ColorProcessor so try to create on
		Img< ARGBType > img = ArrayImgs.argbs( 1, 1 );
		ImageStack stackARGB = new ImageJVirtualStackARGB<>( img, ( i, o ) -> o.set( i.get() ) );
		ImagePlus imagePlus = new ImagePlus( "title", stackARGB );
		ImageStack stack = imagePlus.getStack();
		assertTrue( stack.getProcessor( 1 ) instanceof ColorProcessor );
	}

	@Test
	public void test()
	{
		Img< UnsignedByteType > img = RandomImgs.randomImage( new UnsignedByteType(), 1000, 1000, 10 );
		VirtualStack vs = new ImageJVirtualStackUnsignedByte<>( img, copyConverter() );
		RandomAccess< UnsignedByteType > randomAccess = img.randomAccess();
		for ( int z = 0; z < 10; z++ )
		{
			ImageProcessor processor = vs.getProcessor( z + 1 );
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
		VirtualStack vs = example();
		assertEquals( 2, vs.getProcessor( 1 ).get( 1, 0 ) );
		assertEquals( 6, vs.getProcessor( 2 ).get( 2, 0 ) );
	}

	private VirtualStack example()
	{
		Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( new byte[] { 1, 2, 3, 4, 5, 6 }, 3, 1, 2 );
		return new ImageJVirtualStackUnsignedByte<>( img, copyConverter() );
	}

	@Test
	public void testGetHeight()
	{
		VirtualStack vs = example();
		assertEquals( 1, vs.getHeight() );
	}

	@Test
	public void testGetWidth()
	{
		VirtualStack vs = example();
		assertEquals( 3, vs.getWidth() );
	}

	@Test
	public void testGetSize()
	{
		VirtualStack vs = example();
		assertEquals( 2, vs.getSize() );
	}

	@Test
	public void testBitDepth()
	{
		VirtualStack vs = example();
		assertEquals( 8, vs.getBitDepth() );
	}

	@Test
	public void testGetPixels() {
		VirtualStack vs = example();
		assertArrayEquals( new byte[] {4, 5, 6}, (byte[]) vs.getPixels( 2 ) );
	}

	@Test
	public void testByteProcessor() {
		VirtualStack vs = example();
		assertTrue( vs.getProcessor( 1 ) instanceof ByteProcessor );
	}

	@Test
	public void testFloatProcessor() {
		float value = 42f;
		VirtualStack vs = new ImageJVirtualStackFloat<>( ArrayImgs.floats( new float[]{ value }, 1, 1, 1 ), copyConverter() );
		ImageProcessor processor = vs.getProcessor( 1 );
		assertTrue( processor instanceof FloatProcessor );
		assertEquals( value, processor.getf( 0, 0 ), 0f );
	}

	@Test
	public void testShortProcessor() {
		short value = 13;
		VirtualStack vs = new ImageJVirtualStackUnsignedShort<>( ArrayImgs.unsignedShorts( new short[]{ value }, 1, 1, 1), copyConverter() );
		ImageProcessor processor = vs.getProcessor( 1 );
		assertTrue( processor instanceof ShortProcessor );
		assertEquals( value, processor.get( 0, 0 ) );
	}

	@Test
	public void testProcessor() {
		int value = 43;
		VirtualStack vs = new ImageJVirtualStackARGB<>( ArrayImgs.argbs( new int[]{ value }, 1, 1, 1 ), copyConverter() );
		ImageProcessor processor = vs.getProcessor( 1 );
		assertTrue( processor instanceof ColorProcessor );
		assertEquals( value, processor.get( 0, 0 ) & 0x00ffffff );
	}

	private < T extends Type<T> > Converter< T, T > copyConverter()
	{
		return ( i, o ) -> o.set( i );
	}

	@Test
	public void testProcessorPerPlane()
	{
		Img< UnsignedByteType > expected = RandomImgs.randomImage( new UnsignedByteType(), 100, 100, 100 );
		ImagePlus imagePlus = ImageJFunctions.wrap( expected, "title");
		ImageStack stack = imagePlus.getStack();
		ImageProcessor p1 = stack.getProcessor( 1 );
		ImageProcessor p2 = stack.getProcessor( 2 );
		assertNotSame(p1, p2);
	}
}
