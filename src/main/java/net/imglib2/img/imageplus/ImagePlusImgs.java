/*
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

package net.imglib2.img.imageplus;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.complex.ComplexDoubleType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;

/**
 * Convenience factory methods for creation of {@link ImagePlusImg} instances
 * with the most common pixel {@link Type} variants. Keep in mind that this
 * cannot be a complete collection since the number of existing pixel
 * {@link Type}s may be extended.
 *
 * For pixel {@link Type}s T not present in this collection, use the generic
 * {@link ImagePlusImgFactory#create(long[], net.imglib2.type.NativeType)}, e.g.
 *
 * <pre>
 * img = new ImagePlusImgFactory&lt; MyType &gt;.create( new long[] { 100, 200 }, new MyType() );
 * </pre>
 *
 * @author Stephan Saalfeld
 */
final public class ImagePlusImgs
{
	private ImagePlusImgs()
	{}

	/**
	 * Create a {@link ByteImagePlus}{@code <}{@link UnsignedByteType}{@code >}.
	 *
	 * <p>
	 * (in ImageJ that would be a hyperstack of {@link ByteProcessor}s)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public ByteImagePlus< UnsignedByteType > unsignedBytes( final long... dim )
	{
		return ( ByteImagePlus< UnsignedByteType > ) new ImagePlusImgFactory<>( new UnsignedByteType() ).create( dim );
	}

	/**
	 * Create a {@link ByteImagePlus}{@code <}{@link ByteType}{@code >}.
	 *
	 * <p>
	 * (in ImageJ that would be a hyperstack of {@link ByteProcessor}s)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public ByteImagePlus< ByteType > bytes( final long... dim )
	{
		return ( ByteImagePlus< ByteType > ) new ImagePlusImgFactory<>( new ByteType() ).create( dim );
	}

	/**
	 * Create a
	 * {@link ShortImagePlus}{@code <}{@link UnsignedShortType}{@code >}.
	 *
	 * <p>
	 * (in ImageJ that would be a hyperstack of {@link ShortProcessor}s)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public ShortImagePlus< UnsignedShortType > unsignedShorts( final long... dim )
	{
		return ( ShortImagePlus< UnsignedShortType > ) new ImagePlusImgFactory<>( new UnsignedShortType() ).create( dim );
	}

	/**
	 * Create a {@link ShortImagePlus}{@code <}{@link ShortType}{@code >}.
	 *
	 * <p>
	 * (in ImageJ that would be a hyperstack of {@link ShortProcessor}s)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public ShortImagePlus< ShortType > shorts( final long... dim )
	{
		return ( ShortImagePlus< ShortType > ) new ImagePlusImgFactory<>( new ShortType() ).create( dim );
	}

	/**
	 * Create a {@link IntImagePlus}{@code <}{@link UnsignedIntType}{@code >}.
	 *
	 * <p>
	 * (In ImageJ that would be a hyperstack of {@link ColorProcessor}s. The
	 * integers, however, would be displayed as ARGB unsigned byte channels and
	 * thus look weird.)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public IntImagePlus< UnsignedIntType > unsignedInts( final long... dim )
	{
		return ( IntImagePlus< UnsignedIntType > ) new ImagePlusImgFactory<>( new UnsignedIntType() ).create( dim );
	}

	/**
	 * Create a {@link IntImagePlus}{@code <}{@link IntType}{@code >}.
	 *
	 * <p>
	 * (In ImageJ that would be a hyperstack of {@link ColorProcessor}s. The
	 * integers, however, would be displayed as ARGB unsigned byte channels and
	 * thus look weird.)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public IntImagePlus< IntType > ints( final long... dim )
	{
		return ( IntImagePlus< IntType > ) new ImagePlusImgFactory<>( new IntType() ).create( dim );
	}

	/**
	 * Create a {@link FloatImagePlus}{@code <}{@link FloatType}{@code >}.
	 *
	 * <p>
	 * (in ImageJ that would be a hyperstack of {@link FloatProcessor}s)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public FloatImagePlus< FloatType > floats( final long... dim )
	{
		return ( FloatImagePlus< FloatType > ) new ImagePlusImgFactory<>( new FloatType() ).create( dim );
	}

	/**
	 * Create an {@link IntImagePlus}{@code <}{@link ARGBType}{@code >}.
	 *
	 * <p>
	 * (in ImageJ that would be a hyperstack of {@link ColorProcessor}s)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public IntImagePlus< ARGBType > argbs( final long... dim )
	{
		return ( IntImagePlus< ARGBType > ) new ImagePlusImgFactory<>( new ARGBType() ).create( dim );
	}

	/**
	 * Create a
	 * {@link FloatImagePlus}{@code <}{@link ComplexFloatType}{@code >}.
	 *
	 * <p>
	 * (In ImageJ that would be a hyperstack of {@link FloatProcessor}s with
	 * real and imaginary numbers interleaved in the plane. That means it would
	 * look weird.)
	 * </p>
	 */
	@SuppressWarnings( "unchecked" )
	final static public FloatImagePlus< ComplexFloatType > complexFloats( final long... dim )
	{
		return ( FloatImagePlus< ComplexFloatType > ) new ImagePlusImgFactory<>( new ComplexFloatType() ).create( dim );
	}

	/**
	 * Create an {@link ImagePlusImg}{@code <}{@link ComplexDoubleType},
	 * {@link DoubleArray}{@code >}.
	 */
	final static public < T extends NumericType< T > & NativeType< T > > ImagePlusImg< T, ? > from( final ImagePlus imp )
	{
		return ImagePlusAdapter.< T >wrap( imp );
	}
}
