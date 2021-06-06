package it.albertus.eqbulletin.service.net;

import java.io.InputStream;
import java.time.Duration;
import java.util.logging.Level;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import it.albertus.eqbulletin.config.EarthquakeBulletinConfig;
import it.albertus.eqbulletin.gui.preference.Preference;
import it.albertus.jface.preference.IPreferencesConfiguration;
import lombok.extern.java.Log;

@Log
public abstract class ResilientDownloader {

	protected final IPreferencesConfiguration configuration = EarthquakeBulletinConfig.getPreferencesConfiguration();

	protected final CircuitBreaker circuitBreaker = CircuitBreaker.of(getClass().getSimpleName() + CircuitBreaker.class.getSimpleName(), CircuitBreakerConfig.custom().minimumNumberOfCalls(10).build());
	protected final Retry retry = Retry.of(getClass().getSimpleName() + Retry.class.getSimpleName(), RetryConfig.custom().maxAttempts(configuration.getBoolean(Preference.PROXY_AUTH_REQUIRED, ConnectionFactory.Defaults.PROXY_AUTH_REQUIRED) ? 1 : 3).waitDuration(Duration.ofSeconds(1)).ignoreExceptions(CancelException.class).build());

	protected InputStream connectionInputStream;

	public void cancel() {
		if (connectionInputStream != null) {
			try {
				connectionInputStream.close();
			}
			catch (final Exception e) {
				log.log(Level.FINE, "Error closing the connection input stream:", e);
			}
		}
	}

}