package estreamj.framework;

/**
 * Simple cipher factory.
 */
public interface ICipherMaker {
    /**
     * @return the name of cipher, which is used for queries - so it must be
     * unique
     */
    public String getName();

    /**
     * Create a new cipher instance.
     *
     * @return new instance, which can also be of the type ICipherMAC, use the
     * "instanceof" keyword to find out what you are dealing with
     * @throws ESJException if any error occured
     */
    public ICipher create() throws ESJException;
}
