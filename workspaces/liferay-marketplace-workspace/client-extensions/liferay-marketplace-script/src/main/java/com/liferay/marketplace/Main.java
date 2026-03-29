/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.marketplace;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Page;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Pagination;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;

import java.io.InputStream;

import java.net.URL;

import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.json.JSONObject;

/**
 * @author Thiago Oliveira
 */
public class Main {

	public static void main(String[] args) throws Exception {
		try {
			InputStream inputStream = Main.class.getResourceAsStream(
				"dependencies/application.properties");
			Properties appProps = new Properties();

			appProps.load(inputStream);

			Main main = new Main(
				appProps.getProperty("LIFERAY_MARKETPLACE_OAUTH_CLIENT_ID"),
				appProps.getProperty("LIFERAY_MARKETPLACE_OAUTH_CLIENT_SECRET"),
				new URL(
					appProps.getProperty("LIFERAY_MARKETPLACE_LIFERAY_URL")));
		}
		catch (Exception exception) {
			_log.error("Error: " + exception.getMessage());
		}
	}

	public Main(
			String liferayMarketplaceOAuthClientId,
			String liferayMarketplaceOAuthClientSecret, URL liferayURL)
		throws Exception {

		_liferayURL = liferayURL;

		_liferayOAuthClientId = liferayMarketplaceOAuthClientId;
		_liferayOAuthClientSecret = liferayMarketplaceOAuthClientSecret;

		System.out.println("Liferay URL: " + _liferayURL);

		_initResourceBuilders(_getOAuthAuthorization());

		Page<Product> productsPage = _productResource.getProductsPage(
			null, null, Pagination.of(1, 10), null);

		_productsPage = productsPage;
	}

	private String _getOAuthAuthorization() throws Exception {
		HttpPost httpPost = new HttpPost(_liferayURL + "/o/oauth2/token");

		httpPost.setEntity(
			new UrlEncodedFormEntity(
				Arrays.asList(
					new BasicNameValuePair("client_id", _liferayOAuthClientId),
					new BasicNameValuePair(
						"client_secret", _liferayOAuthClientSecret),
					new BasicNameValuePair(
						"grant_type", "client_credentials"))));
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

		try (CloseableHttpClient closeableHttpClient =
				httpClientBuilder.build()) {

			CloseableHttpResponse closeableHttpResponse =
				closeableHttpClient.execute(httpPost);

			StatusLine statusLine = closeableHttpResponse.getStatusLine();

			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				JSONObject jsonObject = new JSONObject(
					EntityUtils.toString(
						closeableHttpResponse.getEntity(),
						Charset.defaultCharset()));

				return jsonObject.getString("access_token");
			}

			throw new Exception("Unable to get OAuth authorization");
		}
	}

	private void _initResourceBuilders(String authorization) throws Exception {
		ProductResource.Builder productResourceBuilder =
			ProductResource.builder();

		_productResource = productResourceBuilder.bearerToken(
			authorization
		).endpoint(
			_liferayURL.getHost(), _liferayURL.getPort(),
			_liferayURL.getProtocol()
		).build();
	}

	private static final Log _log = LogFactory.getLog(Main.class);

	private final String _liferayOAuthClientId;
	private final String _liferayOAuthClientSecret;
	private final URL _liferayURL;
	private ProductResource _productResource;
	private final Page<Product> _productsPage;

}