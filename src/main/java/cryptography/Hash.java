package cryptography;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

	public static String generateHash(String text, HashType algorithm) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm.getAlgorithm());

			md.update(text.getBytes());

			return byte2String(md.digest());

		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	private static String byte2String(byte[] bytes) {

		StringBuilder s = new StringBuilder();

		for (int i = 0; i < bytes.length; i++) {

			int bigEndian = (bytes[i] >> 4 & 0xf) << 4;

			int littleEndian = bytes[i] & 0xf;

			if (bigEndian == 0) {
				s.append('0');
			}

			s.append(Integer.toHexString(bigEndian | littleEndian));
		}

		return s.toString();
	}
}
