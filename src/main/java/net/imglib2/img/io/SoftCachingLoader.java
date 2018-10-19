/*
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

package net.imglib2.img.io;

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
