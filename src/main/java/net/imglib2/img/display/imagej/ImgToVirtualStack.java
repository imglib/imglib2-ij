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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

import ij.ImagePlus;
import ij.ImageStack;

public class ImgToVirtualStack
{
	// TODO move to image-legacy
	public static ImagePlus wrap( final ImgPlus< ? extends RealType< ? > > imgPlus, final boolean mergeRGB )
	{
		final ImgPlus< ? > imgPlus2 = mergeRGB && ImgPlusViews.canFuseColor( imgPlus ) ? ImgPlusViews.fuseColor( imgPlus ) : imgPlus;
		return wrap( imgPlus2 );
	}

	/**
	 * Wraps an {@link ImgPlus} into an {@link ImagePlus}. The image can be
	 * {@link RealType} or {@link ARGBType}. The {@link ImagePlus} is backed by
	 * a special {@link ij.VirtualStack}, which copies an plane from the given
	 * image, instead of it plane from a file.
	 * <p>
	 * Only up to five dimensions are support. Axes can might be arbitrary. The
	 * image title and calibration are derived from the given image.
	 *
	 * @see ArrayImgToVirtualStack
	 * @see ImgToVirtualStack
	 */
	public static ImagePlus wrap( final ImgPlus< ? > imgPlus )
	{
		return wrap( imgPlus, ImgToVirtualStack::createVirtualStack );
	}

	/**
	 * Similar to {@link #wrap(ImgPlus)}, but works only for {@link ImgPlus} of
	 * {@link BitType}. The pixel values of 0 and 1 are scaled to 0 and 255.
	 *
	 * @see ArrayImgToVirtualStack
	 * @see ImgToVirtualStack
	 */
	public static ImagePlus wrapAndScaleBitType( final ImgPlus< BitType > imgPlus )
	{
		return wrap( imgPlus, ImgToVirtualStack::createVirtualStackBits );
	}

	private static < T > ImagePlus wrap( ImgPlus< T > imgPlus, final Function< RandomAccessibleInterval< T >, ImageJVirtualStack<?> > imageStackWrapper )
	{
		imgPlus = ImgPlusViews.fixAxes( imgPlus );
		final RandomAccessibleInterval< T > sorted = ensureXYCZT( imgPlus );
		final ImageJVirtualStack<?> stack = imageStackWrapper.apply( sorted );
		final ImagePlus result = new ImagePlus( imgPlus.getName(), stack );
		// NB: setWritable after the ImagePlus is created. Otherwise a useless stack.setPixels(...) call would be performed.
		stack.setWritable( true );
		result.setDimensions( ( int ) sorted.dimension( 2 ), ( int ) sorted.dimension( 3 ), ( int ) sorted.dimension( 4 ) );
		CalibrationUtils.copyCalibrationToImagePlus( imgPlus, result );
		return result;
	}

	private static ImageJVirtualStack<?> createVirtualStackBits( final RandomAccessibleInterval< BitType > sorted )
	{
		return ImageJVirtualStackUnsignedByte.wrapAndScaleBitType( sorted );
	}

	private static ImageJVirtualStack<?> createVirtualStack( final RandomAccessibleInterval< ? > rai )
	{
		final Object type = rai.randomAccess().get();
		if ( type instanceof RealType )
			return createVirtualStackRealType( cast( rai ) );
		if ( type instanceof ARGBType )
			return ImageJVirtualStackARGB.wrap( cast( rai ) );
		throw new IllegalArgumentException( "Unsupported type" );
	}

	private static < T > T cast( final Object in )
	{
		@SuppressWarnings( "unchecked" )
		final
		T out = ( T ) in;
		return out;
	}

	private static ImageJVirtualStack< ? > createVirtualStackRealType( final RandomAccessibleInterval< ? extends RealType< ? > > rai )
	{
		final RealType< ? extends RealType< ? > > type = rai.randomAccess().get();
		final int bitDepth = type.getBitsPerPixel();
		final boolean isSigned = type.getMinValue() < 0;

		if ( bitDepth <= 8 && !isSigned )
			return ImageJVirtualStackUnsignedByte.wrap( rai );
		if ( bitDepth <= 16 && !isSigned )
			return ImageJVirtualStackUnsignedShort.wrap( rai );

		// other types translated as 32-bit float data
		return ImageJVirtualStackFloat.wrap( rai );
	}

	private static < T > RandomAccessibleInterval< T > ensureXYCZT( final ImgPlus< T > imgPlus )
	{
		final int[] axes = getPermutation( getAxes( imgPlus ) );
		return permute( imgPlus, axes );
	}

	private static int[] getPermutation( final List< AxisType > axes )
	{
		return axes.stream().mapToInt( axis -> {
			final int index = imagePlusAxisOrder.indexOf( axis );
			if ( index < 0 )
				throw new IllegalArgumentException( "Unsupported axis type: " + axis );
			return index;
		} ).toArray();
	}

	private static List< AxisType > getAxes( final ImgPlus< ? > imgPlus )
	{
		return IntStream.range( 0, imgPlus.numDimensions() )
				.mapToObj( i -> imgPlus.axis( i ).type() )
				.collect( Collectors.toList() );
	}

	private static < T > RandomAccessibleInterval< T > permute( final ImgPlus< T > imgPlus, int[] axes )
	{
		boolean inNaturalOrder = true;
		final boolean[] matchedDimensions = new boolean[ 5 ];
		final long[] min = new long[ 5 ], max = new long[ 5 ];
		for ( int d = 0; d < axes.length; d++ )
		{
			final int index = axes[ d ];
			matchedDimensions[ index ] = true;
			min[ index ] = imgPlus.min( d );
			max[ index ] = imgPlus.max( d );
			if ( index != d )
				inNaturalOrder = false;
		}

		if ( imgPlus.numDimensions() != 5 )
			inNaturalOrder = false;
		if ( inNaturalOrder )
			return imgPlus;

		axes = Arrays.copyOf( axes, 5 );
		RandomAccessibleInterval< T > rai = imgPlus;
		// pad the image to at least 5D
		for ( int i = 0; i < 5; i++ )
		{
			if ( matchedDimensions[ i ] )
				continue;
			axes[ rai.numDimensions() ] = i;
			min[ i ] = 0;
			max[ i ] = 0;
			rai = Views.addDimension( rai, 0, 0 );
		}

		// permute the axis order to XYCZT...
		final MixedTransform t = new MixedTransform( rai.numDimensions(), 5 );
		t.setComponentMapping( axes );
		return Views.interval( new MixedTransformView<>( rai, t ), min, max );
	}

	private static final List< AxisType > imagePlusAxisOrder =
			Arrays.asList( Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME );
}
