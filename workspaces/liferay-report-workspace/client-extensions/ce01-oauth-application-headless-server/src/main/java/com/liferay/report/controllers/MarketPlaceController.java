package com.liferay.report.controllers;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Page;
import com.liferay.report.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/products")
@RestController
public class MarketPlaceController {

//	@GetMapping
//	public ResponseEntity<Page<Product>> getProducts() {
//		Page<Product> listProducts = service.findAllPaged();
//
//		return ResponseEntity.ok(
//		).body(
//			listProducts
//		);
//	}

	@PostMapping(path = "/product")
	public ResponseEntity<Void> insertProduct() throws Exception {
		service.insert();

		return null;
	}

	@Autowired
	private ProductService service;

}