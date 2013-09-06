package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;


public class StatsTest {

    @Test
    public void testStats() {
        Collection<Double> numbers = Arrays.asList(2., 1., 6., 10., 23., 7.);
        Stats stats = new Stats(numbers);
        assertEquals(6, stats.getCount());
        assertEquals(8.167, stats.getMean(), 0.001);
        assertEquals(6.5, stats.getMedian(), 0);
        assertEquals(23, stats.getMax(), 0);
        assertEquals(1, stats.getMin(), 0);
        System.out.println(stats);
    }

    @Test
    public void testRunningStats() {
        Stats stats = new Stats(3);
        stats.add(1);
        assertEquals(1, stats.getMean(), 0);
        stats.add(2);
        assertEquals(1.5, stats.getMean(), 0);
        stats.add(3);
        assertEquals(2, stats.getMean(), 0);
        stats.add(4);
        assertEquals(3, stats.getMean(), 0);
        stats.add(5);
        assertEquals(4, stats.getMean(), 0);
    }

}
