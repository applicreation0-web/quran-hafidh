import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.Base64;

/** Signs ONLY APK v2 signed-data digests. Never handles the private APK or private corpora.
    The historical RSA-4096 private key remains in GitHub's existing encrypted secrets. */
class HaafidhV2Signer {
  static byte[] field(ByteBuffer b) {
    if (b.remaining() < 4) throw new IllegalArgumentException("Truncated input");
    long len = Integer.toUnsignedLong(b.getInt());
    if (len > 8192 || len > b.remaining()) throw new IllegalArgumentException("Invalid length");
    byte[] out = new byte[(int)len]; b.get(out); return out;
  }
  static ByteBuffer buf(byte[] data) {return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);}
  static void check(boolean value, String message) {if (!value) throw new IllegalArgumentException(message);}
  public static void main(String[] args) throws Exception {
    char[] ksPass = System.getenv("HIFZ_KEYSTORE_PASSWORD").toCharArray();
    String alias = System.getenv("HIFZ_KEY_ALIAS");
    char[] keyPass = System.getenv("HIFZ_KEY_PASSWORD").toCharArray();
    KeyStore ks = KeyStore.getInstance(new File(
        System.getenv("HIFZ_KEYSTORE_PATH")), ksPass);
    Certificate cert = ks.getCertificate(alias);
    check(cert != null, "Missing official certificate");
    byte[] certDer=cert.getEncoded();
    String certHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certDer));
    check("66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14".equals(certHash),
       "Wrong signing certificate");
    PrivateKey key=(PrivateKey) ks.getKey(alias,keyPass);
    check(key instanceof java.security.interfaces.RSAPrivateKey, "Historical key must be RSA");
    String encoded=Files.readString(Path.of(".github/signing/haafidh-11722-v2-request.b64")).trim();
    byte[] signedData=Base64.getDecoder().decode(encoded);
    check(signedData.length>500 && signedData.length < 8192,"Invalid signed data size");
    ByteBuffer sd=buf(signedData);
    ByteBuffer allDigests=buf(field(sd));
    ByteBuffer allCertificates=buf(field(sd));
    byte[] attrs=field(sd);
    check(!sd.hasRemaining() && attrs.length==0,"Invalid v2 signed data");
    ByteBuffer record=buf(field(allDigests));
    check(!allDigests.hasRemaining() && record.getInt()==0x0104,
          "Only historical v2 RSA SHA-512 algorithm is allowed");
    byte[] digest=field(record);
    check(!record.hasRemaining() && digest.length==64,"Wrong v2 content digest");
    check(Arrays.equals(certDer,field(allCertificates)) && !allCertificates.hasRemaining(),
          "APK v2 signed-data certificate mismatch");
    Signature signer=Signature.getInstance("SHA512withRSA");
    signer.initSign(key);signer.update(signedData);
    byte[] signature=signer.sign();
    check(signature.length==512,"RSA-4096 signature has wrong size");
    System.out.println("SIGNED_DATA_SHA256="+HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(signedData)));
    System.out.println("HISTORICAL_V2_SIGNATURE_B64="+Base64.getEncoder().encodeToString(signature));
    Arrays.fill(ksPass,'\u0000');Arrays.fill(keyPass,'\u0000');
  }
}