package com.netease.util.tailer;

import java.io.File;

/**
 * Helper for create proper tailer.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class TailerHelper {

    /**
     * Default check interval.
     */
    private static final int DEFAULT_DELAY_MILLIS = 100;

    /**
     * Default buffer size for reading.
     */
    private static final int DEFAULT_BUFSIZE = 4096;

    /**
     * Creates a Tailer for the given file, starting from the beginning, with
     * the default delay of 100ms.
     * 
     * @param file
     *            The file to follow
     * @param listener
     *            the TailerListener to use
     */
    public static Tailer createTailer(File file, TailerListener listener) {
        return createTailer(file, listener, 0);
    }

    /**
     * Creates a Tailer for the given file, starting from the target position,
     * with the default delay of 100ms.
     * 
     * @param file
     *            The file to follow
     * @param listener
     *            the TailerListener to use
     * @param position
     *            position where tailer should start
     */
    public static Tailer createTailer(File file, TailerListener listener, long position) {
        return createTailer(file, listener, position, DEFAULT_DELAY_MILLIS);
    }

    /**
     * Creates a Tailer for the given file, starting from the beginning.
     * 
     * @param file
     *            the file to follow
     * @param listener
     *            the TailerListener to use
     * @param position
     *            position where tailer should start
     * @param delayMillis
     *            the delay between checks of the file for new content in
     *            milliseconds
     */
    public static Tailer createTailer(File file, TailerListener listener, long position, long delayMillis) {
        return createTailer(file, listener, position, delayMillis, DEFAULT_BUFSIZE);
    }

    /**
     * Creates a Tailer for the given file, with a specified buffer size.
     * 
     * @param file
     *            the file to follow
     * @param listener
     *            the TailerListener to use
     * @param position
     *            position where tailer should start
     * @param delayMillis
     *            the delay between checks of the file for new content in
     *            milliseconds
     * @param reOpen
     *            if true, close and reopen the file between reading chunks
     * @param bufSize
     *            buffer size
     */
    public static Tailer createTailer(File file, TailerListener listener, long position, long delayMillis, int bufSize) {
        return new Tailer(file, listener, position, delayMillis, bufSize);
    }
}
