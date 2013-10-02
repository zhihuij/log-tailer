package com.netease.util;

import java.io.IOException;

/**
 * Utility class for getting inode of a file.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class InodeUtil {
    static {
        try {
            NativeLoader.loadLibrary("inodeutil");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("can't find library inodeutil");
            System.exit(1);
        }
        // System.loadLibrary("inodeutil");
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
