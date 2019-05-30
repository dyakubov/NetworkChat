package lesson6;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.geekbrains.Task3;

public class Task3Test {
    private Task3 task3;

    @Before
    public void init(){
        task3 = new Task3();
    }

    @Test
    public void testDoIt1(){
        Integer[] test = new Integer[]{1, 4, 1, 4, 1, 4};
        Assert.assertTrue(task3.doIt(test));
    }

    @Test
    public void testDoIt2(){
        Integer[] test = new Integer[]{3, 4, 5, 4, 6, 4};
        Assert.assertFalse(task3.doIt(test));
    }

    @Test
    public void testDoIt3(){
        Integer[] test = new Integer[]{};
        Assert.assertFalse(task3.doIt(test));
    }

    @Test
    public void testDoIt4(){
        Integer[] test = new Integer[]{1};
        Assert.assertFalse(task3.doIt(test));
    }
}
