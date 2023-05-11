package com.liferay.report.controllers;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Page;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;
import com.liferay.report.dto.ProductDTO;
import com.liferay.report.service.ProductService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping(value = "/products")
public class MarketPlaceController {
    @Autowired
    private ProductService service;

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(){
        Page<Product> listProducts = service.findAllPaged();

        return ResponseEntity.ok().body(listProducts);
    }

    @PostMapping(path = "/product")
    public ResponseEntity<Void> insertProduct(){
        service.insert();
        return null;
    }

}
