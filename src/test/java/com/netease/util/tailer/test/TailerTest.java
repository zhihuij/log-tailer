package com.netease.util.tailer.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.netease.util.tailer.Tailer;
import com.netease.util.tailer.TailerHelper;
import com.netease.util.tailer.TailerListener;

/**
 * Test case for tailer class.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class TailerTest {
    private static final String OLD_FILE_LINE = "old";
    private static final String NEW_FILE_LINE = "new";

    abstract class TestLisenter implements TailerListener {
        @Override
        public void init(Tailer tailer) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void fileNotFound() {
        }

        @Override
        public void handle(Exception ex) {
        }
    }

    class Listener1 extends TestLisenter {
        private List<String> resultList = new ArrayList<String>();

        public List<String> getResult() {
            return resultList;
        }

        @Override
        public void handle(String line, long position, long lastModified) {
            resultList.add(line);
        }

        @Override
        public void fileRotated() {
        }
    }

    /**
     * Old file updated, and no new file created.
     */
    @Test
    public void testOldUpdate() throws Exception {
        File targetFile = new File("test_data/tailer_target");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        Assert.assertFalse(targetFile.exists());

        BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true));
        for (int i = 0; i < 100; i++) {
            writer.write(OLD_FILE_LINE + i);
            writer.newLine();
            writer.flush();
        }

        Listener1 taiListener = new Listener1();

        Tailer tailer = TailerHelper.createTailer(targetFile, taiListener, 0);
        Thread thread = new Thread(tailer);
        thread.start();

        Thread.sleep(1000);

        List<String> resultList = taiListener.getResult();
        Assert.assertEquals(100, resultList.size());

        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(resultList.get(i).equals(OLD_FILE_LINE + i));
        }

        for (int i = 100; i < 200; i++) {
            writer.write(OLD_FILE_LINE + i);
            writer.newLine();
            writer.flush();
        }

        writer.close();

        Thread.sleep(1000);

        resultList = taiListener.getResult();
        Assert.assertEquals(200, resultList.size());

        for (int i = 0; i < 200; i++) {
            Assert.assertTrue(resultList.get(i).equals(OLD_FILE_LINE + i));
        }

        tailer.stop();
        thread.join();
    }

    /**
     * New file created, and old file doesn't change.
     */
    @Test
    public void testOldAndNew() throws Exception {
        File oldFile = new File("test_data/tailer_target.bak");
        File targetFile = new File("test_data/tailer_target");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (oldFile.exists()) {
            oldFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true));
        for (int i = 0; i < 100; i++) {
            writer.write(OLD_FILE_LINE + i);
            writer.newLine();
            writer.flush();
        }
        writer.close();

        Listener1 taiListener = new Listener1();
        Tailer tailer = TailerHelper.createTailer(targetFile, taiListener, 0);
        Thread thread = new Thread(tailer);
        thread.start();

        Thread.sleep(1000);

        targetFile.renameTo(oldFile);
        Assert.assertFalse(targetFile.exists());
        targetFile.createNewFile();
        Assert.assertTrue(targetFile.exists());

        Thread.sleep(1000);

        writer = new BufferedWriter(new FileWriter(targetFile, true));
        for (int i = 0; i < 100; i++) {
            writer.write(NEW_FILE_LINE + i);
            writer.newLine();
            writer.flush();
        }
        writer.close();

        Thread.sleep(1000);

        List<String> resultList = taiListener.getResult();
        Assert.assertEquals(200, resultList.size());

        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(resultList.get(i).equals(OLD_FILE_LINE + i));
        }

        for (int i = 100; i < 200; i++) {
            Assert.assertTrue(resultList.get(i).equals(NEW_FILE_LINE + (i - 100)));
        }

        tailer.stop();
        thread.join();
    }

    class Listener2 extends TestLisenter {
        private List<String> resultList = new ArrayList<String>();
        private boolean newFile = false;

        private volatile boolean readDone = false;

        public List<String> getResult() {
            return resultList;
        }

        public boolean isNewFile() {
            return newFile;
        }

        public boolean isReadDone() {
            return readDone;
        }

        @Override
        public void handle(String line, long position, long lastModified) {
            System.out.println(line);
            resultList.add(line);

            if (resultList.size() == 100) {
                readDone = true;
            }
        }

        @Override
        public void fileRotated() {
            newFile = true;
        }
    }

    private void writeFile(BufferedWriter writer, int start, int size, String line) throws Exception {
        for (int i = start; i < start + size; i++) {
            writer.write(line + i);
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Old file updated, and new file created, and new file is smaller than old
     * file.
     */
    @Test
    public void testOldUpdateAndNew_SmallNewFile() throws Exception {
        File oldFile = new File("test_data/tailer_target.bak");
        File targetFile = new File("test_data/tailer_target");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (oldFile.exists()) {
            oldFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 0, 100, OLD_FILE_LINE);

        Listener2 taiListener = new Listener2();
        Tailer tailer = TailerHelper.createTailer(targetFile, taiListener, 0);
        Thread thread = new Thread(tailer);
        thread.start();

        Thread.sleep(1000);

        while (!taiListener.isReadDone()) {
            Thread.sleep(10);
        }

        /*
         * old file has been read, pause the tailer, update old file, create the
         * new file and write content to it
         */
        tailer.pause();

        // update old file
        writeFile(writer, 100, 100, OLD_FILE_LINE);
        writer.close();

        // rename the old file and create the new file
        targetFile.renameTo(oldFile);
        Assert.assertFalse(targetFile.exists());
        targetFile.createNewFile();
        Assert.assertTrue(targetFile.exists());

        // write the new file
        writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 0, 100, NEW_FILE_LINE);
        writer.close();

        tailer.resume();

        Thread.sleep(1000);

        List<String> resultList = taiListener.getResult();
        Assert.assertTrue(taiListener.isNewFile());
        Assert.assertEquals(300, resultList.size());

        for (int i = 0; i < 200; i++) {
            Assert.assertTrue(resultList.get(i).equals(OLD_FILE_LINE + i));
        }

        for (int i = 200; i < 300; i++) {
            Assert.assertTrue(resultList.get(i).equals(NEW_FILE_LINE + (i - 200)));
        }

        tailer.stop();
        thread.join();
    }

    /**
     * Old file updated, and new file created, and new file is bigger than old
     * file.
     */
    @Test
    public void testOldUpdateAndNew_BigNewFile() throws Exception {

        File oldFile = new File("test_data/tailer_target.bak");
        File targetFile = new File("test_data/tailer_target");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (oldFile.exists()) {
            oldFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 0, 100, OLD_FILE_LINE);

        Listener2 taiListener = new Listener2();
        Tailer tailer = TailerHelper.createTailer(targetFile, taiListener, 0);
        Thread thread = new Thread(tailer);
        thread.start();

        Thread.sleep(1000);

        while (!taiListener.isReadDone()) {
            Thread.sleep(10);
        }

        /*
         * old file has been read, pause the tailer, update old file, create the
         * new file and write content to it
         */
        tailer.pause();

        // update old file
        writeFile(writer, 100, 100, OLD_FILE_LINE);
        writer.close();

        // rename the old file and create the new file
        targetFile.renameTo(oldFile);
        Assert.assertFalse(targetFile.exists());
        targetFile.createNewFile();
        Assert.assertTrue(targetFile.exists());

        // write the new file
        writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 0, 300, NEW_FILE_LINE);
        writer.close();

        tailer.resume();

        Thread.sleep(1000);

        List<String> resultList = taiListener.getResult();
        Assert.assertTrue(taiListener.isNewFile());
        Assert.assertEquals(500, resultList.size());

        for (int i = 0; i < 200; i++) {
            Assert.assertTrue(resultList.get(i).equals(OLD_FILE_LINE + i));
        }

        for (int i = 200; i < 500; i++) {
            Assert.assertTrue(resultList.get(i).equals(NEW_FILE_LINE + (i - 200)));
        }

        tailer.stop();
        thread.join();
    }

    /**
     * Old file updated, and new file created, and new file is equal with old
     * file.
     */
    @Test
    public void testOldUpdateAndNew_Equal() throws Exception {

        File oldFile = new File("test_data/tailer_target.bak");
        File targetFile = new File("test_data/tailer_target");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (oldFile.exists()) {
            oldFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 0, 100, OLD_FILE_LINE);

        Listener2 taiListener = new Listener2();
        Tailer tailer = TailerHelper.createTailer(targetFile, taiListener, 0);
        Thread thread = new Thread(tailer);
        thread.start();

        Thread.sleep(1000);

        while (!taiListener.isReadDone()) {
            Thread.sleep(10);
        }

        /*
         * old file has been read, pause the tailer, update old file, create the
         * new file and write content to it
         */
        tailer.pause();

        // update old file
        writeFile(writer, 100, 100, OLD_FILE_LINE);
        writer.close();

        // rename the old file and create the new file
        targetFile.renameTo(oldFile);
        Assert.assertFalse(targetFile.exists());
        targetFile.createNewFile();
        Assert.assertTrue(targetFile.exists());

        // write the new file
        writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 0, 200, NEW_FILE_LINE);

        tailer.resume();

        Thread.sleep(10 * 60 *1000);

        // new file is not read in this case
        List<String> resultList = taiListener.getResult();
        Assert.assertFalse(taiListener.isNewFile());
        Assert.assertEquals(200, resultList.size());

        for (int i = 0; i < 200; i++) {
            Assert.assertTrue(resultList.get(i).equals(OLD_FILE_LINE + i));
        }

        // we need to update the new file to make the file read
        writer = new BufferedWriter(new FileWriter(targetFile, true));
        writeFile(writer, 200, 10, NEW_FILE_LINE);

        Thread.sleep(1000);

        resultList = taiListener.getResult();
        Assert.assertTrue(taiListener.isNewFile());
        Assert.assertEquals(410, resultList.size());

        for (int i = 200; i < 410; i++) {
            Assert.assertTrue(resultList.get(i).equals(NEW_FILE_LINE + (i - 200)));
        }

        tailer.stop();
        thread.join();
    }
}
