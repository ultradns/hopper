package biz.neustar.hopper;

import static org.junit.Assert.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import org.junit.Test;

import biz.neustar.hopper.message.DNSSEC;

public class DNSSECWithProviderTest {

    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private DNSSEC.Algorithm algorithm = DNSSEC.Algorithm.RSASHA1;
    byte[] toSign = "The quick brown fox jumped over the lazy dog.".getBytes();

    @Test
    public void testSignSoftware() throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator
                .getInstance(KEY_ALGORITHM);
        keyPairGenerator.initialize(512);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
        signer.initSign(keyPair.getPrivate());
        signer.update(toSign);
        byte[] signature = signer.sign();
        assertNotNull(signature);

        // verify the signature
        Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
        verifier.initVerify(keyPair.getPublic());
        verifier.update(toSign);
        boolean verify = verifier.verify(signature);
        assertTrue(verify);

    }
}
