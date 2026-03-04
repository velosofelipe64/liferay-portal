/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.JSONObject;

import com.liferay.portal.kernel.util.Validator;

/**
 * @author Felipe Veloso
 */
interface UsageStrategy {

    public JSONObject toJSONObject();

    public static JSONObject createUsageJSONObject(
        BigDecimal usedCount, long maxCount,
        String maxCountUnit) {

        String usedCountUnit = UNIT_GIB;

        if(Validator.isNull(maxCountUnit)) {
            usedCountUnit = maxCountUnit;
        }

        if (usedCount.compareTo(new BigDecimal("1024")) >= 0) {
            usedCount = usedCount.divide(new BigDecimal(1024));
            usedCountUnit = UNIT_TIB;
        }

        float usageCountFloat = usedCount.setScale(
            2, RoundingMode.DOWN
        ).floatValue();

        float dividend = usageCountFloat;

        if (!usedCountUnit.equals(maxCountUnit)) {
            dividend = usageCountFloat / 1024;
        }

        float percentage = 0;

        if (maxCount > 0) {
            percentage = (dividend / maxCount) * 100;
        }

        JSONObject jsonObject = new JSONObject();

        return jsonObject.put(
            "maxCount", maxCount
        ).put(
            "maxCountUnits", maxCountUnit
        ).put(
            "percentage", percentage
        ).put(
            "usedCount", usageCountFloat
        ).put(
            "usedCountUnits", usedCountUnit
        );
    }

    public static final String UNIT_GIB = "GiB";
    public static final String UNIT_TIB = "TiB";

}
