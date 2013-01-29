package org.apache.mina.http;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for DateUtil
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DateUtilTest {

    @Test
    public void testGetCurrentAsString() {
        Date date = new Date();
        String dateAsString = DateUtil.getCurrentAsString();
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String testConvertedDate = dateFormat.format(date);
        Assert.assertEquals(testConvertedDate, dateAsString);
    }

}
