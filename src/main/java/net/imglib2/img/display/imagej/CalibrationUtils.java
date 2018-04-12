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
package net.imglib2.img.display.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;

import java.util.ArrayList;
import java.util.List;

public class CalibrationUtils {

	/**
	 * Sets the {@link Calibration} data on the provided {@link ImagePlus}.
	 */
	public static void copyCalibrationToImagePlus(final ImgPlus<?> imgPlus, final ImagePlus imp)
	{
		final Calibration calibration = imp.getCalibration();
		final int xIndex = imgPlus.dimensionIndex(Axes.X);
		final int yIndex = imgPlus.dimensionIndex(Axes.Y);
		final int zIndex = imgPlus.dimensionIndex(Axes.Z);
		final int tIndex = imgPlus.dimensionIndex(Axes.TIME);

		if (xIndex >= 0) {
			calibration.pixelWidth = imgPlus.averageScale( xIndex );
			CalibratedAxis axis = imgPlus.axis( xIndex );
			calibration.xOrigin = axis.calibratedValue( 0 );
			calibration.setXUnit( axis.unit());
		}
		if (yIndex >= 0) {
			calibration.pixelHeight = imgPlus.averageScale( yIndex );
			CalibratedAxis axis = imgPlus.axis( yIndex );
			calibration.yOrigin = axis.calibratedValue( 0 );
			calibration.setYUnit( axis.unit());
		}
		if (zIndex >= 0) {
			calibration.pixelDepth = imgPlus.averageScale( zIndex );
			CalibratedAxis axis = imgPlus.axis( zIndex );
			calibration.zOrigin = axis.calibratedValue( 0 );
			calibration.setZUnit( axis.unit());
		}
		if (tIndex >= 0) {
			calibration.frameInterval = imgPlus.averageScale( tIndex );
			calibration.setTimeUnit(imgPlus.axis(tIndex).unit());
		}
	}

	public static CalibratedAxis[] getNonTrivialAxes( ImagePlus image ) {
		List<CalibratedAxis> result = new ArrayList<>();
		Calibration calibration = image.getCalibration();
		result.add(new DefaultLinearAxis(Axes.X, calibration.getXUnit(), calibration.pixelWidth, calibration.xOrigin));
		result.add(new DefaultLinearAxis(Axes.Y, calibration.getYUnit(), calibration.pixelHeight, calibration.yOrigin));
		if(image.getNChannels() > 1)
			result.add(new DefaultLinearAxis(Axes.CHANNEL));
		if(image.getNSlices() > 1)
			result.add(new DefaultLinearAxis(Axes.Z, calibration.getZUnit(), calibration.pixelDepth, calibration.zOrigin));
		if(image.getNFrames() > 1)
			result.add(new DefaultLinearAxis(Axes.TIME, calibration.getTimeUnit(), calibration.frameInterval));
		return result.toArray(new CalibratedAxis[0]);
	}
}
