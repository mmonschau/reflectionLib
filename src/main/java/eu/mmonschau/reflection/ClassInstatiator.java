/*
 * Copyright (c) Michael Monschau 2018.
 */

package eu.mmonschau.reflection;

import eu.mmonschau.reflection.util.JTextLog;

/**
 * Creates an Instance of a class via reflective constructor-call
 */
@SuppressWarnings("unchecked")
public class ClassInstatiator {

	private static final java.util.Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
			= new com.google.common.collect.ImmutableMap.Builder<Class<?>, Class<?>>()
			.put(boolean.class, Boolean.class)
			.put(byte.class, Byte.class)
			.put(char.class, Character.class)
			.put(double.class, Double.class)
			.put(float.class, Float.class)
			.put(int.class, Integer.class)
			.put(long.class, Long.class)
			.put(short.class, Short.class)
			.put(void.class, Void.class)
			.build();


	/**
	 * gets the constructor with maximum number of arguments
	 *
	 * @param aClass
	 * 		the class which constructor to search
	 * @param <T>
	 * 		trivial
	 *
	 * @return the constructor
	 */
	public static <T> java.lang.reflect.Constructor<T> getMaxArgsConstructor(Class<T> aClass) {
		return (java.lang.reflect.Constructor<T>) getUsableConstructors(aClass).stream()
				.max(java.util.Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount)).get();
	}

	/**
	 * gets the constructor with minimum number of arguments
	 *
	 * @param aClass
	 * 		the class which constructor to search
	 * @param <T>
	 * 		trivial
	 *
	 * @return the constructor
	 */
	public static <T> java.lang.reflect.Constructor<T> getMinArgsConstructor(Class<T> aClass) {
		return (java.lang.reflect.Constructor<T>) getUsableConstructors(aClass).stream()
				.min(java.util.Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount)).get();
	}

	/**
	 * gets all constructors with no arguments or only primitive and/or String-Arguments
	 *
	 * @param aClass
	 * 		the class which constructor to search
	 * @param <T>
	 * 		trivial
	 *
	 * @return a list of usable constuctor
	 */
	public static <T> java.util.List<java.lang.reflect.Constructor<T>> getUsableConstructors(Class<T> aClass) {
		return (java.util.List<java.lang.reflect.Constructor<T>>) java.util.Arrays.stream(
				aClass.getConstructors())
				.filter(constructor -> isUsableInCLIContext(getConstructorArguments(constructor)))
				.map(constructor -> (java.lang.reflect.Constructor<T>) constructor)
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * creates a name-type map for the constructor-aruments
	 *
	 * @param c
	 * 		a constructor
	 *
	 * @return the map
	 */
	public static java.util.LinkedHashMap<String, Class<?>> getConstructorArguments(
			java.lang.reflect.Constructor<?> c) {
		java.util.LinkedHashMap<String, Class<?>> result = new java.util.LinkedHashMap<>();
		for (java.lang.reflect.Parameter p : c.getParameters()) {
			result.put(p.getName(), p.getType());
		}
		return result;
	}


	/**
	 * checks whether the given values are Primitives, wrappers or string
	 *
	 * @param arguments
	 * 		the arguments to check
	 *
	 * @return false if one argument is not primitive or String
	 */
	private static boolean isUsableInCLIContext(java.util.Map<String, Class<?>> arguments) {
		//System.out.println(arguments);
		for (Class<?> c : arguments.values()) {
			//System.out.println(c);
			if (!c.equals(String.class) && !c.isPrimitive()) {
				try {
					//Primitive types have ValueOf Method in Wrapper-Class
					c.getMethod("valueOf", String.class);
				} catch (NoSuchMethodException e) {
					//e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Tries to create an instance of the class unsing the given argument and values
	 *
	 * @param c
	 * 		the constructor to use
	 * @param values
	 * 		the arguments for the constructoe
	 * @param <T>
	 * 		the class to create
	 *
	 * @return a new instance of T
	 *
	 * @throws InstantiationException
	 * 		if the instantiation fails
	 */
	public static <T> T createInstance(java.lang.reflect.Constructor<T> c, java.util.List<String> values)
			throws InstantiationException {
		try {
			java.lang.reflect.Parameter[] parameters = c.getParameters();
			if (parameters.length == 0) {
				return (T) c.newInstance();
			}
			if (values == null || parameters.length != values.size()) {
				throw new InstantiationException("Tried to instantiate class without proper Argument number");
			}
			return (T) c.newInstance(castArgsToMatchConstructor(c, values));
		} catch (java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
			throw new InstantiationException(e.toString());
		}
	}

	/**
	 * cast values to matching objects
	 *
	 * @param c
	 * 		the constructor
	 * @param values
	 * 		the arguments for the constructor
	 *
	 * @return an array of objects for passing to the constructor
	 *
	 * @throws java.lang.reflect.InvocationTargetException
	 * 		if the constructor is not callable with the given values
	 * @throws IllegalAccessException
	 * 		if the constructor is not accessible
	 */
	private static Object[] castArgsToMatchConstructor(java.lang.reflect.Constructor<?> c,
	                                                   java.util.List<String> values)
			throws java.lang.reflect.InvocationTargetException, IllegalAccessException {
		java.util.LinkedList<Object>  args       = new java.util.LinkedList<>();
		java.lang.reflect.Parameter[] parameters = c.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Object   arg     = null;
			String   toCast  = values.get(i);
			Class<?> aClass1 = (parameters[i]).getType();
			aClass1 = aClass1.isPrimitive() ? PRIMITIVES_TO_WRAPPERS.get(aClass1) : aClass1;
			if (aClass1.equals(String.class)) {
				arg = toCast;
			} else {
				try {
					arg = aClass1.getMethod("valueOf", String.class).invoke(null, toCast);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			}
			args.add(arg);
		}
		return args.toArray();

	}

	/**
	 * crates an instance of a class using the given arguments
	 *
	 * @param c
	 * 		the class to create an instance of
	 * @param values
	 * 		the arguments to use in the constructor
	 * @param <T>
	 * 		the class to create an instance of
	 *
	 * @return a new instance
	 *
	 * @throws InstantiationException
	 * 		if the instantiation fails
	 */
	public static <T> T createInstance(Class<T> c, java.util.List<String> values) throws InstantiationException {
		T result = null;
		if (values == null || values.isEmpty()) {
			return createInstance(c);

		} else {
			for (java.lang.reflect.Constructor<T> constructor : getUsableConstructors(c)) {
				if (constructor.getParameterCount() == values.size()) {
					try {
						result = createInstance(constructor, values);
						return result;
					} catch (InstantiationException ignored) {
					}
				}
			}
		}
		if (result == null) {
			throw new InstantiationException();
		}
		return result;
	}


	/**
	 * Creates an Instance Factory for given class and arguments
	 *
	 * @param aClass
	 * 		the class to create an instance of
	 * @param values
	 * 		the arguments to use in the constructor
	 * @param <T>
	 * 		the class to create an instance of
	 *
	 * @return a instance factory
	 *
	 * @throws InstantiationException
	 * 		if the class cannot be created with the given args
	 */
	public static <T> ClassInstanceFactory<T> getInstanceFactory(Class<T> aClass, java.util.List<String> values)
			throws InstantiationException {
		java.lang.reflect.Constructor<T> cons = null;
		Object[]                         args = null;
		if (values == null || values.isEmpty()) {
			createInstance(aClass);

		}
		for (java.lang.reflect.Constructor<T> constructor : getUsableConstructors(aClass)) {
			if (constructor.getParameterCount() == values.size()) {
				try {
					createInstance(constructor, values);
					cons = constructor;
					args = castArgsToMatchConstructor(cons, values);
					break;
				} catch (java.lang.reflect.InvocationTargetException |
						IllegalAccessException | InstantiationException ignored) {
				}
			}
		}
		if (cons == null && args == null) {
			throw new InstantiationException();
		}
		return new ClassInstanceFactory<>(cons, args);
	}

	/**
	 * Tries to create an Instance of given class by using default Constructor or getInstance Method
	 *
	 * @param aClass
	 * 		the Class to Instatiate
	 * @param <T>
	 * 		the type represented by the class
	 *
	 * @return a new instance of the class
	 *
	 * @throws InstantiationException
	 * 		if the instatiation fails
	 */
	public static <T> T createInstance(Class<T> aClass) throws InstantiationException {
		try {
			return (T) aClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			JTextLog.getLogger().log(java.util.logging.Level.FINE, e.toString());
		}
		try {
			return (T) aClass.getMethod("getInstance").invoke(null);
		} catch (IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
			JTextLog.getLogger().log(java.util.logging.Level.FINE, e.toString());
		}
		throw new InstantiationException("Could not Create Instance of Class " + aClass.getName());
	}
}
