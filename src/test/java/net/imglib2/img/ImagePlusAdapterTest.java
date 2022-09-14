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

package net.imglib2.img;

import static org.junit.Assert.assertEquals;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

import org.junit.Test;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.measure.Calibration;

public class ImagePlusAdapterTest< T extends NumericType< T > & NativeType< T > >
{

	/** Which dimensions to test. */
	final int[][] dim = new int[][] {
			//  nX		nY		nC		nZ		nT
			{ 	128, 	128, 	1,		1, 		1  },   		// 2D
			{ 	128, 	128, 	1, 		10, 	1  },   		// 3D
			{ 	128, 	128, 	5, 		10, 	1  },   		// 3D over 5 channels
			{ 	128, 	128, 	1,		10, 	30 }, 			// 4D
			{ 	128, 	128, 	5,		10, 	30 }, 			// 4D over 5 channels
			{ 	128, 	128, 	1,		1, 		30 }, 			// 2D + T
			{ 	128, 	128, 	5,		1, 		30 } 			// 2D + T over 5 channels

	};

	/** Corresponding calibrations. */
	final float[][] calibration = new float[][] {
			//	X		Y		C (ignored)		Z			T
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 },
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 },
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 },
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 },
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 },
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 },
			{	0.2f,	0.2f,	Float.NaN,		1.5f,		2 }

	};

	final String[] units = new String[] { "um", "mm", "cm", "minutes" };

	@Test
	public void testDimensionality()
	{
		for ( int i = 0; i < dim.length; i++ )
			testDimensionality( dim[ i ], calibration[ i ] );
	}

	private void testDimensionality( final int[] dim, final float[] calibration )
	{
		final ImagePlus imp = createCalibratedImagePlus( dim, calibration );
		final ImgPlus< T > img = ImagePlusAdapter.wrapImgPlus( imp );
		// Print stuff
//		System.out.println( "got: " + img.getName() );
//		for ( int d = 0; d < img.numDimensions(); d++ )
//		{
//			System.out.println( "    Axis " + d + "\t - " + img.axis( d ) + ", spacing = " + img.calibration( d ) + ", dimension = " + img.dimension( d ) );
//		}
		assertEquals( getExpectedNumDimensions( dim ), img.numDimensions() );
		checkDimensionality( dim, img );
		checkCalibration( dim, calibration, img );
	}

	private ImagePlus createCalibratedImagePlus( final int[] dim, final float[] calibration )
	{
		// Create ImagePlus
		final int slices = dim[ 2 ] * dim[ 3 ] * dim[ 4 ];
		final ImagePlus imp = NewImage.createByteImage( "Test", dim[ 0 ], dim[ 1 ], slices, NewImage.FILL_BLACK );
		imp.setDimensions( dim[ 2 ], dim[ 3 ], dim[ 4 ] );

		// Set calibration
		final Calibration impCal = imp.getCalibration();
		impCal.pixelWidth = calibration[ 0 ];
		impCal.pixelHeight = calibration[ 1 ];
		// 2 is for channels
		impCal.pixelDepth = calibration[ 3 ];
		impCal.frameInterval = calibration[ 4 ];
		impCal.setXUnit( units[ 0 ] );
		impCal.setYUnit( units[ 1 ] );
		impCal.setZUnit( units[ 2 ] );
		impCal.setTimeUnit( units[ 3 ] );

		// Print stuff
//		System.out.println( "\nFor ImagePlus " + imp + " with " + imp.getCalibration() );
		return imp;
	}

	private int getExpectedNumDimensions( final int[] dim )
	{
		// Are num dimension correct?
		int expectedNumDimensions = 0;
		for ( int d = 0; d < dim.length; d++ )
		{
			if ( dim[ d ] > 1 )
				expectedNumDimensions++;
		}
		return expectedNumDimensions;
	}

	private void checkDimensionality( final int[] dim, final ImgPlus< T > img )
	{
		int skipDim = 0;
		for ( int d = 0; d < dim.length; d++ )
		{
			if ( dim[ d ] > 1 )
			{
				// imglib skips singleton dimensions, so we must test only
				// against non-singleton dimension
				assertEquals(
						String.format( "For dimension %d,  expected %d, but got %d.", d, dim[ d ], img.dimension( skipDim ) ),
						dim[ d ], img.dimension( skipDim ) );
				skipDim++;
			}
		}
	}

	private void checkCalibration( final int[] dim, final float[] calibration, final ImgPlus< T > img )
	{
		int skipDim = 0;
		for ( int d = 0; d < calibration.length; d++ )
		{
			if ( dim[ d ] > 1 )
			{
				// Is it the channel axis?
				if ( d < getExpectedNumDimensions( dim ) && img.axis( d ).type() == Axes.CHANNEL )
				{

					// Then the calibration should be 1,
					assertEquals( 1f, img.averageScale( skipDim ),
							Float.MIN_VALUE );

				}
				else
				{

					// otherwise it should be what we set.
					assertEquals( calibration[ d ], img.averageScale( skipDim ),
							Float.MIN_VALUE );
				}
				skipDim++;

			}
		}
	}
}
