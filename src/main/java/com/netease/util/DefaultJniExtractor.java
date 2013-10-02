/*
 * Copyright 2006 MX Telecom Ltd.
 */
package com.netease.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public class DefaultJniExtractor implements JniExtractor {
    private static boolean debug = false;

    /**
     * this is where JNI libraries are extracted to.
     */
    private File outputFile = null;

    static {
        // initialise the debug switch
        String s = System.getProperty("java.library.debug");
        if (s != null && (s.toLowerCase().startsWith("y") || s.startsWith("1")))
            debug = true;
    }

    /**
     * Gets the working directory to use for jni extraction.
     * <p>
     * Attempts to create it if it doesn't exist.
     * 
     * @return jni working dir
     * @throws IOException
     *             if there's a problem creating the dir
     */
    public File getJniFilePath(String filename) throws IOException {
        if (outputFile == null) {
            // Split filename to prexif and suffix (extension)
            String prefix = "";
            String suffix = null;
            String[] parts;
            if (filename != null) {
                parts = filename.split("\\.", 2);
                prefix = parts[0];
                suffix = (parts.length > 1) ? "." + parts[parts.length - 1] : null;
            }

            // Check if the filename is okay
            if (filename == null || prefix.length() < 3) {
                throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
            }

            outputFile = File.createTempFile(prefix, suffix);
            outputFile.deleteOnExit();
            if (debug) {
                System.err.println("Initialised JNI library to '" + outputFile + "'");
            }
        }
        return outputFile;
    }

    /**
     * extract a JNI library from the classpath
     * 
     * @param libname
     *            - System.loadLibrary() - compatible library name
     * @return the extracted file
     * @throws IOException
     */
    public File extractJni(String libname) throws IOException {
        String mappedlib = System.mapLibraryName(libname);

        /*
         * on darwin, the default mapping is to .jnilib; but we use .dylibs so
         * that library interdependencies are handled correctly. if we don't
         * find a .jnilib, try .dylib instead.
         */
        if (mappedlib.endsWith(".dylib")) {
            if (this.getClass().getClassLoader().getResource("META-INF/lib/" + mappedlib) == null)
                mappedlib = mappedlib.substring(0, mappedlib.length() - 6) + ".jnilib";
        }

        return extractResource("META-INF/lib/" + mappedlib, mappedlib);
    }

    /**
     * extract a resource to the tmp dir (this entry point is used for unit
     * testing)
     * 
     * @param resourcename
     *            the name of the resource on the classpath
     * @param outputname
     *            the filename to copy to (within the tmp dir)
     * @return the extracted file
     * @throws IOException
     */
    File extractResource(String resourcename, String outputname) throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resourcename);
        if (in == null)
            throw new IOException("Unable to find library " + resourcename + " on classpath");
        File outfile = getJniFilePath(outputname);
        if (debug)
            System.err.println("Extracting '" + resourcename + "' to '" + outfile.getAbsolutePath() + "'");
        OutputStream out = new FileOutputStream(outfile);
        copy(in, out);
        out.close();
        in.close();
        return outfile;
    }

    /**
     * copy an InputStream to an OutputStream.
     * 
     * @param in
     *            InputStream to copy from
     * @param out
     *            OutputStream to copy to
     * @throws IOException
     *             if there's an error
     */
    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] tmp = new byte[8192];
        int len = 0;
        while (true) {
            len = in.read(tmp);
            if (len <= 0) {
                break;
            }
            out.write(tmp, 0, len);
        }
    }
}
