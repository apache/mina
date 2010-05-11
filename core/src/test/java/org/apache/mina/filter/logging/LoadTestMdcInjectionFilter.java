package org.apache.mina.filter.logging;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.textui.TestRunner;

import java.util.Date;

public class LoadTestMdcInjectionFilter {

    /**
     * The MdcInjectionFilterTest is unstable, it fails sporadically (and only on Windows ?)
     * This is a quick and dirty program to run the MdcInjectionFilterTest many times.
     * To be removed once we consider DIRMINA-784 to be fixed
     *
     */
    public static void main(String[] args) {
        TestRunner runner = new TestRunner();

        try {
            for (int i=0; i<50000; i++) {
                Test test = new JUnit4TestAdapter(MdcInjectionFilterTest.class);
                runner.doRun(test);
                System.out.println("i = " + i + " " + new Date());
            }
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);

    }
}
