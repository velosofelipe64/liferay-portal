package com.liferay.report.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Page;
import com.liferay.report.dto.ProductDTO;
import org.json.JSONObject;

import java.util.List;

public interface ProductService {
    Page<Product> findAllPaged();

    void insert();
}
