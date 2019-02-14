package net.imglib2.img.display.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.gui.Roi;
import ij.plugin.Resizer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

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

	@Test
	public void testConvetToFloat()
	{
		// setup
		final ImageStack original = new TestVirtualStack( 1, 1, 2, new byte[][] { { 4 }, { 2 } } );
		// process
		final ImageStack result = original.convertToFloat();
		// test
		assertEquals( ImageStack.class, result.getClass() );
		assertEquals( 1, result.getWidth() );
		assertEquals( 1, result.getHeight() );
		assertEquals( 2, result.getSize() );
		assertArrayEquals( new float[] { 4 }, ( float[] ) result.getPixels( 1 ), 0 );
		assertArrayEquals( new float[] { 2 }, ( float[] ) result.getPixels( 2 ), 0 );
	}

	@Test
	public void testDeleteFirstSlice()
	{
		byte[][] pixels = { { 1 }, { 2 }, { 3 } };
		AbstractVirtualStack stack = new TestVirtualStack( 1, 1, 3, pixels );
		assertArrayEquals( pixels[ 0 ], ( byte[] ) stack.getPixels( 1 ) );
		stack.deleteSlice( 1 );
		assertArrayEquals( pixels[ 1 ], ( byte[] ) stack.getPixels( 1 ) );
		stack.deleteSlice( 1 );
		assertArrayEquals( pixels[ 2 ], ( byte[] ) stack.getPixels( 1 ) );
		assertEquals( 1, stack.getSize() );
	}

	@Test
	public void testSliceNameAfterDeletion()
	{
		byte[][] pixels = { { 0 }, { 0 }, { 0 } };
		AbstractVirtualStack stack = new TestVirtualStack( 1, 1, 3, pixels );
		assertEquals( "1", stack.getSliceLabel( 1 ) );
		stack.deleteSlice( 1 );
		assertEquals( "2", stack.getSliceLabel( 1 ) );
	}

	@Test
	public void testDeleteLastSlice()
	{
		byte[][] pixels = { { 1 }, { 2 }, { 3 } };
		AbstractVirtualStack stack = new TestVirtualStack( 1, 1, 3, pixels );
		stack.deleteSlice( 3 );
		stack.deleteSlice( 2 );
		assertEquals( 1, stack.getSize() );
	}

	@Test( expected = UnsupportedOperationException.class )
	public void testDeleteMideSlice()
	{
		// NB: This is not implemented yet, because:
		//  * It's maybe not really needed.
		//  * A implementation with good performance would need some thinking.
		// NB: As it's not implemented yet,
		// a UnsupportedOperationException is throw.
		byte[][] pixels = { { 1 }, { 2 }, { 3 } };
		AbstractVirtualStack stack = new TestVirtualStack( 1, 1, 3, pixels );
		stack.deleteSlice( 2 );
	}

	@Test
	public void testResizer()
	{
		assumeFalse( GraphicsEnvironment.isHeadless() );
		// Try to crop the image with ij.plugin.Resizer
		Resizer resizer = new Resizer();
		int width = 5;
		int height = 5;
		int depth = 2;
		int numPixels = width * height * depth;
		ImageStack firstStack = new TestVirtualStack( width, height, depth, new byte[][] { byteRange( 0, 25 ), byteRange( 25, 50 ) } );
		ImagePlus imagePlus = new ImagePlus( "title", firstStack );
		imagePlus.show();
		imagePlus.setRoi( new Roi( 2, 2, 2, 2 ) );
		resizer.run( "crop" );
		ImagePlus result = IJ.getImage();
		ImageStack stack = result.getStack();
		assertEquals( 2, stack.getWidth() );
		assertEquals( 2, stack.getHeight() );
		assertEquals( depth, stack.getSize() );
		assertArrayEquals( new byte[] { 12, 13, 17, 18 }, ( byte[] ) stack.getProcessor( 1 ).getPixels() );
		assertArrayEquals( new byte[] { 37, 38, 42, 43 }, ( byte[] ) stack.getProcessor( 2 ).getPixels() );
	}

	private byte[] byteRange( int start, int endExcluded )
	{
		byte[] result = new byte[ endExcluded - start ];
		for ( int i = start; i < endExcluded; i++ )
			result[ i - start ] = ( byte ) i;
		return result;
	}

	@Test
	public void testSetPixels()
	{
		byte[][] pixels = new byte[ 1 ][ 1 ];
		VirtualStack stack = new TestVirtualStack( 1, 1, 1, pixels );
		stack.setPixels( new byte[] { 42 }, 1 );
		assertEquals( 42, pixels[ 0 ][ 0 ] );
	}

	@Test
	public void testSetProcessor()
	{
		byte[][] pixels = new byte[ 1 ][ 1 ];
		VirtualStack stack = new TestVirtualStack( 1, 1, 1, pixels );
		final ImageProcessor processor = new ByteProcessor( 1, 1, new byte[] { 42 } );
		stack.setProcessor( processor, 1 );
		assertEquals( 42, pixels[ 0 ][ 0 ] );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testSetProcessorWrongSize()
	{
		VirtualStack stack = new TestVirtualStack( 1, 1, 1, new byte[ 1 ][ 1 ] );
		final ImageProcessor processor = new ByteProcessor( 2, 1, new byte[ 2 ] );
		stack.setProcessor( processor, 1 );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testSetProcessorWrongType()
	{
		VirtualStack stack = new TestVirtualStack( 1, 1, 1, new byte[ 1 ][ 1 ] );
		final ImageProcessor processor = new FloatProcessor( 1, 1, new float[ 1 ] );
		stack.setProcessor( processor, 1 );
	}

	@Test
	public void testWriteProtection()
	{
		byte[][] pixels = new byte[ 1 ][ 1 ];
		// NB: use non writable stack
		VirtualStack stack = new TestReadonlyVirtualStack( 1, 1, 1, pixels );
		stack.setPixels( new byte[] { 42 }, 1 );
		stack.setProcessor( new ByteProcessor( 1, 1, new byte[] { 43 } ), 1 );
		stack.setVoxels( 0, 0, 0, 1, 1, 1, new float[] { 44 } );
		assertEquals( 0, pixels[ 0 ][ 0 ] );
	}

	private static class TestVirtualStack extends AbstractVirtualStack
	{

		private final byte[][] pixels;

		public TestVirtualStack( int width, int height, int size, byte[][] pixels )
		{
			super( width, height, size, 8 );
			this.pixels = pixels;
		}

		@Override
		protected Object getPixelsZeroBasedIndex( int index )
		{
			return pixels[ index ];
		}

		@Override
		protected void setPixelsZeroBasedIndex( int index, Object pixels )
		{
			this.pixels[ index ] = ( byte[] ) pixels;
		}
	}

	private static class TestReadonlyVirtualStack extends TestVirtualStack
	{

		public TestReadonlyVirtualStack( int width, int height, int size, byte[][] pixels )
		{
			super( width, height, size, pixels );
		}

		@Override
		protected boolean isWritable()
		{
			return false;
		}
	}

}
