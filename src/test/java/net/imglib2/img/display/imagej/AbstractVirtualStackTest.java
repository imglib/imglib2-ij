package net.imglib2.img.display.imagej;

import ij.ImageStack;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link AbstractVirtualStack}
 *
 * @author Matthias Arzt
 */
public class AbstractVirtualStackTest
{
	@Test
	public void testDuplicate()
	{
		// setup
		byte[][] pixels = { { 4 }, { 2 } };
		final ImageStack original = new TestVirtualStack( 1, 1, 2, pixels );
		// process
		final ImageStack duplicate = original.duplicate();
		// test
		assertEquals( ImageStack.class, duplicate.getClass() );
		assertEquals( 1, duplicate.getWidth() );
		assertEquals( 1, duplicate.getHeight() );
		assertEquals( 2, duplicate.getSize() );
		assertArrayEquals( pixels[ 0 ], ( byte[] ) duplicate.getPixels( 1 ) );
		assertArrayEquals( pixels[ 1 ], ( byte[] ) duplicate.getPixels( 2 ) );
	}

	@Test
	public void testCrop()
	{
		// setup
		byte[][] pixels = new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } };
		final ImageStack original = new TestVirtualStack( 1, 2, 3, pixels );
		// process
		final ImageStack duplicate = original.crop( 0, 1, 1, 1, 1, 2 );
		// test
		assertEquals( ImageStack.class, duplicate.getClass() );
		assertEquals( 1, duplicate.getWidth() );
		assertEquals( 1, duplicate.getHeight() );
		assertEquals( 2, duplicate.getSize() );
		assertArrayEquals( new byte[] { 4 }, ( byte[] ) duplicate.getPixels( 1 ) );
		assertArrayEquals( new byte[] { 6 }, ( byte[] ) duplicate.getPixels( 2 ) );
	}

	private static class TestVirtualStack extends AbstractVirtualStack
	{

		private final byte[][] pixels;

		public TestVirtualStack( int width, int height, int size, byte[][] pixels )
		{
			super( width, height, size, 8 );
			this.pixels = pixels;
		}

		@Override public Object getPixels( int n )
		{
			return pixels[ n - 1 ];
		}
	}
}
