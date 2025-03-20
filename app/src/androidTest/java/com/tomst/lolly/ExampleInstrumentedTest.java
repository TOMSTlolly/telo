package com.tomst.lolly;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import com.tomst.lolly.core.TMSSim;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.tomst.lolly", appContext.getPackageName());
    }

    @Test
    public void test_parser()
    {
       // projde command_ soubory ve vybranem adresari a data pretahne do zvlastniho adresare
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        TMSSim sim = new TMSSim("test.csv");

    }
}