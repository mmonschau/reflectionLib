/*
 * Copyright (c) Michael Monschau 2018.
 */

package eu.mmonschau.reflection.util;

/**
 * Wrapper for Singelton JavaLogger
 *
 * @see java.util.logging.Logger
 */
public class JTextLog {
	private static java.util.logging.Logger  logger;
	private static java.util.logging.Handler handle;

	public static void setLogger(java.util.logging.Logger logger) {
		JTextLog.logger = logger;
	}


	public static java.util.logging.Logger getLogger() {
		if (logger == null) {
			logger = java.util.logging.Logger.getGlobal();
			handle = new SysoutHandler();
			logger.addHandler(handle);
		}
		return logger;
	}

	public static void setLogLevel(java.util.logging.Level l) {
		getLogger().setLevel(l);
		if (handle != null) {
			handle.setLevel(l);
		}
	}


	private static class SysoutHandler extends java.util.logging.ConsoleHandler {
		protected void setOutputStream(java.io.OutputStream out) throws SecurityException {
			super.setOutputStream(System.out); // kitten killed here :-(
		}
	}

}


