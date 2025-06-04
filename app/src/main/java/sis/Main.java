package sis;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Throwable {
        try {
            String libPath = "libbytes.so";
            Linker linker = Linker.nativeLinker();
            try (Arena arena = Arena.ofConfined()) {
                try(FfmHelper.IsolatedThread isolate = FfmHelper.getIsolatedThread(libPath, arena)) {

                    SymbolLookup lookup = SymbolLookup.libraryLookup(Path.of(libPath), arena);

                    MethodHandle getBytesHandle = linker.downcallHandle(
                            lookup.find("get_bytes").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS) // CCharPointer get_bytes(IsolateThread*)
                    );

                    MemorySegment isolateThread = isolate.getThreadPointerContent(); // si lo llamas desde fuera de Graal isolate

                    MemorySegment cStringPointer = (MemorySegment) getBytesHandle.invoke(isolateThread);

                    String result = cStringPointer.reinterpret(100).getString(0); // hasta primer null byte
                    System.out.println("Result: " + result);
                    MethodHandles.lookup().findStatic(Object.class, "toString", MethodType.methodType(String.class, String.class)).invoke(result);
                }
            }
        }catch (Throwable e){
            e.printStackTrace();
            throw e;
        }
    }
}

/*
public class Main {

    public static void main(String[] args) throws Throwable {

        String libPath = "libbytes.so"; // ajusta según plataforma
        try (Arena arena = Arena.ofConfined()) {
            try(FfmHelper.IsolatedThread isolate = FfmHelper.getIsolatedThread(libPath, arena)) {

                SymbolLookup lookup = FfmHelper.loadSymbolLookup(libPath);
                Linker linker = Linker.nativeLinker();

                MethodHandle getBytes = linker.downcallHandle(
                        lookup.find("get_bytes").orElseThrow(),
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS) // retorna uint8_t*, recibe graal_isolatethread_t*
                );

                MemorySegment result = (MemorySegment) getBytes.invoke(MemorySegment.NULL);

                // Lee los primeros 5 bytes
                byte[] bytes = result.asSlice(0, 5).toArray(ValueLayout.JAVA_BYTE);
                System.out.println("Bytes recibidos: " + new String(bytes));
            }
//        }

    }
}
*/