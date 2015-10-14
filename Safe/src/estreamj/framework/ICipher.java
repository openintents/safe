package estreamj.framework;

/**
 * Generic interface every cipher needs to implement, so it can participate in
 * the test framework. If a cipher comes in different flavors (variable key
 * sizes, MAC computation or not, etc.) it should then implement a version for
 * each of them - this keeps the actual testing logic simple. Simple is good.
 */
public interface ICipher {
    /**
     * mode: instance is used for encryption
     */
    public final static int MODE_ENCRYPT = 0;

    /**
     * mode: instance is used for decryption
     */
    public final static int MODE_DECRYPT = 1;

    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return true: algorithm is patented, check with vendor for license
     * details / false: free to use in private and commerical applications
     */
    public boolean isPatented();

    /**
     * @return key size in bytes
     */
    public int getKeySize();

    /**
     * @return nonce size in bytes.
     */
    public int getNonceSize();

    /**
     * @return alignment of data needed during calls into process()
     */
    public int getWordSize();

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Resets the instance, so it can be reused.
     *
     * @throws ESJException if any error occurs
     */
    public void reset() throws ESJException;

    /**
     * Sets up a new key with the existing instance.
     *
     * @param mode see MODE_xxx
     * @param key  buffer with key material
     * @param ofs  where the key starts
     * @throws ESJException if any error occurs
     */
    public void setupKey(int mode, byte[] key, int ofs)
            throws ESJException;

    /**
     * Sets up a new nonce with the existing cipher instance.
     *
     * @param mode  see MODE_xxx
     * @param nonce buffer with nonce material
     * @param ofs   where the nonce starts
     * @throws ESJException if any error occurs
     */
    public void setupNonce(byte[] nonce, int ofs)
            throws ESJException;

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Processes data.
     *
     * @param inbuf  input buffer
     * @param inOfs  where to start reading from the input buffer
     * @param outbuf output buffer
     * @param outOfs where to start writing in the output buffer
     * @param len    number of bytes to process, must be aligned to the cipher's
     *               word size except on the last call where an arbitrary size can be used
     * @throws ESJException in any error occured
     */
    public void process(
            byte[] inbuf,
            int inOfs,
            byte[] outbuf,
            int outOfs,
            int len) throws ESJException;
}
