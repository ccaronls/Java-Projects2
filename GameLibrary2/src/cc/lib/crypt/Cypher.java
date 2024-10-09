package cc.lib.crypt;

/**
 * This interface is the base type for encryption mechanism passed too
 * EncryptionInput/OutputStream.
 *
 * @author ccaron
 */
public interface Cypher {

    /**
     * Return 2 component value
     * value[0] is number of bits used for value
     * value[1] is the encrypted value
     *
     * @param uncrypted
     * @return
     */
    int[] encrypt(int uncrypted);

    /**
     * Return a 2 component value
     * value[0] the number of bits used of encrypted
     * value[1] is the decrypted value
     *
     * @param encrypted
     * @return
     */
    int[] decrypt(int encrypted);

    /**
     * Need to let the encryptor know how large too keep its internal buffer
     * This method should return the maximum number of bits required to encode
     * any given char.  For a normal file, this is usually 8, but for
     * encrypted files, this can easily be much longer.
     *
     * @return
     */
    int getMaxEncodedBitLength();

}
