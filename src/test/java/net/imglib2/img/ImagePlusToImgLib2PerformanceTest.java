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
package net.imglib2.img;

import static junit.framework.TestCase.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.junit.Test;

import ij.ImagePlus;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class ImagePlusToImgLib2PerformanceTest
{

	@Test
	public void testIterateVirtualStack()
	{
		final AtomicInteger counter = new AtomicInteger();
		final ImagePlus image = countingImagePlus( counter );
		final Img< ? extends RealType< ? > > img = VirtualStackAdapter.wrapByte( image );
		runFlatIteration( img );
		assertTrue( counter.get() < 120 ); // don't call getProcessor too often
	}

	@Test
	public void testIteratePermutedVirtualStack()
	{
		final AtomicInteger counter = new AtomicInteger();
		final ImagePlus image = countingImagePlus( counter );
		final Img< ? extends RealType< ? > > img = VirtualStackAdapter.wrapByte( image );
		runFlatIteration( Views.permute( img, 0, 2 ) );
		assertTrue( counter.get() < 120 ); // don't call getProcessor too often
	}

	@Test
	public void testIterateImagePlusAdapter()
	{
		final AtomicInteger counter = new AtomicInteger();
		final ImagePlus image = countingImagePlus( counter );
		final Img< ? extends RealType< ? > > img = ImagePlusAdapter.wrapByte( image );
		runFlatIteration( Views.permute( img, 0, 2 ) );
		assertTrue( counter.get() < 120 ); // don't call getProcessor too often
	}

	private ImagePlus countingImagePlus( final AtomicInteger counter )
	{
		// NB: create an ImagePlus that counts how many ImageProcessors are
		// accessed
		final VirtualStack stack = new VirtualStack( 100, 100, 100 )
		{
			@Override
			public ImageProcessor getProcessor( final int n )
			{
				counter.incrementAndGet();
				return new ByteProcessor( getWidth(), getHeight() );
			}
		};
		return new ImagePlus( "title", stack );
	}

	private void runFlatIteration( final RandomAccessibleInterval< ? extends RealType< ? > > imgPlus )
	{
		for ( final RealType pixel : Views.flatIterable( imgPlus ) )
			;
	}

}
