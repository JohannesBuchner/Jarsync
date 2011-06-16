package org.metastatic;

/**
 * Hashing algorithm to use. Rsync used to use MD4, but MD5 is much better.
 * 
 * @author Johannes Buchner
 */
public class HASH_ALGORITHM
{
  // public static final int DIGEST_LENGTH = 7;
  // public static String DIGEST_NAME = "MD4";
  public static final int DIGEST_LENGTH = 7;
  public static String DIGEST_NAME = "MD5";
}
