/*-
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

import ij.ImageStack;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the methods {@link AbstractVirtualStack#getVoxels}
 * and {@link AbstractVirtualStack#setVoxels} of class
 * {@link AbstractVirtualStack}.
 *
 * @author Matthias Arzt
 */
public class AbstractVirtualStackGetSetVoxelsTest
{

	@Test
	public void testGetVoxelsBytes()
	{
		final byte[][] pixels = { { 42 } };
		ImageStack stack = TestVirtualStack.bytes( 1, 1, pixels );
		float[] voxels = stack.getVoxels( 0, 0, 0, 1, 1, 1, null );
		assertArrayEquals( new float[] { 42 }, voxels, 0 );
	}

	@Test
	public void testGetVoxelsARGB()
	{
		final ImageStack stack = TestVirtualStack.ints( 1, 1, new int[][] { { 0xff010203 } } );
		float[] voxels = stack.getVoxels( 0, 0, 0, 1, 1, 1, null );
		assertArrayEquals( new float[] { 0xff010203 }, voxels, 0 );
	}

	@Test
	public void testGetVoxels()
	{
		final byte[][] bytes = { { 0, 1, 2, 3 }, { 4, 5, 6, 7 } };
		final ImageStack stack = TestVirtualStack.bytes( 2, 2, bytes );
		float[] voxels = new float[ 8 ];
		stack.getVoxels( 0, 0, 0, 2, 2, 2, voxels );
		assertArrayEquals( new float[] { 0, 1, 2, 3, 4, 5, 6, 7 }, voxels, 0 );
	}

	@Test
	public void testGetVoxelsChannel()
	{
		final ImageStack stack = TestVirtualStack.ints( 1, 1, new int[][] { { 0xff010203 } } );
		assertArrayEquals( new float[] { 1 }, stack.getVoxels( 0, 0, 0, 1, 1, 1, null, 0 ), 0 );
		assertArrayEquals( new float[] { 2 }, stack.getVoxels( 0, 0, 0, 1, 1, 1, null, 1 ), 0 );
		assertArrayEquals( new float[] { 3 }, stack.getVoxels( 0, 0, 0, 1, 1, 1, null, 2 ), 0 );
	}

	@Test
	public void testGetVoxelsSubVolume()
	{
		int[][] pixels = { range( 0, 99 ), range( 100, 199 ), range( 200, 299 ), range( 300, 399 ) };
		final ImageStack stack = TestVirtualStack.ints( 10, 10, pixels );
		float[] voxels = stack.getVoxels( 1, 1, 1, 2, 2, 2, null );
		assertArrayEquals( new float[] { 111, 112, 121, 122, 211, 212, 221, 222 }, voxels, 0 );
	}

	private int[] range( int start, int end )
	{
		return IntStream.rangeClosed( start, end ).toArray();
	}

	@Test
	public void testSetVoxels()
	{
		byte[][] pixels = { { 0 } };
		ImageStack stack = TestVirtualStack.bytes( 1, 1, pixels );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 42 } );
		assertEquals( 42, pixels[ 0 ][ 0 ] );
	}

	@Test
	public void testSetVoxelsARGB()
	{
		int[][] pixels = { { 0 } };
		ImageStack stack = TestVirtualStack.ints( 1, 1, pixels );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 42 } );
		assertEquals( 42, pixels[ 0 ][ 0 ] );
	}

	@Test
	public void testSetVoxelsChannel()
	{
		int[][] pixels = { { 0 } };
		ImageStack stack = TestVirtualStack.ints( 1, 1, pixels );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 1 }, 0 );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 2 }, 1 );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 3 }, 2 );
		assertEquals( 0x010203, pixels[ 0 ][ 0 ] );
	}

	@Test
	public void testSetVoxelsSubVolume()
	{
		byte[][] pixels = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
		byte[][] expected = { { 1, 2, 13, 4 }, { 5, 6, 17, 8 } };
		float[] voxels = { 13, 17 };
		ImageStack stack = TestVirtualStack.bytes( 2, 2, pixels );
		stack.setVoxels( 0, 1, 0, 1, 1, 2, voxels );
		assertTrue( Arrays.deepEquals( expected, pixels ) );
	}

	@Test( expected = IndexOutOfBoundsException.class )
	public void testOutOfBounds()
	{
		final ImageStack stack = TestVirtualStack.bytes( 1, 1, new byte[ 1 ][ 4 ] );
		stack.getVoxels( 0, 0, 0, 1, 2, 1, null );
	}

	private static class TestVirtualStack extends AbstractVirtualStack
	{

		private final Object[] pixels;

		private TestVirtualStack( int width, int height, Object[] pixels, int bitDepth )
		{
			super( width, height, pixels.length, bitDepth );
			this.pixels = pixels;
		}

		public static TestVirtualStack bytes( int width, int height, byte[][] pixels )
		{
			return new TestVirtualStack( width, height, pixels, 8 );
		}

		public static TestVirtualStack ints( int width, int height, int[][] pixels )
		{
			return new TestVirtualStack( width, height, pixels, 24 );
		}

		@Override
		protected Object getPixelsZeroBasedIndex( int index )
		{
			return pixels[ index ];
		}

		@Override
		protected void setPixelsZeroBasedIndex( int index, Object pixels )
		{
			this.pixels[ index ] = pixels;
		}
	}
}
