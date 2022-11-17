package net.imglib2.kdtree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.annotations.Setup;

@State( Scope.Benchmark )
public class KDTreeBenchmark
{
//	@Param({"3"})
//	public int n;
//
//	@Param({"10000", "100000", "1000000"})
//	public int numDataVertices;
//
//	@Param({"1000"})
//	public int numTestVertices;
//
//	@Param({"-5"})
//	public double minCoordinateValue;
//
//	@Param({"5"})
//	public double maxCoordinateValue;
//
	public int n = 3;
	public int k = 10;
	public int radius = 1;
	public int numDataVertices = 10000;
	public int numTestVertices = 1000;
	public double minCoordinateValue = -5;
	public double maxCoordinateValue = 5;

	List< RealPoint > dataVertices;
	List< RealPoint > testVertices;

	private net.imglib2.KDTree< RealPoint > kdtreeOld;
	private net.imglib2.kdtree.KDTree< RealPoint > kdtree;

	@Setup
	public void setup()
	{
		createVertices();
		kdtreeOld = new net.imglib2.KDTree<>( dataVertices, dataVertices );
		kdtree = new net.imglib2.kdtree.KDTree<>( dataVertices, dataVertices );
//		spoil();
	}

	public void spoil() {
		final double[][] points = KDTreeUtils.initPositions( n, numDataVertices, dataVertices );
		final int[] tree = KDTreeUtils.makeTree( points );
		final double[][] treePoints = KDTreeUtils.reorder( points, tree );
		final KDTreeImpl impl = new KDTreeImpl.Nested( treePoints );
		final KDTreeImpl.NearestNeighborSearch search = new KDTreeImpl.NearestNeighborSearch( impl );
		for ( RealPoint testVertex : testVertices )
			search.search( testVertex );
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void createKDTreeOld()
	{
		new net.imglib2.KDTree<>( dataVertices, dataVertices );
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void createKDTree()
	{
		new net.imglib2.kdtree.KDTree<>( dataVertices, dataVertices );
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void nearestNeighborSearchOld()
	{
		final net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree< RealPoint > kd = new net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree<>( kdtreeOld );
		for ( final RealLocalizable t : testVertices )
		{
			kd.search( t );
			kd.getSampler().get();
		}
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void nearestNeighborSearch()
	{
		final net.imglib2.kdtree.NearestNeighborSearchOnKDTree< RealPoint > kd = new net.imglib2.kdtree.NearestNeighborSearchOnKDTree<>( kdtree );
		for ( final RealLocalizable t : testVertices )
		{
			kd.search( t );
			kd.getSampler().get();
		}
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void kNearestNeighborSearchOld()
	{
		final net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree< RealPoint > kd = new net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree<>( kdtreeOld, k );
		for ( final RealLocalizable t : testVertices )
		{
			kd.search( t );
			kd.getSampler().get();
//			for ( int i = 0; i < k; i++ )
//			{
//				kd.getSampler( i ).get();
//			}
		}
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void kNearestNeighborSearch()
	{
		final net.imglib2.kdtree.KNearestNeighborSearchOnKDTree< RealPoint > kd = new net.imglib2.kdtree.KNearestNeighborSearchOnKDTree<>( kdtree, k );
		for ( final RealLocalizable t : testVertices )
		{
			kd.search( t );
			kd.getSampler().get();
//			for ( int i = 0; i < k; i++ )
//			{
//				kd.getSampler( i ).get();
//			}
		}
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void radiusNeighborSearchOld()
	{
		final net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree< RealPoint > kd = new net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree<>( kdtreeOld );
		for ( final RealLocalizable t : testVertices )
		{
			kd.search( t, radius, true );
			for ( int i = 0; i < Math.min( kd.numNeighbors(), k ); i++ )
			{
				kd.getSampler( i ).get();
			}
		}
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void radiusNeighborSearch()
	{
		final net.imglib2.kdtree.RadiusNeighborSearchOnKDTree< RealPoint > kd = new net.imglib2.kdtree.RadiusNeighborSearchOnKDTree<>( kdtree );
		for ( final RealLocalizable t : testVertices )
		{
			kd.search( t, radius, true );
			for ( int i = 0; i < Math.min( kd.numNeighbors(), k ); i++ )
			{
				kd.getSampler( i ).get();
			}
		}
	}



	private void createVertices()
	{
		final double[] p = new double[ n ];
		final double size = ( maxCoordinateValue - minCoordinateValue );
		final Random rnd = new Random( 4379 );
		dataVertices = new ArrayList<>();
		for ( int i = 0; i < numDataVertices; ++i )
		{
			for ( int d = 0; d < n; ++d )
				p[ d ] = rnd.nextDouble() * size + minCoordinateValue;
			dataVertices.add( new RealPoint( p ) );
		}
		testVertices = new ArrayList<>();
		for ( int i = 0; i < numTestVertices; ++i )
		{
			for ( int d = 0; d < n; ++d )
				p[ d ] = rnd.nextDouble() * 2 * size + minCoordinateValue - size / 2;
			testVertices.add( new RealPoint( p )  );
		}
	}

	public static void main( final String... args ) throws RunnerException
	{
		final Options opt = new OptionsBuilder()
				.include( KDTreeBenchmark.class.getSimpleName() )
				.forks( 0 )
				.warmupIterations( 4 )
				.measurementIterations( 8 )
				.warmupTime( TimeValue.milliseconds( 500 ) )
				.measurementTime( TimeValue.milliseconds( 500 ) )
				.build();
		new Runner( opt ).run();
	}
}
