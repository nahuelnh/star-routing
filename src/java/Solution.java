import java.util.List;

public class Solution {
    private final List<ElementaryPath> elementaryPaths;

    Solution(List<ElementaryPath> elementaryPaths) {
        this.elementaryPaths = elementaryPaths;
    }

    public List<ElementaryPath> getRoutes() {
        return elementaryPaths;
    }

    @Override
    public String toString() {
        return "Solution{" + "elementaryPaths=" + elementaryPaths + '}';
    }
}
