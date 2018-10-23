package net.imglib2.img.io;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class LoadTest
{
	final String[] paths = new String[]
		{
			"/home/albert/lab/scripts/data/4D-series-zip/SPM00_TM000000_CM00_CHN00.zip",
			"/home/albert/lab/scripts/data/4D-series-zip/SPM00_TM000001_CM00_CHN00.zip",
			"/home/albert/lab/scripts/data/4D-series-zip/SPM00_TM000002_CM00_CHN00.zip",
		};
	
	private class ImgVirtualStack extends VirtualStack
	{
		private final RandomAccessibleInterval< UnsignedShortType > img;
		
		ImgVirtualStack( final RandomAccessibleInterval< UnsignedShortType > img )
		{
			super( ( int )img.dimension( 0 ), ( int )img.dimension( 1 ), ( int )img.dimension( 2 ) * ( int ) img.dimension( 3 ) );
			this.img = img;
		}
		
		@Override
		public Object getPixels( final int n )
		{
			final ArrayImg< UnsignedShortType, ? > aimg = ArrayImgs.unsignedShorts( new long[]{ this.img.dimension( 0 ), this.img.dimension( 1 ) } );
			final long nZ = this.img.dimension( 2 );
			final IntervalView< UnsignedShortType > volume = Views.hyperSlice( this.img, 3, (n - 1) / nZ ); // fixed T
			final IntervalView< UnsignedShortType > plane = Views.hyperSlice( volume, 2, (n - 1)  % nZ );  // fixed Z

			final Cursor< UnsignedShortType > c1 = plane.cursor(),
											  c2 = aimg.cursor();

			while ( c1.hasNext() )
				c2.next().set( c1.next() );

			return ( ( ArrayDataAccess< ? > )aimg.update( null ) ).getCurrentStorageArray();
		}
		
		@Override
		public ImageProcessor getProcessor( final int n )
		{
			return new ShortProcessor( ( int )this.img.dimension( 0 ), ( int )this.img.dimension( 1 ), ( short[] )this.getPixels( n ), null );
		}
	}
	
	public void testLazyStackCached()
	{
		final CachedCellImg< UnsignedShortType, ? > ccimg = Load.lazyStack( Arrays.asList( paths ), new IJLoader< UnsignedShortType >() );
		
		show( ccimg, "lazy stack", 1, ( int )ccimg.dimension( 2 ), ( int )ccimg.dimension( 3 ) );
	}
	
	public void show( final RandomAccessibleInterval< UnsignedShortType > img, final String title, final int nChannels, final int nSlices, final int nFrames )
	{
		final ImgVirtualStack vs = new ImgVirtualStack( img );
		final ImagePlus imp = new ImagePlus( title, vs );
		imp.setDimensions( nChannels, nSlices, nFrames );
		final CompositeImage com = new CompositeImage( imp, CompositeImage.GRAYSCALE );
		com.show();
	}
	
	//@Test
	public void testStack()
	{
		final RandomAccessibleInterval< UnsignedShortType > img = Load.stack( paths, new IJLoader< UnsignedShortType >() );
		
		show( img, "stack", 1, ( int )img.dimension( 2 ), ( int )img.dimension( 3 ) );
	}
	
	public void testStackDefault()
	{
		final RandomAccessibleInterval< ? > img = Views.stack( Arrays.stream( paths )
				.map( IJ::openImage )
				.map( ImageJFunctions::wrap )
				.collect( Collectors.toList() ) );
		
		show( ( RandomAccessibleInterval< UnsignedShortType > )img, "stack", 1, ( int )img.dimension( 2 ), ( int )img.dimension( 3 ) );
	}
	
	static public void main( final String[] args )
	{
		new ImageJ();
		//new LoadTest().testLazyStack();
		//new LoadTest().testStack();
		//new LoadTest().testStackDefault();
		new LoadTest().testLazyStackCached();
	}
}
