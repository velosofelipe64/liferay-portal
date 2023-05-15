package com.liferay.report.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Catalog;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Page;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Pagination;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.CatalogResource;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;

import fr.opensagres.xdocreport.document.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;;
import org.springframework.stereotype.Service;


@Service
public class ProductServiceImpl implements ProductService {

	public Page<Product> findAllPaged() {
		try {

			URL sourceURL = new URL("http://" + _sourceAddress);

			String sourceBearerToken = ProductServiceImpl._getOAuthAuthorization(
					sourceURL, _sourceClientId, _sourceClientSecret);

			return ProductServiceImpl._getProductsByOAuth2(
					sourceBearerToken, _sourceAddress, _protocol);
		}
		catch (Exception e) {
			System.out.println(e.getMessage() + " - " + e.getCause());

			throw new RuntimeException(e);
		}
	}

	@Override
	public void insert() throws Exception {

		URL sourceURL = new URL("http://" + _sourceAddress);
		URL targetURL = new URL("http://" + _targetAddress);

		String sourceBearerToken = ProductServiceImpl._getOAuthAuthorization(
				sourceURL, _sourceClientId, _sourceClientSecret);
		String targetBearerToken = ProductServiceImpl._getOAuthAuthorization(
				targetURL, _targetClientId, _targetClientSecret);

		Page<Product> sourceProducts = ProductServiceImpl._getProductsByOAuth2(
				sourceBearerToken, _sourceAddress, _protocol);

		//        Page<Catalog> sourceCatalogs = Main._getCatalogsByOAuth2(sourceBearerToken, sourceAddress, protocol);

		Page<Catalog> targetCatalogs = ProductServiceImpl._getCatalogsByOAuth2(
				targetBearerToken, _targetAddress, _protocol);

		_insertProductsDestination(
				sourceProducts, targetCatalogs, _targetAddress, _protocol,
				targetBearerToken);

	}

	private static Page<Catalog> _getCatalogsByOAuth2(
			String bearerToken, String address, String protocol)
			throws Exception {

		CatalogResource catalogResource = CatalogResource.builder(
		).bearerToken(
				bearerToken
		).endpoint(
				address, protocol
		).build();

		return catalogResource.getCatalogsPage(
				null, null, Pagination.of(1, 10), null);
	}

	private static String _getOAuthAuthorization(
			URL url, String clientId, String clientSecret)
			throws Exception {

		HttpPost httpPost = new HttpPost(url + "/o/oauth2/token");

		httpPost.setEntity(
				new UrlEncodedFormEntity(
						Arrays.asList(
								new BasicNameValuePair("client_id", clientId),
								new BasicNameValuePair("client_secret", clientSecret),
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

	private static Page<Product> _getProductsByOAuth2(
			String bearerToken, String address, String protocol)
			throws Exception {

		ProductResource productResource = ProductResource.builder(
		).bearerToken(
				bearerToken
		).endpoint(
				address, protocol
		).build();

		return productResource.getProductsPage(
				null, null, Pagination.of(1, 10), null);
	}

	private static void _insertProductsDestination(
			Page<Product> sourceProducts, Page<Catalog> targetCatalogs,
			String address, String protocol, String targetBearerToken)
			throws Exception {

		try {
			ProductResource destinationProductResource =
					ProductResource.builder(
					).bearerToken(
							targetBearerToken
					).endpoint(
							address, protocol
					).build();

			for (Catalog targetCatalog : targetCatalogs.getItems()) {
				for (Product product : sourceProducts.getItems()) {
					if (product.getCatalogId(
					).equals(
							targetCatalog.getId()
					)) {

						product.setCatalogId(targetCatalog.getId());
						destinationProductResource.postProduct(product);
					}
				}
			}
		}
		catch (Exception exception) {
			throw new Exception("Liferay Generic Error");
		}
	}

	private static final String _protocol = "http";
	private static final String _sourceAddress = "localhost:8080";
	private static final String _targetAddress = "localhost:8081";
	private static final String _sourceClientId = "id-7fd92267-5d62-b6b3-a568-c7ed8943eb";
	private static final String _sourceClientSecret = "secret-e02e3435-2dc9-d763-be6e-7f5f7a47bc38";
	private static final String _targetClientId = "id-8517d564-8041-f383-4c0d-f32f51e351";
	private static final String _targetClientSecret = "secret-d44273af-d1b4-8c64-dfa9-d959feaa60";


}