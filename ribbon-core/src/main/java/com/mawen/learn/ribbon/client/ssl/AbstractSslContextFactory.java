package com.mawen.learn.ribbon.client.ssl;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import jdk.nashorn.internal.runtime.logging.Logger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Slf4j
@Getter
public abstract class AbstractSslContextFactory {

	public static final String SOCKET_ALGORITHM = "SSL";

	private KeyStore keyStore;

	private KeyStore trustStore;

	private String keyStorePassword;

	private final int trustStorePasswordLength;

	private final int keyStorePasswordLength;

	protected AbstractSslContextFactory( final KeyStore trustStore, final String trustStorePassword, final  KeyStore keyStore, final String keyStorePassword) {
		this.trustStore = trustStore;
		this.keyStorePassword = keyStorePassword;
		this.keyStore = keyStore;

		this.keyStorePasswordLength = keyStorePassword != null ? keyStorePassword.length() : -1;
		this.trustStorePasswordLength = trustStorePassword != null ? trustStorePassword.length() : -1;
	}

	public SSLContext getSSLContext() throws ClientSslSocketFactoryException {
		return createSSLContext();
	}

	private SSLContext createSSLContext() throws ClientSslSocketFactoryException {
		final KeyManager[] keyManagers = this.keyStore != null ? createKeyManagers() : null;
		final TrustManager[] trustManagers = this.trustStore != null ? createTrustManagers() : null;

		try {
			final SSLContext sslContext = SSLContext.getInstance(SOCKET_ALGORITHM);

			sslContext.init(keyManagers, trustManagers, null);

			return sslContext;
		}
		catch (NoSuchAlgorithmException e) {
			throw new ClientSslSocketFactoryException(String.format("Failed to create an SSL context that supports algorithm %s: %s", SOCKET_ALGORITHM, e.getMessage()), e);
		}
		catch (KeyManagementException e) {
			throw new ClientSslSocketFactoryException(String.format("Failed to initialize an SSL context: %s", e.getMessage()), e);
		}
	}

	private KeyManager[] createKeyManagers() throws ClientSslSocketFactoryException {

		final KeyManagerFactory factory;

		try {
			factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			factory.init(this.keyStore, this.keyStorePassword.toCharArray());
		}
		catch (NoSuchAlgorithmException e) {
			throw new ClientSslSocketFactoryException(String.format("Failed to create the key store because the algorithm %s is not supported.",
					KeyManagerFactory.getDefaultAlgorithm()), e);
		}
		catch (UnrecoverableKeyException e) {
			throw new ClientSslSocketFactoryException("Unrecoverable Key Exception initializing key manager factory; this is probably fatal", e);
		}
		catch (KeyStoreException e) {
			throw new ClientSslSocketFactoryException("KeyStore exception initializing key manager factory;	this is probably fatal ", e);
		}

		KeyManager[] managers = factory.getKeyManagers();

		log.debug("Key managers are initialized. Total {} managers.", managers.length);

		return managers;
	}

	private TrustManager[] createTrustManagers() throws ClientSslSocketFactoryException {
		final TrustManagerFactory factory;

		try {
			factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init(this.trustStore);
		} catch (NoSuchAlgorithmException e) {
			throw new ClientSslSocketFactoryException(String.format("Failed to create the trust store because the algorithm %s is not supported. ",
					KeyManagerFactory.getDefaultAlgorithm()), e);
		} catch (KeyStoreException e) {
			throw new ClientSslSocketFactoryException("KeyStore exception initializing trust manager factory; this is probably fatal", e);
		}

		final TrustManager[] managers = factory.getTrustManagers();

		log.debug("Trust managers are initialized. Total {} managers.", managers.length);

		return managers;
	}
}
