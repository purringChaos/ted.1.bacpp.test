/*
 * -----------------------------------------------------------------------------
 *                     B L A C K A R R O W      I N C.
 * -----------------------------------------------------------------------------
 *
 * %W%\t%G%
 *
 * Copyright 2010 by BlackArrow Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of BlackArrow Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Black Arrow Inc.
 * 
 * @author Tom Nevin (tnevin@blackarrow.tv)
 *
 * @version  %I%, %G%
 *
 * @note There are copies of this code in barules and bapois.  If 
 * you make changes here, you need to make them there as well.
 *        
 * -----------------------------------------------------------------------------
 */
package tv.blackarrow.cpp.utils;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

public class UUIDUtils {

    /**
     * Wrapper to get the UUDI, synchronized is required UUID is not thread
     * safe. Actually it's a bug in the UUID class.
     * <p>
     * The UUID returned is a type 4, which is also known as a random UUID.
     * 
     * @return UUID
     */
    public static synchronized UUID getUUID() {
        return UUID.randomUUID();
    }

    /**
     * Encodes a UUID using a base 64 url safe scheme. The encoded ID is 22
     * characters long and uses only standard alpha-numeric characters plus '-'
     * and '_' so it is URL safe.
     * 
     * @return encoded UUID
     */
    public static String getBase64UrlEncodedUUID() {
        return encodeBase64UrlUUID(getUUID());
    }

    /**
     * Encodes a UUID using a base 64 url safe scheme. The encoded ID is 22
     * characters long and uses only standard alpha-numeric characters plus '-'
     * and '_' so it is URL safe.
     * 
     * @return encoded UUID
     */
    public static String encodeBase64UrlUUID(UUID uuid) {
        byte[] uuidArr = asByteArray(uuid);
        String encodedUUID = new String(Base64.encodeBase64(uuidArr));
        return encodedUUID.split("=")[0].replace('+', '-').replace('/', '_');
    }

    /**
     * Decodes a UUID that has been encoded by this classes encoder.
     * 
     * @return decoded UUID
     * @throws IOException
     */
    public static UUID decodeBase64UrlUUID(String encodedUUID) throws IOException {
        String base64Format = encodedUUID.replace('-', '+').replace('_', '/') + "==";
        byte[] uuidArr = Base64.decodeBase64(base64Format.getBytes());
        return toUUID(uuidArr);
    }

    public static UUID toUUID(byte[] byteArray) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (byteArray[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (byteArray[i] & 0xff);
        UUID result = new UUID(msb, lsb);
        return result;
    }

    public static byte[] asByteArray(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
        }
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> 8 * (7 - i));
        }
        return buffer;
    }

}