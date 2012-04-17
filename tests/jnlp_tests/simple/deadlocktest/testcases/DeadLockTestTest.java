/* DeadLockTestTest.java
Copyright (C) 2011 Red Hat, Inc.

This file is part of IcedTea.

IcedTea is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 2.

IcedTea is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with IcedTea; see the file COPYING.  If not, write to
the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version.
 */

import java.util.ArrayList;
import net.sourceforge.jnlp.ServerAccess;
import net.sourceforge.jnlp.ServerAccess.ProcessResult;
import org.junit.Assert;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;

import org.junit.Test;

public class DeadLockTestTest {

    private static ServerAccess server = new ServerAccess();
    private static String deadlocktest_1 = "/deadlocktest_1.jnlp";
    private static String deadlocktest = "/deadlocktest.jnlp";

    @BeforeClass
    public static void printJavas() throws Exception {
        System.out.println("Currently runnng javas1 " + countJavaInstances());

    }

    @Test
    public void testDeadLockTestTerminated() throws Exception {
        testDeadLockTestTerminatedBody(deadlocktest);
    }

    @Test
    public void testDeadLockTestTerminated2() throws Exception {
        testDeadLockTestTerminatedBody(deadlocktest_1);
    }

    public void testDeadLockTestTerminatedBody(String jnlp) throws Exception {
        List<String> before = countJavaInstances();
        System.out.println("java1 "+jnlp+" : " + before.size());
        System.out.println("connecting " + jnlp + " request");
        System.err.println("connecting " + jnlp + " request");
        ServerAccess.ProcessResult pr = server.executeJavawsHeadless(null, jnlp);
        System.out.println(pr.stdout);
        System.err.println(pr.stderr);
        assertDeadlockTestLaunched(pr);
        List<String> after = countJavaInstances();
        System.out.println("java2 "+jnlp+" : " + after.size());
        String ss="This process is hanging more than 30s. Should be killed";
        Assert.assertFalse("stdout shoud not contains: "+ss+", but did",pr.stdout.contains(ss));
//        Assert.assertTrue(pr.stderr.contains("xception"));, exception is thrown by engine,not by application
        Assert.assertTrue("testDeadLockTestTerminated should be terminated, but wasn't", pr.wasTerminated);
        Assert.assertEquals(null, pr.returnValue);//killed process have no value
        killDiff(before, after);
        List<String> afterKill = countJavaInstances();
        System.out.println("java3 "+jnlp+" : " + afterKill.size());
        Assert.assertEquals("assert that just old javas remians", 0, (before.size() - afterKill.size()));
    }

    @Test
    public void ensureAtLeasOneJavaIsRunning() throws Exception {
        Assert.assertTrue("at least one java should be running, but isn't! Javas are probably counted badly", countJavaInstances().size() > 0);

    }

    @Test
    public void testSimpletest1lunchFork() throws Exception {
        System.out.println("connecting " + deadlocktest_1 + " request");
        System.err.println("connecting " + deadlocktest_1 + " request");
        List<String> before = countJavaInstances();
        System.out.println("java4: " + before.size());
        BackgroundDeadlock bd = new BackgroundDeadlock(deadlocktest_1, null);
        bd.start();
        Thread.sleep(ServerAccess.PROCESS_TIMEOUT * 2 / 3);
        List<String> during = countJavaInstances();
        System.out.println("java5: " + during.size());
        waitForBackgroundDeadlock(bd);
        List<String> after = countJavaInstances();
        System.out.println("java6: " + after.size());
        Assert.assertNotNull("proces inside background deadlock cant be null. Was.", bd.getPr());
        System.out.println(bd.getPr().stdout);
        System.err.println(bd.getPr().stderr);
        assertDeadlockTestLaunched(bd.getPr());
        killDiff(before, during);
        List<String> afterKill = countJavaInstances();
        System.out.println("java66: " + afterKill.size());
        Assert.assertEquals("assert that just old javas remians", 0, (before.size() - afterKill.size()));
        // div by two is caused by jav in java process hierarchy
        Assert.assertEquals("launched JVMs must be exactly 2, was " + (during.size() - before.size()) / 2, 2, (during.size() - before.size()) / 2);
    }

    @Test
    public void testSimpletest1lunchNoFork() throws Exception {
        System.out.println("connecting " + deadlocktest_1 + " Xnofork request");
        System.err.println("connecting " + deadlocktest_1 + " Xnofork request");
        List<String> before = countJavaInstances();
        System.out.println("java7: " + before.size());
        BackgroundDeadlock bd = new BackgroundDeadlock(deadlocktest_1, Arrays.asList(new String[]{"-Xnofork"}));
        bd.start();
        Thread.sleep(ServerAccess.PROCESS_TIMEOUT * 2 / 3);
        List<String> during = countJavaInstances();
        System.out.println("java8: " + during.size());
        waitForBackgroundDeadlock(bd);
        List<String> after = countJavaInstances();
        System.out.println("java9: " + after.size());
        Assert.assertNotNull("proces inside background deadlock cant be null. Was.", bd.getPr());
        System.out.println(bd.getPr().stdout);
        System.err.println(bd.getPr().stderr);
        assertDeadlockTestLaunched(bd.getPr());
        killDiff(before, during);
        List<String> afterKill = countJavaInstances();
        System.out.println("java99: " + afterKill.size());
        Assert.assertEquals("assert that just old javas remians", 0, (before.size() - afterKill.size()));
        // div by two is caused by jav in java process hierarchy
        Assert.assertEquals("launched JVMs must be exactly 1, was  " + (during.size() - before.size()) / 2, 1, (during.size() - before.size()) / 2);
    }

    /**
     * by process assasin destroyed processes are hanging random amount of time as zombies.
     * Kill -9 is handling zombies pretty well.
     *
     * This function kills or  processes which are in nw but are not in old
     * (eq.to killing new zombies:) )
     *
     * @param old
     * @param nw
     * @return
     * @throws Exception
     */
    private static List<String> killDiff(List<String> old, List<String> nw) throws Exception {
        ensureLinux();
        List<String> result = new ArrayList<String>();
        for (String string : nw) {
            if (old.contains(string)) {
                continue;
            }
            System.out.println("Killing " + string);
            ServerAccess.ProcessResult pr = ServerAccess.executeProcess(Arrays.asList(new String[]{"kill", "-9", string}));
            result.add(string);
            //System.out.println(pr.stdout);
            // System.err.println(pr.stderr);
            System.out.println("Killed " + string);
        }
        return result;
    }

    private static List<String> countJavaInstances() throws Exception {
        ensureLinux();
        List<String> result = new ArrayList<String>();
        ServerAccess.ProcessResult pr = ServerAccess.executeProcess(Arrays.asList(new String[]{"ps", "-eo", "pid,ppid,stat,fname"}));
        Matcher m = Pattern.compile("\\s*\\d+\\s+\\d+ .+ java\\s*").matcher(pr.stdout);
        //System.out.println(pr.stdout);
        //System.err.println(pr.stderr);
        int i = 0;
        while (m.find()) {
            i++;
            String ss = m.group();
            //System.out.println(i+": "+ss);
            result.add(ss.trim().split("\\s+")[0]);
        }
        return result;

    }

    public static void main(String[] args) throws Exception {
        System.out.println(countJavaInstances());
    }

    private void assertDeadlockTestLaunched(ProcessResult pr) {
        String s = "Deadlock test started";
        Assert.assertTrue("Deadlock test should print out " + s + ", but did not", pr.stdout.contains(s));
        String ss = "xception";
        Assert.assertFalse("Deadlock test should not stderr " + ss + " but did", pr.stderr.contains(ss));
    }

    private void waitForBackgroundDeadlock(final BackgroundDeadlock bd) throws InterruptedException {
        while (!bd.isFinished()) {
            Thread.sleep(500);

        }
    }

    private static class BackgroundDeadlock extends Thread {

        private boolean finished = false;
        private ProcessResult pr = null;
        String jnlp;
        List<String> args;

        public BackgroundDeadlock(String jnlp, List<String> args) {
            this.jnlp = jnlp;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                pr = server.executeJavawsHeadless(args, jnlp);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                finished = true;
            }

        }

        public ProcessResult getPr() {
            return pr;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    private static void ensureLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!(os.contains("linux") || os.contains("unix"))) {
            throw new IllegalStateException("This test can be procesed only on linux like machines");
        }
    }
}
