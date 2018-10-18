package net.imglib2.io;

import net.imglib2.img.Img;
import net.imglib2.type.Type;

public interface Loader< T extends Type< T > >
{
	public Img< T > load( final String path );
}
