/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2022 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
import net.imagej.ImgPlus;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

import java.util.AbstractList;
import java.util.List;

/**
 * Utility class to convert a {@link net.imglib2.img.cell.CellImg} (and SCIFIOCellImg,
 * CachedCellImg) to ImagePlus. It is restricted to planar cells (where each
 * cells contains exactly one image plane), and certain pixel types:
 * UnsignedByteType, UnsignedShortType, ARGBType and FloatType.
 *
 * @see ImgToVirtualStack
 * @see PlanarImgToVirtualStack
 * @see ArrayImgToVirtualStack
 */
public class CellImgToVirtualStack
{

	/**
	 * Returns true if {@link #wrap(ImgPlus)} supports the given image.
	 */
	public static boolean isSupported( ImgPlus< ? > imgPlus )
	{
		return isCellImgWithPlanarCells( imgPlus.getImg() ) &&
				PlanarImgToVirtualStack.isSupported( toPlanarImgPlus( imgPlus ) );
	}

	private static boolean isCellImgWithPlanarCells( Img< ? > imgPlus )
	{
		return ( imgPlus instanceof AbstractCellImg ) &&
				areCellsPlanar( ( ( AbstractCellImg ) imgPlus ).getCellGrid() );
	}

	private static boolean areCellsPlanar( CellGrid cellGrid )
	{
		for ( int i = 0; i < 2; i++ )
			if ( cellGrid.cellDimension( i ) < cellGrid.imgDimension( i ) )
				return false;
		for ( int i = 2; i < cellGrid.numDimensions(); i++ )
			if ( cellGrid.cellDimension( i ) != 1 && cellGrid.imgDimension( i ) != 1 )
				return false;
		return true;
	}

	/**
	 * Wraps the given image as {@link ImagePlus}. The given image must be backed
	 * by an {@link AbstractCellImg} with planar cells. The pixel type must be
	 * UnsignedByte-, UnsignedShort-, ARGB- or FloatType. First two axes must be
	 * X and Y (or unknown).
	 */
	public static ImagePlus wrap( ImgPlus< ? > imgPlus )
	{
		return PlanarImgToVirtualStack.wrap( toPlanarImgPlus( imgPlus ) );
	}

	private static < T > ImgPlus< T > toPlanarImgPlus( ImgPlus< T > image )
	{
		if ( !isCellImgWithPlanarCells( image.getImg() ) )
			throw new IllegalArgumentException( "ERROR: Image must be a CellImg, with planar cells." );
		return new ImgPlus< T >( toPlanar( ( AbstractCellImg ) image.getImg() ), image );
	}

	private static < T extends NativeType< T >, A extends ArrayDataAccess< A > > PlanarImg< ?, ? >
	toPlanar( AbstractCellImg< T, A, ?, ? > cellImage )
	{
		final long[] dim = Intervals.dimensionsAsLongArray( cellImage );
		final T type = Util.getTypeFromInterval( cellImage ).copy();
		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();
		List< A > slices = new SlicesList<>( cellImage );
		final PlanarImg< T, A > ts = new PlanarImg<>( slices, dim, entitiesPerPixel );
		ts.setLinkedType( ( T ) ( ( NativeTypeFactory ) type.getNativeTypeFactory() ).createLinkedType( ts ) );
		return ts;
	}

	private static class SlicesList< A extends ArrayDataAccess< ? > > extends AbstractList< A >
	{
		final RandomAccessible< ? extends Cell< A > > cells;

		final Dimensions gridDim;

		public SlicesList( AbstractCellImg< ?, A, ?, ? > cellImage )
		{
			cells = cellImage.getCells();
			gridDim = new FinalDimensions( cellImage.getCellGrid().getGridDimensions() );
		}

		@Override
		public A set( int index, A element )
		{
			final A destination = get( index );
			System.arraycopy(
					element.getCurrentStorageArray(), 0,
					destination.getCurrentStorageArray(), 0,
					Math.min( element.getArrayLength(), destination.getArrayLength() ) );
			return destination;
		}

		@Override
		public A get( int index )
		{
			RandomAccess< ? extends Cell< A > > ra = cells.randomAccess();
			IntervalIndexer.indexToPosition( index, gridDim, ra );
			return ra.get().getData();
		}

		@Override
		public int size()
		{
			return ( int ) Intervals.numElements( gridDim );
		}
	}
}
