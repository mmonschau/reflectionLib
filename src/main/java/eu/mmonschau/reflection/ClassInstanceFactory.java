/*
 * Copyright (c) Michael Monschau 2018.
 */

package eu.mmonschau.reflection;

/**
 * Creates a new Instance of a class using a constructor and arguments
 *
 * @param <T>
 * 		the Class to create
 *
 * @see eu.mmonschau.reflection.ClassInstatiator#getInstanceFactory(Class, java.util.List)
 */
public class ClassInstanceFactory<T> implements ClassInstanceCreator<T> {
	private final java.lang.reflect.Constructor<T> constructor;
	private final Object[]                         args;

	@Override
	public T newInstance() {
		try {
			return this.constructor.newInstance(args);
		} catch (InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Besic constuctor for constructor-calls
	 * @param constructor the constructor
	 * @param args args matching to the constructor
	 */
	ClassInstanceFactory(java.lang.reflect.Constructor<T> constructor, Object[] args) {
		this.constructor = constructor;
		this.args = args;
	}
}
