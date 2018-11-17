package com.networkrecorder.rtp;

import java.util.Hashtable;

public class ByteUtil {

   public static int unsignedByteToInt(byte b) {
      return (int) b & 0xFF;
   }

   public static long unsignedByteToLong(byte b) {
      return (long) b & 0xFF;
   }

   public static long unsynchedByteToLong(byte b) {
      return (long) b & 0x7F;
   }

   public static long unsignedBytesToLong(byte b1, byte b2, byte b3, byte b4) {
      return unsignedByteToLong(b1) * 16777216L + unsignedByteToLong(b2) * 65536L + unsignedByteToLong(b3) * 256L + unsignedByteToLong(b4);
   }

   public static int unsignedBytesToInt(byte b3, byte b4) {
      return unsignedByteToInt(b3) * 256 + unsignedByteToInt(b4);
   }

   public static long unsynchedBytesToLong(byte b1, byte b2, byte b3, byte b4) {
      return unsynchedByteToLong(b1) * 2097152L + unsynchedByteToLong(b2) * 16384L + unsynchedByteToLong(b3) * 128L + unsynchedByteToLong(b4);
   }

   public static void intToBytes(int val, byte dest[], int pos) {
      dest[pos] = (byte) ((val >>> 24) & 0xFF);
      dest[pos + 1] = (byte) ((val >>> 16) & 0xFF);
      dest[pos + 2] = (byte) ((val >>> 8) & 0xFF);
      dest[pos + 3] = (byte) (val & 0xFF);
   }

   public static void intToUnsynchedBytes(int val, byte dest[], int pos) {
      dest[pos] = (byte) ((val >>> 21) & 0x7F);
      dest[pos + 1] = (byte) ((val >>> 14) & 0x7F);
      dest[pos + 2] = (byte) ((val >>> 7) & 0x7F);
      dest[pos + 3] = (byte) (val & 0x7F);
   }
   
   public static String replaceChars(String in) {
      return in.replace(':', '_').replace('?', '_').replace('*', '_').replace('|', '_');
   }

   public static long readLE(byte[] buf, int start, int bytes) throws Exception {

      long result = 0;
      try {
         for (int i = start + bytes - 1; i >= start; i--) {
            int x = ByteUtil.unsignedByteToInt(buf[i]);
            result = result * 256 + x; 
         }
      } catch (Exception e) { }
      return result;
   }

   public static long readBE(byte[] buf, int start, int bytes) throws Exception {

      long result = 0;
      try {
         for (int i = 0; i < bytes; i++) {
            int x = ByteUtil.unsignedByteToInt(buf[start+i]);
            result = result * 256 + x; 
         }
      } catch (Exception e) { }
      return result;
   }
   
   public static Float getUserTextValueFloat(Hashtable<String, String> userText, String key) {

      if (userText != null) {
         String val = userText.get(key);
         try {
            if (val != null && val.length() > 0) {
               int pos = val.indexOf(' ');
               if (pos > 0) {
                  return Float.parseFloat(val.substring(0, pos).trim());
               } else {
                  return Float.parseFloat(val);
               }
            }
         } catch (Exception e) { }
      }
      return null;
   }

   public static String getFrameEncoding(byte[] buf, int pos) {
      return (buf[pos] == 1 ? "UTF-16" : "ISO-8859-1");
   }
   
   public static FrameText readFrameText(byte[] data, int pos, int size, String enc) {
      
      FrameText f = new FrameText();
      if (enc == null) enc = "ISO-8859-1";
      try {
         int i;
         for (i = pos; i < size && data[i] != 0; i++);
         f.text = new String(data, pos, i - pos, enc);
         f.nextPos = (enc.contains("16") ? i+2 : i+1);
      } catch (Exception e) { }
      return f;
   }

   public static class FrameText {
      public String text = null;
      public int nextPos = 0;
   }

   public static int stringToBytes(String src, byte dest[], int pos, int max) {
      return stringToBytes(src, dest, pos, max, false);
   }

   public static int stringToBytes(String src, byte dest[], int pos, int max, boolean zeroTerm) {
      int len = 0;
      if (src != null) {
         len = src.length();
         if (len > max) len = max;
         for (int j = 0; j < len; j++, pos++) {
            dest[pos] = (byte) src.charAt(j);
         }
      }
      if (zeroTerm) {
         dest[pos] = (byte) 0;
         len++;
      }
      return len;
   }

   public static int storeInt(int val, byte[] buf, int pos) {
      buf[pos++] = (byte) ((val >>> 24) & 0xFF);
      buf[pos++] = (byte) ((val >>> 16) & 0xFF);
      buf[pos++] = (byte) ((val >>> 8) & 0xFF);
      buf[pos++] = (byte) (val & 0xFF);
      return pos;
   }

}
