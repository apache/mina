/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

/**
 * Contains SumUp protocol constants.
 *
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Constants
{
    public static final int TYPE_LEN = 2;

    public static final int SEQUENCE_LEN = 4;

    public static final int HEADER_LEN = TYPE_LEN + SEQUENCE_LEN;

    public static final int BODY_LEN = 12;

    public static final int RESULT = 0;

    public static final int ADD = 1;

    public static final int RESULT_CODE_LEN = 2;

    public static final int RESULT_VALUE_LEN = 4;

    public static final int ADD_BODY_LEN = 4;

    public static final int RESULT_OK = 0;

    public static final int RESULT_ERROR = 1;

    private Constants()
    {
    }

}