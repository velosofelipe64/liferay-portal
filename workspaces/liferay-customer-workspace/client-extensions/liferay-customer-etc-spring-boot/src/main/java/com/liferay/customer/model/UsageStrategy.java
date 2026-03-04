/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
interface UsageStrategy {

    public JSONObject toJSONObject();

    public static JSONObject createUsageJSONObject(
        float usedCount, String usedCountUnit, long maxCount,
        String maxCountUnit) {

        float dividend = usedCount;

        if (!usedCountUnit.equals(maxCountUnit)) {
            dividend = usedCount / 1024;
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
            "usedCount", usedCount
        ).put(
            "usedCountUnits", usedCountUnit
        );
    }

    public static final String UNIT_GIB = "GiB";
    public static final String UNIT_TIB = "TiB";

}
