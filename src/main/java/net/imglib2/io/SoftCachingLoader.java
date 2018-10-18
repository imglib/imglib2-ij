package net.imglib2.io;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

import net.imglib2.img.Img;
import net.imglib2.type.Type;

/**
 * A proxy {@link Loader} with an LRU cache implementation
 * where the user determines how many {@link Img} should be stored,
 * but where the JVM is free to remove them from memory
 * if they aren't in use anywhere else.
 * 
 * @author Albert Cardona
 *
 * @param <T>
 */
public class SoftCachingLoader< T extends Type< T > > implements Loader< T >
{
	private final LinkedHashMap< String, SoftReference< Img< T > > > cache;
	private final Loader< T > loader;

	public SoftCachingLoader( final Loader< T > loader, final int max_entries )
	{
		this.cache = new LRUCache< String, SoftReference< Img< T > > >( max_entries );
		this.loader = loader;
	}
	
	@Override
	synchronized public Img< T > load( final String path )
	{
		final SoftReference< Img< T > > ref = this.cache.get( path );
		Img< T > img = null;
		if ( null != ref )
		{
			this.cache.remove( path );
			img = ref.get();
		}
		if ( null == img )
		{
			img = this.loader.load( path );
		}
		this.cache.put( path, new SoftReference< Img< T > >( img ) ); // will call removeEldestEntry
		return img;
	}

	private final class LRUCache< A, B > extends LinkedHashMap< A, B >
	{
		private static final long serialVersionUID = 1L;

		private final int max_entries;

		LRUCache( final int max_entries )
		{
			this.max_entries = max_entries;
		}
		
		@Override
		protected final boolean removeEldestEntry( final Map.Entry< A , B > entry )
		{
			return this.size() > this.max_entries;
		}
	}
}
