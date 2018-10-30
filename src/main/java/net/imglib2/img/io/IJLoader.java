package net.imglib2.img.io;

import ij.IJ;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

/**
 * An ImageJ-powered implementation of a disk-based {@link CacheLoader}:
 * the file system is the cache, and requests to load image file paths
 * are executed using ImageJ's {@link IJ#openImage(java.lang.String)},
 * and wrapped as {@link PlanarImg} via the {@link ImageJFunctions#wrap(ij.ImagePlus)}.
 * 
 * @author Albert Cardona
 *
 * @param <T>
 */
public class IJLoader< T extends NumericType< T > & NativeType< T > > implements CacheLoader< String, Img< T > >
{
	@Override
	public Img< T > get( final String path )
	{
		return ImageJFunctions.wrap( IJ.openImage( path ) );
	}
}
