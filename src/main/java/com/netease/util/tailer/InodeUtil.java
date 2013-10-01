package com.netease.util.tailer;

/**
 * Utility class for getting inode of a file.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class InodeUtil {
    static {
        System.loadLibrary("inodeutil");
    }

    /**
     * Get inode of a file with the absolute path.
     * 
     * @param path
     *            path of the file
     * @return the inode of the file
     */
    public static native long getInode(String path);
}
