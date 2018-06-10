package it.albertus.eqbulletin.service.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import it.albertus.eqbulletin.config.EarthquakeBulletinConfig;
import it.albertus.eqbulletin.gui.preference.Preference;
import it.albertus.eqbulletin.model.MapImage;
import it.albertus.jface.preference.IPreferencesConfiguration;
import it.albertus.util.IOUtils;

public class ImageDownloader {

	private static final int BUFFER_SIZE = 8192;

	private static final IPreferencesConfiguration configuration = EarthquakeBulletinConfig.getInstance();

	private ImageDownloader() {
		throw new IllegalAccessError();
	}

	public static MapImage downloadImage(final URL url, final String etag) throws IOException {
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = prepareConnection(url);
			if (etag != null && !etag.isEmpty()) {
				urlConnection.setReadTimeout(Math.min(3000, configuration.getInt(Preference.HTTP_READ_TIMEOUT_MS, ConnectionFactory.Defaults.READ_TIMEOUT_IN_MILLIS)));
				urlConnection.setRequestProperty("If-None-Match", etag);
			}
			if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) { // Connection starts here
				return null;
			}
			else {
				final String responseContentEncoding = urlConnection.getContentEncoding();
				final boolean gzip = responseContentEncoding != null && responseContentEncoding.toLowerCase().contains("gzip");
				try (final InputStream internalInputStream = urlConnection.getInputStream(); final InputStream inputStream = gzip ? new GZIPInputStream(internalInputStream) : internalInputStream; final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
					IOUtils.copy(inputStream, buffer, BUFFER_SIZE);
					return new MapImage(buffer.toByteArray(), urlConnection.getHeaderField("etag"));
				}
			}
		}
		finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	private static HttpURLConnection prepareConnection(final URL url) throws IOException {
		final HttpURLConnection urlConnection = ConnectionFactory.createHttpConnection(url);
		urlConnection.setRequestProperty("Accept", "image/*,*/*;0.9");
		urlConnection.setRequestProperty("Accept-Encoding", "gzip");
		return urlConnection;
	}

}
