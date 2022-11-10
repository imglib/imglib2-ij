package net.imglib2.kdtree;

import ij.ImageJ;

import java.util.Arrays;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class SparseExample2
{
	public static void main( String[] args )
	{
		new ImageJ();

		List< ARGBType > colors = Arrays.asList( SparseExample1.colorsArray );
		List< RealPoint > coordinates = Arrays.asList( SparseExample1.coordinatesArray );

		// the interval we want to display
		Interval interval = Intervals.createMinSize( 0, 0, 320, 200 );

		final KDTree< ARGBType > kdtree = new KDTree<>( colors, coordinates );
		NearestNeighborSearch< ARGBType > search = new NearestNeighborSearchOnKDTree<>( kdtree );
		RealRandomAccessible< ARGBType > interpolated = Views.interpolate( search, new NearestNeighborSearchInterpolatorFactory<>() );
		RandomAccessibleInterval< ARGBType > view = Views.interval( Views.raster( interpolated ), interval );
		ImageJFunctions.show( view );
	}
}
