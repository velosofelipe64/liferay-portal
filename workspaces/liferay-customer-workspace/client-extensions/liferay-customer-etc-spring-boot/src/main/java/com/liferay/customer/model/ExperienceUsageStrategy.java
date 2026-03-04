/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.json.JSONObject;

import static com.liferay.customer.model.UsageStrategy.createUsageJSONObject;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

/**
 * @author Felipe Veloso
 */
public class ExperienceUsageStrategy implements UsageStrategy {

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
                UNIT_GIB));

        _extensionsCapacityCPUMax = GetterUtil.getInteger(
            productProperties.get(machineType + "-extensions-vcpus"));

        _extensionsCapacityRAMMax = GetterUtil.getInteger(
            productProperties.get(machineType + "-extensions-ram"));

        _logCapacityMax = GetterUtil.getInteger(
            StringUtil.removeSubstring(
                productProperties.get(machineType + "-logs"), UNIT_GIB));

        if (machineType.equals("high")) {
            _networkingCapacityUnit = UNIT_TIB;
        }

        _networkingCapacityMax = GetterUtil.getInteger(
            StringUtil.removeSubstring(
                productProperties.get(machineType + "-traffic-networking"),
                _networkingCapacityUnit));

        _storageCapacityMax = GetterUtil.getInteger(
            StringUtil.removeSubstring(
                productProperties.get(machineType + "-storage"),
                UNIT_TIB));

        if (usageJSONObject != null) {
            usageJSONObject = usageJSONObject.getJSONObject("usage");

            _databaseCapacityUsed = 
                usageJSONObject.optBigDecimal(
                    "databaseStorage", BigDecimal.ZERO);

            _extensionsCapacityCPUUsed = usageJSONObject.optBigDecimal(
                "clientExtensionsCPU", BigDecimal.ZERO);

            _extensionsCapacityRAMUsed =
                usageJSONObject.optBigDecimal(
                    "clientExtensionsRAM", BigDecimal.ZERO);

            _logCapacityUsed = 
                usageJSONObject.optBigDecimal(
                    "logStorage", BigDecimal.ZERO);

            _networkingCapacityUsed = 
                usageJSONObject.optBigDecimal(
                    "networkTraffic", BigDecimal.ZERO);

            _storageCapacityUsed =
                usageJSONObject.optBigDecimal(
                    "documentLibraryAndBackupStorage", BigDecimal.ZERO);
        }
        else {
            _databaseCapacityUsed = BigDecimal.ZERO;
            _extensionsCapacityCPUUsed = BigDecimal.ZERO;
            _extensionsCapacityRAMUsed = BigDecimal.ZERO;
            _logCapacityUsed = BigDecimal.ZERO;
            _networkingCapacityUsed = BigDecimal.ZERO;
            _storageCapacityUsed = BigDecimal.ZERO;
        }
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        return jsonObject.put(
            "clientExtensionsCPU",
            createUsageJSONObject(
                _extensionsCapacityCPUUsed,
                _extensionsCapacityCPUMax, StringPool.BLANK)
        ).put(
            "clientExtensionsRAM",
            createUsageJSONObject(
                _convert(_extensionsCapacityRAMUsed),
                _extensionsCapacityRAMMax, UNIT_GIB)
        ).put(
            "databaseStorage",
            createUsageJSONObject(
                _convert(_databaseCapacityUsed),
                _databaseCapacityMax, UNIT_TIB)
        ).put(
            "documentLibraryAndBackupStorage",
            createUsageJSONObject(
                _convert(_storageCapacityUsed),
                _storageCapacityMax, UNIT_GIB)
        ).put(
            "logStorage",
            createUsageJSONObject(
                _convert(_logCapacityUsed),
                _logCapacityMax, UNIT_GIB)
        ).put(
            "networkTraffic",
            createUsageJSONObject(
                _convert(_networkingCapacityUsed),
                _networkingCapacityMax, _networkingCapacityUnit)
        );
    }

    private BigDecimal _convert(BigDecimal bigDecimal) {

        BigDecimal divisorGB = new BigDecimal(1024L * 1024L * 1024L);

        bigDecimal.divide(divisorGB);

        return bigDecimal.setScale(
					2, RoundingMode.DOWN
				);

    }

    private final int _databaseCapacityMax;
    private final BigDecimal _databaseCapacityUsed;
    private final int _extensionsCapacityCPUMax;
    private final BigDecimal _extensionsCapacityCPUUsed;
    private final int _extensionsCapacityRAMMax;
    private final BigDecimal _extensionsCapacityRAMUsed;
    private final long _logCapacityMax;
    private final BigDecimal _logCapacityUsed;
    private final int _networkingCapacityMax;
    private String _networkingCapacityUnit = UNIT_GIB;
    private final BigDecimal _networkingCapacityUsed;
    private final int _storageCapacityMax;
    private final BigDecimal _storageCapacityUsed;

}
