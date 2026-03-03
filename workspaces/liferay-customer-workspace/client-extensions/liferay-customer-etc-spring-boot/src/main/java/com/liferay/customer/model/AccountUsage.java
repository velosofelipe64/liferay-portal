/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import com.liferay.customer.constants.ProductConstants;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Amos Fong
 */
public class AccountUsage {

	public AccountUsage(
		List<ProductPurchase> productPurchases, JSONObject usageJSONObject) {

		_usageStrategy = new SaaSUsageStrategy(
			productPurchases, usageJSONObject);
	}

	public AccountUsage(
		ProductPurchase productPurchase, JSONObject usageJSONObject) {

		_usageStrategy = new ExperienceUsageStrategy(
			productPurchase, usageJSONObject);
	}

	public JSONObject toJSONObject() {
		return _usageStrategy.toJSONObject();
	}

	private static JSONObject _createUsageJSONObject(
		float usedCount, String usedCountUnit, long maxCount,
		String maxCountUnit) {

		float dividend = usedCount;

		if (!usedCountUnit.equals(maxCountUnit)) {
			dividend = usedCount / 1024;
		}

		float percentage = (dividend / maxCount) * 100;

		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"maxCount", maxCount
		).put(
			"maxCountUnits", maxCountUnit
		).put(
			"percentage", percentage
		).put(
			"usedCount", usedCount
		).put(
			"usedCountUnits", usedCountUnit
		);

		return jsonObject;
	}

	private static final String _UNIT_GIB = "GiB";

	private static final String _UNIT_TIB = "TiB";

	private final UsageStrategy _usageStrategy;

	private static class ExperienceUsageStrategy implements UsageStrategy {

		public ExperienceUsageStrategy(
			ProductPurchase productPurchase, JSONObject usageJSONObject) {

			Map<String, String> productPurchaseProperties =
				productPurchase.getProperties();

			String machineType = StringUtil.toLowerCase(
				GetterUtil.getString(
					productPurchaseProperties.get("machineType")));

			Product product = productPurchase.getProduct();

			Map<String, String> productProperties = product.getProperties();

			_databaseCapacityMax = GetterUtil.getInteger(
				StringUtil.removeSubstring(
					productProperties.get(machineType + "-database"),
					_UNIT_GIB));

			_extensionsCapacityCPUMax = GetterUtil.getInteger(
				productProperties.get(machineType + "-extensions-vcpus"));

			_extensionsCapacityRAMMax = GetterUtil.getInteger(
				productProperties.get(machineType + "-extensions-ram"));

			_logCapacityMax = GetterUtil.getInteger(
				StringUtil.removeSubstring(
					productProperties.get(machineType + "-logs"), _UNIT_GIB));

			if (machineType.equals("high")) {
				_networkingCapacityUnit = _UNIT_TIB;
			}

			_networkingCapacityMax = GetterUtil.getInteger(
				StringUtil.removeSubstring(
					productProperties.get(machineType + "-traffic-networking"),
					_networkingCapacityUnit));

			_storageCapacityMax = GetterUtil.getInteger(
				StringUtil.removeSubstring(
					productProperties.get(machineType + "-storage"),
					_UNIT_TIB));

			if (usageJSONObject != null) {
				usageJSONObject = usageJSONObject.getJSONObject("usage");

				_databaseCapacityUsed = _convert(
					usageJSONObject.optBigDecimal(
						"databaseStorage", BigDecimal.ZERO),
					_METRIC_DATABASE_STORAGE);

				_extensionsCapacityCPUUsed = usageJSONObject.optInt(
					"clientExtensionsCPU", 0);

				_extensionsCapacityRAMUsed = _convert(
					usageJSONObject.optBigDecimal(
						"clientExtensionsRAM", BigDecimal.ZERO),
					_METRIC_CLIENT_EXTENSIONS_RAM);

				_logCapacityUsed = _convert(
					usageJSONObject.optBigDecimal(
						"logStorage", BigDecimal.ZERO),
					_METRIC_LOG_STORAGE);

				_networkingCapacityUsed = _convert(
					usageJSONObject.optBigDecimal(
						"networkTraffic", BigDecimal.ZERO),
					_METRIC_NETWORK_TRAFFIC);

				_storageCapacityUsed = _convert(
					usageJSONObject.optBigDecimal(
						"documentLibraryAndBackupStorage", BigDecimal.ZERO),
					_METRIC_STORAGE);
			}
			else {
				_databaseCapacityUsed = 0;
				_extensionsCapacityCPUUsed = 0;
				_extensionsCapacityRAMUsed = 0;
				_logCapacityUsed = 0;
				_networkingCapacityUsed = 0;
				_storageCapacityUsed = 0;
			}
		}

		@Override
		public JSONObject toJSONObject() {
			JSONObject jsonObject = new JSONObject();

			return jsonObject.put(
				"clientExtensionsCPU",
				_createUsageJSONObject(
					_extensionsCapacityCPUUsed, StringPool.BLANK,
					_extensionsCapacityCPUMax, StringPool.BLANK)
			).put(
				"clientExtensionsRAM",
				_createUsageJSONObject(
					_extensionsCapacityRAMUsed,
					_usageUnits.get(_METRIC_CLIENT_EXTENSIONS_RAM),
					_extensionsCapacityRAMMax, _UNIT_GIB)
			).put(
				"databaseStorage",
				_createUsageJSONObject(
					_databaseCapacityUsed,
					_usageUnits.get(_METRIC_DATABASE_STORAGE),
					_databaseCapacityMax, _UNIT_TIB)
			).put(
				"documentLibraryAndBackupStorage",
				_createUsageJSONObject(
					_storageCapacityUsed, _usageUnits.get(_METRIC_STORAGE),
					_storageCapacityMax, _UNIT_GIB)
			).put(
				"logStorage",
				_createUsageJSONObject(
					_logCapacityUsed, _usageUnits.get(_METRIC_LOG_STORAGE),
					_logCapacityMax, _UNIT_GIB)
			).put(
				"networkTraffic",
				_createUsageJSONObject(
					_networkingCapacityUsed,
					_usageUnits.get(_METRIC_NETWORK_TRAFFIC),
					_networkingCapacityMax, _networkingCapacityUnit)
			);
		}

		private float _convert(BigDecimal bigDecimal, String metric) {
			if (bigDecimal != null) {
				String unit = _UNIT_GIB;

				BigDecimal divisorGB = new BigDecimal(1024L * 1024L * 1024L);

				bigDecimal = bigDecimal.divide(divisorGB);

				if (bigDecimal.compareTo(new BigDecimal("1024")) >= 0) {
					bigDecimal = bigDecimal.divide(new BigDecimal(1024));
					unit = _UNIT_TIB;
				}

				_setUsageUnit(metric, unit);

				return bigDecimal.setScale(
					2, RoundingMode.DOWN
				).floatValue();
			}

			return BigDecimal.ZERO.floatValue();
		}

		private void _setUsageUnit(String metric, String unit) {
			_usageUnits.put(metric, unit);
		}

		private static final String _METRIC_CLIENT_EXTENSIONS_RAM =
			"clientExtensionsRAMMetric";

		private static final String _METRIC_DATABASE_STORAGE =
			"databaseStorageMetric";

		private static final String _METRIC_LOG_STORAGE = "logStorageMetric";

		private static final String _METRIC_NETWORK_TRAFFIC =
			"networkTrafficMetric";

		private static final String _METRIC_STORAGE =
			"documentLibraryAndBackupStorageMetric";

		private final int _databaseCapacityMax;
		private final float _databaseCapacityUsed;
		private final int _extensionsCapacityCPUMax;
		private final int _extensionsCapacityCPUUsed;
		private final int _extensionsCapacityRAMMax;
		private final float _extensionsCapacityRAMUsed;
		private final long _logCapacityMax;
		private final float _logCapacityUsed;
		private final int _networkingCapacityMax;
		private String _networkingCapacityUnit = _UNIT_GIB;
		private final float _networkingCapacityUsed;
		private final int _storageCapacityMax;
		private final float _storageCapacityUsed;
		private final Map<String, String> _usageUnits = new HashMap<>();

	}

	private static class SaaSUsageStrategy implements UsageStrategy {

		public SaaSUsageStrategy(
			List<ProductPurchase> productPurchases,
			JSONObject usageJSONObject) {

			int additionalCPUAndRAMMax = 0;
			int additionalStorageCapacityDocumentLibraryMax = 0;
			long anonymousPageViewsMax = 0;
			Product liferaySaasPlanProduct = null;
			long monthlyActiveLoggedInUsersMax = 0;

			for (ProductPurchase productPurchase : productPurchases) {
				Product product = productPurchase.getProduct();

				String name = product.getName();

				if (name.equals(
						ProductConstants.
							NAME_ADDITIONAL_EXTENSION_CAPACITY_1GB_1VCPU)) {

					if (productPurchase.getQuantity() >
							additionalCPUAndRAMMax) {

						additionalCPUAndRAMMax = productPurchase.getQuantity();
					}
				}
				else if (name.equals(
							ProductConstants.NAME_ADDITIONAL_STORAGE_100GB) ||
						 name.equals(
							 ProductConstants.
								 NAME_LIFERAY_SAAS_100GB_EXTRA_STORAGE_DOCUMENT_LIBRARY)) {

					if ((productPurchase.getQuantity() * 100) >
							additionalStorageCapacityDocumentLibraryMax) {

						additionalStorageCapacityDocumentLibraryMax =
							productPurchase.getQuantity() * 100;
					}
				}
				else if (name.equals(
							ProductConstants.NAME_LIFERAY_SAAS_BUSINESS_PLAN)) {

					if (liferaySaasPlanProduct == null) {
						liferaySaasPlanProduct = product;
					}
					else {
						String liferaySaasPlanName =
							liferaySaasPlanProduct.getName();

						if (liferaySaasPlanName.equals(
								ProductConstants.NAME_LIFERAY_SAAS_PRO_PLAN)) {

							liferaySaasPlanProduct = product;
						}
					}
				}
				else if (name.equals(
							ProductConstants.NAME_LIFERAY_SAAS_CUSTOM_APVS)) {

					if (productPurchase.getQuantity() > anonymousPageViewsMax) {
						anonymousPageViewsMax = productPurchase.getQuantity();
					}
				}
				else if (name.equals(
							ProductConstants.NAME_LIFERAY_SAAS_CUSTOM_MALUS)) {

					if (productPurchase.getQuantity() >
							monthlyActiveLoggedInUsersMax) {

						monthlyActiveLoggedInUsersMax =
							productPurchase.getQuantity();
					}
				}
				else if (name.equals(
							ProductConstants.
								NAME_LIFERAY_SAAS_ENTERPRISE_PLAN)) {

					liferaySaasPlanProduct = product;
				}
				else if (name.startsWith(
							ProductConstants.
								NAME_LIFERAY_SAAS_ENTITLEMENTS_PREFIX) &&
						 name.endsWith("APVs")) {

					long curAnonymousPageViewsMax = _getAnonymousPageViewsMax(
						name);

					if (curAnonymousPageViewsMax > anonymousPageViewsMax) {
						anonymousPageViewsMax = curAnonymousPageViewsMax;
					}
				}
				else if (name.startsWith(
							ProductConstants.
								NAME_LIFERAY_SAAS_ENTITLEMENTS_PREFIX) &&
						 name.endsWith("MALUs")) {

					long curMonthlyActiveLoggedInUsersMax =
						_getMonthlyActiveLoggedInUsersMax(name);

					if (curMonthlyActiveLoggedInUsersMax >
							monthlyActiveLoggedInUsersMax) {

						monthlyActiveLoggedInUsersMax =
							curMonthlyActiveLoggedInUsersMax;
					}
				}
				else if (name.equals(
							ProductConstants.NAME_LIFERAY_SAAS_PRO_PLAN)) {

					if (liferaySaasPlanProduct == null) {
						liferaySaasPlanProduct = product;
					}
				}
			}

			_initLiferaySaasPlan(liferaySaasPlanProduct);

			_anonymousPageViewsMax = anonymousPageViewsMax;
			_extensionsCapacityCPUMax += additionalCPUAndRAMMax;
			_extensionsCapacityRAMMax += additionalCPUAndRAMMax;
			_monthlyActiveLoggedInUsersMax = monthlyActiveLoggedInUsersMax;
			_storageCapacityMax += additionalStorageCapacityDocumentLibraryMax;

			if (usageJSONObject != null) {
				_anonymousPageViewsUsed = usageJSONObject.optLong(
					"totalAnonymousPageViewsCount");
				_extensionsCapacityCPUUsed = usageJSONObject.optBigDecimal(
					"totalClientExtensionsCapacityCPUCount", BigDecimal.ZERO);
				_extensionsCapacityRAMUsed = usageJSONObject.optBigDecimal(
					"totalClientExtensionsCapacityRAM", BigDecimal.ZERO);
				_monthlyActiveLoggedInUsersUsed = usageJSONObject.optLong(
					"totalMonthlyActiveLoggedInUsersCount");
				_sitesUsed = usageJSONObject.optInt("totalSitesCount");
				_storageCapacityUsed = usageJSONObject.optBigDecimal(
					"totalStorageCapacityDocumentLibrary", BigDecimal.ZERO);
			}
			else {
				_anonymousPageViewsUsed = 0;
				_extensionsCapacityCPUUsed = BigDecimal.ZERO;
				_extensionsCapacityRAMUsed = BigDecimal.ZERO;
				_monthlyActiveLoggedInUsersUsed = 0;
				_sitesUsed = 0;
				_storageCapacityUsed = BigDecimal.ZERO;
			}
		}

		@Override
		public JSONObject toJSONObject() {
			JSONObject jsonObject = new JSONObject();

			jsonObject.put(
				"anonymousPageViews",
				_createUsageJSONObject(
					_anonymousPageViewsUsed, _UNIT_GIB, _anonymousPageViewsMax,
					_UNIT_GIB)
			).put(
				"clientExtensionsCapacityCPU",
				_createUsageJSONObject(
					_format(_extensionsCapacityCPUUsed), _UNIT_GIB,
					_extensionsCapacityCPUMax, _UNIT_GIB)
			).put(
				"clientExtensionsCapacityRAM",
				_createUsageJSONObject(
					_format(_extensionsCapacityRAMUsed), _UNIT_GIB,
					_extensionsCapacityRAMMax, _UNIT_GIB)
			).put(
				"monthlyActiveLoggedInUsers",
				_createUsageJSONObject(
					_monthlyActiveLoggedInUsersUsed, _UNIT_GIB,
					_monthlyActiveLoggedInUsersMax, _UNIT_GIB)
			).put(
				"sites",
				_createUsageJSONObject(
					_sitesUsed, _UNIT_GIB, _sitesMax, _UNIT_GIB)
			).put(
				"storageCapacityDocumentLibrary",
				_createUsageJSONObject(
					_format(_storageCapacityUsed), _UNIT_GIB,
					_storageCapacityMax, _UNIT_GIB)
			);

			return jsonObject;
		}

		private float _format(BigDecimal bigDecimal) {
			if (bigDecimal != null) {
				return bigDecimal.setScale(
					2, RoundingMode.DOWN
				).floatValue();
			}

			return BigDecimal.ZERO.floatValue();
		}

		private long _getAnonymousPageViewsMax(String name) {
			String anonymousPageViewsMaxString = name.substring(
				ProductConstants.NAME_LIFERAY_SAAS_ENTITLEMENTS_PREFIX.
					length());

			anonymousPageViewsMaxString = StringUtil.removeSubstrings(
				anonymousPageViewsMaxString, StringPool.COMMA, " APVs");

			return GetterUtil.getLong(anonymousPageViewsMaxString);
		}

		private long _getMonthlyActiveLoggedInUsersMax(String name) {
			String monthlyActiveLoggedInUsersMaxString = name.substring(
				ProductConstants.NAME_LIFERAY_SAAS_ENTITLEMENTS_PREFIX.
					length());

			monthlyActiveLoggedInUsersMaxString = StringUtil.removeSubstrings(
				monthlyActiveLoggedInUsersMaxString, StringPool.COMMA,
				" MALUs");

			return GetterUtil.getLong(monthlyActiveLoggedInUsersMaxString);
		}

		private void _initLiferaySaasPlan(Product product) {
			if (product == null) {
				return;
			}

			Map<String, String> properties = product.getProperties();

			_extensionsCapacityCPUMax = GetterUtil.getInteger(
				properties.get("vcpu"));

			String clientExtensionsCapacityRAMMaxPropertyValue = properties.get(
				"ram");

			clientExtensionsCapacityRAMMaxPropertyValue =
				StringUtil.removeSubstring(
					clientExtensionsCapacityRAMMaxPropertyValue, " GB");

			_extensionsCapacityRAMMax = GetterUtil.getInteger(
				clientExtensionsCapacityRAMMaxPropertyValue);

			String sitesPropertyValue = properties.get("sites");

			if (sitesPropertyValue.equals("Unlimited")) {
				_sitesMax = -1;
			}
			else {
				_sitesMax = GetterUtil.getInteger(sitesPropertyValue);
			}

			String storageCapacityDocumentLibraryMaxPropertyValue =
				properties.get("document-library-size");

			storageCapacityDocumentLibraryMaxPropertyValue =
				StringUtil.removeSubstring(
					storageCapacityDocumentLibraryMaxPropertyValue, " GB");

			_storageCapacityMax = GetterUtil.getInteger(
				storageCapacityDocumentLibraryMaxPropertyValue);
		}

		private final long _anonymousPageViewsMax;
		private final long _anonymousPageViewsUsed;
		private int _extensionsCapacityCPUMax;
		private final BigDecimal _extensionsCapacityCPUUsed;
		private int _extensionsCapacityRAMMax;
		private final BigDecimal _extensionsCapacityRAMUsed;
		private final long _monthlyActiveLoggedInUsersMax;
		private final long _monthlyActiveLoggedInUsersUsed;
		private int _sitesMax;
		private final int _sitesUsed;
		private int _storageCapacityMax;
		private final BigDecimal _storageCapacityUsed;

	}

	private interface UsageStrategy {

		public JSONObject toJSONObject();

	}

}