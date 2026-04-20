/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class AccountSubscriptionService extends BaseService {

	public void delete(Set<String> externalReferenceCodes) {
		if ((externalReferenceCodes == null) ||
			externalReferenceCodes.isEmpty()) {

			return;
		}

		JSONArray payloadJSONArray = new JSONArray();

		for (String externalReferenceCode : externalReferenceCodes) {
			payloadJSONArray.put(
				new JSONObject(
				).put(
					"externalReferenceCode", externalReferenceCode
				));
		}

		try {
			delete(
				_getAuthorization(), payloadJSONArray.toString(),
				UriComponentsBuilder.fromPath(
					_PATH + "/batch"
				).build(
				).toUri());
		}
		catch (Exception exception) {
			_log.error(
				"Failed to delete account subscriptions", exception);
		}
	}

	public Set<String> getERCsByAccountKey(String accountKey)
		throws Exception {

		Set<String> externalReferenceCodes = new HashSet<>();

		int page = 1;

		while (true) {
			JSONObject jsonObject = new JSONObject(
				get(
					_getAuthorization(),
					UriComponentsBuilder.fromPath(
						_PATH
					).queryParam(
						"filter", "accountKey eq '" + accountKey + "'"
					).queryParam(
						"page", page
					).queryParam(
						"pageSize", 500
					).build(
					).toUri()));

			JSONArray jsonArray = jsonObject.optJSONArray("items");

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject entryJSONObject = jsonArray.getJSONObject(i);

				externalReferenceCodes.add(
					entryJSONObject.getString("externalReferenceCode"));
			}

			if (jsonObject.getInt("lastPage") <= page) {
				break;
			}

			page++;
		}

		return externalReferenceCodes;
	}

	public void upsert(JSONArray payload) {
		if (payload.isEmpty()) {
			return;
		}

		post(
			_getAuthorization(), payload.toString(),
			UriComponentsBuilder.fromUriString(
				_PATH + "/batch?createStrategy=UPSERT"
			).build(
			).toUri());
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(
		AccountSubscriptionService.class);

	private static final String _PATH = "/o/c/accountsubscriptions";

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}
