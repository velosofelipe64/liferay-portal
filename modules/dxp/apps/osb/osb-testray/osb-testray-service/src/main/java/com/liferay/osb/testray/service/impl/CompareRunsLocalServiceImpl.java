/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.osb.testray.service.impl;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.petra.sql.dsl.DynamicObjectDefinitionTable;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalServiceUtil;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.osb.testray.service.base.CompareRunsLocalServiceBaseImpl;
import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.Table;
import com.liferay.petra.sql.dsl.expression.Predicate;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.petra.sql.dsl.query.JoinStep;
import com.liferay.portal.aop.AopService;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

/**
 * @author José Abelenda
 */
@Component(
	property = "model.class.name=com.liferay.osb.testray.model.CompareRuns",
	service = AopService.class
)
public class CompareRunsLocalServiceImpl
	extends CompareRunsLocalServiceBaseImpl {


	public int getComparison(long runIdA, long runIdB, String statusA, String statusB, Long companyId) {
//		Case Table
		ObjectDefinition caseObjectDefinition = _getObjectDefinitionByTableName(_CASE, companyId).get(0);
		DynamicObjectDefinitionTable caseDynamicObjectDefinitionTable = _getDynamicObjectDefinitionTable(caseObjectDefinition);
// 		CaseResult Table
		ObjectDefinition caseResultObjectDefinition = _getObjectDefinitionByTableName(_CASE_RESULT, companyId).get(0);
		DynamicObjectDefinitionTable caseResultDynamicObjectDefinitionTable = _getDynamicObjectDefinitionTable(caseResultObjectDefinition);
//		Case_x Table
		ObjectDefinition caseXObjectDefinition = _getObjectDefinitionByTableName(_CASE_X, companyId).get(0);
		DynamicObjectDefinitionTable caseXDynamicObjectDefinitionTable = _getDynamicObjectDefinitionTable(caseXObjectDefinition);
//		CaseResult_x Table
		ObjectDefinition caseResultXObjectDefinition = _getObjectDefinitionByTableName(_CASE_RESULT_X, companyId).get(0);
		DynamicObjectDefinitionTable caseResultXDynamicObjectDefinitionTable = _getDynamicObjectDefinitionTable(caseResultXObjectDefinition);

		Column<DynamicObjectDefinitionTable, Long> runToCaseResultColumn =
			(Column<DynamicObjectDefinitionTable, Long>) caseResultXDynamicObjectDefinitionTable.getColumn("r_runToCaseResult_c_runId");

		Column<DynamicObjectDefinitionTable, String> dueStatucColumn =
			(Column<DynamicObjectDefinitionTable, String>) caseResultDynamicObjectDefinitionTable.getColumn("dueStatus_");

		Predicate predicate1 = runToCaseResultColumn.eq(runIdA).and(dueStatucColumn.eq(statusA));

		Predicate predicate2 = runToCaseResultColumn.eq(runIdB).and(dueStatucColumn.eq(statusB));

		JoinStep genericTable = DSLQueryFactoryUtil.selectDistinct(
			caseXDynamicObjectDefinitionTable
		).from(caseResultXDynamicObjectDefinitionTable).
			innerJoinON(caseXDynamicObjectDefinitionTable,
				caseXDynamicObjectDefinitionTable.getPrimaryKeyColumn().
					eq(caseResultXDynamicObjectDefinitionTable.getPrimaryKeyColumn())
			).innerJoinON(caseResultDynamicObjectDefinitionTable,
				caseResultDynamicObjectDefinitionTable.getPrimaryKeyColumn().
					eq(caseResultXDynamicObjectDefinitionTable.getPrimaryKeyColumn())
			);

		Table table1 = (Table) genericTable.where(predicate1);

		DSLQuery table2 = genericTable.where(predicate2);

		return ObjectEntryLocalServiceUtil.dslQueryCount(
			DSLQueryFactoryUtil.count().from(
			table1
		).where(
			table1.getColumn("c_caseId_").in(table2)
		));
	}

	private List<ObjectDefinition> _getObjectDefinitionByTableName(String tableName, Long companyId) {

		DynamicQuery dynamicQuery = _objectDefinitionLocalService.dynamicQuery();

		Criterion criterion = RestrictionsFactoryUtil.eq("dbTableName", tableName);

		RestrictionsFactoryUtil.and(criterion, RestrictionsFactoryUtil.eq("companyId", companyId));

		dynamicQuery.add(criterion);

		return _objectDefinitionLocalService.dynamicQuery(dynamicQuery);

	}

	private DynamicObjectDefinitionTable _getDynamicObjectDefinitionTable(ObjectDefinition objectDefinition) {
		return new DynamicObjectDefinitionTable(
			objectDefinition,
			_objectFieldLocalService.getObjectFields(
				objectDefinition.getObjectDefinitionId(), objectDefinition.getDBTableName()),
			objectDefinition.getDBTableName()
		);
	}

	private static String _CASE = "O_20096_Case";
	private static String _CASE_X = "O_20096_Case_x";
	private static String _CASE_RESULT_X= "O_20096_CaseResult_x";
	private static String _CASE_RESULT = "O_20096_CaseResult";
	
	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;
	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;


}