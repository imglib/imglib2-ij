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
import ij.VirtualStack;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.PlanarAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.IntervalIndexer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PlanarImgToVirtualStack extends AbstractVirtualStack
{

	// static

	public static boolean isSupported( ImgPlus< ? > imgPlus )
	{
		imgPlus = ImgPlusViews.fixAxes( imgPlus );
		return imgPlus.getImg() instanceof PlanarImg &&
				checkAxisOrder( getAxes( imgPlus ) ) &&
				ImageProcessorUtils.isSupported( ( NativeType< ? > ) imgPlus.randomAccess().get() );
	}

	public static ImagePlus wrap( ImgPlus< ? > imgPlus )
	{
		imgPlus = ImgPlusViews.fixAxes( imgPlus );
		Img< ? > img = imgPlus.getImg();
		if ( !( img instanceof PlanarImg ) )
			throw new IllegalArgumentException( "Image must be a PlanarImg." );
		IntUnaryOperator indexer = getIndexer( imgPlus );
		VirtualStack stack = new PlanarImgToVirtualStack( ( PlanarImg< ?, ? > ) img, indexer );
		ImagePlus imagePlus = new ImagePlus( imgPlus.getName(), stack );
		imagePlus.setDimensions( dimension( imgPlus, Axes.CHANNEL ), dimension( imgPlus, Axes.Z ), dimension( imgPlus, Axes.TIME ) );
		CalibrationUtils.copyCalibrationToImagePlus( imgPlus, imagePlus );
		return imagePlus;
	}

	private static int dimension( ImgPlus imgPlus, AxisType axisType )
	{
		int index = imgPlus.dimensionIndex( axisType );
		return index < 0 ? 1 : ( int ) imgPlus.dimension( index );
	}

	public static VirtualStack wrap( PlanarImg< ?, ? > img )
	{
		return new PlanarImgToVirtualStack( img, x -> x - 1 );
	}

	// fields

	private final PlanarAccess< ? extends ArrayDataAccess< ? > > img;

	private final IntUnaryOperator indexer;

	// constructor

	private PlanarImgToVirtualStack( PlanarImg< ?, ? > img, IntUnaryOperator indexer )
	{
		super( ( int ) img.dimension( 0 ), ( int ) img.dimension( 1 ), initSize( img ), getBitDepth( img.randomAccess().get() ) );
		this.img = img;
		this.indexer = indexer;
	}

	private static int initSize( Interval interval )
	{
		return IntStream.range( 2, interval.numDimensions() ).map( x -> ( int ) interval.dimension( x ) ).reduce( 1, ( a, b ) -> a * b );
	}

	// public methods

	@Override
	public Object getPixels( int n )
	{
		return img.getPlane( indexer.applyAsInt( n ) ).getCurrentStorageArray();
	}

	// Helper methods

	private static int getBitDepth( Type< ? > type )
	{
		if ( type instanceof UnsignedByteType )
			return 8;
		if ( type instanceof UnsignedShortType )
			return 16;
		if ( type instanceof ARGBType )
			return 24;
		if ( type instanceof FloatType )
			return 32;
		throw new IllegalArgumentException( "unsupported type" );
	}

	private static IntUnaryOperator getIndexer( ImgPlus< ? > imgPlus )
	{
		List< AxisType > axes = getAxes( imgPlus );
		if ( !checkAxisOrder( axes ) )
			throw new IllegalArgumentException( "Unsupported axis order, first axis must be X, second axis must be Y, and then optionally, arbitrary ordered: channel, Z and time." );
		if ( inPreferredOrder( axes ) )
			return x -> x - 1;
		int[] stackSizes = { dimension( imgPlus, Axes.CHANNEL ), dimension( imgPlus, Axes.Z ), dimension( imgPlus, Axes.TIME ) };
		int channelSkip = getSkip( imgPlus, Axes.CHANNEL );
		int zSkip = getSkip( imgPlus, Axes.Z );
		int timeSkip = getSkip( imgPlus, Axes.TIME );
		return stackIndex -> {
			int[] stackPosition = new int[ 3 ];
			IntervalIndexer.indexToPosition( stackIndex - 1, stackSizes, stackPosition );
			return channelSkip * stackPosition[ 0 ] + zSkip * stackPosition[ 1 ] + timeSkip * stackPosition[ 2 ];
		};
	}

	private static List< AxisType > getAxes( ImgPlus< ? > img )
	{
		return IntStream.range( 0, img.numDimensions() ).mapToObj( img::axis ).map( CalibratedAxis::type ).collect( Collectors.toList() );
	}

	private static boolean checkAxisOrder( List< AxisType > axes )
	{
		return axes.size() >= 2 && axes.size() <= 5 && testUnique( axes ) &&
				axes.get( 0 ) == Axes.X && axes.get( 1 ) == Axes.Y &&
				axes.stream().allMatch( ALLOWED_AXES::contains );
	}

	private static final List< AxisType > ALLOWED_AXES = Arrays.asList( Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME );

	private static boolean inPreferredOrder( List< AxisType > axes )
	{
		for ( int i = 0; i < axes.size() - 1; i++ )
			if ( preferredPosition( axes.get( i ) ) >= preferredPosition( axes.get( i + 1 ) ) )
				return false;
		return true;
	}

	private static int preferredPosition( AxisType axisType )
	{
		if ( axisType == Axes.X )
			return 0;
		if ( axisType == Axes.Y )
			return 1;
		if ( axisType == Axes.CHANNEL )
			return 2;
		if ( axisType == Axes.Z )
			return 3;
		if ( axisType == Axes.TIME )
			return 4;
		throw new IllegalArgumentException( "unknown axis" );
	}

	private static int getSkip( ImgPlus< ? > imgPlus, AxisType axis )
	{
		int channelIndex = imgPlus.dimensionIndex( axis );
		return IntStream.range( 2, channelIndex ).map( i -> ( int ) imgPlus.dimension( i ) ).reduce( 1, ( a, b ) -> a * b );
	}

	private static < T > boolean testUnique( List< T > list )
	{
		Set< T > set = new HashSet<>( list );
		return set.size() == list.size();
	}

}
