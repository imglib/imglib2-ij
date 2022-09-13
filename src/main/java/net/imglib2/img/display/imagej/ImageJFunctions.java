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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.ImgPlus;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.ComplexPowerGLogFloatConverter;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import ij.ImagePlus;
import ij.VirtualStack;

/**
 * Provides static convenience methods to facilitate interacting with ImageJ
 * 1.x.
 * <p>
 * When interacting between ImageJ 1.x and ImgLib2, it is desirable to adapt
 * data structures instead of copying data.
 * </p>
 * <p>
 * For example, when an {@link ImagePlus} is made available by ImageJ 1.x, you
 * can pass it to ImgLib2 as an {@link Img} via
 * {@code ImageJFunctions.wrap(imp)}.
 * </p>
 * <p>
 * Likewise, when an ImgLib2 {@link RandomAccessibleInterval} needs to be passed
 * to ImageJ 1.x, it can be wrapped into an {@link ImagePlus} via
 * {@code ImageJFunctions.wrap(img, title)}.
 * (For details on this see {@link ImageJVirtualStack}).
 * </p>
 *
 * @author Tobis Pietzsch
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ImageJFunctions
{
	final static AtomicInteger ai = new AtomicInteger();

	public static < T extends NumericType< T > & NativeType< T > > Img< T > wrap( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrap( imp );
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > Img< T > wrapReal( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapReal( imp );
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > & NativeType< T > > Img< T > wrapRealNative( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapReal( imp );
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends NumericType< T > > Img< T > wrapNumeric( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapNumeric( imp );
	}

	public static < T extends NumericType< T > & NativeType< T > > Img< T > wrapNumericNative( final ImagePlus imp )
	{
		return wrap( imp );
	}

	public static Img< UnsignedByteType > wrapByte( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapByte( imp );
	}

	public static Img< UnsignedShortType > wrapShort( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapShort( imp );
	}

	public static Img< UnsignedIntType > wrapInt( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapInt( imp );
	}

	public static Img< ARGBType > wrapRGBA( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapRGBA( imp );
	}

	public static Img< FloatType > wrapFloat( final ImagePlus imp )
	{
		return ImagePlusAdapter.wrapFloat( imp );
	}

	public static Img< FloatType > convertFloat( final ImagePlus imp )
	{
		return ImagePlusAdapter.convertFloat( imp );
	}

	/**
	 * Display and return a single channel {@link ImagePlus}, wrapping a
	 * {@link RandomAccessibleInterval}. The image type of the result
	 * (ImagePlus.GRAY8, ImagePlus.GRAY16, ImagePlus.GRAY32, ImagePlus.COLOR_256
	 * or ImagePlus.COLOR_RGB) is inferred from the generic type of the input
	 * {@link RandomAccessibleInterval}.
	 */
	public static < T extends NumericType< T > > ImagePlus show( final RandomAccessibleInterval< T > img,
			final ExecutorService service )
	{
		return show( img, "Image " + ai.getAndIncrement(), service );
	}

	public static < T extends NumericType< T > > ImagePlus show( final RandomAccessibleInterval< T > img )
	{
		return show( img, ( ExecutorService ) null );
	}

	/**
	 * Displays a complex type as power spectrum, phase spectrum, real values or
	 * imaginary values depending on the converter
	 */
	public static < T extends ComplexType< T > > ImagePlus show( final RandomAccessibleInterval< T > img, final Converter< T, FloatType > converter,
			final ExecutorService service )
	{
		return show( img, converter, "Complex image " + ai.getAndIncrement(), service );
	}

	public static < T extends ComplexType< T > > ImagePlus show( final RandomAccessibleInterval< T > img, final Converter< T, FloatType > converter )
	{
		return show( img, converter, "Complex image " + ai.getAndIncrement(), null );
	}

	/**
	 * Displays a complex type as power spectrum, phase spectrum, real values or
	 * imaginary values depending on the converter
	 */
	public static < T extends ComplexType< T > > ImagePlus show(
			final RandomAccessibleInterval< T > img,
			final Converter< T, FloatType > converter,
			final String title,
			final ExecutorService service )
	{
		final ImageJVirtualStackFloat stack = new ImageJVirtualStackFloat( img, converter, service );
		final ImagePlus imp = new ImagePlus( title, stack );
		imp.show();

		return imp;
	}

	public static < T extends ComplexType< T > > ImagePlus show(
			final RandomAccessibleInterval< T > img,
			final Converter< T, FloatType > converter,
			final String title )
	{
		return show( img, converter, title, null );
	}

	/**
	 * Create a single channel {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval}. The image type of the result
	 * (ImagePlus.GRAY8, ImagePlus.GRAY16, ImagePlus.GRAY32, ImagePlus.COLOR_256
	 * or ImagePlus.COLOR_RGB) is inferred from the generic type of the input
	 * {@link RandomAccessibleInterval}.
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T extends NumericType< T > > ImagePlus wrap( final RandomAccessibleInterval< T > img, final String title,
			final ExecutorService service )
	{
		ImagePlus target;
		final T t = Util.getTypeFromInterval( img );

		// NB: Casting madness thanks to a long standing javac bug;
		// see e.g. http://bugs.sun.com/view_bug.do?bug_id=6548436
		// TODO: remove casting madness as soon as the bug is fixed
		final Object oImg = img;
		if ( ARGBType.class.isInstance( t ) )
			target = wrapRGB( ( RandomAccessibleInterval< ARGBType > ) oImg, title, service );
		else if ( UnsignedByteType.class.isInstance( t ) )
			target = wrapUnsignedByte( ( RandomAccessibleInterval< RealType > ) oImg, title, service );
		else if ( BitType.class.isInstance( t ) )
		{
			target = wrapBit( ( RandomAccessibleInterval< RealType > ) oImg, title, service );
		}
		else if ( IntegerType.class.isInstance( t ) )
			target = wrapUnsignedShort( ( RandomAccessibleInterval< RealType > ) oImg, title, service );
		else if ( RealType.class.isInstance( t ) )
			target = wrapFloat( ( RandomAccessibleInterval< RealType > ) oImg, title, service );
		else if ( ComplexType.class.isInstance( t ) )
			target = wrapFloat( ( RandomAccessibleInterval< ComplexType > ) oImg, new ComplexPowerGLogFloatConverter(), title, service );
		else
		{
			System.out.println( "Do not know how to display Type " + t.getClass().getSimpleName() );
			target = null;
		}

		// Retrieve and set calibration if we can. ImgPlus has calibration and
		// axis types
		if ( null != target && img instanceof ImgPlus )
		{

			final ImgPlus< T > imgplus = ( ImgPlus< T > ) img;
			CalibrationUtils.copyCalibrationToImagePlus( imgplus, target );
			target.setTitle( imgplus.getName() );
		}

		return target;
	}

	public static < T extends NumericType< T > > ImagePlus wrap( final RandomAccessibleInterval< T > img, final String title )
	{
		return wrap( img, title, null );
	}

	public static < T extends NumericType< T > > ImagePlus show( final RandomAccessibleInterval< T > img, final String title,
			final ExecutorService service )
	{
		final ImagePlus imp = wrap( img, title, service );
		if ( null == imp ) { return null; }

		imp.show();
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();

		return imp;
	}

	public static < T extends NumericType< T > > ImagePlus show( final RandomAccessibleInterval< T > img, final String title )
	{
		return show( img, title, null );
	}

	/**
	 * Create a single channel 32-bit float {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} using a custom {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus wrapFloat(
			final RandomAccessibleInterval< T > img,
			final String title,
			final ExecutorService service )
	{
		final ImageJVirtualStackFloat stack = ImageJVirtualStackFloat.wrap( img );
		stack.setExecutorService( service );
		return makeImagePlus( img, stack, title );
	}

	public static < T extends RealType< T > > ImagePlus wrapFloat(
			final RandomAccessibleInterval< T > img,
			final String title )
	{
		return wrapFloat( img, title, null );
	}

	private static ImagePlus makeImagePlus( final Dimensions dims, final VirtualStack stack, final String title )
	{
		final ImagePlus imp = new ImagePlus( title, stack );
		final int n = dims.numDimensions();
		if ( n > 2 )
		{
			imp.setOpenAsHyperStack( true );
			final int c = ( int ) dims.dimension( 2 ), s, f;
			if ( n > 3 )
			{
				s = ( int ) dims.dimension( 3 );
				if ( n > 4 )
					f = ( int ) dims.dimension( 4 );
				else
					f = 1;
			}
			else
			{
				s = 1;
				f = 1;
			}
			imp.setDimensions( c, s, f );
		}
		return imp;
	}

	/**
	 * Create a single channel 32-bit float {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} using a custom {@link Converter}.
	 */
	public static < T > ImagePlus wrapFloat(
			final RandomAccessibleInterval< T > img,
			final Converter< T, FloatType > converter,
			final String title,
			final ExecutorService service )
	{
		final ImageJVirtualStackFloat stack = new ImageJVirtualStackFloat( img, converter, service );
		return makeImagePlus( img, stack, title );
	}

	public static < T > ImagePlus wrapFloat(
			final RandomAccessibleInterval< T > img,
			final Converter< T, FloatType > converter,
			final String title )
	{
		return wrapFloat( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} as single channel 32-bit float
	 * {@link ImagePlus} using a custom {@link Converter}.
	 */
	public static < T > ImagePlus showFloat(
			final RandomAccessibleInterval< T > img,
			final Converter< T, FloatType > converter,
			final String title,
			final ExecutorService service )
	{
		return showFloat( Converters.convert( img, converter, new FloatType() ), title, service );
	}

	public static < T > ImagePlus showFloat(
			final RandomAccessibleInterval< T > img,
			final Converter< T, FloatType > converter,
			final String title )
	{
		return showFloat( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} of {@link RealType} pixels as
	 * single channel 32-bit float using a default {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus showFloat( final RandomAccessibleInterval< T > img, final String title,
			final ExecutorService service )
	{
		final ImagePlus imp = wrapFloat( img, title, service );
		imp.show();
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();
		return imp;
	}

	public static < T extends RealType< T > > ImagePlus showFloat( final RandomAccessibleInterval< T > img, final String title )
	{
		return showFloat( img, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} of {@link RealType} pixels as
	 * single channel 32-bit float using a default {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus showFloat( final RandomAccessibleInterval< T > img,
			final ExecutorService service )
	{
		return showFloat( img, "Image " + ai.getAndIncrement(), service );
	}

	public static < T extends RealType< T > > ImagePlus showFloat( final RandomAccessibleInterval< T > img )
	{
		return showFloat( img, ( ExecutorService ) null );
	}

	/**
	 * Create a 24bit RGB {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} a using a default (identity)
	 * {@link Converter}.
	 */
	public static ImagePlus wrapRGB( final RandomAccessibleInterval< ARGBType > img, final String title,
			final ExecutorService service )
	{
		final ImageJVirtualStackARGB stack = ImageJVirtualStackARGB.wrap( img );
		stack.setExecutorService(service);
		return makeImagePlus( img, stack, title );
	}

	public static ImagePlus wrapRGB( final RandomAccessibleInterval< ARGBType > img, final String title )
	{
		return wrapRGB( img, title, null );
	}

	/**
	 * Create a 24bit RGB {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} a using a custom {@link Converter}.
	 */
	public static < T > ImagePlus wrapRGB( final RandomAccessibleInterval< T > img, final Converter< T, ARGBType > converter, final String title,
			final ExecutorService service )
	{
		return wrapRGB( Converters.convert( img, converter, new ARGBType() ), title, service );
	}

	public static < T > ImagePlus wrapRGB( final RandomAccessibleInterval< T > img, final Converter< T, ARGBType > converter, final String title )
	{
		return wrapRGB( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} as 24bit RGB {@link ImagePlus}
	 * using a custom {@link Converter}.
	 */
	public static < T > ImagePlus showRGB( final RandomAccessibleInterval< T > img, final Converter< T, ARGBType > converter, final String title,
			final ExecutorService service )
	{
		final ImagePlus imp = wrapRGB( img, converter, title, service );
		imp.show();
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();

		return imp;
	}

	public static < T > ImagePlus showRGB( final RandomAccessibleInterval< T > img, final Converter< T, ARGBType > converter, final String title )
	{
		return showRGB( img, converter, title, null );
	}

	/**
	 * Create a single channel 8-bit unsigned integer {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} using a custom {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus wrapUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final String title,
			final ExecutorService service )
	{
		final ImageJVirtualStackUnsignedByte stack = ImageJVirtualStackUnsignedByte.wrap( img );
		stack.setExecutorService( service );
		return makeImagePlus( img, stack, title );
	}

	public static < T extends RealType< T > > ImagePlus wrapUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final String title )
	{
		return wrapUnsignedByte( img, title, null );
	}

	/**
	 * Create a single channel 8-bit unsigned integer {@link ImagePlus} from a
	 * BitType {@link RandomAccessibleInterval} using a custom {@link Converter}
	 * .
	 */
	public static < T extends RealType< T > > ImagePlus wrapBit(
			final RandomAccessibleInterval< T > img,
			final String title,
			final ExecutorService service )
	{
		return wrapUnsignedByte( img, new RealUnsignedByteConverter< T >( 0, 1 ), title, service );
	}

	public static < T extends RealType< T > > ImagePlus wrapBit(
			final RandomAccessibleInterval< T > img,
			final String title )
	{
		return wrapBit( img, title, null );
	}

	/**
	 * Create a single channel 8-bit unsigned integer {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} using a custom {@link Converter}.
	 */
	public static < T > ImagePlus wrapUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedByteType > converter,
			final String title,
			final ExecutorService service )
	{
		return wrapUnsignedByte( Converters.convert( img, converter, new UnsignedByteType() ), title, service );
	}

	public static < T > ImagePlus wrapUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedByteType > converter,
			final String title )
	{
		return wrapUnsignedByte( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} as single channel 8-bit unsigned
	 * integer {@link ImagePlus} using a custom {@link Converter}.
	 */
	public static < T > ImagePlus showUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedByteType > converter,
			final String title,
			final ExecutorService service )
	{
		return showUnsignedByte( Converters.convert( img, converter, new UnsignedByteType() ), title, service );
	}

	public static < T > ImagePlus showUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedByteType > converter,
			final String title )
	{
		return showUnsignedByte( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} of {@link RealType} pixels as
	 * single channel 8-bit unsigned integer {@link ImagePlus} using a default
	 * {@link Converter} (clamp values to range [0, 255]).
	 */
	public static < T extends RealType< T > > ImagePlus showUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final String title,
			final ExecutorService service )
	{
		final ImagePlus imp = wrapUnsignedByte( img, title, service );
		imp.show();
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();
		return imp;
	}

	public static < T extends RealType< T > > ImagePlus showUnsignedByte(
			final RandomAccessibleInterval< T > img,
			final String title )
	{
		return showUnsignedByte( img, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} of {@link RealType} pixels as
	 * single channel 8-bit unsigned integer {@link ImagePlus} using a default
	 * {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus showUnsignedByte( final RandomAccessibleInterval< T > img,
			final ExecutorService service )
	{
		return showUnsignedByte( img, "Image " + ai.getAndIncrement(), service );
	}

	public static < T extends RealType< T > > ImagePlus showUnsignedByte( final RandomAccessibleInterval< T > img )
	{
		return showUnsignedByte( img, ( ExecutorService ) null );
	}

	/**
	 * Create a single channel 16-bit unsigned integer {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} using a default {@link Converter} (clamp
	 * values to range [0, 65535]).
	 */
	public static < T extends RealType< T > > ImagePlus wrapUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final String title,
			final ExecutorService service )
	{
		final ImageJVirtualStackUnsignedShort stack = ImageJVirtualStackUnsignedShort.wrap( img );
		stack.setExecutorService( service );
		return makeImagePlus( img, stack, title );
	}

	public static < T extends RealType< T > > ImagePlus wrapUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final String title )
	{
		return wrapUnsignedShort( img, title, null );
	}

	/**
	 * Create a single channel 16-bit unsigned integer {@link ImagePlus} from a
	 * {@link RandomAccessibleInterval} using a custom {@link Converter}.
	 */
	public static < T > ImagePlus wrapUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedShortType > converter,
			final String title,
			final ExecutorService service )
	{
		return wrapUnsignedShort( Converters.convert( img, converter, new UnsignedShortType() ), title, service );
	}

	public static < T > ImagePlus wrapUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedShortType > converter,
			final String title )
	{
		return wrapUnsignedShort( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} as single channel 16-bit unsigned
	 * integer {@link ImagePlus} using a custom {@link Converter}.
	 */
	public static < T > ImagePlus showUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedShortType > converter,
			final String title,
			final ExecutorService service )
	{
		return showUnsignedShort( Converters.convert( img, converter, new UnsignedShortType() ), title, service );
	}

	public static < T > ImagePlus showUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final Converter< T, UnsignedShortType > converter,
			final String title )
	{
		return showUnsignedShort( img, converter, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} of {@link RealType} pixels as
	 * single channel 16-bit unsigned integer {@link ImagePlus} using a default
	 * {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus showUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final String title,
			final ExecutorService service )
	{
		final ImagePlus imp = wrapUnsignedShort( img, title, service );
		imp.show();
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();
		return imp;
	}

	public static < T extends RealType< T > > ImagePlus showUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final String title )
	{
		return showUnsignedShort( img, title, null );
	}

	/**
	 * Show a {@link RandomAccessibleInterval} of {@link RealType} pixels as
	 * single channel 16-bit unsigned integer {@link ImagePlus} using a default
	 * {@link Converter}.
	 */
	public static < T extends RealType< T > > ImagePlus showUnsignedShort(
			final RandomAccessibleInterval< T > img,
			final ExecutorService service )
	{
		return showUnsignedShort( img, "Image " + ai.getAndIncrement(), service );
	}

	public static < T extends RealType< T > > ImagePlus showUnsignedShort(
			final RandomAccessibleInterval< T > img )
	{
		return showUnsignedShort( img, ( ExecutorService ) null );
	}

	/*
	 * public static <T extends Type<T>> ImagePlus copy( final Img<T> img,
	 * String title ) { }
	 */

}
