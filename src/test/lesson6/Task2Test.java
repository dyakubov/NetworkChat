package lesson6;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.geekbrains.Task2;

public class Task2Test {

    private Task2 task2;


    @Before
    public void init(){
        task2 = new Task2();
    }

    @Test
    public void testDoIt1() {
        int[] test = {1, 2, 4, 4, 2, 3, 4, 1, 7};
        int[] result = {1, 7};
        Assert.assertArrayEquals(result, task2.doIt(test));
    }

    @Test
    public void testDoIt2() {
        int[] test = {};
        Assert.assertArrayEquals(null, task2.doIt(test));
    }

    @Test(expected = RuntimeException.class)
    public void testDoIt3() {
        int[] test = {1, 2, 3, 5, 6, 7, 8};
        Assert.assertArrayEquals(null, task2.doIt(test));
    }

    @Test
    public void testDoIt4() {
        int[] test = {4};
        Assert.assertArrayEquals(null, task2.doIt(test));
    }

    @Test
    public void testDoIt5() {
        int[] test = {1, 2, 4};
        Assert.assertArrayEquals(null, task2.doIt(test));
    }
}
