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
package net.imglib2.ij.display.imagej;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.ij.display.imagej.ImgPlusViews;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

public class ImgPlusViewsTest
{

	@Test
	public void testHyperSlice()
	{
		final ArrayImg< UnsignedByteType, ByteArray > img = ArrayImgs.unsignedBytes( 1, 1, 1, 1 );
		final ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( img, "image", new AxisType[] { Axes.X, Axes.Y, Axes.Z, Axes.TIME } );
		final ImgPlus< UnsignedByteType > result = ImgPlusViews.hyperSlice( imgPlus, 2, 0 );
		assertEquals( 3, result.numDimensions() );
		assertEquals( Axes.X, result.axis( 0 ).type() );
		assertEquals( Axes.Y, result.axis( 1 ).type() );
		assertEquals( Axes.TIME, result.axis( 2 ).type() );
	}

	@Test
	public void testFixAxes()
	{
		final AxisType[] in = { Axes.X, Axes.unknown(), Axes.Y, Axes.Z, Axes.Y };
		final List< AxisType > expected = Arrays.asList( Axes.X, Axes.CHANNEL, Axes.Y, Axes.Z, Axes.TIME );
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( 1, 1, 1, 1, 1 );
		final ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( img, "test", in );
		final ImgPlus< UnsignedByteType > result = ImgPlusViews.fixAxes( imgPlus );
		assertEquals( expected, ImgPlusViews.getAxes( result ) );
	}

	@Test
	public void testReplaceDuplicates()
	{
		final Predicate< Integer > isDuplicate = ImgPlusViews.createIsDuplicatePredicate();
		assertFalse( isDuplicate.test( 1 ) );
		assertTrue( isDuplicate.test( 1 ) );
		assertFalse( isDuplicate.test( 2 ) );
	}

	@Test
	public void testReplaceNulls()
	{
		final List< AxisType > in = Arrays.asList( Axes.X, null, Axes.Y, Axes.Z, null );
		final List< AxisType > expected = Arrays.asList( Axes.X, Axes.CHANNEL, Axes.Y, Axes.Z, Axes.TIME );
		final Supplier< AxisType > replacements = Arrays.asList( Axes.CHANNEL, Axes.TIME ).iterator()::next;
		final List< AxisType > result = ImgPlusViews.replaceMatches( in, Objects::isNull, replacements );
		assertEquals( expected, result );
	}
}
