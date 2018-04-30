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

package net.imglib2.img;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.cache.Cache;
import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.display.imagej.CalibrationUtils;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Fraction;

import java.util.AbstractList;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.LongStream;

/**
 * Wrapper for ImagePlus using imglib2-caches.
 * It loads the planes lazily, which is especially useful when wrapping a virtual stack.
 *
 * @author Matthias Arzt
 */

public class VirtualStackAdapter
{

	/**
	 * Wraps a 8 bit {@link ImagePlus}, into an {@link ImgPlus}, that is backed by a {@link PlanarImg}.
	 * The {@link PlanarImg} loads the planes only if needed, and caches them.
	 * The axes of the returned image are set according to the calibration of the given image.
	 */
	public static ImgPlus< UnsignedByteType > wrapByte( ImagePlus image )
	{
		return internWrap( image, ImagePlus.GRAY8, new UnsignedByteType(), array -> new ByteArray( ( byte[] ) array ) );
	}

	/**
	 * Wraps a 16 bit {@link ImagePlus}, into an {@link ImgPlus}, that is backed by a {@link PlanarImg}.
	 * The {@link PlanarImg} loads the planes only if needed, and caches them.
	 * The axes of the returned image are set according to the calibration of the given image.
	 */
	public static ImgPlus< UnsignedShortType > wrapShort( ImagePlus image )
	{
		return internWrap( image, ImagePlus.GRAY16, new UnsignedShortType(), array -> new ShortArray( ( short[] ) array ) );
	}

	/**
	 * Wraps a 32 bit {@link ImagePlus}, into an {@link ImgPlus}, that is backed by a {@link PlanarImg}.
	 * The {@link PlanarImg} loads the planes only if needed, and caches them.
	 * The axes of the returned image are set according to the calibration of the given image.
	 */
	public static ImgPlus< FloatType > wrapFloat( ImagePlus image )
	{
		return internWrap( image, ImagePlus.GRAY32, new FloatType(), array -> new FloatArray( ( float[] ) array ) );
	}

	/**
	 * Wraps a 24 bit {@link ImagePlus}, into an {@link ImgPlus}, that is backed by a {@link PlanarImg}.
	 * The {@link PlanarImg} loads the planes only if needed, and caches them.
	 * The axes of the returned image are set according to the calibration of the given image.
	 */
	public static ImgPlus< ARGBType > wrapRGBA( ImagePlus image )
	{
		return internWrap( image, ImagePlus.COLOR_RGB, new ARGBType(), array -> new IntArray( ( int[] ) array ) );
	}

	/**
	 * Wraps an {@link ImagePlus}, into an {@link ImgPlus}, that is backed by a {@link PlanarImg}.
	 * The {@link PlanarImg} loads the planes only if needed, and caches them.
	 * The pixel type of the returned image depends on the type of the ImagePlus.
	 * The axes of the returned image are set according to the calibration of the given image.
	 */
	public static ImgPlus< ? > wrap( ImagePlus image )
	{
		switch ( image.getType() )
		{
		case ImagePlus.GRAY8:
			return wrapByte( image );
		case ImagePlus.GRAY16:
			return wrapShort( image );
		case ImagePlus.GRAY32:
			return wrapFloat( image );
		case ImagePlus.COLOR_RGB:
			return wrapRGBA( image );
		}
		throw new RuntimeException( "Only 8, 16, 32-bit and RGB supported!" );
	}

	private static < T extends NativeType< T >, A extends ArrayDataAccess< A > > ImgPlus< T > internWrap( ImagePlus image, int expectedType, T type, Function< Object, A > createArrayAccess )
	{
		if ( image.getType() != expectedType )
			throw new IllegalArgumentException();
		ImagePlusLoader< A > loader = new ImagePlusLoader<>( image, createArrayAccess );
		final long[] dimensions = getNonTrivialDimensions(image);
		PlanarImg< T, A > cached = new PlanarImg<T, A>( loader, dimensions, new Fraction() );
		cached.setLinkedType( (( NativeTypeFactory< T, A > ) type.getNativeTypeFactory()).createLinkedType( cached ) );
		CalibratedAxis[] axes = CalibrationUtils.getNonTrivialAxes( image );
		ImgPlus< T > wrap = new ImgPlus<>(cached, image.getTitle(), axes);
		return wrap;
	}

	private static long[] getNonTrivialDimensions(ImagePlus image) {
		LongStream xy = LongStream.of(image.getWidth(), image.getHeight());
		LongStream czt = LongStream.of(image.getNChannels(), image.getNSlices(), image.getNFrames());
		return LongStream.concat(xy, czt.filter(x -> x > 1)).toArray();
	}

	private static class ImagePlusLoader< A extends ArrayDataAccess< A > > extends AbstractList< A >
	{
		private final ImagePlus image;

		private final Cache<Integer, A> cache;

		private final Function< Object, A > arrayFactory;

		public ImagePlusLoader( final ImagePlus image, Function< Object, A > arrayFactory )
		{
			this.arrayFactory = arrayFactory;
			this.image = image;
			cache = new SoftRefLoaderRemoverCache<Integer, A>().withLoader(this::load).withRemover((key, value) -> {});
		}

		@Override
		public A get(int key) {
			try {
				return cache.get(key);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		private A load(Integer key) {
			return arrayFactory.apply( image.getStack().getPixels(key + 1) );
		}

		@Override
		public int size() {
			return image.getStackSize();
		}
	}
}
