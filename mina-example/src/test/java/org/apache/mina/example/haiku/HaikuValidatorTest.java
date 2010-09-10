/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.example.haiku;

import org.junit.Before;
import org.junit.Test;


/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HaikuValidatorTest {
    // from http://allrileyedup.blogspot.com/2006/10/dont-hassle-haiku.html -- good friend of proyal@apache.org
    private static final String[] HAIKUS = {
            "This class is boring.\n" + "Will David ever shut up?\n"
                    + "What is Steph wearing?",

            "Oh, I drank too much.\n" + "Why, oh why did I sign up\n"
                    + "For an eight thirty?",

            "Which one should I do?\n" + "Wax my chest or perm my hair?\n"
                    + "Can’t wait to decide.",

            "Watch my video.\n" + "I can't stop this fee-ee-ling!\n"
                    + "What is wrong with me?",

            "The car chases me.\n" + "I must get away from it.\n"
                    + "Turbo Boost! Oh, yeah.",

            "My new slogan is\n" + "Don't hassle me... I'm oiling.\n"
                    + "You know it’s so true.",

            "Michael, I love you.\n" + "I long for you to tell me\n"
                    + "\"KITT, need you buddy.\"",

            "In Knight Rider, I’m\n" + "A Man Who Does Not Exist.\n"
                    + "(Except in your dreams).",

            "Yes, I’m Michael Knight\n" + "Check out my unbuttoned shirt.\n"
                    + "And sexy tight pants.",

            "My bitch ex-wife sucks.\n" + "And so do all the airlines.\n"
                    + "I miss Knight Rider.",

            "I am Michael Knight.\n" + "I am David Hasselhoff.\n"
                    + "I’m not Rick James, bitch." };

    private HaikuValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new HaikuValidator();
    }

    @Test
    public void testValidateHaikus() throws Exception {
        for (String s : HAIKUS) {
            String[] lines = s.split("\n");

            Haiku haiku = new Haiku(lines);

            validator.validate(haiku);
        }
    }
}
