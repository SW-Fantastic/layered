package org.swdc.layer.test.pointers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.layered.calling.LayerInvokeHandler;
import org.swdc.layered.calling.LayerInvoker;
import org.swdc.layered.library.LayerRuntimeLibrary;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.IntPointer;
import org.swdc.layered.pointers.UniquePointer;

import java.io.File;
import java.lang.reflect.Proxy;

public class PointerTests {

    @BeforeAll
    public static void setup() {

        LayerRuntimeLibrary library = LayerRuntimeLibrary.getInstance();
        library.load(new File("./assets"));

    }

    @Test
    public void testIntPtr() {

        Allocator allocator = new Allocator();
        IntPointer pointer = allocator.allocateInt(10);
        pointer.set(0, 123);

        UniquePointer<IntPointer> unique = pointer.move();
        IntPointer moved = new IntPointer(unique);

        System.out.println(moved.get(0));
        Assertions.assertThrows(RuntimeException.class, () -> {
            System.out.println(pointer.get(0));
        });
        Assertions.assertEquals(123, moved.get(0));
        moved.close();

    }


}
