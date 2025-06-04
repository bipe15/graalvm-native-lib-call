package sis;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FfmHelper {

    private static Linker linker = Linker.nativeLinker();
    private static MethodHandle graalCreateIsolate = null;
    private static MethodHandle graalTearDown = null;
    private static Map<String, SymbolLookup> symbolLookupMap = new HashMap<>();

    public static synchronized MethodHandle getCreateIsolate(String path){
        if(graalCreateIsolate == null) {
            graalCreateIsolate = linker.downcallHandle(
                    loadSymbolLookup(path).find("graal_create_isolate").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, // params (graal_create_isolate_params_t*) → null
                            ValueLayout.ADDRESS, // isolate out → graal_isolate_t**
                            ValueLayout.ADDRESS  // thread out → graal_isolatethread_t**
                    )
            );
        }
        return graalCreateIsolate;
    }

    public static synchronized MethodHandle getTearDown(String path){
        if(graalTearDown == null) {
            graalTearDown = linker.downcallHandle(
                    loadSymbolLookup(path).find("graal_tear_down_isolate").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS // isolate out → graal_isolate_t**
                    )
            );
        }
        return graalTearDown;
    }

    public static synchronized SymbolLookup loadSymbolLookup(String path){
        SymbolLookup ret = symbolLookupMap.get(path);
        if(ret == null){
            ret = SymbolLookup.libraryLookup(Path.of(path), Arena.global());
            symbolLookupMap.put(path, ret);
        }
        return ret;
    }

    public static IsolatedThread getIsolatedThread(String path, Arena arena){
        MemorySegment isolateOut = arena.allocate(ValueLayout.ADDRESS); // graal_isolate_t**
        MemorySegment threadOut = arena.allocate(ValueLayout.ADDRESS);  // graal_isolatethread_t**
        try {
            int res = (int) getCreateIsolate(path).invoke(MemorySegment.NULL, isolateOut, threadOut);
            if(res != 0){
                throw new RuntimeException("graal_create_isolate failed with code: %s".formatted(res));
            }
            return new IsolatedThread(path, isolateOut, threadOut);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public record IsolatedThread(String path, MemorySegment isolatedThreadPointer, MemorySegment threadPointer) implements AutoCloseable {

        public MemorySegment getIsolatedThreadPointerContent(){
            return this.isolatedThreadPointer().get(ValueLayout.ADDRESS, 0);
        }

        public MemorySegment getThreadPointerContent(){
            return this.threadPointer().get(ValueLayout.ADDRESS, 0);
        }

        @Override
        public void close() throws Exception {
            try {
                int res = (int) FfmHelper.getTearDown(this.path()).invoke(this.getThreadPointerContent());
                if(res != 0){
                    throw new RuntimeException("graal_tear_down_isolate failed with code: %s".formatted(res));
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}

