/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2023 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

package net.imglib2.img.imageplus;

import net.imglib2.Dimensions;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

/**
 * Factory that creates an appropriate {@link ImagePlusImg}.
 *
 * @author Jan Funke
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 * @author Johannes Schindelin
 * @author Tobias Pietzsch
 */
public class ImagePlusImgFactory< T extends NativeType< T > > extends PlanarImgFactory< T >
{
	public ImagePlusImgFactory( final T type )
	{
		super( type );
	}

	@Override
	public ImagePlusImg< T, ? > create( final long... dimensions )
	{
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ImagePlusImg< T, ? > img = create( dimensions, type(), ( NativeTypeFactory ) type().getNativeTypeFactory() );
		return img;
	}

	@Override
	public ImagePlusImg< T, ? > create( final Dimensions dimensions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ) );
	}

	@Override
	public ImagePlusImg< T, ? > create( final int[] dimensions )
	{
		return create( Util.int2long( dimensions ) );
	}

	@SuppressWarnings( "unchecked" )
	private < A extends ArrayDataAccess< A > > ImagePlusImg< T, ? > create(
			final long[] dimensions,
			final T type,
			final NativeTypeFactory< T, A > typeFactory )
	{
		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();
		final ImagePlusImg< T, A > img;
		switch ( typeFactory.getPrimitiveType() )
		{
		case BYTE:
			img = new ByteImagePlus( dimensions, entitiesPerPixel );
			break;
		case FLOAT:
			img = new FloatImagePlus( dimensions, entitiesPerPixel );
			break;
		case INT:
			img = new IntImagePlus( dimensions, entitiesPerPixel );
			break;
		case SHORT:
			img = new ShortImagePlus( dimensions, entitiesPerPixel );
			break;
		default:
			img = new ImagePlusImg<>( ArrayDataAccessFactory.get( typeFactory ), dimensions, entitiesPerPixel );
		}
		img.setLinkedType( typeFactory.createLinkedType( img ) );
		return img;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
	{
		if ( NativeType.class.isInstance( type ) )
			return new ImagePlusImgFactory( ( NativeType ) type );
		throw new IncompatibleTypeException( this, type.getClass().getCanonicalName() + " does not implement NativeType." );
	}

	/*
	 * -----------------------------------------------------------------------
	 *
	 * Deprecated API.
	 *
	 * Supports backwards compatibility with ImgFactories that are constructed
	 * without a type instance or supplier.
	 *
	 * -----------------------------------------------------------------------
	 */

	@Deprecated
	public ImagePlusImgFactory()
	{
		super();
	}

	@Deprecated
	@Override
	public ImagePlusImg< T, ? > create( final long[] dimensions, final T type )
	{
		cache( type );
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ImagePlusImg< T, ? > img = create( dimensions, type, ( NativeTypeFactory ) type.getNativeTypeFactory() );
		return img;
	}

}
