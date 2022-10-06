package org.optaplanner.python.translator.types;

import java.nio.ByteBuffer;

import org.optaplanner.python.translator.PythonLikeObject;

public interface PythonBytesLikeObject extends PythonLikeObject {
    ByteBuffer asByteBuffer();

    default byte[] asByteArray() {
        ByteBuffer byteBuffer = asByteBuffer();
        byte[] out = new byte[byteBuffer.limit()];
        byteBuffer.get(out);
        return out;
    }
}
