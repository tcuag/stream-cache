package de.fernsehfee.streamcache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.InputStream;

public interface OldCacheCallback {
	@Nullable
	InputStream onCacheExpired(@NonNull File file);
}
