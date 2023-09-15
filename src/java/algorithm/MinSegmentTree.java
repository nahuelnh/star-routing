package algorithm;

public class MinSegmentTree {
    private final int size;
    private final double[] tree;

    public MinSegmentTree(double[] arr) {
        this.size = arr.length;
        this.tree = new double[2 * size];
        build(arr);
    }

    private void build(double[] arr) {
        System.arraycopy(arr, 0, tree, size, size);
        for (int i = size - 1; i > 0; --i) {
            tree[i] = Math.min(tree[i << 1], tree[i << 1 | 1]);
        }
    }

    public void update(int index, double value) {
        tree[index + size] = value;
        index = index + size;
        for (int i = index; i > 1; i >>= 1) {
            tree[i >> 1] = Math.min(tree[i], tree[i ^ 1]);
        }
    }

    public double query(int l, int r) {
        double res = Double.MAX_VALUE;
        for (l += size, r += size; l < r; l >>= 1, r >>= 1) {
            if ((l & 1) > 0) {
                res = Math.min(res, tree[l++]);
            }
            if ((r & 1) > 0) {
                res = Math.min(res, tree[--r]);
            }
        }
        return res;
    }
}
