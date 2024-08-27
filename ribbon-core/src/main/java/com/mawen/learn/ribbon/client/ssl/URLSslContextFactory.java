package com.mawen.learn.ribbon.client.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Slf4j
public class URLSslContextFactory extends AbstractSslContextFactory {

	private final URL keyStoreUrl;
	private final URL trustStoreUrl;

	public URLSslContextFactory(final URL trustStoreUrl, final String trustStorePassword, final URL keyStoreUrl, final String keyStorePassword) throws ClientSslSocketFactoryException {
		super(createKeyStore(trustStoreUrl, trustStorePassword), trustStorePassword, createKeyStore(keyStoreUrl, keyStorePassword), keyStorePassword);

		this.trustStoreUrl = trustStoreUrl;
		this.keyStoreUrl = keyStoreUrl;

		log.info("Loaded keyStore from: {}", keyStoreUrl);
		log.info("Loaded trustStore from: {}", trustStoreUrl);
	}

	private static KeyStore createKeyStore(final URL storeFile, final String password) throws ClientSslSocketFactoryException {

		if(storeFile == null){
			return null;
		}

		Preconditions.checkArgument(StringUtils.isNotEmpty(password), "Null keystore should have empty password, defined keystore must have password");

		KeyStore keyStore = null;

		try{
			keyStore = KeyStore.getInstance("jks");

			InputStream is = storeFile.openStream();

			try {
				keyStore.load(is, password.toCharArray());
			} catch (NoSuchAlgorithmException e) {
				throw new ClientSslSocketFactoryException(String.format("Failed to create a keystore that supports algorithm %s: %s", SOCKET_ALGORITHM, e.getMessage()), e);
			} catch (CertificateException e) {
				throw new ClientSslSocketFactoryException(String.format("Failed to create keystore with algorithm %s due to certificate exception: %s", SOCKET_ALGORITHM, e.getMessage()), e);
			} finally {
				try {
					is.close();
				} catch (IOException ignore) { // NOPMD
				}
			}
		}catch(KeyStoreException e){
			throw new ClientSslSocketFactoryException(String.format("KeyStore exception creating keystore: %s", e.getMessage()), e);
		} catch (IOException e) {
			throw new ClientSslSocketFactoryException(String.format("IO exception creating keystore: %s", e.getMessage()), e);
		}

		return keyStore;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();

		builder.append("ClientSslSocketFactory [trustStoreUrl=").append(trustStoreUrl);
		if (trustStoreUrl != null) {
			builder.append(", trustStorePassword=");
			builder.append(Strings.repeat("*", this.getTrustStorePasswordLength()));
		}
		builder.append(", keyStoreUrl=").append(keyStoreUrl);
		if (keyStoreUrl != null) {
			builder.append(", keystorePassword = ");
			builder.append(Strings.repeat("*", this.getKeyStorePasswordLength()));
		}
		builder.append(']');

		return builder.toString();
	}
}
