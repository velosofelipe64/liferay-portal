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

package com.liferay.report.dto;

/**
 * @author Nilton Vieira
 */
public class Template {

	public Template(String name, String age, String date) {
		this.name = name;
		this.age = age;
		this.date = date;
	}

	public String getAge() {
		return age;
	}

	public String getDate() {
		return date;
	}

	public String getName() {
		return name;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String age;
	public String date;
	public String name;

}