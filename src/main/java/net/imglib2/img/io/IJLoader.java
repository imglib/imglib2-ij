package net.imglib2.img.io;

import ij.IJ;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

public class IJLoader< T extends NumericType< T > & NativeType< T > > implements Loader< T >
{

	@Override
	public Img< T > load( final String path )
	{
		return ImageJFunctions.wrap( IJ.openImage( path ) );
	}
}
