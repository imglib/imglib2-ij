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

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State( Scope.Benchmark )
public class ImgLib2ToVirtualStackBenchmark
{

	long[] smallDims = { 10, 10, 10 };
	long[] deepDims = { 10, 10, 1000000 };
	long[] cubicDims = { 1000, 1000, 1000 };

	private final ImgPlus< UnsignedByteType > smallCellImage = makeImgPlus( createCellImg( deepDims ) );
	private final ImgPlus< UnsignedByteType > deepCellImage = makeImgPlus( createCellImg( deepDims ) );
	private final ImgPlus< UnsignedByteType > cubicCellImage = makeImgPlus( createCellImg( cubicDims ) );
	private final ImgPlus< UnsignedByteType > smallPlanarImg = makeImgPlus( PlanarImgs.unsignedBytes( smallDims ) );
	private final ImgPlus< UnsignedByteType > cubicPlanarImg = makeImgPlus( PlanarImgs.unsignedBytes( cubicDims ) );
	private final ImgPlus< UnsignedByteType > deepPlanarImg = makeImgPlus( PlanarImgs.unsignedBytes( deepDims ) );
	private final ImgPlus< UnsignedByteType > small2dArrayImg = makeImgPlus( ArrayImgs.unsignedBytes( 10, 10 ) );
	private final ImgPlus< UnsignedByteType > big2dArrayImg = makeImgPlus( ArrayImgs.unsignedBytes( 10000, 10000 ) );

	@Benchmark
	public void testSmallCellImg()
	{
		ImgToVirtualStack.wrap( smallCellImage );
	}

	@Benchmark
	public void testDeepCellImg()
	{
		ImgToVirtualStack.wrap( deepCellImage );
	}

	@Benchmark
	public void testCubicCellImg()
	{
		ImgToVirtualStack.wrap( cubicCellImage );
	}

	@Benchmark
	public void testSmallPlanarImg()
	{
		PlanarImgToVirtualStack.wrap( smallPlanarImg );
	}

	@Benchmark
	public void testCubicPlanarImg()
	{
		PlanarImgToVirtualStack.wrap( cubicPlanarImg );
	}

	@Benchmark
	public void testDeepPlanarImg()
	{
		PlanarImgToVirtualStack.wrap( deepPlanarImg );
	}

	@Benchmark
	public void testSmall2dArrayImg()
	{
		ArrayImgToVirtualStack.wrap( small2dArrayImg );
	}

	@Benchmark
	public void testLarge2dArrayImg()
	{
		ArrayImgToVirtualStack.wrap( big2dArrayImg );
	}

	private ImgPlus< UnsignedByteType > makeImgPlus( final Img< UnsignedByteType > deepPlanarImg )
	{
		final AxisType[] axes = { Axes.X, Axes.Y, Axes.Z };
		return new ImgPlus<>( deepPlanarImg, "title", axes );
	}

	private Img< UnsignedByteType > createCellImg( final long... dim )
	{
		return new CellImgFactory<>( new UnsignedByteType() ).create( dim );
	}

	public static void main( final String... args ) throws RunnerException
	{
		final Options opt = new OptionsBuilder()
				.include( ImgLib2ToVirtualStackBenchmark.class.getSimpleName() )
				.forks( 0 )
				.warmupIterations( 4 )
				.measurementIterations( 8 )
				.warmupTime( TimeValue.milliseconds( 100 ) )
				.measurementTime( TimeValue.milliseconds( 100 ) )
				.build();
		new Runner( opt ).run();
	}
}
