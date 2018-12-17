/*
 * Copyright (c) Michael Monschau 2018.
 */

package eu.mmonschau.reflection;

import static java.io.File.pathSeparator;


/**
 * A Class to determine Super-/Sub-Classes of a specific class
 */
public class ClassScanner {

	/**
	 * Gets all subclasses for given superclasses (Interfaces are excluded)
	 *
	 * @param superClasses
	 * 		the classes to search subclasses for
	 *
	 * @return a class-subclasses Map
	 */
	public static java.util.Map<Class, java.util.Collection<Class<?>>> getSubclasses(
			java.util.Collection<Class<?>> superClasses) {
		//Create Result-Set
		java.util.HashMap<Class, java.util.Collection<Class<?>>> subclasses = new java.util.HashMap<>();
		for (Class sc : superClasses) {
			subclasses.put(sc, new java.util.HashSet<>());
		}
		//iterate over all Classes
		for (Class c : getAllLoadableClasses()
				.filter(o -> !((Class) o).isInterface())//Remove Interfaces
				.collect(java.util.stream.Collectors.toSet())) {
			java.util.Collection<Class> superCls = getSuperClassesAndInterfaces(c);
			for (Class sc : superClasses) {
				if (superCls.contains(sc)) {
					subclasses.get(sc).add(c);
				}
			}
		}
		return subclasses;
	}

	/**
	 * Gets all subclasses for given superclass
	 *
	 * @param superClass
	 * 		the class which subclasses should be found
	 *
	 * @return a collection of subclasses
	 */
	public static java.util.Collection<Class<?>> getSubclasses(Class<?> superClass) {
		java.util.Collection<Class<?>> subClasses = new java.util.HashSet<>();
		//iterate over all Classes
		for (Class c : getAllLoadableClasses()
				.filter(o -> !((Class) o).isInterface())//Remove Interfaces
				.collect(java.util.stream.Collectors.toSet())) {
			java.util.Collection<Class> superCls = getSuperClassesAndInterfaces(c);
			if (superCls.contains(superClass)) {
				subClasses.add(c);
			}
		}
		return subClasses;
	}


	/**
	 * Creates a Stream of All Classes in Classpath and not in $JAVAHOME
	 *
	 * @return a stream of classes
	 */
	private static java.util.stream.Stream<? extends Class<?>> getAllLoadableClasses() {
		java.util.Collection<java.io.File>          allClassLocations = getAllClassLocations();
		java.util.stream.Stream<? extends Class<?>> classesFromJars   = null;
		classesFromJars = getClassesFromJars(allClassLocations);
		java.util.stream.Stream<? extends Class<?>> allClassesOutsideOfJars = null;
		allClassesOutsideOfJars = getAllClassesOutsideOfJars(allClassLocations);
		return java.util.stream.Stream.concat(classesFromJars, allClassesOutsideOfJars);
	}

	/**
	 * Scans the classpath for jars and class-files (except from $JAVAHOME
	 *
	 * @return a Collection of class- und jar-Files
	 */
	private static java.util.Collection<java.io.File> getAllClassLocations() {
		return java.util.Arrays.stream(
				System.getProperty("java.class.path").split(pathSeparator)
		)
				//Remove Java Home
				.filter(s -> !s.startsWith(System.getProperty("java.home")))
				.map(java.io.File::new)
				.collect(java.util.stream.Collectors.toList());
	}

	private static boolean filterStdLibrary(String s) {
		java.util.regex.Pattern p = java.util.regex.Pattern
				.compile("^((javaf?x?)|(jdk)|(oracle)|((com\\.)?sun))((\\.)|/|(\\\\)).*");
		return !p.matcher(s).matches();
	}

	/**
	 * Extracts all class Files contained in a collection of jar-files
	 *
	 * @param files
	 * 		a Collection of jar files
	 *
	 * @return Stream of classes
	 */
	private static java.util.stream.Stream<? extends Class<?>> getClassesFromJars(
			java.util.Collection<java.io.File> files) {
		return files.stream()
				.filter(file -> file.getName().endsWith(".jar"))
				.distinct()
				//Create JarFile Object
				.map(jarFile -> {
					java.util.jar.JarFile jFile = null;
					try {
						jFile = new java.util.jar.JarFile(jarFile);
					} catch (java.io.IOException e) {
						new java.io.IOError(e);
					} finally {
						return jFile;//To circumvent Missing-Return-Error
					}
				})
				//get all Jar-Entries
				.flatMap(jarFile -> jarFile.stream())
				//get only class-Files
				.filter(jarEntry -> !jarEntry.isDirectory() && jarEntry.getName().endsWith(".class"))
				//get FileName (Path)
				.map(java.util.zip.ZipEntry::getName)
				.distinct()
				//remove .class
				.map(eu.mmonschau.reflection.ClassScanner::convertFilePathToFullyQualifiedName)
				.map(eu.mmonschau.reflection.ClassScanner::loadClass)
				.filter(aClass -> aClass != null)//*/
				;
	}

	/**
	 * Creates a Class from given fully qualified name
	 *
	 * @param s
	 * 		a fully qualified name
	 *
	 * @return a class object if the class exists or null
	 */
	private static Class<?> loadClass(String s) {
		{
			Class<?> c = null;
			try {
				c = Class.forName(s);
			} catch (ClassNotFoundException | NoClassDefFoundError e) {
				eu.mmonschau.reflection.util.JTextLog.getLogger()
						.log(java.util.logging.Level.FINEST, "Could not load Class:");
				eu.mmonschau.reflection.util.JTextLog.getLogger()
						.log(java.util.logging.Level.FINEST, e.getMessage());
			} finally {
				return c;
			}
		}
	}

	/**
	 * Converts a file path to a fully qualified name
	 *
	 * @param path
	 * 		a String representing a Path
	 *
	 * @return a String representing a fully qulified name
	 */
	private static String convertFilePathToFullyQualifiedName(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return path
				//remove .class
				.substring(0, path.length() - 6)
				//replace / to . for access via class-Loader
				.replace("/", ".");
	}

	/**
	 * Reads Converts all class Files in given Collection to  Class-Objects
	 *
	 * @param files
	 * 		A collection of Files
	 *
	 * @return a stream of class files
	 */
	private static java.util.stream.Stream<? extends Class<?>> getAllClassesOutsideOfJars(
			java.util.Collection<java.io.File> files) {
		return files.stream()
				.filter(file -> !file.getName().endsWith(".jar"))
				.filter(java.io.File::isDirectory)
				.map(file ->
						     new java.util.AbstractMap.SimpleEntry<>(
								     file,
								     getClassFilesInDirectory(
										     file)
						     )
				)
				.map(fileStreamSimpleEntry ->
						     getRelativePathsforSubfolders(fileStreamSimpleEntry.getKey(),
						                                   fileStreamSimpleEntry.getValue()))
				.flatMap(java.util.Collection::stream)
				.distinct()
				.map(ClassScanner::convertFilePathToFullyQualifiedName)
				.map(ClassScanner::loadClass)
				.filter(java.util.Objects::nonNull);
	}

	/**
	 * Gets all class-Files in given directory
	 *
	 * @param directory
	 * 		the directory to search for class-files
	 *
	 * @return a collection of class-files
	 */
	private static java.util.Collection<java.io.File> getClassFilesInDirectory(java.io.File directory) {
		java.util.LinkedList<java.io.File> files = new java.util.LinkedList<>();
		try {
			if (directory != null && directory.listFiles() != null) {
				for (java.io.File f : directory.listFiles()) {
					if (f.isDirectory()) {
						files.addAll(getClassFilesInDirectory(f));
					} else if (f.isFile() &&
					           (f.getName().endsWith(".class"))
					) {
						files.add(f);
					}
				}
			}
		} catch (RuntimeException e) {
			throw new java.io.IOError(e);
		}
		return files;
	}


	/**
	 * Creates a collection of strings representing the relative path of "files" to "directory"
	 * surrently only supported for subfolders
	 *
	 * @param directory
	 * 		the root directory
	 * @param files
	 * 		a collection of files
	 *
	 * @return a collection of strings representing realativ paths
	 */
	private static java.util.Collection<String> getRelativePathsforSubfolders(java.io.File directory,
	                                                                          Iterable<java.io.File> files) {
		java.util.Collection<String> paths = new java.util.LinkedList<>();
		for (java.io.File f : files) {
			try {
				paths.add(
						f.getCanonicalPath().replace(directory.getCanonicalPath(), ""));
			} catch (java.io.IOException e) {
				eu.mmonschau.reflection.util.JTextLog.getLogger()
						.log(java.util.logging.Level.FINEST, e.getMessage());
			}
		}
		return paths;
	}


	/**
	 * gets all superclasses and interfaces of a given class
	 *
	 * @param aClass
	 * 		the class to inspect
	 *
	 * @return a collection of superclasses and interfaces of the given class
	 */
	public static java.util.Collection<Class> getSuperClassesAndInterfaces(Class aClass) {
		java.util.Collection<Class> superClasses = new java.util.HashSet<>();
		Class                       currentSuper = aClass.getSuperclass();
		if (currentSuper != null) {
			superClasses.addAll(java.util.Arrays.asList(aClass.getInterfaces()));
			if (!currentSuper.equals(Object.class)) {
				superClasses.add(currentSuper);
				superClasses.addAll(getSuperClassesAndInterfaces(currentSuper));
			}
		}
		return superClasses;
	}


	/**
	 * Prints the class-Locations and all loadable classes, which are not shipped with the jre/jdk
	 *
	 * @param args
	 * 		CLIParams (unused)
	 */
	public static void main(String[] args) {
		try {
			java.util.Collection<java.io.File> allClassLocations = getAllClassLocations();
			System.out.println(allClassLocations);
			System.out.println("\n\n");
			java.util.stream.Stream<? extends Class<?>> classesFromJars = null;
			classesFromJars = getClassesFromJars(allClassLocations);
			java.util.stream.Stream<? extends Class<?>> allClassesOutsideOfJars = null;
			allClassesOutsideOfJars = getAllClassesOutsideOfJars(allClassLocations);
			//allClassesOutsideOfJars.forEach(System.out::println);
			java.util.stream.Stream.concat(classesFromJars, allClassesOutsideOfJars)
					.collect(java.util.stream.Collectors.toSet()).forEach(
					System.out::println);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println();
			System.err.println(e.getMessage());
			java.util.Arrays.asList(e.getStackTrace()).forEach(System.err::println);
			System.err.println();
			System.err.println(e.getCause().getMessage());
			java.util.Arrays.asList(e.getCause().getStackTrace())
					.forEach(System.err::println);
		}
	}
}
