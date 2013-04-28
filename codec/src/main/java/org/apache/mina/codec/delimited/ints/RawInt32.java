package org.apache.mina.codec.delimited.ints;

import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;

/**
 * 
 * Class providing raw/canonical representation of integers.
 * 
 * 
 * <style type="text/css"> pre-fw { color: rgb(0, 0, 0); display: block;
 * font-family:courier, "courier new", monospace; font-size: 13px; white-space:
 * pre; } </style>
 * 
 * <h2>RawInt32 encoder and decoder</h2>
 * <p>
 * This pair provides a mechanism called canonical form serialization.
 * 
 * In this representations all 32-bits integer are encoded over 4 bytes.
 * 
 * 
 * This library provides two variants <i>big-endian</i> and <i>small-endian</i>.
 * 
 * 
 * In both cases, the inner bits of each byte are ordered from the most to the
 * least significant bit.
 * 
 * The difference between the two variants is the ordering of the four bytes.
 * 
 * <ul>
 * <li>Big-endian: <i>The bytes are ordered from the most to the least
 * significant one</i></li>
 * <li>Little-endian: <i>The bytes are ordered from the least to the most
 * significant one</i></li>
 * </ul>
 * 
 * <p>
 * This representation is often used since it is used internally in CPUs,
 * therefore programmers using a low level languages (assembly, C, ...)
 * appreciate using it (for ease of use or performance reasons). When integers
 * are directly copied from memory, it is required to ensure this serializer
 * uses the appropriate endianness on both ends.
 * <ul>
 * <li>Big-endian: 68k, MIPS, Alpha, SPARC</li>
 * <li>Little-endian: x86, x86-64, ARM</li>
 * <li><i>Bi-</i>endian (depends of the operating system): PowerPC, Itanium</li>
 * </ul>
 * </p>
 * 
 * <p>
 * More details availabile on the Wikipedia
 * "<a href="http://en.wikipedia.org/wiki/Endianness">Endianness page</a>".
 * </p>
 * 
 * <h2>On-wire representation</h2>
 * <p>
 * Encoding of the value 67305985
 * </p>
 * <i>Big-Endian variant:</i>
 * 
 * <pre-fw>
 * 0000 0100  0000 0011  0000 0010  0000 0001
 * ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾
 *     4          3          2          1      // 4·2<sup>24</sup> + 3·2<sup>16</sup> + 2·2<sup>8</sup> + 1·2<sup>0</sup>  = 67305985
 * 
 * </pre-fw>
 * 
 * <i>Little-Endian variant:</i>
 * 
 * <pre-fw>
 * 0000 0001  0000 0010  0000 0011  0000 0100
 * ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾
 *     1          2          3          4      // 1·2<sup>0</sup> + 2·2<sup>8</sup> + 3·2<sup>16</sup> + 4·2<sup>24</sup>  = 67305985
 * </pre-fw>
 * 
 * </p>
 * 
 * <p>
 * n.b. This class doesn't have any dependency against Apache Thrift or any
 * other library in order to provide this convenient integer serialization
 * module to any software using FramedMINA.
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class RawInt32 {
    // this class should not be instanciated
    private RawInt32() {
    }

    /**
     * Documentation available in the {@link RawInt32} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    static public class Decoder extends ByteBufferDecoder<Integer> {

        final private Endianness endianness;

        public Decoder(Endianness endianness) {
            super();
            this.endianness = endianness;
        }

        @Override
        public Integer decode(ByteBuffer input) throws ProtocolDecoderException {
            if (input.remaining() < 4)
                return null;

            if (endianness == Endianness.BIG) {
                if ((input.get(0) & 0x80) != 0)
                    throw new ProtocolDecoderException("Not the big endian representation of a signed int32");
                return ((input.get() & 0xff) << 24) | ((input.get() & 0xff) << 16) | ((input.get() & 0xff) << 8)
                        | ((input.get() & 0xff));
            } else {
                if ((input.get(3) & 0x80) != 0)
                    throw new ProtocolDecoderException("Not the small endian representation of a signed int32");
                return ((input.get() & 0xff)) | ((input.get() & 0xff) << 8) | ((input.get() & 0xff) << 16)
                        | ((input.get() & 0xff) << 24);
            }
        }
    }

    /**
     * Documentation available in the {@link RawInt32} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    static public class Encoder extends ByteBufferEncoder<Integer> {

        final private Endianness endianness;

        public Encoder(Endianness endianness) {
            super();
            this.endianness = endianness;
        }

        @Override
        public void writeTo(Integer message, ByteBuffer buffer) {
            // VarInts don't support negative values
            if (message < 0)
                message = 0;
            if (endianness == Endianness.BIG) {
                buffer.put((byte) (0xff & (message >> 24)));
                buffer.put((byte) (0xff & (message >> 16)));
                buffer.put((byte) (0xff & (message >> 8)));
                buffer.put((byte) (0xff & (message)));
            } else {
                buffer.put((byte) (0xff & (message)));
                buffer.put((byte) (0xff & (message >> 8)));
                buffer.put((byte) (0xff & (message >> 16)));
                buffer.put((byte) (0xff & (message >> 24)));
            }
            buffer.flip();

        }

        @Override
        public int getEncodedSize(Integer message) {
            return 4;
        }

    }

    /**
     * 
     * This enumeration is used to select the endianness of the dncoder and the
     * decoder class.
     * 
     * Documentation available in the {@link RawInt32} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     */
    public enum Endianness {
        BIG, LITTLE
    }

}
