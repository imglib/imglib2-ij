/*-
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
package net.imglib2.img;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.imageplus.ByteImagePlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import ij.ImagePlus;
import ij.gui.NewImage;

@State( Scope.Benchmark )
public class ImagePlusToImgLib2Benchmark
{
	private final ImagePlus small = NewImage.createByteImage( "deep", 10, 10, 10, NewImage.FILL_BLACK );
	private final ImagePlus deep = NewImage.createByteImage( "deep", 10, 10, 1000000, NewImage.FILL_BLACK );
	private final ImagePlus wide = NewImage.createByteImage( "deep", 1000, 1000, 1000, NewImage.FILL_BLACK );
	private final ImagePlus imageForIteration = NewImage.createByteImage( "deep", 10, 10, 1000, NewImage.FILL_BLACK );

	@Benchmark
	public void wrapSmall()
	{
		VirtualStackAdapter.wrap( small );
	}

	@Benchmark
	public void wrapDeep()
	{
		VirtualStackAdapter.wrap( deep );
	}

	@Benchmark
	public void wrapWide()
	{
		VirtualStackAdapter.wrap( wide );
	}

	@Benchmark
	public void wrapSmallOld()
	{
		ImagePlusAdapter.wrap( small );
	}

	@Benchmark
	public void wrapDeepOld()
	{
		ImagePlusAdapter.wrap( deep );
	}

	@Benchmark
	public void wrapWideOld()
	{
		ImagePlusAdapter.wrap( wide );
	}

	private final ImgPlus< UnsignedByteType > wrapped = VirtualStackAdapter.wrapByte( imageForIteration );
	private final ByteImagePlus< UnsignedByteType > wrappedOld = ImagePlusAdapter.wrapByte( imageForIteration );

	@Benchmark
	public void iterateWrapped()
	{
		flatIterate( wrapped );
	}

	@Benchmark
	public void iterateWrappedOld()
	{
		flatIterate( wrappedOld );
	}

	@Benchmark
	public void iteratePermutedWrapped()
	{
		flatIterate( Views.permute( wrapped, 0, 2 ) );
	}

	@Benchmark
	public void iteratePermutedWrappedOld()
	{
		flatIterate( Views.permute( wrappedOld, 0, 2 ) );
	}

	private < T > void flatIterate( final RandomAccessibleInterval< T > image )
	{
		for ( final T pixel : Views.flatIterable( image ) )
			;
	}

	public static void main( final String... args ) throws RunnerException
	{
		final Options opt = new OptionsBuilder()
				.include( ImagePlusToImgLib2Benchmark.class.getSimpleName() )
				.forks( 0 )
				.warmupIterations( 4 )
				.measurementIterations( 8 )
				.warmupTime( TimeValue.milliseconds( 100 ) )
				.measurementTime( TimeValue.milliseconds( 100 ) )
				.build();
		new Runner( opt ).run();
	}
}
