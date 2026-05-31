package org.swdc.layer.test.pointers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.IntPointer;

import java.io.File;

public class IntPointerTests {

    private static Allocator allocator = null;

    @BeforeAll
    public static void beforeAll() {
        System.load(new File("liblayer.dll").getAbsolutePath());
        allocator = new Allocator();
    }

    @Test
    public void testReadWrite() {

        IntPointer pointer = allocator.allocateInt(10);
        for (int i = 0; i < pointer.getCapacity(); ++i) {
            pointer.set(i, i);
        }

        for (int i = 0; i < pointer.getCapacity(); ++i) {
            Assertions.assertEquals(i, pointer.get(i));
        }

        pointer.free();
        System.out.println();
    }


    @Test
    public  void testArrayReadWrite() {

        int[] array = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        IntPointer pointer = allocator.allocateInt(10);
        pointer.setArray(array);

        int[] readed = pointer.getArray(pointer.getCapacity());
        Assertions.assertArrayEquals(array, readed);
        pointer.free();


    }

    @Test
    public void testFreeWithoutZeroRef() {

        int[] array = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        IntPointer pointer = allocator.allocateInt(10);
        pointer.setArray(array);
        IntPointer sub = pointer.at(5);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            pointer.free();
        }, "Pointer is not null and has no zero ref");
        sub.free();
        pointer.free();

    }

    @Test
    public void testAt() {

        int[] array = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        IntPointer pointer = allocator.allocateInt(10);
        pointer.setArray(array);
        IntPointer sub = pointer.at(5);
        int[] readed = sub.getArray(sub.getCapacity());

        Assertions.assertArrayEquals(new int[] { 5, 6, 7, 8, 9 }, readed);

        sub.free();
        pointer.free();
        System.out.println();

    }

    @Test
    public void testClear() {

        int[] array = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        IntPointer pointer = allocator.allocateInt(10);
        pointer.setArray(array);
        pointer.clear();

        for (int i = 0; i < pointer.getCapacity(); ++i) {
            Assertions.assertEquals(0, pointer.get(i));
        }
    }

}
