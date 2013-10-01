package com.netease.util.tailer;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Utility class for tailing log file.
 * <p>
 * Based on <code>Tailer</code> from apache-commons-io library, and rewrite by
 * adding channel size check to make the tailer more robust to file rotating.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class Tailer implements Runnable {
    /**
     * Open mode for RandomAccessFile.
     */
    private static final String RAF_MODE = "r";

    /**
     */
    private final byte inbuf[];

    /**
     * The file which will be tailed.
     */
    private final File file;

    /**
     * The amount of time to wait for the file to be updated.
     */
    private final long delayMillis;

    /**
     * The listener to notify of events when tailing.
     */
    private final TailerListener listener;

    /**
     * The tailer will run as long as this value is true.
     */
    private volatile boolean run = true;

    /**
     * The tailer will pause as long as this value is true.
     */
    private volatile boolean pause = false;

    /**
     * Whether to close and reopen the file whilst waiting for more input.
     */
    private final boolean reOpen;

    /**
     * Last modified time of the target file.
     */
    private long lastModified = 0;

    /**
     * Last position the tailer has read.
     */
    private long lastPosition = 0;

    /**
     * Creates a Tailer for the given file, with a specified buffer size.
     * 
     * @param file
     *            the file to follow
     * @param listener
     *            the TailerListener to use
     * @param position
     *            position where tailer should start
     * @param lastModified
     *            last modified time we should use for the first check
     * @param delayMillis
     *            the delay between checks of the file for new content in
     *            milliseconds
     * @param reOpen
     *            if true, close and reopen the file between reading chunks
     * @param bufSize
     *            buffer size
     */
    public Tailer(File file, TailerListener listener, long position, long lastModified, long delayMillis, int bufSize,
            boolean reOpen) {
        this.file = file;
        this.lastPosition = position;
        this.delayMillis = delayMillis;
        this.lastModified = lastModified;
        this.inbuf = new byte[bufSize];
        this.reOpen = reOpen;

        // save and prepare the listener
        this.listener = listener;
        this.listener.init(this);
    }

    /**
     * Allows the tailer to complete its current loop and return.
     */
    public void stop() {
        this.run = false;
    }

    /**
     * Allows the tailer to complete its current loop and pause.
     */
    public void pause() {
        this.pause = true;
    }

    /**
     * Allows the tailer to continue its current loop.
     */
    public void resume() {
        this.pause = false;
    }

    /**
     * Return the file.
     * 
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Return the delay in milliseconds.
     * 
     * @return the delay in milliseconds.
     */
    public long getDelay() {
        return delayMillis;
    }

    /**
     * Follows changes in the file, calling the TailerListener's handle method
     * for each new line.
     */
    public void run() {
        RandomAccessFile reader = null;
        try {
            // Open the file
            while (run && reader == null) {
                try {
                    reader = new RandomAccessFile(file, RAF_MODE);
                } catch (FileNotFoundException e) {
                    listener.fileNotFound();
                }

                if (reader == null) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException e) {
                    }
                } else {
                    // last modified and last position already set in
                    // constructor
                    reader.seek(lastPosition);
                }
            }

            while (run) {
                while (pause) {
                    Thread.sleep(delayMillis);
                }

                boolean newer = isFileNewer(file, lastModified);
                long size = reader.getChannel().size();
                long length = file.length();

                System.out.println("newer=" + newer + ", size=" + size + ", lastPostion=" + lastPosition + ", length="
                        + length + ", fileLastModified=" + file.lastModified() + ", lastModified=" + lastModified);

                if (size > lastPosition) {
                    // the file has more content than it did last time
                    lastPosition = readLines(reader);
                    if (length != size) {
                        System.out.println("new file created");
                        // new file created
                        continue;
                    } else {
                        /*
                         * we don't know it's the old file updated or a new file
                         * with same size was created.
                         * 
                         * FIXME if the new file with same size was created,
                         * then: if this new file will be updated before next
                         * file rotate, then the content in the new file will be
                         * read in a future check; but if it's not true, the
                         * content in the new file will be lost.
                         */
                        lastModified = System.currentTimeMillis();
                    }
                } else if (newer && size == lastPosition) {
                    // file was rotated
                    listener.fileRotated();

                    while (length == 0) {
                        // file does not exist or have nothing
                        try {
                            Thread.sleep(delayMillis);
                        } catch (InterruptedException e) {
                            // do nothing
                        }

                        length = file.length();
                    }

                    try {
                        /*
                         * make sure the file exist and have content, reopen the
                         * reader after rotation, ensure that the old file is
                         * closed iff we re-open it successfully
                         */
                        RandomAccessFile save = reader;
                        reader = new RandomAccessFile(file, RAF_MODE);
                        // use the old last modified time
                        lastPosition = 0;

                        /*
                         * close old file explicitly rather than relying on GC
                         * picking up previous RAF
                         */
                        closeQuietly(save);
                    } catch (FileNotFoundException e) {
                        /*
                         * in this case we continue to use the previous reader
                         * and position values
                         */
                        listener.fileNotFound();
                    }
                    continue;
                } else {
                    // file not change
                }

                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    // ignore
                }

                if (reOpen) {
                    closeQuietly(reader);

                    reader = new RandomAccessFile(file, RAF_MODE);
                    reader.seek(lastPosition);
                }
            }

            listener.stop();
        } catch (Exception e) {
            listener.handle(e);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Read new lines.
     * 
     * @param reader
     *            The file to read
     * @return The new position after the lines have been read
     * @throws java.io.IOException
     *             if an I/O error occurs.
     */
    protected long readLines(RandomAccessFile reader) throws IOException {
        StringBuilder sb = new StringBuilder();

        long pos = reader.getFilePointer();
        long rePos = pos; // position to re-read

        int num;
        while (run && ((num = reader.read(inbuf)) != -1)) {
            for (int i = 0; i < num; i++) {
                byte ch = inbuf[i];
                switch (ch) {
                case '\n':
                    listener.handle(sb.toString(), pos + i + 1, file.lastModified());
                    sb.setLength(0);
                    rePos = pos + i + 1;
                    break;
                case '\r':
                    if (sb.length() != 0) {
                        listener.handle(sb.toString(), pos + i + 1, file.lastModified());
                        sb.setLength(0);
                    }
                    rePos = pos + i + 1;
                    break;
                default:
                    sb.append((char) ch); // add character, not its ascii value
                }
            }

            pos = reader.getFilePointer();
        }

        reader.seek(rePos); // Ensure we can re-read if necessary
        return rePos;
    }

    protected void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * Check if the target file is updated.
     * 
     * @param file
     *            the target file
     * @param timeMillis
     *            last modified time that saved on last check
     * @return true if file is updated, false otherwise
     */
    protected boolean isFileNewer(File file, long timeMillis) {
        if (file == null) {
            throw new IllegalArgumentException("No specified file");
        }
        if (!file.exists()) {
            return false;
        }
        return file.lastModified() > timeMillis;
    }

}
