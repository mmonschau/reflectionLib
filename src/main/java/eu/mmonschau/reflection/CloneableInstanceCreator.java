/*
 * Copyright (c) Michael Monschau 2018.
 */

package eu.mmonschau.reflection;

/**
 * Creates a new Instance of an Object by using {@link Object#clone()} of {@link Cloneable} classes
 *
 * @param <T> the class to clone
 */
@SuppressWarnings("unchecked")
public class CloneableInstanceCreator<T extends Cloneable> implements ClassInstanceCreator<T> {

	private final T templateObject;

	/**
	 * A basic constructor
	 *
	 * @param templateObject
	 * 		the object to clone
	 */
	public CloneableInstanceCreator(T templateObject) {
		this.templateObject = templateObject;
	}

	@Override
	public T newInstance() {
		try {
			return (T) templateObject.getClass().getMethod("clone").invoke(templateObject);
		} catch (IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
}
