package estreamj.ciphers.trivium;

import estreamj.framework.ESJException;
import estreamj.framework.Engine;
import estreamj.framework.ICipher;
import estreamj.framework.ICipherMaker;
import estreamj.framework.Utils;

public class Trivium implements ICipher {
    final static int KEY_SIZE_BITS = 80;
    final static int IV_SIZE_BITS = 80;

    ///////////////////////////////////////////////////////////////////////////

    byte[] key = new byte[10];
    int[] s = new int[10];

    ///////////////////////////////////////////////////////////////////////////

    public int getKeySize() {
        return KEY_SIZE_BITS >> 3;
    }

    public int getNonceSize() {
        return IV_SIZE_BITS >> 3;
    }

    public int getWordSize() {
        return 4;
    }

    public boolean isPatented() {
        return false;
    }

    public void process(
            byte[] inbuf,
            int inOfs,
            byte[] outbuf,
            int outOfs,
            int len) throws ESJException {
        int s11 = s[0];
        int s12 = s[1];
        int s13 = s[2];
        int s21 = s[3];
        int s22 = s[4];
        int s23 = s[5];
        int s31 = s[6];
        int s32 = s[7];
        int s33 = s[8];
        int s34 = s[9];

        int outEnd = outOfs + (len & ~3);

        for (; outOfs < outEnd; outOfs += 4, inOfs += 4) {
            int t1, t2, t3, reg;

            t1 = ((s13 << 96 - 66) | (s12 >>> 66 - 64)) ^ ((s13 << 96 - 93) | (s12 >>> 93 - 64));
            t2 = ((s23 << 96 - 69) | (s22 >>> 69 - 64)) ^ ((s23 << 96 - 84) | (s22 >>> 84 - 64));
            t3 = ((s33 << 96 - 66) | (s32 >>> 66 - 64)) ^ ((s34 << 128 - 111) | (s33 >>> 111 - 96));

            reg = t1 ^ t2 ^ t3;
            outbuf[outOfs] = (byte) (inbuf[inOfs] ^ reg);
            outbuf[outOfs + 1] = (byte) (inbuf[inOfs + 1] ^ reg >> 8);
            outbuf[outOfs + 2] = (byte) (inbuf[inOfs + 2] ^ reg >> 16);
            outbuf[outOfs + 3] = (byte) (inbuf[inOfs + 3] ^ reg >> 24);

            t1 ^= (((s13 << 96 - 91) | (s12 >>> 91 - 64)) & ((s13 << 96 - 92) | (s12 >>> 92 - 64))) ^ ((s23 << 96 - 78) | (s22 >>> 78 - 64));
            t2 ^= (((s23 << 96 - 82) | (s22 >>> 82 - 64)) & ((s23 << 96 - 83) | (s22 >>> 83 - 64))) ^ ((s33 << 96 - 87) | (s32 >>> 87 - 64));
            t3 ^= (((s34 << 128 - 109) | (s33 >>> 109 - 96)) & ((s34 << 128 - 110) | (s33 >>> 110 - 96))) ^ ((s13 << 96 - 69) | (s12 >>> 69 - 64));

            s13 = s12;
            s12 = s11;
            s11 = t3;
            s23 = s22;
            s22 = s21;
            s21 = t1;
            s34 = s33;
            s33 = s32;
            s32 = s31;
            s31 = t2;
        }

        // NOTE: could save some code memory by merging the two blocks, but that
        // would decrease the speed because of additional conditional jumps...
        outEnd = outOfs + (len & 3);
        //if (0 < outEnd)
        if (outOfs < outEnd) // Peli: Apr 3, 2009: BUGFIX
        {
            int t1, t2, t3, reg;

            t1 = ((s13 << 96 - 66) | (s12 >>> 66 - 64)) ^ ((s13 << 96 - 93) | (s12 >>> 93 - 64));
            t2 = ((s23 << 96 - 69) | (s22 >>> 69 - 64)) ^ ((s23 << 96 - 84) | (s22 >>> 84 - 64));
            t3 = ((s33 << 96 - 66) | (s32 >>> 66 - 64)) ^ ((s34 << 128 - 111) | (s33 >>> 111 - 96));

            reg = t1 ^ t2 ^ t3;
            for (; outOfs < outEnd; outOfs++, inOfs++) {
                outbuf[outOfs] = (byte) (inbuf[inOfs] ^ reg);
                reg >>= 8;
            }

            t1 ^= (((s13 << 96 - 91) | (s12 >>> 91 - 64)) & ((s13 << 96 - 92) | (s12 >>> 92 - 64))) ^ ((s23 << 96 - 78) | (s22 >>> 78 - 64));
            t2 ^= (((s23 << 96 - 82) | (s22 >>> 82 - 64)) & ((s23 << 96 - 83) | (s22 >>> 83 - 64))) ^ ((s33 << 96 - 87) | (s32 >>> 87 - 64));
            t3 ^= (((s34 << 128 - 109) | (s33 >>> 109 - 96)) & ((s34 << 128 - 110) | (s33 >>> 110 - 96))) ^ ((s13 << 96 - 69) | (s12 >>> 69 - 64));

            s13 = s12;
            s12 = s11;
            s11 = t3;
            s23 = s22;
            s22 = s21;
            s21 = t1;
            s34 = s33;
            s33 = s32;
            s32 = s31;
            s31 = t2;
        }

        s[0] = s11;
        s[1] = s12;
        s[2] = s13;
        s[3] = s21;
        s[4] = s22;
        s[5] = s23;
        s[6] = s31;
        s[7] = s32;
        s[8] = s33;
        s[9] = s34;
    }

    public void reset() throws ESJException {
        // key is cached already, nothing to do here
    }

    public void setupKey(
            int mode,
            byte[] key,
            int ofs) throws ESJException {
        System.arraycopy(key, ofs, this.key, 0, this.key.length);
    }

    public void setupNonce(
            byte[] nonce,
            int ofs) throws ESJException {
        byte[] key = this.key;
        int[] s = this.s;

        int s11 = Utils.readInt32LE(key, 0);
        int s12 = Utils.readInt32LE(key, 4);
        int s13 = (key[8] & 0x0ff) |
                ((key[9] << 8) & 0x0ff00);
        int s21 = Utils.readInt32LE(nonce, ofs);
        int s22 = Utils.readInt32LE(nonce, ofs + 4);
        int s23 = (nonce[ofs + 8] & 0x0ff) |
                ((nonce[ofs + 9] << 8) & 0x0ff00);
        int s31 = 0;
        int s32 = 0;
        int s33 = 0;
        int s34 = 0x07000;

        ///////////////!!!!!!!!!!!!!!!!!!!!!!!!
//		  System.out.printf(
//				  "s11=%08x\n" +
//				  "s12=%08x\n" +
//				  "s13=%08x\n" +
//				  "s21=%08x\n" +
//				  "s22=%08x\n" +
//				  "s23=%08x\n" +
//				  "s31=%08x\n" +
//				  "s32=%08x\n" +
//				  "s33=%08x\n" +
//				  "s34=%08x\n",
//					s11, s12, s13,
//					s21, s22, s23,
//					s31, s32, s33, s34);		

        for (int i = 0; i < 4 * 9; i++) {
            int t1, t2, t3;

            t1 = ((s13 << 96 - 66) | (s12 >>> 66 - 64)) ^ ((s13 << 96 - 93) | (s12 >>> 93 - 64));
            t2 = ((s23 << 96 - 69) | (s22 >>> 69 - 64)) ^ ((s23 << 96 - 84) | (s22 >>> 84 - 64));
            t3 = ((s33 << 96 - 66) | (s32 >>> 66 - 64)) ^ ((s34 << 128 - 111) | (s33 >>> 111 - 96));

            t1 ^= (((s13 << 96 - 91) | (s12 >>> 91 - 64)) & ((s13 << 96 - 92) | (s12 >>> 92 - 64))) ^ ((s23 << 96 - 78) | (s22 >>> 78 - 64));
            t2 ^= (((s23 << 96 - 82) | (s22 >>> 82 - 64)) & ((s23 << 96 - 83) | (s22 >>> 83 - 64))) ^ ((s33 << 96 - 87) | (s32 >>> 87 - 64));
            t3 ^= (((s34 << 128 - 109) | (s33 >>> 109 - 96)) & ((s34 << 128 - 110) | (s33 >>> 110 - 96))) ^ ((s13 << 96 - 69) | (s12 >>> 69 - 64));

            s13 = s12;
            s12 = s11;
            s11 = t3;
            s23 = s22;
            s22 = s21;
            s21 = t1;
            s34 = s33;
            s33 = s32;
            s32 = s31;
            s31 = t2;
        }

        s[0] = s11;
        s[1] = s12;
        s[2] = s13;
        s[3] = s21;
        s[4] = s22;
        s[5] = s23;
        s[6] = s31;
        s[7] = s32;
        s[8] = s33;
        s[9] = s34;
    }

    ///////////////////////////////////////////////////////////////////////////

    static class Maker implements ICipherMaker {
        public ICipher create() throws ESJException {
            return new Trivium();
        }

        public String getName() {
            return "Trivium";
        }
    }

    public static void register() {
        Engine.registerCipher(new Maker());
    }
}
