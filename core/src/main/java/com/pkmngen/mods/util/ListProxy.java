package com.pkmngen.mods.util;

import java.util.*;

public class ListProxy<A, B extends A> implements List<A> {

    private final List<B> inner;

    public ListProxy(List<B> inner) {
        this.inner = inner;
    }

    @Override
    public int size() {
        return this.inner.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inner.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.inner.contains(o);
    }

    @Override
    public Iterator<A> iterator() {
        final Iterator<B> iter = this.inner.iterator();
        return new Iterator<A>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public A next() {
                return iter.next();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return inner.toArray(new Object[0]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) this.toArray();
    }

    @Override
    public boolean add(A a) {
        return this.inner.add((B) a);
    }

    @Override
    public boolean remove(Object o) {
        return this.inner.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.inner.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends A> c) {
        return this.inner.addAll((Collection<? extends B>) c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends A> c) {
        return this.inner.addAll(index, (Collection<? extends B>) c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.inner.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.inner.retainAll(c);
    }

    @Override
    public void clear() {
        this.inner.clear();
    }

    @Override
    public A get(int index) {
        return this.inner.get(index);
    }

    @Override
    public A set(int index, A element) {
        return this.inner.set(index, (B) element);
    }

    @Override
    public void add(int index, A element) {
        this.inner.add(index, (B) element);
    }

    @Override
    public A remove(int index) {
        return this.inner.remove((index));
    }

    @Override
    public int indexOf(Object o) {
        return this.inner.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.inner.lastIndexOf(o);
    }

    @Override
    public ListIterator<A> listIterator() {
        return null;
    }

    @Override
    public ListIterator<A> listIterator(int index) {
        return null;
    }

    @Override
    public List<A> subList(int fromIndex, int toIndex) {
        return null;
    }
}
