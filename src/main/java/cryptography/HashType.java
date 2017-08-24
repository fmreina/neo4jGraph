package cryptography;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HashType {

	NONE("None"),
	BCrypt("BCrypt"),
	MD5("MD5"),
	SHA1("SHA-1"),
	SHA256("SHA-256");

	String algorithm;

}
