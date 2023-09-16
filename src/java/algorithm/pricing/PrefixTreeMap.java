package algorithm.pricing;

import java.util.ArrayList;
import java.util.BitSet;
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
        BitSet b1 = new BitSet(5);
        t.insert(b1, "cachi");
        BitSet b2 = new BitSet(5);
        b2.set(1,2);
        t.insert(b2, "fdsfdf");
        BitSet b3 = new BitSet(5);
        b3.set(1,3);

        System.out.println(t.contains(b1));
        System.out.println(t.get(b1));
        System.out.println(t.contains(b2));
        System.out.println(t.get(b2));

        System.out.println(t.getValuesAtAllPrefixes(b1));
        System.out.println(t.getValuesAtAllPrefixes(b2));
        System.out.println(t.getValuesAtAllPrefixes(b3));


    }

    public void insert(BitSet bitset, T elem) {
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length(); i++) {
            if (bitset.get(i)) {
                if (crawl.getChild(i) == null) {
                    crawl.setChild(i, new TrieNode<>(alphabetSize));
                }
                crawl = crawl.getChild(i);
            }
        }
        crawl.setElem(elem);
    }

    public boolean contains(BitSet bitset) {
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length(); i++) {
            if (bitset.get(i)) {
                if (crawl.getChild(i) == null) {
                    return false;
                }
                crawl = crawl.getChild(i);
            }
        }
        return crawl.getElem() != null;
    }

    public T get(BitSet bitset) {
        TrieNode<T> crawl = root;
        for (int i = 0; i < bitset.length(); i++) {
            if (bitset.get(i)) {
                if (crawl.getChild(i) == null) {
                    return null;
                }
                crawl = crawl.getChild(i);
            }
        }
        return crawl.getElem();
    }

    public List<T> getValuesAtAllPrefixes(BitSet bitset) {
        List<T> ret = new ArrayList<>();
        TrieNode<T> crawl = root;
        if(crawl.getElem() != null){
            ret.add(crawl.getElem());
        }
        for (int i = 0; i < bitset.length(); i++) {
            if (bitset.get(i)) {
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



