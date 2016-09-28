package de.fernsehfee.streamcache;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * The main access point to the caching library
 */
public class Cache {
	/**
	 * Our singleton instance
	 */
	private static Cache mInstance;

	/**
	 * Hold only a weak reference to the current context to allow it to be freed
	 */
	private WeakReference<Context> mContext;

	/**
	 * The location that the cache should be stored. By default
	 * {@link Context#getExternalCacheDir()}+"/stream-cache/".
	 */
	private File mCacheFolder = null;

	/**
	 * Get (and create) an instance of the caching library
	 *
	 * @param context The current {@link Context}
	 * @return The instance of this library
	 */
	@NonNull
	public static Cache getInstance(@NonNull Context context) {
		if (mInstance == null) {
			synchronized (Cache.class) {
				if (mInstance == null) {
					mInstance = new Cache();
				}
			}
		}

		mInstance.mContext = new WeakReference<>(context);

		return mInstance;
	}

	private Cache() {

	}

	/**
	 * Write the {@link InputStream} to a file designated by the SHA1 hash of the key given.
	 * This thread will block as it writes to file, it will then return a new {@link InputStream}
	 * with which to read to get the contents of the stream for continued work.
	 * @param key The name of the file (will be hashed) that points to the cache
	 * @param inputStream The stream of data that should be written to the cache
	 * @return An {@link InputStream} with which to continue work after storing the data.
	 */
	@WorkerThread
	@Nullable
	public InputStream put(@NonNull final String key, @NonNull final InputStream inputStream) {
		File file = new File(getCacheFolder(), Utils.sha1(key));
		//Log.v("TEST", "Writing stream to '"+file.getAbsolutePath()+"'");
		if(file.exists()) {
			if(!file.delete()) {
				throw new IllegalStateException("Could not delete a cache file. Likely an issue " +
						"with permissions: "+file);
			}
		}

		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
			int read = 0;
			byte[] buffer = new byte[8192];
			while((read = inputStream.read(buffer)) != -1) {
				//Log.v("TEST", "Writing "+read+" bytes");
				out.write(buffer, 0, read);
			}
		} catch (IOException ignored) {
			ignored.printStackTrace();
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(inputStream);
		}

		try {
			//Log.v("TEST", "Returning '"+file.getAbsolutePath()+"' to read");
			return new FileInputStream(file);
		} catch (FileNotFoundException ignored) {
			ignored.printStackTrace();
		}

		return null;
	}

	/**
	 * Check whether a cache exists (and is valid)
	 * @param key The name of the file (will be hashed) that points to the cache
	 * @param unit The unit of time to use in conjunction with 'age'
	 * @param age The number of time units
	 * @return True if the cache exists and is within the age constraints
	 */
	public boolean has(@NonNull final String key, @NonNull TimeUnit unit, long age) {
		File file = new File(getCacheFolder(), Utils.sha1(key));
		if(file.exists()) {
			long modified = file.lastModified();
			long timeDiff = System.currentTimeMillis() - modified;
			long ageMillis = TimeUnit.MILLISECONDS.convert(age, unit);

			if(timeDiff <= ageMillis) {
				// Cache is current
				return true;
			}
		}

		return false;
	}

	/**
	 * Get an {@link InputStream} that points to the cache, or create the cache using the
	 * {@link OldCacheCallback callback} and point to that.
	 * @param key The name of the file (will be hashed) that points to the cache
	 * @param unit The unit of time to use in conjunction with 'age'
	 * @param age The number of time units
	 * @param callback The callback to use if there is no acceptable cache
	 * @return An {@link InputStream} that points to a cache
	 */
	@Nullable
	public InputStream get(@NonNull final String key, @NonNull TimeUnit unit, long age, @NonNull OldCacheCallback callback) {
		File file = new File(getCacheFolder(), Utils.sha1(key));
		if(file.exists()) {
			long modified = file.lastModified();
			long timeDiff = System.currentTimeMillis() - modified;
			long ageMillis = TimeUnit.MILLISECONDS.convert(age, unit);

			if(timeDiff > ageMillis) {
				// Cache is too old.
				InputStream is = callback.onCacheExpired(file);
				if(is != null) {
					return put(key, is);
				}
			} else {
				// Cache is current
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
					// Not reachable.
				}
			}
		} else {
			InputStream is = callback.onCacheExpired(file);
			if(is != null) {
				return put(key, is);
			}
		}

		return null;
	}

	/**
	 * Delete the cache item from the cache folder
	 * @param key The name of the file (will be hashed) that points to the cache
	 */
	@SuppressWarnings("unused")
	public boolean deleteKey(@NonNull String key) {
		File file = new File(getCacheFolder(), Utils.sha1(key));
		return FileUtils.deleteQuietly(file);
	}

	/**
	 * Delete all items from the cache folder
	 * @return The number of items deleted
	 */
	@SuppressWarnings("unused")
	public int deleteAll() {
		File[] files = getCacheFolder().listFiles();
		int count = 0;
		for(File file : files) {
			if(FileUtils.deleteQuietly(file)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return The folder where cached files will be stored. Null if a cache folder could not be
	 * found or created.
	 */
	@SuppressWarnings("unused")
	@NonNull
	public File getCacheFolder() {
		if (mCacheFolder == null || !mCacheFolder.exists()) {
			File dir = null;

			dir = mContext.get().getExternalCacheDir();
			if(dir == null) {
				dir = mContext.get().getCacheDir();
			}

			if(dir == null) {
				throw new IllegalStateException("Could not create a location to cache. This leaves " +
						"the caching library in a bad state.");
			}

			mCacheFolder = new File(dir, "stream-cache");
		}

		if (!mCacheFolder.exists()) {
			if (!mCacheFolder.mkdirs()) {
				throw new IllegalStateException("Could not create a location to cache. This leaves " +
						"the caching library in a bad state.: "+mCacheFolder);
			}
		}

		return mCacheFolder;
	}

	/**
	 * Set the location to cache to
	 *
	 * @param cacheFolder The folder whereby caches will be saved
	 */
	@SuppressWarnings("unused")
	public void setCacheFolder(@NonNull File cacheFolder) {
		this.mCacheFolder = cacheFolder;
	}
}
