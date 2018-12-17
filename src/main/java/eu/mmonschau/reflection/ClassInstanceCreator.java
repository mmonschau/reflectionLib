/*
 * Copyright (c) Michael Monschau 2018.
 */

package eu.mmonschau.reflection;

/**
 * Creates new Instances of the generic parameter
 *
 * @param <T>
 * 		the Class which is to be instantiated
 */
public interface ClassInstanceCreator<T> {

	/**
	 * Creates a new Instance of T
	 *
	 * @return a new Instance of T
	 */
	T newInstance();
}
