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

package com.liferay.dynamic.data.mapping.internal.upgrade;

import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.Validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public abstract class BaseTemplateUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		_upgradeDDMTemplates();
		_upgradeFragmentEntries();
	}

	protected String getTemplateContextVariable() {
		return null;
	}

	protected abstract Pattern getTemplatePattern() throws Exception;

	protected abstract String getTemplatePatternReplacement() throws Exception;

	private void _upgradeDDMTemplates() throws Exception {
		try (PreparedStatement selectPreparedStatement =
				connection.prepareStatement(
					"select templateId, script from DDMTemplate");
			PreparedStatement updatePreparedStatement =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update DDMTemplate set script = ? where templateId = ?")) {

			try (ResultSet resultSet = selectPreparedStatement.executeQuery()) {
				while (resultSet.next()) {
					String script = resultSet.getString(2);

					Matcher templateMatcher = getTemplatePattern().matcher(
						script);

					while (templateMatcher.find()) {
						script = StringUtil.replace(
							script, templateMatcher.group(),
							getTemplatePatternReplacement());

						if (Validator.isNotNull(getTemplateContextVariable())) {
							script = StringUtil.replace(
								script, templateMatcher.group(1),
								getTemplateContextVariable());
						}

						Matcher isAssignEmptyMatcher =
							_isAssignEmptyDDMTemplatePattern.matcher(script);

						if (isAssignEmptyMatcher.find()) {
							script = isAssignEmptyMatcher.replaceAll(
								getTemplatePatternReplacement());
						}
					}

					long templateId = resultSet.getLong(1);

					updatePreparedStatement.setString(1, script);
					updatePreparedStatement.setLong(2, templateId);

					updatePreparedStatement.addBatch();
				}

				updatePreparedStatement.executeBatch();
			}
		}
	}

	private void _upgradeFragmentEntries() throws Exception {
		try (PreparedStatement selectPreparedStatement =
				connection.prepareStatement(
					"select fragmentEntryId, html from FragmentEntry");
			PreparedStatement updatePreparedStatement =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update FragmentEntry set html = ? where fragmentEntryId " +
						"= ?")) {

			try (ResultSet resultSet = selectPreparedStatement.executeQuery()) {
				while (resultSet.next()) {
					String html = resultSet.getString(2);

					Matcher templateMatcher = getTemplatePattern().matcher(
						html);

					while (templateMatcher.find()) {
						html = StringUtil.replace(
							html, templateMatcher.group(),
							getTemplatePatternReplacement());

						if (Validator.isNotNull(getTemplateContextVariable())) {
							html = StringUtil.replace(
								html, templateMatcher.group(1),
								getTemplateContextVariable());
						}

						Matcher isAssignEmptyMatcher =
							_isAssignEmptyFragmentEntryPattern.matcher(html);

						if (isAssignEmptyMatcher.find()) {
							html = isAssignEmptyMatcher.replaceAll(
								getTemplatePatternReplacement());
						}
					}

					long fragmentEntryId = resultSet.getLong(1);

					updatePreparedStatement.setString(1, html);
					updatePreparedStatement.setLong(2, fragmentEntryId);

					updatePreparedStatement.addBatch();
				}

				updatePreparedStatement.executeBatch();
			}
		}
	}

	private static final Pattern _isAssignEmptyDDMTemplatePattern =
		Pattern.compile("\\<\\#assign\\s*\\/?\\>");
	private static final Pattern _isAssignEmptyFragmentEntryPattern =
		Pattern.compile("\\[\\#assign\\s*\\/?\\]");

}