package sis;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

public class BytesLibrary {

    @CEntryPoint(name = "get_bytes")
    public static CCharPointer getBytes(IsolateThread thread) {
        String message = "HELLO";
        CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString(message);
        return holder.get(); // El GC nativo se encarga de liberar la memoria
    }
}