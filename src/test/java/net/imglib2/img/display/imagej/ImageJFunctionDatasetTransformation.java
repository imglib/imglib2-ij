/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.imglib2.img.display.imagej;

import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.logic.BitType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author cyril
 */
public class ImageJFunctionDatasetTransformation
{

	@Test
	public void transformBitDataset()
	{

		final ImgFactory< BitType > imgFactory = new CellImgFactory< >( 5 );

		// create an 3d-Img with dimensions 20x30x40 (here cellsize is 5x5x5)Ø
		final Img< BitType > img1 = imgFactory.create( new long[] { 20, 30, 40 }, new BitType() );

		Cursor< BitType > cursor = img1.cursor();

		cursor.reset();
		// setting all the pixels as white
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.get().set( new BitType( true ) );
		}

		ImagePlus imp = ImageJFunctions.wrap( img1, "" );

		Assert.assertEquals( ImagePlus.GRAY8, imp.getType() );
		Assert.assertEquals( 255, imp.getStatistics().min, 0 );

	}
}
