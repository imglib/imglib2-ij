package net.imglib2.kdtree;

import net.imglib2.RealLocalizable;

import static net.imglib2.kdtree.KDTreeUtils.leftChildIndex;
import static net.imglib2.kdtree.KDTreeUtils.parentIndex;
import static net.imglib2.kdtree.KDTreeUtils.rightChildIndex;

/**
 * Accessing the positions tree (without values).
 */
public class KDTreeImpl {

    private final double[][] positions;

    private final int numDimensions;

    private final int numPoints;

    KDTreeImpl(final double[][] positions) {
        this.positions = positions;
        numDimensions = positions.length;
        numPoints = positions[0].length;
    }

    /**
     * Get the root node of the tree.
     *
     * @return index of the root node
     */
    public int root() {
        return 0;
    }

    /**
     * Get the left child of node {@code i}.
     *
     * @param i node index
     * @return index of left child or {@code -1} if no left child exists
     */
    public int left(final int i) {
		return ifExists( leftChildIndex( i ) );
    }

    /**
     * Get the right child of node {@code i}.
     *
     * @param i node index
     * @return index of right child or {@code -1} if no right child exists
     */
    public int right(final int i) {
		return ifExists( rightChildIndex( i ) );
    }

    /**
     * Get the parent of node {@code i}.
     *
     * @param i node index
     * @return index of parent
     */
    public int parent(final int i) {
		return i == root() ? -1 : parentIndex( i );
    }

    /**
     * If a node with index {@code i} exists, returns {@code i}.
     * Otherwise, returns {@code -1}.
     */
    private int ifExists(final int i) {
        return i < numPoints ? i : -1;
    }

    /**
     * Get the dimension along which node {@code i} divides the space.
     *
     * @param i node index
     * @return splitting dimension.
     */
    public int splitDimension(final int i) {
        return (31 - Integer.numberOfLeadingZeros(i + 1)) % numDimensions;
    }

	public double getDoublePosition(final int i, final int d) {
		return positions[d][i];
	}

	public double squDistance(final int i, final double[] pos) {
		double sum = 0;
		for (int d = 0; d < numDimensions; ++d) {
			final double diff = pos[d] - positions[d][i];
			sum += diff * diff;
		}
		return sum;
	}

    public int numDimensions() {
        return numDimensions;
    }

    public int size() {
        return numPoints;
    }

    public int depth() {
        return 32 - Integer.numberOfLeadingZeros(numPoints);
    }



    public class NearestNeighborSearch {

        private final double[] pos;

        private int bestIndex;

        private double bestSquDistance;

        private final double[] axisDiffs;

        private final int[] awayChilds;

        NearestNeighborSearch() {
            pos = new double[numDimensions];
            bestIndex = -1;
            bestSquDistance = Double.POSITIVE_INFINITY;
			final int depth = depth();
			axisDiffs = new double[ depth + 1];
            awayChilds = new int[ depth + 1];
        }

        public void search(final RealLocalizable p) {
            p.localize(pos);
            int current = root();
            int depth = 0;
            double bestSquDistanceL = Double.POSITIVE_INFINITY;
            int bestIndexL = -1;
            while (true) {
                final double distance = squDistance(current, pos);
                if (distance < bestSquDistanceL) {
                    bestSquDistanceL = distance;
                    bestIndexL = current;
                }

                final int d = depth % numDimensions;
                final double axisDiff = pos[d] - getDoublePosition(current, d);
                final boolean leftIsNearBranch = axisDiff < 0;

                // search the near branch
                final int nearChild = leftIsNearBranch ? left(current) : right(current);
                final int awayChild = leftIsNearBranch ? right(current) : left(current);
                ++depth;
                awayChilds[depth] = awayChild;
                axisDiffs[depth] = axisDiff * axisDiff;
                if (nearChild < 0) {
                    while (awayChilds[depth] < 0 || axisDiffs[depth] > bestSquDistanceL) {
                        if (--depth == 0) {
                            bestSquDistance = bestSquDistanceL;
                            bestIndex = bestIndexL;
                            return;
                        }
                    }
                    current = awayChilds[depth];
                    awayChilds[depth] = -1;
                } else {
                    current = nearChild;
                }
            }
        }

        public int bestIndex() {
            return bestIndex;
        }

        public double bestSquDistance() {
            return bestSquDistance;
        }

		public NearestNeighborSearch copy() {
			final NearestNeighborSearch copy = new NearestNeighborSearch();
			System.arraycopy( pos, 0, copy.pos, 0, pos.length );
			copy.bestIndex = bestIndex;
			copy.bestSquDistance = bestSquDistance;
			return copy;
		}
    }
}
