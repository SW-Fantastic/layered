package org.swdc.clang.libtooling.context;

/**
 * AbstractMetaType的修饰符。
 */
public interface Modifiers {

    int IsConst = 0x01;
    int IsVolatile = 0x02;

    int IsPublic = 0x04;
    int IsProtected = 0x08;
    int IsPrivate = 0x10;
    int AccessorMask  = IsPublic | IsProtected | IsPrivate;

    int IsUnsigned = 0x20;
    int UnsignedMask = IsUnsigned;

    int IsMovable = 0x40;
    int HasNoArgsCtor = 0x80;

}
