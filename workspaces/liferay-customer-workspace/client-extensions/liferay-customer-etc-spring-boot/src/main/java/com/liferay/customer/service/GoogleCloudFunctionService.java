/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.common.io.CharStreams;
import com.liferay.petra.string.StringBundler;

/**
 * @author Felipe Franca
 * @author Amos Fong
 */
@Component
public class GoogleCloudFunctionService {

	@Cacheable("accountUsage")
	public JSONObject fetchCustomerAccountUsage(String accountKey)
		throws Exception {

	JSONObject root = new JSONObject();

        // String values
        root.put("koroneikiAccountId", "KOR-12345");

        // Integer/Long values for counts
        root.put("totalMonthlyActiveLoggedInUsersCount", 12345L);
        root.put("totalAnonymousPageViewsCount", 12345L);
        root.put("totalSitesCount", 12345);

        // Double values for capacities and storage
        // Note: Using decimals to match your requested format
        root.put("totalClientExtensionsCapacityCPUCount", 12345.678910);
        root.put("totalClientExtensionsCapacityRAM", 12345.678910);
        root.put("totalStorageCapacityDocumentLibrary", 12345.678910);

        return root;
	}

	@Cacheable("accountUsage")
	public JSONObject fetchCustomerAccountUsage(String accountKey, String month)
		throws Exception {

		JSONObject root = new JSONObject();

			root.put("accountKey", "KOR-12345");
			root.put("month", "2025-01");
			
			// 2. Create the nested 'usage' object
			JSONObject usage = new JSONObject();
			
			// Note: Using 'L' for Long values to handle large byte counts
			usage.put("clientExtensionsCPU", 4.0);
			usage.put("clientExtensionsRAM", 537395200L);
			usage.put("databaseStorage", 53687091200L);
			usage.put("documentLibraryAndBackupStorage", 107374182400L);
			usage.put("logStorage", 11274289152L);
			usage.put("networkTraffic", 16642998272L);
			
			// 3. Assemble and add the timestamp
			root.put("usage", usage);
			root.put("updatedAt", "2025-01-31T23:59:59Z");

			return root;
	}

	@CacheEvict(allEntries = true, value = "accountUsage")
	@Scheduled(cron = "0 0 * * * *")
	public void scheduledCacheEviction() throws Exception {
	}

	private JSONObject _handleRequest(String url, String accountKey)
		throws Exception {

		try (InputStream inputStream = new ByteArrayInputStream(
				_gcfServiceAccountKey.getBytes())) {

			IdTokenCredentials idTokenCredential =
				IdTokenCredentials.newBuilder(
				).setIdTokenProvider(
					(IdTokenProvider)GoogleCredentials.fromStream(inputStream)
				).setTargetAudience(
					_gcfBaseUrl + _FUNCTION_CUSTOMER_USAGE_API_PATH
				).build();

			HttpRequest httpRequest = new NetHttpTransport(
			).createRequestFactory(
				new HttpCredentialsAdapter(idTokenCredential)
			).buildGetRequest(
				new GenericUrl(url)
			).setThrowExceptionOnExecuteError(
				false
			);

			HttpResponse httpResponse = null;

			try {
				httpResponse = httpRequest.execute();

				if (httpResponse.isSuccessStatusCode()) {
					String result = CharStreams.toString(
						new InputStreamReader(
							httpResponse.getContent(), StandardCharsets.UTF_8));

					return new JSONObject(result);
				}

				if (httpResponse.getStatusCode() ==
						HttpStatus.NOT_FOUND.value()) {

					return null;
				}

				throw new Exception(
					StringBundler.concat(
						httpResponse.getStatusCode(), " ",
						httpResponse.getStatusMessage(), " for account ",
						accountKey));
			}
			finally {
				if (httpResponse != null) {
					httpResponse.disconnect();
				}
			}
		}
	}

	private static final String _FUNCTION_CUSTOMER_USAGE_API_PATH =
		"/customer_usage_api";

	@Value("${liferay.customer.gcf.base.url}")
	private String _gcfBaseUrl;

	@Value("${liferay.customer.gcf.service.account.key}")
	private String _gcfServiceAccountKey;

}