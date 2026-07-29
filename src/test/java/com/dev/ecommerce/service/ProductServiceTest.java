package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.ProductRequest;
import com.dev.ecommerce.dto.request.ProductSearchRequest;
import com.dev.ecommerce.dto.request.ProductVariantRequest;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.dto.response.ProductImageResponse;
import com.dev.ecommerce.dto.response.ProductResponse;
import com.dev.ecommerce.dto.response.ProductVariantResponse;
import com.dev.ecommerce.entity.Brand;
import com.dev.ecommerce.entity.Category;
import com.dev.ecommerce.entity.Product;
import com.dev.ecommerce.entity.ProductImage;
import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.mapper.ProductMapper;
import com.dev.ecommerce.repository.BrandRepository;
import com.dev.ecommerce.repository.CategoryRepository;
import com.dev.ecommerce.repository.ProductImageRepository;
import com.dev.ecommerce.repository.ProductRepository;
import com.dev.ecommerce.repository.ProductVariantRepository;
import com.dev.ecommerce.storage.FileStorageService;
import com.dev.ecommerce.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductImageRepository imageRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SlugService slugService;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() {
        category = new Category("Electronics", "electronics", null, null);
        category.setId(1L);
        category.setProducts(new java.util.HashSet<>());

        brand = new Brand("Apple", "apple", null, null);
        brand.setId(1L);
        brand.setProducts(new java.util.HashSet<>());

        product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setSlug("iphone-15");
        product.setDescription("Apple smartphone");
        product.setBasePrice(BigDecimal.valueOf(999));
        product.setDiscountPercent(BigDecimal.ZERO);
        product.setStockQuantity(100);
        product.setActive(true);
        product.setFeatured(false);
        product.setCategory(category);
        product.setBrand(brand);
        product.setVariants(new HashSet<>());
        product.setImages(new HashSet<>());
    }

    // --- CREATE ---

    @Test
    void create_shouldSaveProductWithCategoryAndBrand() {
        ProductRequest request = makeRequest();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(slugService.slugify("iPhone 15")).thenReturn("iphone-15");
        when(productRepository.existsBySlug("iphone-15")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(productRepository.findWithDetailsById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.create(request);

        assertThat(response.getName()).isEqualTo("iPhone 15");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_shouldThrowWhenCategoryNotFound() {
        ProductRequest request = makeRequest();
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    void create_shouldThrowWhenBrandNotFound() {
        ProductRequest request = makeRequest();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Brand");
    }

    // --- UPDATE ---

    @Test
    void update_shouldUpdateProductFields() {
        ProductRequest request = makeRequest();
        request.setName("iPhone 16");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(slugService.slugify("iPhone 16")).thenReturn("iphone-16");
        when(productRepository.existsBySlug("iphone-16")).thenReturn(false);
        when(productRepository.findWithDetailsById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.update(1L, request);

        // Service uses JPA dirty checking, no explicit save() call needed
        assertThat(product.getName()).isEqualTo("iPhone 16");
        assertThat(response.getName()).isEqualTo("iPhone 16");
    }

    @Test
    void update_shouldThrowWhenProductNotFound() {
        ProductRequest request = makeRequest();
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- DELETE ---

    @Test
    void delete_shouldRemoveProductAndImages() {
        ProductImage img = new ProductImage(product, "http://cdn/img.jpg", null, 0, true);
        product.getImages().add(img);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(fileStorageService).delete("http://cdn/img.jpg");
        verify(productRepository).delete(product);
    }

    @Test
    void delete_shouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- GET ---

    @Test
    void getBySlug_shouldReturnProductWithDetails() {
        when(productRepository.findWithDetailsBySlug("iphone-15")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getBySlug("iphone-15");

        assertThat(response.getSlug()).isEqualTo("iphone-15");
    }

    @Test
    void getBySlug_shouldThrowWhenNotFound() {
        when(productRepository.findWithDetailsBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySlug("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- SEARCH ---

    @Test
    void search_shouldReturnFilteredResults() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setCategoryId(1L);
        request.setActive(true);
        request.setPage(0);
        request.setSize(20);

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.findWithDetailsById(1L)).thenReturn(Optional.of(product));

        PageResponse<ProductResponse> response = productService.search(request);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_shouldFilterByPriceRange() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setMinPrice(BigDecimal.valueOf(100));
        request.setMaxPrice(BigDecimal.valueOf(1000));
        request.setPage(0);
        request.setSize(20);

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.findWithDetailsById(1L)).thenReturn(Optional.of(product));

        PageResponse<ProductResponse> result = productService.search(request);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void search_shouldFilterByKeyword() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("iphone");
        request.setPage(0);
        request.setSize(20);

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.findWithDetailsById(1L)).thenReturn(Optional.of(product));

        PageResponse<ProductResponse> result = productService.search(request);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void search_shouldParseValidSort() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setSort("PRICE,asc");
        request.setPage(0);
        request.setSize(20);

        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> result = productService.search(request);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void search_shouldThrowOnInvalidSortFormat() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setSort("invalid-format");
        request.setPage(0);
        request.setSize(20);

        assertThatThrownBy(() -> productService.search(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid sort format");
    }

    @Test
    void search_shouldThrowOnUnknownSortField() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setSort("unknownField,asc");
        request.setPage(0);
        request.setSize(20);

        assertThatThrownBy(() -> productService.search(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unknown sort field");
    }

    // --- VARIANTS ---

    @Test
    void addVariant_shouldCreateVariant() {
        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("IPHONE15-BLK-128");
        request.setColor("Black");
        request.setSize("128GB");
        request.setPrice(BigDecimal.valueOf(999));
        request.setStockQuantity(50);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("IPHONE15-BLK-128")).thenReturn(false);
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        ProductVariantResponse response = productService.addVariant(1L, request);

        assertThat(response.getSku()).isEqualTo("IPHONE15-BLK-128");
        verify(variantRepository).save(any(ProductVariant.class));
    }

    @Test
    void addVariant_shouldRejectDuplicateSku() {
        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("EXISTING-SKU");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("EXISTING-SKU")).thenReturn(true);

        assertThatThrownBy(() -> productService.addVariant(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU already exists")
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateVariant_shouldUpdateFields() {
        ProductVariant variant = new ProductVariant(
                product, "OLD-SKU", "Black", "128GB",
                BigDecimal.valueOf(999), 50, null
        );
        variant.setId(1L);

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("OLD-SKU");
        request.setColor("White");
        request.setSize("256GB");
        request.setPrice(BigDecimal.valueOf(1099));
        request.setStockQuantity(30);

        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(variantRepository.save(any(ProductVariant.class))).thenReturn(variant);

        ProductVariantResponse response = productService.updateVariant(1L, request);

        assertThat(variant.getColor()).isEqualTo("White");
        assertThat(variant.getSize()).isEqualTo("256GB");
    }

    @Test
    void updateVariant_shouldRejectNewDuplicateSku() {
        ProductVariant variant = new ProductVariant(
                product, "OLD-SKU", "Black", "128GB",
                BigDecimal.valueOf(999), 50, null
        );
        variant.setId(1L);

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("TAKEN-SKU");
        request.setColor("White");
        request.setSize("256GB");
        request.setPrice(BigDecimal.valueOf(1099));

        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(variantRepository.existsBySku("TAKEN-SKU")).thenReturn(true);

        assertThatThrownBy(() -> productService.updateVariant(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    void removeVariant_shouldDeleteVariant() {
        when(variantRepository.existsById(1L)).thenReturn(true);

        productService.removeVariant(1L);

        verify(variantRepository).deleteById(1L);
    }

    @Test
    void removeVariant_shouldThrowWhenNotFound() {
        when(variantRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.removeVariant(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- IMAGES ---

    @Test
    void uploadImage_shouldStoreAndPersist() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone.jpg", "image/jpeg", "fake-image-data".getBytes()
        );
        StoredFile stored = new StoredFile("http://cdn/products/1/uuid-iphone.jpg", "products/1/uuid-iphone.jpg", 15L, "image/jpeg");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.upload(file, "products/1")).thenReturn(stored);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setId(1L);
            return img;
        });

        ProductImageResponse response = productService.uploadImage(1L, file);

        assertThat(response.getImageUrl()).isNotNull();
        verify(fileStorageService).upload(file, "products/1");
        verify(imageRepository).save(any(ProductImage.class));
    }

    @Test
    void uploadImage_shouldSetFirstImageAsPrimary() {
        // No existing images — first should be primary
        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone.jpg", "image/jpeg", "data".getBytes()
        );
        StoredFile stored = new StoredFile("http://cdn/img.jpg", "key", 10L, "image/jpeg");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.upload(file, "products/1")).thenReturn(stored);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setId(1L);
            return img;
        });

        ProductImageResponse response = productService.uploadImage(1L, file);

        assertThat(response.isPrimary()).isTrue();
    }

    @Test
    void removeImage_shouldDeleteFromStorageAndDb() {
        ProductImage image = new ProductImage(product, "http://cdn/img.jpg", null, 0, true);
        image.setId(1L);
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

        productService.removeImage(1L);

        verify(fileStorageService).delete("http://cdn/img.jpg");
        verify(imageRepository).delete(image);
    }

    @Test
    void setPrimaryImage_shouldUnsetOthersAndSetPrimary() {
        ProductImage img1 = new ProductImage(product, "http://cdn/1.jpg", null, 0, true);
        img1.setId(1L);
        ProductImage img2 = new ProductImage(product, "http://cdn/2.jpg", null, 1, false);
        img2.setId(2L);
        product.getImages().add(img1);
        product.getImages().add(img2);
        img1.setPrimary(true);

        when(imageRepository.findById(2L)).thenReturn(Optional.of(img2));
        when(imageRepository.save(img2)).thenReturn(img2);

        ProductImageResponse response = productService.setPrimaryImage(2L);

        assertThat(img2.isPrimary()).isTrue();
        assertThat(img1.isPrimary()).isFalse();
        verify(imageRepository).save(img2);
    }

    // --- HELPERS ---

    private ProductRequest makeRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("iPhone 15");
        request.setDescription("Apple smartphone");
        request.setBasePrice(BigDecimal.valueOf(999));
        request.setCategoryId(1L);
        request.setBrandId(1L);
        request.setActive(true);
        request.setFeatured(false);
        return request;
    }
}
