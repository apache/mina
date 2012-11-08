package org.apache.mina.http;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Test class for DateUtil
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
