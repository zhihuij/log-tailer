package com.netease.util.tailer;

/**
 * Tailer listener.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public interface TailerListener {
    /**
     * Init listener with the tailer.
     * 
     * @param tailer
     *            the tailer
     */
    void init(Tailer tailer);

    /**
     * Stop the listener.
     */
    void stop();

    /**
     * This method is called if the tailed file is not found.
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     */
    void fileNotFound();

    /**
     * Called if a file rotation is detected.
     * 
     * This method is called before the file is reopened, and fileNotFound may
     * be called if the new file has not yet been created.
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     */
    void fileRotated();

    /**
     * Handles a line from a Tailer.
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     * 
     * @param line
     *            the line.
     * @param position
     *            the read position.
     * @param lastModified
     *            last modified time.
     */
    void handle(String line, long position, long lastModified);

    /**
     * Handles an Exception .
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     * 
     * @param ex
     *            the exception.
     */
    void handle(Exception ex);
}
