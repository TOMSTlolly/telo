package com.tomst.lolly;

import static com.tomst.lolly.LollyActivity.DIRECTORY_LOGS;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


import android.content.Context;
import org.mockito.Mock;
import com.tomst.lolly.BuildConfig;



import com.tomst.lolly.core.TMSSim;
//@RunWith(MockitoJUnitRunner.class)
//@RunWith(PowerMockRunner.class)
///@PrepareForTest({Log.class})
//import com.tomst.lolly.core.TMSSim;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Before
    public void setUp() {
       // PowerMockito.mockStatic(Log.class);
       // Mockito.when(Log.e(anyString(), anyString())).thenReturn(0);
    }

    @Mock
    Context mockContext;

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void dividing_zero()
    {
        int i =1;
        int j = 10 /i;
    }

    @Test
    public void SimTest()
    {
        DIRECTORY_LOGS = "/home/krata/db/telo";  // adresar, kde je otisk komunikace s TMs
        TMSSim sim = new TMSSim(mockContext,"test.csv");
    }


}