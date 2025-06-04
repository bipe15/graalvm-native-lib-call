package sis;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;

class ForeignRegistrationFeature implements Feature {
    public void duringSetup(Feature.DuringSetupAccess access) {
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
//        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ValueLayout.ADDRESS));

        RuntimeForeignAccess.registerForDowncall(
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)  // CCharPointer get_bytes(IsolateThread)
        );

        RuntimeForeignAccess.registerForDowncall(
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)  // CCharPointer get_bytes(IsolateThread)
        );
    }
}
