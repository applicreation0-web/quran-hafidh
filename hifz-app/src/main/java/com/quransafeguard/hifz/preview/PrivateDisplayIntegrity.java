package com.quransafeguard.hifz.preview;
import java.security.MessageDigest;

/** Pins approved private display bytes; hashes reveal no heading text. */
final class PrivateDisplayIntegrity {
    private PrivateDisplayIntegrity() {}
    static void verify(String asset,byte[] bytes) throws Exception {
        String expected=null;
        if ("ayah_themes_private/titles_114.json".equals(asset)) expected="a42914ae968c77bc7224f5126a41cc3e92099fbc797a91b01d74405d242eb209";
        if ("ayah_themes_private/macros_114.json".equals(asset)) expected="ff3e8562c68acbcd57c5a4a4c83e005c982489489486eb32ca2f95091619b06b";
        if ("ayah_themes_private/special_27.json".equals(asset)) expected="d3fb2a7cc23f84851f83caa14ab23a549b8f410529930131dd2c2560d7450404";
        if(expected==null||!matches(bytes,expected))throw new IllegalStateException("unapproved private display asset");
    }
    static boolean matches(byte[] bytes,String expected)throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex=new StringBuilder();
        for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return hex.toString().equals(expected);
    }
}
