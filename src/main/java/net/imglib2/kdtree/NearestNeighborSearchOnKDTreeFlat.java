/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2021 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

package net.imglib2.kdtree;

import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.neighborsearch.NearestNeighborSearch;

/**
 * Implementation of {@link NearestNeighborSearch} search for kd-trees.
 * 
 * 
 * @author Tobias Pietzsch
 */
public class NearestNeighborSearchOnKDTreeFlat< T > implements NearestNeighborSearch< T >
{
	private final FlatKDTreeFlat.KDTree< T > tree;

	private final KDTreeImplFlat.NearestNeighborSearch search;

	private final FlatKDTreeFlat.KDTreeNode< T > bestPoint;

	public NearestNeighborSearchOnKDTreeFlat( final FlatKDTreeFlat.KDTree< T > tree )
	{
		this.tree = tree;
		search = tree.impl.new NearestNeighborSearch();
		bestPoint = tree.getRoot();
	}

	private NearestNeighborSearchOnKDTreeFlat( final NearestNeighborSearchOnKDTreeFlat< T > nn )
	{
		tree = nn.tree;
		search = nn.search.copy();
		bestPoint = tree.getRoot();
		bestPoint.setNodeIndex( nn.search.index() );
	}

	@Override
	public int numDimensions()
	{
		return tree.numDimensions();
	}

	@Override
	public void search( final RealLocalizable p )
	{
		search.search( p );
		bestPoint.setNodeIndex( search.index() );
	}

	@Override
	public Sampler< T > getSampler()
	{
		return bestPoint;
	}

	@Override
	public RealLocalizable getPosition()
	{
		return bestPoint;
	}

	@Override
	public double getSquareDistance()
	{
		return search.squDistance();
	}

	@Override
	public double getDistance()
	{
		return Math.sqrt( search.squDistance() );
	}

	@Override
	public NearestNeighborSearchOnKDTreeFlat< T > copy()
	{
		return new NearestNeighborSearchOnKDTreeFlat<>( this );
	}
}
