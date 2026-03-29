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

package com.liferay.report;

import com.liferay.report.dto.Template;

import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nilton Vieira
 */
@RestController
public class CE01RestController {

	@PostMapping("/generatepdf")
	public ResponseEntity<?> generatePDF(@RequestBody Template template)
		throws Exception {

		System.out.println(template.getName());

		File tempOdtFinal = File.createTempFile("reportODTFinal", ".odt");

		generateReport(template, tempOdtFinal);

		String tmpdir = System.getProperty("java.io.tmpdir");

		ProcessBuilder processBuilder = new ProcessBuilder(
			_libreOfficePath, "--headless", "--convert-to",
			"pdf:writer_pdf_Export", "--outdir", tmpdir,
			tempOdtFinal.getAbsolutePath());

		Process process = processBuilder.start();

		process.waitFor();

		String fileName = tempOdtFinal.getName();

		File pdfFile = new File(tmpdir, fileName.split(".odt")[0] + ".pdf");

		FileInputStream fileInputStream = null;
		InputStreamResource inputStreamResource = null;
		HttpHeaders httpHeaders = new HttpHeaders();

		try {
			httpHeaders.setContentLength(pdfFile.length());

			fileInputStream = new FileInputStream(pdfFile);

			inputStreamResource = new InputStreamResource(fileInputStream);
		}
		catch (FileNotFoundException fileNotFoundException) {
			return new ResponseEntity<>(
				"Failed to generate response from PDF file " +
					fileNotFoundException.toString(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		}

		tempOdtFinal.delete();
		pdfFile.delete();
		System.out.println("End :: [generatePDF]");

		return new ResponseEntity<>(
			inputStreamResource, httpHeaders, HttpStatus.OK);
	}

	public void generateReport(Template template, File file) throws Exception {
		InputStream inputStream = CE01RestController.class.getResourceAsStream(
			"/template-report1.odt");

		XDocReportRegistry xDocReportRegistry =
			XDocReportRegistry.getRegistry();

		IXDocReport report = xDocReportRegistry.loadReport(
			inputStream, TemplateEngineKind.Freemarker);

		IContext context = report.createContext();

		context.put("template", template);

		OutputStream outputStream = new FileOutputStream(file);

		report.process(context, outputStream);

		outputStream.close();
	}

	@GetMapping("/")
	public ResponseEntity<String> home() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("test", "Success");

		if (_log.isInfoEnabled()) {
			_log.info("Return: " + jsonObject);
		}

		return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(CE01RestController.class);

	@Value("${libreoffice.path}")
	private String _libreOfficePath;

}