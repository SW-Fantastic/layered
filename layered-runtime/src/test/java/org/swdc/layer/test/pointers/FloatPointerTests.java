package org.swdc.layer.test.pointers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.FloatPointer;

import java.io.File;

public class FloatPointerTests {

    private static Allocator allocator = null;

    @BeforeAll
    public static void beforeAll() {
        System.load(new File("liblayer.dll").getAbsolutePath());
        allocator = new Allocator();
    }

    @Test
    public void testReadWrite() {

        FloatPointer pointer = allocator.allocateFloat(10);
        for (int i = 0; i < 10; i++) {
            pointer.set(i, (float)i);
        }

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals((float)i, pointer.get(i));
        }
        pointer.free();

    }

    @Test
    public void testArrayReadWrite() {

        float[] array = new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f};
        FloatPointer pointer = allocator.allocateFloat(10);
        pointer.setArray(array);

        float[] readed = pointer.getArray(pointer.getCapacity());
        Assertions.assertArrayEquals(array, readed);
        pointer.free();

    }

}
