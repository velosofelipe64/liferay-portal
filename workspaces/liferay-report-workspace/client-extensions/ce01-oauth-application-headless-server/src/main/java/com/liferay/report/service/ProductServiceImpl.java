package com.liferay.report.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Catalog;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Page;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Pagination;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.CatalogResource;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;


import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class ProductServiceImpl implements ProductService{


    public Page<Product> findAllPaged() {
        try {
            return getProductsPage();
        } catch (Exception e) {
            System.out.println(e.getMessage() + " - "+ e.getCause());
            throw new RuntimeException(e);
        }
    }

    private static Page<Product> getProductsPage() throws Exception {
        ProductResource productResource = getProductResource(
                "test@liferay.com", "test", "localhost:8080", "http");
        Page<Product> productPage = productResource.getProductsPage(
                null, null, Pagination.of(1,50), null);

        return productPage;
    }

    private static ProductResource getProductResource(
            String email, String password, String address, String protocol) {
        ProductResource productResource = ProductResource.builder(
        ).authentication(
                email, password
        ).endpoint(
                address, protocol
        ).build();
        return productResource;
    }
    private static CatalogResource getCatalogResource(
            String email, String password, String address, String protocol) {
        CatalogResource catalogResource = CatalogResource.builder(
        ).authentication(
                email, password
        ).endpoint(
                address, protocol
        ).build();
        return catalogResource;
    }
    private static Page<Catalog> getCatalogsPage() throws Exception {
        CatalogResource catalogResource = getCatalogResource(
                "test@liferay.com", "test", "localhost:8080", "http");
        Page<Catalog> catalogPage = catalogResource.getCatalogsPage(
                null, null, Pagination.of(1,50), null);

        return catalogPage;
    }

    @Override
    public void insert() {
        ProductResource destinationProductResource = getProductResource(
                "test@liferay.com", "test", "localhost:8081", "http");
        CatalogResource destinationCatalogResource = getCatalogResource(
                "test@liferay.com", "test", "localhost:8081", "http");
        try {
            Page<Product> originProductPage = getProductsPage();
            Page<Catalog> originCatalogPage = getCatalogsPage();
            Map<String, Long> catalogBridgeOriginDestination = new HashMap<>();
            for (Catalog catalog : originCatalogPage.getItems()) {
                if (catalog.getExternalReferenceCode().equals("mkt-catalog-1")) {
                    catalogBridgeOriginDestination.put("mkt-catalog-1", catalog.getId());
                }
            }
            Catalog catalog = destinationCatalogResource.getCatalogByExternalReferenceCode("mkt-catalog-1");
            for (Product product : originProductPage.getItems()) {
                if (product.getCatalogId().equals(catalogBridgeOriginDestination.get("mkt-catalog-1"))) {
                    product.setCatalogId(catalog.getId());
                    destinationProductResource.postProduct(product);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
