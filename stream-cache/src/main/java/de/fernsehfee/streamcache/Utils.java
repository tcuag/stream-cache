package de.fernsehfee.streamcache;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Utils {

	@NonNull
	public static String sha1(@NonNull String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(text.getBytes("UTF-8"), 0, text.length());
			byte[] sha1hash = md.digest();
			return convertToHex(sha1hash);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		throw new IllegalStateException("This should not get reached by an Android device. Check " +
				"that the device allows UTF-8 encoding.");
	}

	@NonNull
	private static String convertToHex(@NonNull byte[] data) {
		StringBuilder buf = new StringBuilder();
		for (byte aData : data) {
			int halfbyte = (aData >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = aData & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String convertStreamToString(InputStream inputStream) throws IOException {
		Reader reader = null;
		Writer writer = null;
		try {
			if (inputStream != null) {
				writer = new StringWriter();

				char[] buffer = new char[4096];
				reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
				return writer.toString();
			}
		} finally {
			if(reader != null) {
				reader.close();
			}
			if(inputStream != null) {
				inputStream.close();
			}
			if(writer != null) {
				writer.close();
			}
		}
		return "";
	}
}
