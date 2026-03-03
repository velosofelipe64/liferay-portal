/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class ExperienceUsageStrategy implements UsageStrategy {

	public ExperienceUsageStrategy(
		ProductPurchase productPurchase, JSONObject usageJSONObject) {

		Map<String, String> productPurchaseProperties =
			productPurchase.getProperties();

		String machineType = StringUtil.toLowerCase(
			GetterUtil.getString(productPurchaseProperties.get("machineType")));

		Product product = productPurchase.getProduct();

		Map<String, String> productProperties = product.getProperties();

		_databaseCapacityMax = GetterUtil.getInteger(
			StringUtil.removeSubstring(
				productProperties.get(machineType + "-database"), UNIT_GIB));

		_extensionsCapacityCPUMax = GetterUtil.getInteger(
			productProperties.get(machineType + "-extensions-vcpus"));

		_extensionsCapacityRAMMax = GetterUtil.getInteger(
			productProperties.get(machineType + "-extensions-ram"));

		if (machineType.equals("high")) {
			_logCapacityUnit = UNIT_TIB;
		}

		_logCapacityMax = GetterUtil.getInteger(
			StringUtil.removeSubstring(
				productProperties.get(machineType + "-logs"),
				_logCapacityUnit));

		_networkingCapacityMax = GetterUtil.getInteger(
			StringUtil.removeSubstring(
				productProperties.get(machineType + "-traffic-networking"),
				UNIT_TIB));

		_storageCapacityMax = GetterUtil.getInteger(
			StringUtil.removeSubstring(
				productProperties.get(machineType + "-storage"), UNIT_TIB));

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
				usageJSONObject.optBigDecimal("logStorage", BigDecimal.ZERO),
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
			UsageStrategy.createUsageJSONObject(
				_extensionsCapacityCPUUsed, StringPool.BLANK,
				_extensionsCapacityCPUMax, StringPool.BLANK)
		).put(
			"clientExtensionsRAM",
			UsageStrategy.createUsageJSONObject(
				_extensionsCapacityRAMUsed,
				_usageUnits.get(_METRIC_CLIENT_EXTENSIONS_RAM),
				_extensionsCapacityRAMMax, UNIT_GIB)
		).put(
			"databaseStorage",
			UsageStrategy.createUsageJSONObject(
				_databaseCapacityUsed,
				_usageUnits.get(_METRIC_DATABASE_STORAGE), _databaseCapacityMax,
				UNIT_GIB)
		).put(
			"documentLibraryAndBackupStorage",
			UsageStrategy.createUsageJSONObject(
				_storageCapacityUsed, _usageUnits.get(_METRIC_STORAGE),
				_storageCapacityMax, UNIT_TIB)
		).put(
			"logStorage",
			UsageStrategy.createUsageJSONObject(
				_logCapacityUsed, _usageUnits.get(_METRIC_LOG_STORAGE),
				_logCapacityMax, _logCapacityUnit)
		).put(
			"networkTraffic",
			UsageStrategy.createUsageJSONObject(
				_networkingCapacityUsed,
				_usageUnits.get(_METRIC_NETWORK_TRAFFIC),
				_networkingCapacityMax, UNIT_TIB)
		);
	}

	private float _convert(BigDecimal bigDecimal, String metric) {
		if (bigDecimal != null) {
			String unit = UNIT_GIB;

			BigDecimal divisorGB = new BigDecimal(1024L * 1024L * 1024L);

			bigDecimal = bigDecimal.divide(divisorGB);

			if (bigDecimal.compareTo(new BigDecimal("1024")) >= 0) {
				bigDecimal = bigDecimal.divide(new BigDecimal(1024));
				unit = UNIT_TIB;
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
	private String _logCapacityUnit = UNIT_GIB;
	private final float _logCapacityUsed;
	private final int _networkingCapacityMax;
	private final float _networkingCapacityUsed;
	private final int _storageCapacityMax;
	private final float _storageCapacityUsed;
	private final Map<String, String> _usageUnits = new HashMap<>();

}