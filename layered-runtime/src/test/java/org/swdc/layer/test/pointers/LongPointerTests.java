package org.swdc.layer.test.pointers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.LongPointer;

import java.io.File;

public class LongPointerTests {

    private static Allocator allocator;

    @BeforeAll
    public static void beforeAll() {

        System.load(new File("liblayer.dll").getAbsolutePath());
        allocator = new Allocator();
    }

    @Test
    public void testReadWrite() {

        LongPointer pointer = allocator.allocateLong(10);
        for (int i = 0; i < pointer.getCapacity(); ++i) {
            pointer.set(i, (long) i);
        }

        for (int i = 0; i < pointer.getCapacity(); ++i) {
            Assertions.assertEquals(i, pointer.get(i));
        }

        pointer.free();
    }

    @Test
    public void testArrayReadWrite() {

        long[] array = new long[] { 0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L };
        LongPointer pointer = allocator.allocateLong(10);
        pointer.setArray(array);

        long[] readed = pointer.getArray(pointer.getCapacity());
        Assertions.assertArrayEquals(array, readed);

    }

    @Test
    public void testFreeWithoutZeroRef() {

        System.out.println("> Test : Free without zero ref");
        long[] array = new long[] { 0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L };
        LongPointer pointer = allocator.allocateLong(10);
        pointer.setArray(array);
        LongPointer sub = pointer.at(5);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            pointer.free();
        });
        sub.free();
        pointer.free();

    }

    @Test
    public void testAt() {

        long[] array = new long[] { 0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L };
        LongPointer pointer = allocator.allocateLong(10);
        pointer.setArray(array);
        LongPointer sub = pointer.at(5);
        long[] readed = sub.getArray(sub.getCapacity());
        Assertions.assertArrayEquals(new long[] { 5L, 6L, 7L, 8L, 9L }, readed);
        sub.free();
        pointer.free();
        System.out.println();

    }

}
