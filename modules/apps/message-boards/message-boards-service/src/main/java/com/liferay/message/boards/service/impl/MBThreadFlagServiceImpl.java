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

package com.liferay.message.boards.service.impl;

import com.liferay.message.boards.model.MBThread;
import com.liferay.message.boards.model.MBThreadFlag;
import com.liferay.message.boards.service.base.MBThreadFlagServiceBaseImpl;
import com.liferay.portal.aop.AopService;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import org.osgi.service.component.annotations.Component;

/**
 * @author Brian Wing Shun Chan
 */
@Component(
	property = {
		"json.web.service.context.name=mb",
		"json.web.service.context.path=MBThreadFlag"
	},
	service = AopService.class
)
public class MBThreadFlagServiceImpl extends MBThreadFlagServiceBaseImpl {


	@Override
	public MBThreadFlag addThreadFlag(
		long classPK, long reportingUser, String title ,String reason, long creatorUserId, ServiceContext serviceContext)
		throws PortalException {

		return mbThreadFlagLocalService.addThreadFlag(
			getUserId(), classPK, reportingUser, title, reason, creatorUserId, serviceContext);
	}



}