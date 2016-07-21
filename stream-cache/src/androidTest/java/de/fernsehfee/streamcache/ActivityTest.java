package de.fernsehfee.streamcache;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.test.ActivityTestCase;
import android.test.ApplicationTestCase;
import android.test.mock.MockContext;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ActivityTest extends ActivityTestCase {
	public void testCache() {
		Log.v("TEST", "Starting testCache");
		Context context = getInstrumentation().getContext();

		final Random r = new Random();
		final String[] choice = new String[] {
				"a", "b", "c", "d", "e", "f", "g", "h"
		};

		InputStream is;
		Cache cache = Cache.getInstance(context);
//		cache.setCacheFolder(new File("/sdcard/"));

		OldCacheCallback callback = new OldCacheCallback() {
			@Nullable
			@Override
			public InputStream onCacheExpired(@NonNull File file) {
				Log.w("TEST", "Cache expired - requery data");
				InputStream is;

				String s = "";
				int len = r.nextInt(50)+10;
				for(int i = 0; i < len; i++) {
					s = s + choice[r.nextInt(choice.length-1)];
				}

				is = new ByteArrayInputStream(s.getBytes());

				assertNotNull(is);
				return is;
			}
		};

		for(int i = 0; i < 30; i++) {
			is = cache.get("test.1", TimeUnit.SECONDS, 5, callback);
			assertNotNull(is);

			try {
				String data = Utils.convertStreamToString(is);
				int length = data.length();

				assertTrue(length > 0);

				Log.v("TEST", "Data length: " + length);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {
			}
		}

		Log.v("TEST", "Has cache? "+cache.has("test.1", TimeUnit.SECONDS, 5));

		cache.deleteKey("test.1");

		assertFalse("We deleted a key - this should now not exist", cache.has("test.1", TimeUnit.SECONDS, 5));
	}
}