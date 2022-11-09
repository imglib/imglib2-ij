package net.imglib2.kdtree;

import java.util.function.Function;
import java.util.function.Supplier;
import net.imglib2.IterableRealInterval;
import net.imglib2.RealCursor;
import net.imglib2.converter.AbstractConvertedIterableRealInterval;
import net.imglib2.converter.AbstractConvertedRealCursor;

// TODO find a better name
// TODO implement AbstractConvertedRealInterval in terms of this
// TODO move to imglib2 core in appropriate place
// TODO figure out general ClassCopyProvider pattern for this
public class CursorConvertedIterableRealInterval< A, B > extends AbstractConvertedIterableRealInterval< A, B >
{
	private Supplier< Function< RealCursor< A >, B > > functionSupplier; // TODO find a better name

	CursorConvertedIterableRealInterval( IterableRealInterval< A > sourceInterval, Supplier< Function< RealCursor< A >, B > > functionSupplier )
	{
		super( sourceInterval );
		this.functionSupplier = functionSupplier;
	}

	public class Cursor extends AbstractConvertedRealCursor< A, B >
	{
		private final Function< RealCursor< A >, B > function;

		Cursor( final RealCursor< A > source )
		{
			super( source );
			function = functionSupplier.get();
		}

		@Override
		public B get()
		{
			return function.apply( source );
		}

		@Override
		public Cursor copy()
		{
			return new Cursor( source.copyCursor() );
		}
	}

	@Override
	public AbstractConvertedRealCursor< A, B > cursor()
	{
		return new Cursor( sourceInterval.cursor() );
	}

	@Override
	public AbstractConvertedRealCursor< A, B > localizingCursor()
	{
		return new Cursor( sourceInterval.localizingCursor() );
	}
}
