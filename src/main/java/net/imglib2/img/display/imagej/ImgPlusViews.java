/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2018 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.imglib2.view.composite.Composite;

import java.util.function.IntUnaryOperator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: migrate to imagej-common
public class ImgPlusViews {

	/**
	 * Same as {@link Views#hyperSlice(RandomAccessible, int, long)}.
	 * But works on {@link ImgPlus} and manages axes information too.
	 */
	public static <T extends Type<T>> ImgPlus<T> hyperSlice(ImgPlus<T> image, int d, long position) {
		IntUnaryOperator axesMapping = i -> (i < d) ? i : i + 1;
		return newImgPlus( image, Views.hyperSlice( image.getImg(), d, position ), axesMapping );
	}

	/**
	 * Same as {@link Views#permute(RandomAccessible, int, int)}. But works on {@link ImgPlus}.
	 * But works on {@link ImgPlus} and manages axes information too.
	 */
	public static <T extends Type<T>> ImgPlus<T> permute(ImgPlus<T> image, int fromAxis, int toAxis) {
		if(fromAxis == toAxis)
			return image;
		IntUnaryOperator axesMapping = i -> {
			if(i == fromAxis) return toAxis;
			if(i == toAxis) return fromAxis;
			return i;
		};
		return newImgPlus( image, Views.permute( image.getImg(), fromAxis, toAxis ), axesMapping );
	}

	/**
	 * Permutes the axes of the image. One axis is moved, while the order of the other axes is preserved.
	 * If an image has axis order XYCZT, fromAxis=2 and toAxis=4, then the returned axis order is XYZTC.
	 */
	public static <T extends Type<T>> ImgPlus<T> moveAxis(ImgPlus<T> image, int fromAxis, int toAxis) {
		if(fromAxis == toAxis)
			return image;
		int direction = toAxis > fromAxis ? 1 : -1;
		ImgPlus<T> res = image;
		for ( int i = fromAxis; i != toAxis; i += direction)
			res = permute(res, i, i + direction);
		return res;
	}

	/**
	 * Indicates it {@link #fuseColor(ImgPlus)} can by used.
	 */
	public static boolean canFuseColor( ImgPlus< ? extends RealType< ? > > image) {
		int d = image.dimensionIndex( Axes.CHANNEL );
		return d >= 0 && image.dimension( d ) == 3;
	}

	/**
	 * Generates a color image from a gray scale image that has a color axes of dimension 3.
	 */
	public static ImgPlus< ARGBType > fuseColor( ImgPlus< ? extends RealType<?>> image) {
		int d = image.dimensionIndex( Axes.CHANNEL );
		RandomAccessibleInterval< ARGBType > colors = fuseColor( image.getImg(), d );
		IntUnaryOperator axisMapping = i -> ( i < d ) ? i : i + 1;
		return newImgPlus( image, colors, axisMapping );
	}

	private static RandomAccessibleInterval< ARGBType > fuseColor( Img< ? extends RealType< ? > > image, int d )
	{
		if ( d < 0 || image.dimension( d ) != 3 )
			throw new IllegalArgumentException();
		return Converters.convert(
				Views.collapse( moveAxis( image, d, image.numDimensions() - 1 ) ),
				ImgPlusViews::convertToColor,
				new ARGBType()
		);
	}

	/**
	 * Change the axis types of an image, such that each axis is uniquely typed as X, Y, Z, channel or time.
	 * Existing unique axis of type: X, Y, Z, channel or time are preserved.
	 */
	public static <T> ImgPlus<T> fixAxes( ImgPlus< T > in ) {
		List< AxisType > newAxisTypes = fixAxes( getAxes( in ) );
		CalibratedAxis[] newAxes = IntStream.range(0, in.numDimensions() ).mapToObj( i -> {
			CalibratedAxis newAxis = in.axis( i ).copy();
			newAxis.setType( newAxisTypes.get( i ) );
			return newAxis;
		} ).toArray( CalibratedAxis[]::new );
		return new ImgPlus< T >( in.getImg(), in.getName(), newAxes );
	}

	// -- Helper methods --

	private static < T extends Type< T > > ImgPlus< T > newImgPlus( ImgPlus< ? > image, RandomAccessibleInterval< T > newContent, IntUnaryOperator axesMapping )
	{
		T type = Util.getTypeFromInterval( newContent );
		Img< T > newImg = ImgView.wrap( newContent, image.factory().imgFactory( type ) );
		ImgPlus< T > result = new ImgPlus<>( newImg, image.getName() );
		for ( int i = 0; i < result.numDimensions(); i++ )
			result.setAxis( image.axis( axesMapping.applyAsInt( i ) ).copy(), i );
		return result;
	}

	// TODO: move to imglib2 Views
	private static < T > RandomAccessibleInterval< T > moveAxis( RandomAccessibleInterval< T > image, int fromAxis, int toAxis )
	{
		if ( fromAxis == toAxis )
			return image;
		int direction = toAxis > fromAxis ? 1 : -1;
		RandomAccessibleInterval< T > res = image;
		for ( int i = fromAxis; i != toAxis; i += direction )
			res = Views.permute( res, i, i + direction );
		return res;
	}

	private static void convertToColor( Composite< ? extends RealType< ? > > in, ARGBType out )
	{
		out.set( ARGBType.rgba( toInt( in.get( 0 ) ), toInt( in.get( 1 ) ), toInt( in.get( 2 ) ), 255 ) );
	}

	private static int toInt( RealType< ? > realType )
	{
		return ( int ) realType.getRealFloat();
	}

	private static final List<AxisType > imagePlusAxisOrder =
			Arrays.asList( Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME );

	private static List< AxisType > fixAxes(List<AxisType> in) {
		List<AxisType> unusedAxis = new ArrayList<>(imagePlusAxisOrder);
		unusedAxis.removeAll(in);
		Predicate<AxisType> isDuplicate = createIsDuplicatePredicate();
		Predicate<AxisType> replaceIf = axis -> isDuplicate.test(axis) || !imagePlusAxisOrder.contains( axis );
		Iterator< AxisType > iterator = unusedAxis.iterator();
		Supplier< AxisType > replacements = () -> iterator.hasNext() ? iterator.next() : Axes.unknown();
		return replaceMatches(in, replaceIf, replacements );
	}

	// NB: Package-private to allow tests.
	static List< AxisType > getAxes( ImgPlus< ? > in )
	{
		return IntStream.range(0, in.numDimensions())
				.mapToObj(in::axis).map(CalibratedAxis::type )
				.collect( Collectors.toList() );
	}

	// NB: Package-private to allow tests.
	static < T > Predicate< T > createIsDuplicatePredicate() {
		Set<T> before = new HashSet<>();
		return element -> {
			boolean isDuplicate = before.contains(element);
			if(!isDuplicate) before.add(element);
			return isDuplicate;
		};
	}

	// NB: Package-private to allow tests.
	static < T > List< T > replaceMatches( List< T > in, Predicate< T > predicate, Supplier< T > replacements ) {
		return in.stream().map( value -> predicate.test(value) ? replacements.get() : value ).collect( Collectors.toList());
	}
}
