package algorithm.pricing;

import java.util.ArrayList;
import java.util.List;

public class PrefixTreeMap<T> {

    private final TrieNode<T> root;
    private final int alphabetSize;

    public PrefixTreeMap(int alphabetSize) {
        this.alphabetSize = alphabetSize;
        this.root = new TrieNode<>(alphabetSize);
    }

    public static void main(String[] args) {
        PrefixTreeMap<String> t = new PrefixTreeMap<>(5);
        boolean[] b1 = {true, true, false, false, true};
        t.insert(b1, "cachi");
        boolean[] b2 = {true, true, false, false, false};
        t.insert(b1, "fdsfdf");

        System.out.println(t.contains(b1));
        System.out.println(t.contains(b2));

    }

    public void insert(boolean[] bitset, T elem) {
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i]) {
                if (crawl.getChild(i) == null) {
                    crawl.setChild(i, new TrieNode<>(alphabetSize));
                }
                crawl = crawl.getChild(i);
            }
        }
        crawl.setElem(elem);
    }

    public boolean contains(boolean[] bitset) {
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i]) {
                if (crawl.getChild(i) == null) {
                    return false;
                }
                crawl = crawl.getChild(i);
            }
        }
        return crawl.getElem() != null;
    }

    public T get(boolean[] bitset) {
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i]) {
                if (crawl.getChild(i) == null) {
                    return null;
                }
                crawl = crawl.getChild(i);
            }
        }
        return crawl.getElem();
    }

    public List<T> getValuesAtAllPrefixes(boolean[] bitset) {
        List<T> ret = new ArrayList<>();
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i]) {
                if (crawl.getChild(i) == null) {
                    return ret;
                }
                crawl = crawl.getChild(i);
                if(crawl.getElem() != null){
                    ret.add(crawl.getElem());
                }
            }
        }
        return ret;
    }


    public List<T> getAllValues(){
        return getAllValuesAux(root);
    }
    private List<T> getAllValuesAux(TrieNode<T> n){
        List<T>ret = new ArrayList<>();
        if(n.getElem()!=null){
            ret.add(n.getElem());
        }
        for (int i = 0; i < alphabetSize; i++) {
            if(n.getChild(i)!= null){
                ret.addAll(getAllValuesAux(n.getChild(i)));
            }
        }
        return ret;
    }

    private static class TrieNode<T> {

        private final List<TrieNode<T>> children;
        private T elem;

        public TrieNode(int alphabetSize) {
            this.elem = null;
            this.children = new ArrayList<>(alphabetSize);
            for (int i = 0; i < alphabetSize; i++) {
                children.add(null);
            }
        }

        public T getElem() {
            return elem;
        }

        public void setElem(T elem) {
            this.elem = elem;
        }

        public TrieNode<T> getChild(int i) {
            return children.get(i);
        }

        public void setChild(int i, TrieNode<T> child) {
            children.set(i, child);
        }
    }
}



