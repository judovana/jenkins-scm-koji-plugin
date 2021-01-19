package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public class ResultsDbTest {

    private static ResultsDb db;
    private static AtomicInteger failures = new AtomicInteger(0);
    private static AtomicInteger checks = new AtomicInteger(0);
    private static AtomicInteger exs = new AtomicInteger(0);

    private static AtomicInteger ttl = new AtomicInteger(200_000);

    private static abstract class Trouble implements Runnable {
        @Override
        public void run() {
            while (ttl.get() > 0) {
                int q = ttl.addAndGet(-1);
                int action = q % 4;
                switch (action) {
                    case 0:
                        set();
                        break;
                    case 1:
                        del();
                        break;
                    case 2:
                        get();
                        break;
                    case 3:
                        nvrs();
                        break;
                }
            }
        }

        private static void checkCheck(String check) {
            if (check.trim().isEmpty()) {
                return;
            }
            checks.incrementAndGet();
            String[] ch = check.split("\n");
            for (String s : ch) {
                String[] ss = s.split(":");
                if (ss.length < 4) {
                    System.out.println("bad record: " + s);
                    failures.incrementAndGet();
                }
                for (int i = 3; i < ss.length; i++) {
                    String[] sss = ss[i].split(";");
                    if (sss.length != 2) {
                        System.out.println("bad record: " + s + " (" + ss[i] + ")");
                        failures.incrementAndGet();
                    }
                }

            }
        }

        private void set() {
            try {
                String s = "" + getNext();
                String a = db.getSet(s, s, s, s);
                String check = db.getScore(null, null, null);
                checkCheck(check);
            }catch (ResultsDb.ItemNotFoundException e){
                //ok
            } catch (Exception ex) {
                ex.printStackTrace();
                exs.incrementAndGet();
            }
        }

        private void get() {
            try {
                String s = "" + getNext();
                String a = db.getScore(s, s, s);
                String check = db.getScore(null, null, null);
                checkCheck(check);
            }catch (ResultsDb.ItemNotFoundException e){
                //ok
            } catch (Exception ex) {
                ex.printStackTrace();
                exs.incrementAndGet();
            }
        }

        private void nvrs() {
            try {
                String a = db.getNvrs();
                String check = db.getScore(null, null, null);
                checkCheck(check);
            }catch (ResultsDb.ItemNotFoundException e){
                //ok
            } catch (Exception ex) {
                ex.printStackTrace();
                exs.incrementAndGet();
            }
        }

        private void del() {
            try {
                String s = "" + getNext();
                String a = db.getSet(s, s, s, s);
                String check = db.getScore(null, null, null);
                checkCheck(check);
            }catch (ResultsDb.ItemNotFoundException e){
                //ok
            } catch (Exception ex) {
                ex.printStackTrace();
                exs.incrementAndGet();
            }
        }

        abstract int getNext();
    }

    private static class TenSequentialTroubles extends Trouble {
        int counter = -1;

        @Override
        int getNext() {
            counter++;
            if (counter > 10) {
                counter = 0;
            }
            return counter;
        }
    }

    private static class TenRandomTroubles extends Trouble {
        Random r = new Random();

        @Override
        int getNext() {
            return r.nextInt(10);
        }
    }

    private static class TenStacionaryTroubles extends Trouble {
        private final int i;

        public TenStacionaryTroubles(int i) {
            this.i = i;
        }

        @Override
        int getNext() {
            return i;
        }
    }

    @Test
    public void stressDb() throws IOException, InterruptedException {
        JenkinsCliWrapper.killCli();
        final File oTool = Files.createTempDirectory("oTool").toFile();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(oTool);
        DataGenerator.getSettings(folderHolder).getResultsFile().createNewFile();
        db = new ResultsDb(DataGenerator.getSettings(folderHolder));

        List<Thread> l = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            l.add(new Thread(new TenRandomTroubles()));
            l.add(new Thread(new TenSequentialTroubles()));
            l.add(new Thread(new TenStacionaryTroubles(i)));
        }
        for (Thread t : l) {
            t.start();
        }
        for (Thread t : l) {
            t.join();
        }
        System.out.println("exceptions: " + exs.toString() + " from " + checks.toString());
        System.out.println("failed    : " + failures.toString() + " from " + checks.toString());
        Assert.assertEquals(0, failures.get());
        Assert.assertEquals(0, exs.get());
    }

    @Test
    public void checkSave() throws IOException, InterruptedException {
        JenkinsCliWrapper.killCli();
        final File oTool = Files.createTempDirectory("oTool").toFile();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(oTool);
        DataGenerator.getSettings(folderHolder).getResultsFile().createNewFile();
        db = new ResultsDb(DataGenerator.getSettings(folderHolder));
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getSet("aa","bb","1", "100000000");
        }
        //x rewrites of same item, no save
        long l1 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("1)"+l1);
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getSet("a","b","1", ""+i);
        }
        //now updating item
        long l2 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("2)"+l2);
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getSet("a","b",""+i, "0");
        }
        //now adding items
        long l3 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("3)"+l3);
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getSet("a","b"+i,"1", "0");
        }
        long l4 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("4)"+l4);
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getSet("a"+i,"b","1", "100000000");
        }
        long l5 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("5)"+l5);
        //removal of mising item;
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getDel("aa","bb","1", "100000000");
        }
        long l6 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("6)"+l6);
        //removal of same item;
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getSet("a","b","1", ""+i);
        }
        long l7 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("7)"+l7);
        //real remval of items
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getDel("a","b",""+i, "0");
        }
        long l8 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("8)"+l8);
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getDel("a","b"+i,"1", "0");
        }
        long l9 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("9)"+l9);
        for(int i = 0; i <= ResultsDb.LIMIT_TO_SAVE; i++){
            String a = db.getDel("a"+i,"b","1", "100000000");
        }
        long l0 = DataGenerator.getSettings(folderHolder).getResultsFile().length();
        System.out.println("0)"+l0);
        //878 is minimal growth of file
        Assert.assertTrue(l1+800<l2);
        Assert.assertTrue(l2+800<l3);
        Assert.assertTrue(l3+800<l4);
        Assert.assertTrue(l4+800<l5);
        Assert.assertTrue(l5==l6);
        Assert.assertTrue(l6==l7);
        Assert.assertTrue(l7>l8+800);
        Assert.assertTrue(l8>l9+800);
        Assert.assertTrue(l9>l0+800);


    }

}
