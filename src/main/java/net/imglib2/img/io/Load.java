/*
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

package net.imglib2.img.io;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.UncheckedCache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.img.io.proxyaccess.ByteAccessProxy;
import net.imglib2.img.io.proxyaccess.FloatAccessProxy;
import net.imglib2.img.io.proxyaccess.IntAccessProxy;
import net.imglib2.img.io.proxyaccess.LongAccessProxy;
import net.imglib2.img.io.proxyaccess.ShortAccessProxy;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.view.Views;

public class Load
{	
	/** Return a {@link CachedCellImg} representation of the ordered list of file paths,
	 * with each file path pointing to an image that can be loaded with the {@link CacheLoader}.
	 * All images are expected to be of the same dimensions and of {@link NativeType}.
	 * Each image is loaded as a {@link Cell} of the {@link LazyCellImg}, or,
	 * in the case of {@link PlanarImg}, each stack plane is loaded as a {@link Cell}.
	 *
	 * For example, load a 4D volume by providing a list of file paths to the 3D volume of each time point.
	 * Can equally load a 3D volume by providing a list of file paths to the 2D images.
	 *
	 * The first image will be loaded to find out the dimensions, but it is cached.
	 *
	 * @param paths The ordered list of file paths, one per image to load.
	 * @param loader The reader that turns a file path into an {@link Img}.
	 */
	static public final < T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< A > >
	CachedCellImg< T, A > lazyStack(
			final List< String > paths,
			final CacheLoader< String, Img< T > > loader
			)
	{
		final UncheckedCache< Integer, Img< T > > loading_cache = new SoftRefLoaderCache< Integer, Img< T > >()
				.withLoader( i -> loader.get( paths.get( i ) ) )
				.unchecked();

		final Img< T > first = loading_cache.get( 0 );
		
		final long[] dimensions_all = new long[ first.numDimensions() + 1 ];
		first.dimensions( dimensions_all );
		dimensions_all[ dimensions_all.length - 1 ] = paths.size();
		
		final CacheLoader< Long, Cell< A > > cache_loader;
		final int[] dimensions_cell = new int[ first.numDimensions() + 1 ];

		if ( first instanceof PlanarImg )
		{
			@SuppressWarnings("unchecked")
			final int numSlices = ( ( PlanarImg< T, ? > )first ).numSlices();

			dimensions_cell[ 0 ] = ( int )first.dimension( 0 );
			dimensions_cell[ 1 ] = ( int )first.dimension( 1 );
			dimensions_cell[ 2 ] = 1; // single unit in Z
			dimensions_cell[ 3 ] = 1; // single unit in T

			cache_loader = new CacheLoader< Long, Cell< A > >()
			{
				@Override
				final public Cell< A > get( final Long index ) throws Exception {
					final int i = index.intValue();
					final int t = ( ( int )i ) / numSlices;
					final int z = ( ( int )i ) % numSlices;
					// Origin of coordinates for the Cell: 0,0,z,t
					final long[] min = new long[ first.numDimensions() + 1 ];
					min[ 2 ] = z;
					min[ 3 ] = t;
					@SuppressWarnings("unchecked")
					final PlanarImg< T, A > stack = ( PlanarImg< T, A > )loading_cache.get( t );
					return new Cell< A >( dimensions_cell, min, stack.getPlane( z ) );
				}
			};
		}
		else
		{
			for ( int d = 0; d < dimensions_cell.length -1; ++ d )
				dimensions_cell[ d ] = ( int )first.dimension( d );

			dimensions_cell[ dimensions_cell.length - 1 ] = 1;

			cache_loader = new CacheLoader< Long, Cell< A > >()
			{
				@Override
				final public Cell< A > get( final Long index ) throws Exception {
					final long[] min = new long[ first.numDimensions() + 1 ];
					min[ min.length - 1 ] = index;
					return new Cell< A >( dimensions_cell, min, extractDataAccess( loading_cache.get( index.intValue() ) ) );
				}
			};
		}
		
		final CachedCellImg< T, A > ccimg = new ReadOnlyCachedCellImgFactory().createWithCacheLoader(
				dimensions_all,
				first.firstElement().createVariable(),
				cache_loader,
				ReadOnlyCachedCellImgOptions.options().volatileAccesses( true ).cellDimensions( dimensions_cell ) );
		
		
		return ccimg;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static private final < T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< ? > > A extractDataAccess( final Img< T > img )
	{
		if ( ImgPlus.class.isAssignableFrom( img.getClass() ) )
			return extractDataAccess( ( ( ImgPlus< T > )img ).getImg() );

		if ( ArrayImg.class.isAssignableFrom( img.getClass() ) )
			return ( A )( ( ArrayImg< T, A > )img ).update( null );
		
		// If images are not of type ArrayImg, provide read/write proxy random access
		final T type = img.firstElement();
		
		if ( type instanceof GenericByteType )
			return ( A ) new ByteAccessProxy( img );
					
		if ( type instanceof GenericShortType )
			return ( A ) new ShortAccessProxy( img );
		
		if ( type instanceof GenericIntType )
			return ( A ) new IntAccessProxy( img );
		
		if ( type instanceof GenericLongType )
			return ( A ) new LongAccessProxy( img );
		
		if ( type instanceof RealType )
			return ( A ) new FloatAccessProxy( img );
		
		return null;
	}
	
	/**
	 * Convenient method that invokes {@link Load#lazyStack(List, CacheLoader)}
	 * with a cache-enabled {@link IJLoader}.
	 * 
	 * @see Load#lazyStack(List, CacheLoader)
	 * 
	 * @param paths The ordered list of file paths, one per image to load.
	 * @return a {@link LazyCellImg} that lazily loads each time point, one per file path.
	 */
	static public final < T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< A > > LazyCellImg< T, A > lazyStack( final String[] paths )
	{
		return Load.lazyStack( Arrays.asList( paths ), new IJLoader< T >() );
	}

	/**
	 * Convenient method that invokes {@link Load#lazyStack(List, CacheLoader)}
	 * with a cache-enabled {@link IJLoader}.
	 * 
	 * @see Load#lazyStack(List, CacheLoader)
	 * 
	 * @param paths The ordered list of file paths, one per image to load.
	 * @return a {@link LazyCellImg} that lazily loads each time point, one per file path.
	 */
	static public final < T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< A > > LazyCellImg< T, A > lazyStack( final List< String > paths )
	{
		return Load.lazyStack( paths, new IJLoader< T >() );
	}

	/**
	 * Return an {@link Img} representation of the ordered list of file paths,
	 * with each file path pointing to an image that can be loaded with the {@link CacheLoader}. 
	 * All images are expected to be of the same dimensions and of {@link NativeType}.
	 * Eager: loads all images right away.
	 * 
	 * @param paths The ordered list of file paths, one per image to load.
	 * @param loader The reader that turns a file path into an {@link Img}.
	 * @return
	 */
	static public final < T extends NumericType< T > & NativeType< T > > RandomAccessibleInterval< T > stack( final List< String > paths, final CacheLoader< String, Img< T > > loader )
	{
		return Views.stack( paths.stream().map( path -> {
			try {
				return loader.get( path );
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		} ).collect( Collectors.toList() ) );
	}

	/**
	 * @see Load#stack(List, CacheLoader)
	 */
	static public final < T extends NumericType< T > & NativeType< T > > RandomAccessibleInterval< T > stack( final String[] paths, final CacheLoader< String, Img< T > > loader )
	{
		return stack( Arrays.asList( paths ), loader );
	}
}
