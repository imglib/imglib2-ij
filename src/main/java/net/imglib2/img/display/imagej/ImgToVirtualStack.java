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
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImgToVirtualStack
{
	public static ImagePlus wrap( ImgPlus< ? extends RealType< ? > > imgPlus, boolean mergeRGB )
	{
		ImgPlus< ? > imgPlus2 = mergeRGB && ImgPlusViews.canFuseColor( imgPlus ) ? ImgPlusViews.fuseColor( imgPlus ) : imgPlus;
		return wrap( imgPlus2 );
	}

	public static ImagePlus wrap( ImgPlus< ? > imgPlus )
	{
		imgPlus = ImgPlusViews.fixAxes( imgPlus );
		RandomAccessibleInterval<?> sorted = ensureXYCZT(imgPlus);
		ImageStack stack = createVirtualStack(sorted);
		ImagePlus result = new ImagePlus( imgPlus.getName(), stack );
		result.setDimensions( (int) sorted.dimension(2), (int) sorted.dimension(3), (int) sorted.dimension(4) );
		CalibrationUtils.copyCalibrationToImagePlus(imgPlus, result);
		return result;
	}

	private static ImageStack createVirtualStack( RandomAccessibleInterval< ? > rai )
	{
		final Object type = rai.randomAccess().get();
		if( type instanceof RealType )
		{
			ImageJVirtualStack< ?, ? > result = createVirtualStackRealType( cast( rai ) );
			result.setWritable( true );
			return result;
		}
		if( type instanceof ARGBType )
			return ImageJVirtualStackARGB.wrap( cast( rai ) );
		throw new IllegalArgumentException( "Unsupported type" );
	}

	private static <T> T cast( Object in ) {
		@SuppressWarnings( "unchecked" )
		T out = ( T ) in;
		return out;
	}

	private static ImageJVirtualStack< ?, ? > createVirtualStackRealType( RandomAccessibleInterval< ? extends RealType< ? > > rai )
	{
		final RealType<? extends RealType<?>> type = rai.randomAccess().get();
		final int bitDepth = type.getBitsPerPixel();
		final boolean isSigned = type.getMinValue() < 0;

		if (bitDepth <= 8 && !isSigned)
			return ImageJVirtualStackUnsignedByte.wrap( rai );
		if (bitDepth <= 16 && !isSigned)
			return ImageJVirtualStackUnsignedShort.wrap( rai );

		// other types translated as 32-bit float data
		return ImageJVirtualStackFloat.wrap( rai );
	}

	private static <T> RandomAccessibleInterval<T> ensureXYCZT(final ImgPlus<T> imgPlus) {
		final int[] axes = getPermutation(getAxes(imgPlus));
		return permute(imgPlus, axes);
	}

	private static int[] getPermutation(List<AxisType> axes) {
		return axes.stream().mapToInt(axis -> {
			int index = imagePlusAxisOrder.indexOf(axis);
			if (index < 0)
				throw new IllegalArgumentException("Unsupported axis type: " + axis);
			return index;
		}).toArray();
	}

	private static List<AxisType> getAxes(ImgPlus<?> imgPlus) {
		return IntStream.range(0, imgPlus.numDimensions())
				.mapToObj(i -> imgPlus.axis(i).type())
				.collect(Collectors.toList());
	}

	private static <T> RandomAccessibleInterval<T> permute(ImgPlus<T> imgPlus, int[] axes) {
		boolean inNaturalOrder = true;
		final boolean[] matchedDimensions = new boolean[5];
		final long[] min = new long[5], max = new long[5];
		for (int d = 0; d < axes.length; d++) {
			int index = axes[d];
			matchedDimensions[index] = true;
			min[index] = imgPlus.min(d);
			max[index] = imgPlus.max(d);
			if (index != d) inNaturalOrder = false;
		}

		if (imgPlus.numDimensions() != 5) inNaturalOrder = false;
		if (inNaturalOrder) return imgPlus;

		axes = Arrays.copyOf(axes, 5);
		RandomAccessibleInterval<T> rai = imgPlus;
		// pad the image to at least 5D
		for (int i = 0; i < 5; i++) {
			if (matchedDimensions[i]) continue;
			axes[rai.numDimensions()] = i;
			min[i] = 0;
			max[i] = 0;
			rai = Views.addDimension(rai, 0, 0);
		}

		// permute the axis order to XYCZT...
		final MixedTransform t = new MixedTransform(rai.numDimensions(), 5);
		t.setComponentMapping(axes);
		return Views.interval(new MixedTransformView< >( rai, t ), min, max);
	}

	private static final List<AxisType > imagePlusAxisOrder =
			Arrays.asList(Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME);
}
