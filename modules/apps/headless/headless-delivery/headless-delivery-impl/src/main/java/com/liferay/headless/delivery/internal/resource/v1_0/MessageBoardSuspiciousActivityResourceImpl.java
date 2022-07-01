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

package com.liferay.headless.delivery.internal.resource.v1_0;

import com.liferay.headless.delivery.dto.v1_0.Field;
import com.liferay.headless.delivery.dto.v1_0.MessageBoardSuspiciousActivity;
import com.liferay.headless.delivery.resource.v1_0.MessageBoardSuspiciousActivityResource;

import com.liferay.message.boards.model.MBSuspiciousActivity;
import com.liferay.message.boards.service.MBSuspiciousActivityLocalService;
import com.liferay.message.boards.service.MBSuspiciousActivityService;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.vulcan.aggregation.Aggregation;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.util.TransformUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;


import java.util.List;
import java.util.Map;

/**
 * @author Javier Gamarra
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/message-board-suspicious-activity.properties",
	scope = ServiceScope.PROTOTYPE,
	service = MessageBoardSuspiciousActivityResource.class
)
public class MessageBoardSuspiciousActivityResourceImpl
	extends BaseMessageBoardSuspiciousActivityResourceImpl {


	public Page<MessageBoardSuspiciousActivity> getMessageBoardThreadMessageBoardSuspiciousActivityPage(
		Map<String, Map<String, String>> actions,
		Long messageBoardThreadId, Long siteId, Boolean flatten,
		String keywords, Aggregation aggregation,Filter filter,
		Pagination pagination, Sort[] sorts)
		throws Exception{

			List<MBSuspiciousActivity> dados = _mbSuspiciousActivityService.getThreadSuspiciousActivities(messageBoardThreadId);

			return Page.of(transform(dados, null));
//			return Page.of(TransformUtil.transform(new Array[]{(Array) dados},null ,null));

	}

	public Page<MessageBoardSuspiciousActivity> getMessageBoardMessageMessageBoardSuspiciousActivityPage(
		Map<String, Map<String, String>> actions,
		Long messageBoardMessageId, Long siteId, Boolean flatten,
		String keywords, Aggregation aggregation,Filter filter,
		Pagination pagination, Sort[] sorts
	)
		throws Exception{
			List<MBSuspiciousActivity> dados = _mbSuspiciousActivityService.getMessageSuspiciousActivities(messageBoardMessageId);
			return Page.of(transform(dados, null));

	}


	@Reference
	private MBSuspiciousActivityService _mbSuspiciousActivityService;
}