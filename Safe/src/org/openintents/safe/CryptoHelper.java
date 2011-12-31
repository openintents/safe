/* $Id$
 * 
 * Copyright 2007-2008 Steven Osborn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openintents.safe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.openintents.safe.service.ServiceDispatchImpl;
import org.openintents.util.SecureDelete;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import estreamj.ciphers.trivium.Trivium;
import estreamj.framework.ESJException;

/**
 * Crypto helper class.
 * 
 * Basic crypto class that uses Bouncy Castle Provider to
 * encrypt/decrypt data using PBE (Password Based Encryption) via
 * 128Bit AES. I'm fairly new to both Crypto and Java so if you 
 * notice I've done something terribly wrong here please let me
 * know.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class CryptoHelper {

	private static final boolean debug = false;
	private static String TAG = "CryptoHelper";

	public static final String OISAFE_EXTENSION = ".oisafe";

	protected static PBEKeySpec pbeKeySpec;
	protected static PBEParameterSpec pbeParamSpec;
	protected static SecretKeyFactory keyFac;

	public final static int EncryptionMedium=1;
	public final static int EncryptionStrong=2;

	protected static String algorithmMedium = "PBEWithMD5And128BitAES-CBC-OpenSSL";
//  protected static String algorithm = "PBEWithSHA1And128BitAES-CBC-BC";  // slower
	protected static String algorithmStrong = "PBEWithSHA1And256BitAES-CBC-BC";
	private String algorithm = "";
	protected static String desAlgorithm = "DES";
	protected static String password = null;
	protected static SecretKey pbeKey;
	protected static Cipher pbeCipher;
	private boolean status=false;	// status of the last encrypt/decrypt

	private static byte[] salt = null; 

	private static final int count = 20;

	/**
	 * Session key for content provider.
	 */
	private String sessionKey = null;


	/**
	 * Constructor which defaults to a medium encryption level.
	 */
	public CryptoHelper() {
//    	initialize(EncryptionMedium);
	}
	/**
	 * Constructor which allows the specification of the encryption level.
	 * 
	 * @param strength encryption strength
	 * @param salt salt to be used
	 */
	public void init(int strength, String salt) throws CryptoHelperException {
		try {
			setSalt(salt);
			initialize(strength);
		} catch (CryptoHelperException e) {
			e.printStackTrace();
			throw e;
		}
	}
	/**
	 * Initialize the class.  Sets the encryption level for the instance
	 * and generates the secret key factory.
	 * 
	 * @param Strength
	 */
	private void initialize(int Strength) {
		switch (Strength) {
		case EncryptionMedium:
			algorithm=algorithmMedium;
			break;
		case EncryptionStrong:
			algorithm=algorithmStrong;
			break;
		}
		pbeParamSpec = new PBEParameterSpec(salt,count);
		try {
			keyFac = SecretKeyFactory
					.getInstance(algorithm,"BC");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG,"CryptoHelper(): "+e.toString());
		} catch (NoSuchProviderException e) {
			Log.e(TAG,"CryptoHelper(): "+e.toString());		
		}
	}

	/**
	 * Generate a random salt for use with the cipher.
	 * 
	 * @author Randy McEoin
	 * @return String version of the 8 byte salt
	 */
	public static String generateSalt() throws NoSuchAlgorithmException {
		byte[] salt = new byte[8];
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
			sr.nextBytes(salt);
			if (debug) Log.d(TAG,"generateSalt: salt="+salt.toString());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw e;
		}
		return toHexString(salt);
	}
	/**
	 * @author Isaac Potoczny-Jones
	 * 
	 * @return null if failure, otherwise hex string version of key
	 */
	public static String generateMasterKey () throws NoSuchAlgorithmException {
		try {
			KeyGenerator keygen;
			keygen = KeyGenerator.getInstance("AES");
			keygen.init(256);
			SecretKey genDesKey = keygen.generateKey();
			return toHexString(genDesKey.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG,"generateMasterKey(): "+e.toString());
			throw e;
		}
	}

	/**
	 * 
	 * @param message
	 * @return MD5 digest of message in a byte array
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static byte[] md5String(String message) {
	
		byte[] input = message.getBytes();
	
		MessageDigest hash;
		ByteArrayInputStream	bIn = null;
		DigestInputStream	dIn = null;
	
		try {
			hash = MessageDigest.getInstance("MD5");
	
			bIn = new ByteArrayInputStream(input);
			dIn = new DigestInputStream(bIn, hash);
	
			for(int i=0;i<input.length;i++) {
				dIn.read();
			}
	
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG,"md5String(): "+e.toString());
		} catch (IOException e) {
			Log.e(TAG,"md5String(): "+e.toString());
		}
	
		return dIn.getMessageDigest().digest();
	}

	/**
	 * 
	 * @param bytes
	 * @return String version in Hex format of input byte array
	 */
	public static String toHexString(byte bytes[]) {
	
		StringBuffer retString = new StringBuffer();
		for (int i = 0; i < bytes.length; ++i) {
			retString.append(Integer
					.toHexString(0x0100 + (bytes[i] & 0x00FF))
					.substring(1));
		}
		return retString.toString();
	}

	public static byte[] hexStringToBytes(String hex) {

		byte [] bytes = new byte [hex.length() / 2];
		int j = 0;
		for (int i = 0; i < hex.length(); i += 2)
		{
			try {
				String hexByte=hex.substring(i, i+2);

				Integer I = new Integer (0);
				I = Integer.decode("0x"+hexByte);
				int k = I.intValue ();
				bytes[j++] = new Integer(k).byteValue();
			} catch (NumberFormatException e)
			{
				Log.i(TAG,e.getLocalizedMessage());
				return bytes;
			} catch (StringIndexOutOfBoundsException e)
			{
				Log.i(TAG,"StringIndexOutOfBoundsException");
				return bytes;
			}
		}
		return bytes;
	}

	/**
	 * Set the password to be used as an encryption key
	 * 
	 * @param pass - might be a user-entered key, or one generated by generateMasterKey.
	 * @throws Exception
	 */
	public void setPassword(String pass) {
		if (debug) Log.d(TAG,"setPassword("+pass+")");
		password = pass;
		pbeKeySpec = new PBEKeySpec(password.toCharArray());
		try {
			pbeKey = keyFac.generateSecret(pbeKeySpec);
			pbeCipher = Cipher
					.getInstance(algorithm,"BC");
		} catch (InvalidKeySpecException e) {
			Log.e(TAG,"setPassword(): "+e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG,"setPassword(): "+e.toString());
		} catch (NoSuchProviderException e) {
			Log.e(TAG,"setPassword(): "+e.toString());
		} catch (NoSuchPaddingException e) {
			Log.e(TAG,"setPassword(): "+e.toString());
		}
		
		// Every time we set a new password, also the session key changes:
		sessionKey = createNewSessionKey();
	}

	private void setSalt(String saltIn) throws CryptoHelperException {
		if (saltIn==null) {
			String msg = "Salt must not be null.";
			throw new CryptoHelperException(msg);
		}
		byte[] byteSaltIn=hexStringToBytes(saltIn);

		if (byteSaltIn.length != 8) {
			String msg = "Salt must be 8 bytes in length.";
			throw new CryptoHelperException(msg);
		}
		salt=byteSaltIn;
		if (debug) Log.d(TAG,"setSalt: salt="+toHexString(salt));
	}

	/**
	 * Returns the current session key, which is only valid until the
	 * user logs out of OI Safe.
	 * 
	 * The session key is used when encrypting or decrypting files
	 * through the content provider.
	 * 
	 * @return current session key.   If there is none, return null.
	 */
	public String getCurrentSessionKey() {

		if (ServiceDispatchImpl.ch != null) {
			// Return a global session key created in ServiceDispatchImpl
			return ServiceDispatchImpl.ch.sessionKey; 
		}
		return null;
		// This should be the same session key as is used in CryptoContentProvider.
		// TODO: Clean up the code above?
	}

	/**
	 * Creates a new random session key
	 * @return
	 */
	private String createNewSessionKey() {
		try {
			// simply create a new salt:
			return generateSalt();
		} catch (NoSuchAlgorithmException e) {
			return "12345"; // better than nothing... :-/
		}
	}

	/**
	 * encrypt a string
	 * 
	 * @param plaintext
	 * @return encrypted String
	 * @throws Exception
	 */
	public String encrypt(String plaintext) throws CryptoHelperException {
		status=false; // assume failure
		if(password == null) {
			String msg = "Must call setPassword before running encrypt.";
			throw new CryptoHelperException(msg);
		}
		if (salt==null) {
			String msg = "Must call setSalt before running encrypt.";
			throw new CryptoHelperException(msg);
		}
		byte[] ciphertext = {};
		if (plaintext==null) {
			return "";
		}
	
		try {
			pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
			ciphertext = pbeCipher.doFinal(plaintext.getBytes());
			status=true;
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG,"encrypt(): "+e.toString());
		} catch (BadPaddingException e) {
			Log.e(TAG,"encrypt(): "+e.toString());
		} catch (InvalidKeyException e) {
			Log.e(TAG,"encrypt(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(TAG,"encrypt(): "+e.toString());
		}
	
		String stringCiphertext=toHexString(ciphertext);
		return stringCiphertext;
	}

	/**
	 * unencrypt previously encrypted string
	 * 
	 * @param ciphertext
	 * @return decrypted String
	 * @throws Exception
	 */
	public String decrypt(String ciphertext) throws CryptoHelperException {
		status=false; // assume failure
		if(password == null) {
			String msg = "Must call setPassword before running decrypt.";
			throw new CryptoHelperException(msg);
		}
		if (salt==null) {
			String msg = "Must call setSalt before running decrypt.";
			throw new CryptoHelperException(msg);
		}
	
		if ((ciphertext==null) || (ciphertext.length()==0)) {
			return "";
		}
		byte[] byteCiphertext=hexStringToBytes(ciphertext);
		byte[] plaintext = {};
		
		try {
			pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
			plaintext = pbeCipher.doFinal(byteCiphertext);
			status=true;
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (BadPaddingException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidKeyException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		}
	
		return new String(plaintext);
	}

	/**
	 * Status of the last encrypt or decrypt.
	 * 
	 * @return true if last operation was successful
	 */
	public boolean getStatus() {
		return status;
	}


	/**
	 * encrypt a string using a random session key
	 * 
	 * @author Peli
	 * 
	 * @param plaintext
	 * @return encrypted String
	 * @throws Exception
	 */
	public String encryptWithSessionKey(String plaintext) throws CryptoHelperException {
		if (debug) Log.i(TAG, "Encrypt with session key");
		status=false; // assume failure
		if(password == null) {
			String msg = "Must call setPassword before runing encrypt.";
			throw new CryptoHelperException(msg);
		}
		byte[] cipherSessionKey = {};
		byte[] ciphertext = {};
		
		// First create a session key
		SecretKey sessionKey = null;
		byte[] sessionKeyEncoded = null;
		String sessionKeyString = null;
		try {
			KeyGenerator keygen;
			keygen = KeyGenerator.getInstance("AES");
			keygen.init(256); // needs 96 bytes
			//keygen.init(128); // needs 64 bytes
			sessionKey = keygen.generateKey();
			sessionKeyEncoded = sessionKey.getEncoded();
			sessionKeyString = new String(sessionKeyEncoded);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG,"generateMasterKey(): "+e.toString());
		}

		// Convert this to a Pbe key
		PBEKeySpec sessionPbeKeySpec = new PBEKeySpec(sessionKeyString.toCharArray());
		SecretKey sessionPbeKey = null;
		try {
			sessionPbeKey = keyFac.generateSecret(sessionPbeKeySpec);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG,"setPassword(): "+e.toString());
		}

		// Encrypt the session key using the master key
		try {
			pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
			cipherSessionKey = pbeCipher.doFinal(sessionKeyEncoded);
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
		} catch (BadPaddingException e) {
			Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
		} catch (InvalidKeyException e) {
			Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
		}

		// Now encrypt the text using the session key
		try {
			pbeCipher.init(Cipher.ENCRYPT_MODE, sessionPbeKey, pbeParamSpec);
			ciphertext = pbeCipher.doFinal(plaintext.getBytes());
			status=true;
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG,"encryptWithSessionKey2(): "+e.toString());
		} catch (BadPaddingException e) {
			Log.e(TAG,"encryptWithSessionKey2(): "+e.toString());
		} catch (InvalidKeyException e) {
			Log.e(TAG,"encryptWithSessionKey2(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(TAG,"encryptWithSessionKey2(): "+e.toString());
		}
	
		String stringCipherVersion = "A";
		String stringCipherSessionKey = toHexString(cipherSessionKey);
		String stringCiphertext=toHexString(ciphertext);
		if (debug) Log.i(TAG, "Length: " + stringCipherSessionKey.length() + ", " + stringCipherSessionKey);
		
		StringBuilder sb = new StringBuilder(stringCipherVersion.length() 
				+ stringCipherSessionKey.length() 
				+ stringCiphertext.length());
		sb.append(stringCipherVersion);
		sb.append(stringCipherSessionKey);
		sb.append(stringCiphertext);
		return sb.toString();
	}

	/**
	 * unencrypt encrypted string previously encrypted with
	 * encryptWithSessionKey()
	 * 
	 * @author Peli
	 * 
	 * @param ciphertext
	 * @return decrypted String
	 * @throws Exception
	 */
	public String decryptWithSessionKey(String ciphertext) throws CryptoHelperException {
		status=false; // assume failure
		if(password == null) {
			String msg = "Must call setPassword before running decrypt.";
			throw new CryptoHelperException(msg);
		}
	
		if ((ciphertext==null) || (ciphertext=="")) {
			return "";
		}
		String cipherVersion = null;
		String cipherSessionKey = null;
		
		// Split cipher into session key and text
		try {
			cipherVersion = ciphertext.substring(0,1);
			if (cipherVersion.equals("A")) {
				cipherSessionKey = ciphertext.substring(1,97); // 64 if init(128) had been chosen
				ciphertext = ciphertext.substring(97);
			} else {
				Log.e(TAG, "Unknown cipher version" + cipherVersion);
				return "";
			}
		} catch (IndexOutOfBoundsException e) {
			Log.e(TAG, "Invalid ciphertext (with session key)");
			return "";
		}
		
		// Decrypt the session key
		byte[] byteCipherSessionKey=hexStringToBytes(cipherSessionKey);
		byte[] byteSessionKey = {};
		
		try {
			pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
			byteSessionKey = pbeCipher.doFinal(byteCipherSessionKey);
			status=true;
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (BadPaddingException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidKeyException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		}

		// Convert the session key into a Pbe key
		String stringSessionKey = new String(byteSessionKey);
		PBEKeySpec sessionPbeKeySpec = new PBEKeySpec(stringSessionKey.toCharArray());
		SecretKey sessionPbeKey = null;
		try {
			sessionPbeKey = keyFac.generateSecret(sessionPbeKeySpec);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG,"setPassword(): "+e.toString());
		}

		// Use the session key to decrypt the text
		byte[] byteCiphertext=hexStringToBytes(ciphertext);
		byte[] plaintext = {};
		
		try {
			pbeCipher.init(Cipher.DECRYPT_MODE, sessionPbeKey, pbeParamSpec);
			plaintext = pbeCipher.doFinal(byteCiphertext);
			status=true;
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (BadPaddingException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidKeyException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(TAG,"decrypt(): "+e.toString());
		}
		
		return new String(plaintext);
	}


	/**
	 * encrypt a file using a random session key
	 * 
	 * @author Peli
	 * 
	 * @param contentResolver is used to be able to read the stream
	 * @param fileUri is the stream or file to read from
	 * @return Uri to the created plaintext file
	 * @throws Exception
	 */
	public Uri encryptFileWithSessionKey(ContentResolver contentResolver, Uri fileUri)
		throws CryptoHelperException {
		if (debug) Log.d(TAG, "Encrypt with session key");
		status=false; // assume failure
		if(password == null) {
			String msg = "Must call setPassword before runing encrypt.";
			throw new CryptoHelperException(msg);
		}
		
		String outputPath = "";
		try {
			InputStream is;
			if (fileUri.getScheme().equals("file")) {
				is = new java.io.FileInputStream(fileUri.getPath());
				outputPath = fileUri.getPath() + OISAFE_EXTENSION;
			} else {
				is = contentResolver.openInputStream(fileUri);
				outputPath = getTemporaryFileName();
			}
			
			FileOutputStream os = new FileOutputStream(outputPath);
			
			byte[] cipherSessionKey = {};
//			byte[] ciphertext = {};
			
			// First create a session key
			SecretKey sessionKey = null;
			byte[] sessionKeyEncoded = null;
//			String sessionKeyString = null;
			try {
				KeyGenerator keygen;
				keygen = KeyGenerator.getInstance("AES");
				keygen.init(256); // needs 96 bytes
				//keygen.init(128); // needs 64 bytes
				sessionKey = keygen.generateKey();
				sessionKeyEncoded = sessionKey.getEncoded();
//				sessionKeyString = new String(sessionKeyEncoded);
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG,"generateMasterKey(): "+e.toString());
				return null;
			}

			// Encrypt the session key using the master key
			try {
				pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
				cipherSessionKey = pbeCipher.doFinal(sessionKeyEncoded);
				status=true;
			} catch (IllegalBlockSizeException e) {
				Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
			} catch (BadPaddingException e) {
				Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
			} catch (InvalidKeyException e) {
				Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
			} catch (InvalidAlgorithmParameterException e) {
				Log.e(TAG,"encryptWithSessionKey(): "+e.toString());
			}
			if (status==false) {
				return null;
			}
			status=false;
	
			String stringCipherVersion = "A";
			byte[] bytesCipherVersion = stringCipherVersion.getBytes();
			os.write(bytesCipherVersion, 0, bytesCipherVersion.length);
	
			os.write(cipherSessionKey, 0, cipherSessionKey.length);
	
			if (debug) Log.d(TAG, "bytesCipherVersion.length: " + bytesCipherVersion.length);
			if (debug) Log.d(TAG, "cipherSessionKey.length: " + cipherSessionKey.length);
			
			Trivium tri = new Trivium();
			try {
				tri.setupKey(Trivium.MODE_ENCRYPT,
						sessionKeyEncoded, 0);
				tri.setupNonce(sessionKeyEncoded, 10);
				
				// Create the byte array to hold the data
				final int bytesLen = 4096; // buffer length
				byte[] bytesIn = new byte[bytesLen];
				byte[] bytesOut = new byte[bytesLen];
	
				int offset = 0;
				int numRead = 0;
				while ((numRead = is.read(bytesIn, 0, bytesLen)) >= 0) {
					tri.process(bytesIn, 0,
							bytesOut, 0, numRead);

					os.write(bytesOut, 0, numRead);
					offset += numRead;
				}
				
				// Ensure all the bytes have been read in
				if (offset < is.available()) {
					throw new IOException("Could not completely read file ");
				}
	
				// Close the input stream and return bytes
				is.close();
				os.close();
				
				// Securely delete the original file:
				SecureDelete.delete(new File(fileUri.getPath()));
				status=true;
				
			} catch (ESJException e) {
				Log.e(TAG, "Error encrypting file", e);
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found", e);
		} catch (IOException e) {
			Log.e(TAG, "IO Exception", e);
		}
		
		if (status==false) {
			return null;
		}
		return Uri.fromFile(new File(outputPath)); //Uri.parse("file://" + outputPath); // TODO: UUEncode
	}
	/**
	 * @return
	 */
	private String getTemporaryFileName() throws CryptoHelperException {
		String randomPart;
		try {
			// create a random session name
			randomPart=generateSalt();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			String msg = "Decrypt error: "+e1.getLocalizedMessage();
			throw new CryptoHelperException(msg);
		}
		
		return Environment
		.getExternalStorageDirectory().toString() + "/tmp-" + randomPart;
	}

	/**
	 * Dencrypt a file previously encrypted with
	 * encryptFileWithSessionKey().
	 * 
	 * Creates a new file without the .oisafe extension.
	 * 
	 * @author Peli
	 * 
	 * @param ctx Context of activity in order to store temp file
	 * @param fileUri Uri to either a stream or a file to read from
	 * @return If decryption is successful, returns Uri of a content 
	 * 		provider to read the plaintext file.  Upon failure,
	 * 		return null.
	 * @throws Exception
	 */
	public Uri decryptFileWithSessionKey(Context ctx, Uri fileUri) throws CryptoHelperException {
		if (debug) Log.d(TAG, "fileUri="+fileUri.toString());
		ContentResolver contentResolver = ctx.getContentResolver();

		String inputPath = null;
		String outputPath = null;
		Uri resultUri = null;
		boolean result = false;
		
		try {
			InputStream is;
			if (fileUri.getScheme().equals("file")) {
				inputPath = fileUri.getPath();
				is = new java.io.FileInputStream(inputPath);
				if (debug) Log.d(TAG, "Decrypt: Input from " + inputPath);
				if (inputPath.endsWith(OISAFE_EXTENSION)) {
					outputPath = inputPath.substring(0, inputPath.length() - OISAFE_EXTENSION.length());
				}
			} else {
				is = contentResolver.openInputStream(fileUri);
				if (debug) Log.d(TAG, "Decrypt: Input from " + fileUri.toString());
			}

			if (outputPath == null) {
				outputPath = getTemporaryFileName();
			}
			
			FileOutputStream os = new FileOutputStream(outputPath);
	
			// after writing the decrypted content to a temporary file,
			// pass back a Uri that can be used to read back the contents
			resultUri = Uri.fromFile(new File(outputPath)); //Uri.parse("file://" + outputPath); // TODO: UUEncode?
			
			result = decryptStreamWithSessionKey(ctx, is, os);
	
			// Close the input stream
			is.close();
			os.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found", e);
		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "IllegalArgumentException", e);
		}


		if (result == true) {
			// Successful

			// Securely delete the original file:
			if (inputPath != null) {
				SecureDelete.delete(new File(inputPath));
			}
		} else {
			resultUri = null;
			
			// Unsuccessful. Clean up
			//ctx.deleteFile(sessionFile);
		}
		
		return resultUri;
	}

	/**
	 * Dencrypt a file previously encrypted with
	 * encryptFileWithSessionKey().
	 * 
	 * The original file is not modified
	 * 
	 * @author Peli
	 * 
	 * @param ctx Context of activity in order to store temp file
	 * @param fileUri Uri to either a stream or a file to read from
	 * @return If decryption is successful, returns Uri of a content 
	 * 		provider to read the plaintext file.  Upon failure,
	 * 		return null.
	 * @throws Exception
	 */
	public Uri decryptFileWithSessionKeyThroughContentProvider(Context ctx, Uri fileUri) throws CryptoHelperException {
		if (debug) Log.d(TAG, "fileUri="+fileUri.toString());
		ContentResolver contentResolver = ctx.getContentResolver();
		
		String sessionFile = "";
		Uri resultUri = null;
		boolean result = false;
		
		try {
			InputStream is;
			if (fileUri.getScheme().equals("file")) {
				is = new java.io.FileInputStream(fileUri.getPath());
				if (debug) Log.d(TAG, "Decrypt: Input from " + fileUri.getPath());
			} else {
				is = contentResolver.openInputStream(fileUri);
				if (debug) Log.d(TAG, "Decrypt: Input from " + fileUri.toString());
			}
			FileOutputStream os = null;
			
			String decryptSession;
			try {
				// create a random session name
				decryptSession=generateSalt();
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
				String msg = "Decrypt error: "+e1.getLocalizedMessage();
				throw new CryptoHelperException(msg);
			}
			sessionFile = CryptoContentProvider.SESSION_FILE+"."+decryptSession;
			if (debug) Log.d(TAG, "Decrypt: Output to " + sessionFile);
				
			// openFileOutput creates a file in /data/data/{packagename}/files/
			// In our case, /data/data/org.openintents.safe/files/
			// This file is owned and only readable by our application
			os = ctx.openFileOutput(sessionFile,
						Context.MODE_PRIVATE);
	
			// after writing the decrypted content to a temporary file,
			// pass back a Uri that can be used to read back the contents
			resultUri = Uri.withAppendedPath(CryptoContentProvider.CONTENT_URI, "decrypt/" + decryptSession);
			
			result = decryptStreamWithSessionKey(ctx, is, os);
	
			// Close the input stream
			is.close();
			os.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found", e);
		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
		}

		if (result == false) {
			resultUri = null;
			
			// Unsuccessful. Clean up
			ctx.deleteFile(sessionFile);
		}
		
		return resultUri;
	}

	/**
	 * Unencrypt a file previously encrypted with
	 * encryptFileWithSessionKey().
	 * 
	 * @author Peli
	 * 
	 * @param ctx Context of activity in order to store temp file
	 * @param fileUri Uri to either a stream or a file to read from
	 * @param useContentProvider true for using Content Provider,
	 *        false for creating a file without ".oisafe" extension and
	 *        deleting the original file.
	 * @return True if successful, otherwise false.
	 * @throws Exception
	 */
	public boolean decryptStreamWithSessionKey(Context ctx, InputStream is, OutputStream os) throws CryptoHelperException {
		if (debug) Log.d(TAG, "decryptStreamWithSessionKey");
		status=false; // assume failure
		if(password == null) {
			String msg = "Must call setPassword before running decrypt.";
			throw new CryptoHelperException(msg);
		}

		try {
			
			int numReadTotal = 0;
			int numRead = 0;
			
			byte[] byteCipherVersion = new byte[1];
			while ((numRead = is.read(byteCipherVersion, numRead, 
					byteCipherVersion.length - numRead)) >= 0
					&& numReadTotal < byteCipherVersion.length) {
				if (debug) Log.d(TAG, "read bytes: " + numRead);
				numReadTotal += numRead;
			}
			String cipherVersion = new String(byteCipherVersion);
			
			byte[] byteCipherSessionKey = null;
				
			// Split cipher into session key and text
			try {
				if (debug) Log.d(TAG, "cipherVersion : " + cipherVersion);
				if (cipherVersion.equals("A")) {
	
					numRead = 0;
					numReadTotal = 0;
					byteCipherSessionKey = new byte[48];
					// Read the first few bytes that contain the encrypted key
					while ((numRead = is.read(byteCipherSessionKey, numRead, 
							byteCipherSessionKey.length - numRead)) >= 0
							&& numReadTotal < byteCipherSessionKey.length) {
						if (debug) Log.d(TAG, "read bytes sessionKey: " + numRead);
						numReadTotal += numRead;
					}
				} else {
					Log.e(TAG, "Unknown cipher version" + cipherVersion);
					return false;
				}
			} catch (IndexOutOfBoundsException e) {
				Log.e(TAG, "Invalid ciphertext (with session key)");
				return false;
			}
			
			// Decrypt the session key
			byte[] byteSessionKey = {};
			
			try {
				pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
				byteSessionKey = pbeCipher.doFinal(byteCipherSessionKey);
				status=true;
			} catch (IllegalBlockSizeException e) {
				Log.e(TAG,"decrypt(): "+e.toString());
			} catch (BadPaddingException e) {
				Log.e(TAG,"decrypt(): "+e.toString());
			} catch (InvalidKeyException e) {
				Log.e(TAG,"decrypt(): "+e.toString());
			} catch (InvalidAlgorithmParameterException e) {
				Log.e(TAG,"decrypt(): "+e.toString());
			}
	
			// Now decrypt the message
			Trivium tri = new Trivium();
			try {
				tri.setupKey(Trivium.MODE_DECRYPT,
						byteSessionKey, 0);
				tri.setupNonce(byteSessionKey, 10);
				
				// Create the byte array to hold the data
				final int bytesLen = 4096; // buffer length
				byte[] bytesIn = new byte[bytesLen];
				byte[] bytesOut = new byte[bytesLen];
	
				int offset = 0;
				numRead = 0;
				while ((numRead = is.read(bytesIn, 0, bytesLen)) >= 0) {
					tri.process(bytesIn, 0,
							bytesOut, 0, numRead);

					os.write(bytesOut, 0, numRead);
					offset += numRead;
				}
				
				// Ensure all the bytes have been read in
				if (offset < is.available()) {
					throw new IOException("Could not completely read file ");
				}
				status=true;
				
			} catch (ESJException e) {
				Log.e(TAG, "Error decrypting file", e);
			}

		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
		}
		
		return status;
	}
}
