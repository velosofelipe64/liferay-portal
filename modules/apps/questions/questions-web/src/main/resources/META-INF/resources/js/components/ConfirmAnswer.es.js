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

import React from 'react';
import {withRouter} from 'react-router-dom';

import Modal from './Modal.es';

export default withRouter(
	({confirmAnswerModalVisibility, setConfirmAnswerModalVisibility}) => {
		return (
			<Modal
				body={
					<div className="d-flex flex-column justify-content-center">
						<div>
							{Liferay.Language.get(
								'are-you-sure-you-want-to-add-another-answer?'
							)}

							<br />

							{Liferay.Language.get(
								'you-could-use-the-add-comment-to-continue-the-existing-thread-instead.'
							)}
						</div>
					</div>
				}
				last={false}
				onClose={() => {
					setConfirmAnswerModalVisibility(false);
				}}
				status="warning"
				title={Liferay.Language.get('confirm-answer')}
				visible={confirmAnswerModalVisibility}
			/>
		);
	}
);
