package net.imglib2.img.imageplus;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

abstract class AbstractImagePlusImg< T extends NativeType< T >, A extends ArrayDataAccess<A>> extends ImagePlusImg< T, A > {

	final ImagePlus imp;

	public AbstractImagePlusImg( final long[] dim, final Fraction entitiesPerPixel )
	{
		super( dim, entitiesPerPixel );

		if ( entitiesPerPixel.getRatio() == 1 )
		{
			imp = createEmptyImagePlus();
			fillMirror();
		}
		else
		{
			imp = null;
			mirror.clear();
			for ( int i = 0; i < numSlices; ++i )
				mirror.add(createArray(numEntities(entitiesPerPixel)));
		}
	}

	public AbstractImagePlusImg( final ImagePlus imp )
	{
		super( imp );
		this.imp = imp;
		fillMirror();
	}

	private ImagePlus createEmptyImagePlus() {
		final ImageStack stack = new ImageStack( width, height );
		for ( int i = 0; i < numSlices; ++i )
			stack.addSlice( "", createProcessor());
		ImagePlus imp = new ImagePlus( "image", stack );
		imp.setDimensions( channels, depth, frames );
		if ( numSlices > 1 )
			imp.setOpenAsHyperStack( true );
		return imp;
	}

	private void fillMirror() {
		mirror.clear();
		for ( int t = 0; t < frames; ++t )
			for ( int z = 0; z < depth; ++z )
				for ( int c = 0; c < channels; ++c )
					mirror.add(createArray(imp.getStack().getProcessor( imp.getStackIndex( c + 1, z + 1 , t + 1 ) ).getPixels()));
	}

	protected abstract A createArray(Object pixels);

	protected abstract A createArray(int numEntities);

	protected abstract ImageProcessor createProcessor();

	protected abstract int getArrayLength(A plane);

	/**
	 * This has to be overwritten, otherwise two different instances exist (one in the imageplus, one in the mirror)
	 */
	@Override
	public void setPlane( final int no, final A plane )
	{
		System.arraycopy( plane.getCurrentStorageArray(), 0, mirror.get( no ).getCurrentStorageArray(), 0, getArrayLength(plane));
	}

	@Override
	public void close()
	{
		if ( imp != null )
			imp.close();
	}

	@Override
	public ImagePlus getImagePlus() throws ImgLibException
	{
		if ( imp == null )
			throw new ImgLibException( this, "has no ImagePlus instance, it is not a standard type of ImagePlus (" + entitiesPerPixel + " entities per pixel)" );
		return imp;
	}

}
