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

package net.imglib2.test;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.util.Random;
import java.util.function.Consumer;

public class RandomImgs
{
	// TODO generalize & move to imglib2
	public static < T extends NativeType< T > > Img< T > randomImage( T type, long... dims )
	{
		Img< T > expected = new ArrayImgFactory< T >().create( dims, type );
		return setRandomValues( expected );
	}

	private static < I extends RandomAccessibleInterval< T >, T extends NativeType< T > >
	I setRandomValues( I image )
	{
		T type = Util.getTypeFromInterval( image );
		Views.iterable( image ).forEach( randomSetter( type ) );
		return image;
	}

	private static < T extends NativeType< T > > Consumer< T > randomSetter( T type )
	{
		Random random = new Random();
		if ( type instanceof UnsignedByteType )
			return b -> ( ( UnsignedByteType ) b ).setInteger( random.nextInt( ( 2 << 8 ) - 1 ) );
		if ( type instanceof UnsignedShortType )
			return b -> ( ( UnsignedShortType ) b ).setInteger( random.nextInt( ( 2 << 16 ) - 1 ) );
		if ( type instanceof IntType )
			return b -> ( ( IntType ) b ).setInteger( random.nextInt() );
		if ( type instanceof ARGBType )
			return b -> ( ( ARGBType ) b ).set( random.nextInt() );
		if ( type instanceof FloatType )
			return b -> ( ( FloatType ) b ).setReal( random.nextFloat() );
		if ( type instanceof DoubleType )
			return b -> ( ( DoubleType ) b ).setReal( random.nextDouble() );
		throw new UnsupportedOperationException();
	}
}
