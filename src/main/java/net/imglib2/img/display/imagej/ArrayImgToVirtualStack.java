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
import ij.process.ImageProcessor;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ArrayImgToVirtualStack
{
	private ArrayImgToVirtualStack()
	{
		// prevent from instantiation
	}

	public static boolean isSupported( ImgPlus< ? > imgPlus )
	{
		return imgPlus.getImg() instanceof ArrayImg &&
				imgPlus.numDimensions() == 2 &&
				checkAxis( getAxes( imgPlus ) ) &&
				ImageProcessorUtils.isSupported( ( NativeType< ? > ) imgPlus.randomAccess().get() );
	}

	public static ImagePlus wrap( ImgPlus< ? > imgPlus )
	{
		Img< ? > img = imgPlus.getImg();
		if ( !( img instanceof ArrayImg ) )
			throw new IllegalArgumentException( "Expecting ArrayImg" );
		ArrayImg< ?, ArrayDataAccess< ? > > arrayImg = ( ArrayImg< ?, ArrayDataAccess< ? > > ) img;
		int sizeX = ( int ) img.dimension( 0 );
		int sizeY = ( int ) img.dimension( 1 );
		Object pixels = arrayImg.update( null ).getCurrentStorageArray();
		ImageProcessor processor = ImageProcessorUtils.initProcessor( sizeX, sizeY, pixels, null );
		return new ImagePlus( imgPlus.getName(), processor );
	}

	private static boolean checkAxis( List< AxisType > axes )
	{
		return axes.size() == 2 && axes.get( 0 ) == Axes.X && axes.get( 1 ) == Axes.Y;
	}

	private static List< AxisType > getAxes( ImgPlus< ? > img )
	{
		return IntStream.range( 0, img.numDimensions() ).mapToObj( img::axis ).map( CalibratedAxis::type ).collect( Collectors.toList() );
	}

}
