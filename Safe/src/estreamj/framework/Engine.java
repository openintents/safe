package estreamj.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

/**
 * The engine accumulates all the stream ciphers available. All implementations
 * register with the engine by themselves.
 */
public class Engine {
    /**
     * @return the names of all ciphers registered; can be empty if no ciphers
     * have been registered so far
     */
    public static String[] getCipherNames() {
        String[] result;

        synchronized (_cphMks) {
            Vector<String> lst = new Vector<String>();
            lst.addAll(_cphMks.keySet());
            Collections.sort(lst);
            result = lst.toArray(new String[lst.size()]);
        }

        return result;
    }

    /**
     * Creates a new cipher instance.
     *
     * @param name name of the cipher to make
     * @return new cipher instance
     * @throws ESJException if any error occured
     */
    public static ICipher createCipher(String name) throws ESJException {
        ICipherMaker maker;

        synchronized (_cphMks) {
            maker = _cphMks.get(name);
            if (null == maker) {
                throw new ESJException(
                        "no maker registered for cipher \"" +
                                name + "\""
                );
            }
        }
        return maker.create();
    }

    ///////////////////////////////////////////////////////////////////////////

    static HashMap<String, ICipherMaker> _cphMks =
            new HashMap<String, ICipherMaker>();

    /**
     * Called by cipher implementations to register their factories, usually
     * during startup time.
     *
     * @param cphMk the factory to register
     */
    public static void registerCipher(ICipherMaker cphMk) {
        String name = cphMk.getName();

        synchronized (_cphMks) {
            if (_cphMks.containsKey(name)) {
                System.err.println(
                        "cipher \"" + name +
                                "\" has been registered already"
                );
                System.exit(1);
            }
            _cphMks.put(cphMk.getName(), cphMk);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    // all the cipher classes must be listed here, the rest of the registration
    // is done via reflection; this is done by having every class implementing
    // a 'public static void register()' method (see below)
    @SuppressWarnings("rawtypes")
    static Class[] _cipherClasses =
            {
                    //estreamj.ciphers.phelix.Phelix.class,
                    //estreamj.ciphers.hc256.HC256.class,
                    //estreamj.ciphers.salsa20.Salsa20.class,
                    //estreamj.ciphers.aes.AESCTR.class,
                    //estreamj.ciphers.mickey.MICKEY.class,
                    //estreamj.ciphers.mickey.MICKEY128.class,
                    //estreamj.ciphers.hermes8.Hermes8.class,
                    //estreamj.ciphers.rc4.RC4.class,
                    //estreamj.ciphers.dragon.Dragon.class,
                    //estreamj.ciphers.lex.LEX.class,
                    estreamj.ciphers.trivium.Trivium.class//,
                    //estreamj.ciphers.sosemanuk.Sosemanuk.class,
                    //estreamj.ciphers.grain.GrainP2.class,
                    //estreamj.ciphers.grain.Grain128.class,
                    //estreamj.ciphers.nil.Nil.class
            };

    ///////////////////////////////////////////////////////////////////////////

    static {
        try {
            for (int i = 0; i < _cipherClasses.length; i++) {
                // check if the register method is there and static
                Class<?> cls = _cipherClasses[i];
                Method mthd = cls.getMethod("register", (Class[]) null);
                if (Modifier.STATIC != (mthd.getModifiers() & Modifier.STATIC)) {
                    throw new Exception(
                            "register() method is not static (" + cls + ")"
                    );
                }
                // ready to register
                mthd.invoke(null, (Object[]) null);
            }
        } catch (Exception e) {
            System.err.println(
                    "cipher registration error (" + e.getMessage() + ")"
            );
            System.exit(1);
        }
    }
}
