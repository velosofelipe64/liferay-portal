/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.customer.constants.ProductPurchaseConstants;
import com.liferay.customer.model.AccountSubscriptionGroup;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Account;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class AccountSubscriptionGroupService extends BaseService {

	public void sync(Account koroneikiAccount) throws Exception {
		String accountKey = koroneikiAccount.getKey();

		if (_log.isInfoEnabled()) {
			_log.info("Synchronizing subscriptions for account: " + accountKey);
		}

		Set<String> currentGroupERCs = _getERCsByAccountKey(accountKey);

		Map<String, List<ProductPurchase>> productPurchaseGroupsMap =
			new HashMap<>();

		for (ProductPurchase productPurchase :
				koroneikiAccount.getProductPurchases()) {

			String status = productPurchase.getStatusAsString();

			if (Validator.isNull(status) ||
				status.equals(ProductPurchaseConstants.STATUS_CANCELLED)) {

				continue;
			}

			Product product = productPurchase.getProduct();

			if ((product == null) || (product.getProperties() == null)) {
				continue;
			}

			String displayGroupName = product.getProperties(
			).get(
				"display-group-name"
			);

			if (Validator.isNull(displayGroupName) ||
				displayGroupName.isEmpty()) {

				continue;
			}

			productPurchaseGroupsMap.computeIfAbsent(
				displayGroupName, k -> new ArrayList<>()
			).add(
				productPurchase
			);
		}

		JSONArray postJSONArray = new JSONArray();
		List<JSONObject> patchList = new ArrayList<>();
		JSONArray subscriptionsUpsertJSONArray = new JSONArray();

		Set<String> processedGroupERCs = new HashSet<>();
		Set<String> processedSubscriptionERCs = new HashSet<>();

		for (Map.Entry<String, List<ProductPurchase>> entry :
				productPurchaseGroupsMap.entrySet()) {

			AccountSubscriptionGroup accountSubscriptionGroup =
				new AccountSubscriptionGroup(
					entry.getKey(), entry.getValue(), accountKey,
					koroneikiAccount.getExternalLinks(),
					_liferayPaasManageContactURL);

			JSONObject accountSubscriptionGroupJSONObject =
				accountSubscriptionGroup.toJSONObject();

			String externalReferenceCode =
				accountSubscriptionGroupJSONObject.getString(
					"externalReferenceCode");

			processedGroupERCs.add(externalReferenceCode);

			JSONArray accountSubscriptionsJSONArray =
				accountSubscriptionGroup.getAccountSubscriptionsJSONArray();

			for (int j = 0; j < accountSubscriptionsJSONArray.length(); j++) {
				JSONObject accountSubscriptionJSONObject =
					accountSubscriptionsJSONArray.getJSONObject(j);

				processedSubscriptionERCs.add(
					accountSubscriptionJSONObject.getString(
						"externalReferenceCode"));

				subscriptionsUpsertJSONArray.put(
					accountSubscriptionJSONObject);
			}

			if (currentGroupERCs.contains(externalReferenceCode)) {
				patchList.add(accountSubscriptionGroupJSONObject);
			}
			else {
				postJSONArray.put(accountSubscriptionGroupJSONObject);
			}
		}

		_post(postJSONArray);

		for (JSONObject payload : patchList) {
			_patch(payload.getString("externalReferenceCode"), payload);
		}

		_accountSubscriptionService.upsert(subscriptionsUpsertJSONArray);

		Set<String> currentSubscriptionERCs =
			_accountSubscriptionService.getERCsByAccountKey(accountKey);

		currentSubscriptionERCs.removeAll(processedSubscriptionERCs);

		_accountSubscriptionService.delete(currentSubscriptionERCs);

		currentGroupERCs.removeAll(processedGroupERCs);

		_delete(currentGroupERCs);

		if (_log.isInfoEnabled()) {
			_log.info(
				"Successfully synced subscriptions for account: " + accountKey);
		}
	}

	private void _delete(Set<String> externalReferenceCodes) {
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
				"Failed to delete account subscription groups", exception);
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private Set<String> _getERCsByAccountKey(String accountKey)
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

	private void _patch(String externalReferenceCode, JSONObject payload) {
		try {
			patch(
				_getAuthorization(), payload.toString(),
				UriComponentsBuilder.fromPath(
					_PATH
				).pathSegment(
					"by-external-reference-code", externalReferenceCode
				).build(
				).toUri());
		}
		catch (Exception exception) {
			_log.error(
				"Failed to patch account subscription group: " +
					externalReferenceCode,
				exception);
		}
	}

	private void _post(JSONArray payload) {
		if (payload.isEmpty()) {
			return;
		}

		post(
			_getAuthorization(), payload.toString(),
			UriComponentsBuilder.fromUriString(
				_PATH + "/batch"
			).build(
			).toUri());
	}

	private static final Log _log = LogFactory.getLog(
		AccountSubscriptionGroupService.class);

	private static final String _PATH = "/o/c/accountsubscriptiongroups";

	@Autowired
	private AccountSubscriptionService _accountSubscriptionService;

	@Value("${liferay.customer.liferay.paas.manage.contact.url}")
	private String _liferayPaasManageContactURL;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}
