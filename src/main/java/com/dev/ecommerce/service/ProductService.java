package com.dev.ecommerce.service;

import com.dev.ecommerce.config.CacheConfig;
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
import com.dev.ecommerce.entity.enums.ProductSortField;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.mapper.ProductMapper;
import com.dev.ecommerce.repository.BrandRepository;
import com.dev.ecommerce.repository.CategoryRepository;
import com.dev.ecommerce.repository.ProductImageRepository;
import com.dev.ecommerce.repository.ProductRepository;
import com.dev.ecommerce.repository.ProductVariantRepository;
import com.dev.ecommerce.repository.specification.ProductSpecifications;
import com.dev.ecommerce.storage.FileStorageService;
import com.dev.ecommerce.storage.StoredFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final SlugService slugService;

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_PRODUCT_DETAIL}, allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", request.getBrandId()));

        String baseSlug = slugService.slugify(request.getName());
        String slug = ensureUniqueSlug(baseSlug);

        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setDiscountPercent(
                request.getDiscountPercent() == null ? java.math.BigDecimal.ZERO : request.getDiscountPercent()
        );
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }
        product.setCategory(category);
        product.setBrand(brand);

        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(loadWithDetails(saved.getId()));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_PRODUCT_DETAIL}, allEntries = true)
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", request.getBrandId()));

        String baseSlug = slugService.slugify(request.getName());
        String slug = baseSlug.equals(product.getSlug()) ? baseSlug : ensureUniqueSlug(baseSlug);

        product.setName(request.getName());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setDiscountPercent(
                request.getDiscountPercent() == null ? java.math.BigDecimal.ZERO : request.getDiscountPercent()
        );
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }
        product.setCategory(category);
        product.setBrand(brand);

        return ProductMapper.toResponse(loadWithDetails(product.getId()));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_PRODUCT_DETAIL}, allEntries = true)
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        // Best-effort cleanup of stored images before delete
        for (ProductImage img : product.getImages()) {
            fileStorageService.delete(img.getImageUrl());
        }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductMapper.toResponse(loadWithDetails(id));
    }

    @Transactional(readOnly = true)
    // @Cacheable disabled: Redis serializer doesn't support LocalDateTime
    public ProductResponse getBySlug(String slug) {
        return ProductMapper.toResponse(
                productRepository.findWithDetailsBySlug(slug)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", slug))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchRequest request) {
        Pageable pageable = buildPageable(request);

        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasCategory(request.getCategoryId()))
                .and(ProductSpecifications.hasBrand(request.getBrandId()))
                .and(ProductSpecifications.priceBetween(request.getMinPrice(), request.getMaxPrice()))
                .and(ProductSpecifications.hasKeyword(request.getKeyword()))
                .and(ProductSpecifications.isActive(request.getActive()));

        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.from(page, p -> ProductMapper.toResponse(
                productRepository.findWithDetailsByIdFetch(p.getId()).orElse(p)
        ));
    }

    @Transactional
    public ProductVariantResponse addVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (variantRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU already exists: " + request.getSku(), HttpStatus.CONFLICT);
        }
        ProductVariant variant = new ProductVariant(
                product,
                request.getSku(),
                request.getColor(),
                request.getSize(),
                request.getPrice(),
                request.getStockQuantity() == null ? 0 : request.getStockQuantity(),
                request.getImageUrl()
        );
        variantRepository.save(variant);
        recalculateProduct(product);
        return ProductMapper.toResponse(variant);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!variant.getSku().equals(request.getSku()) && variantRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU already exists: " + request.getSku(), HttpStatus.CONFLICT);
        }
        Product product = variant.getProduct();
        variant.setSku(request.getSku());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) {
            variant.setStockQuantity(request.getStockQuantity());
        }
        variant.setImageUrl(request.getImageUrl());
        variantRepository.save(variant);
        recalculateProduct(product);
        return ProductMapper.toResponse(variant);
    }

    @Transactional
    public void removeVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        Product product = variant.getProduct();
        product.getVariants().remove(variant);
        variantRepository.delete(variant);
        recalculateProduct(product);
    }

    private void recalculateProduct(Product product) {
        int total = product.getVariants().stream()
                .mapToInt(ProductVariant::getStockQuantity)
                .sum();
        product.setStockQuantity(total);
        var cheapest = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(java.math.BigDecimal::compareTo)
                .orElse(product.getBasePrice());
        product.setBasePrice(cheapest);
        productRepository.save(product);
    }

    @Transactional
    public ProductImageResponse uploadImage(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        StoredFile stored = fileStorageService.upload(file, "products/" + productId);
        int nextOrder = product.getImages().stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
        boolean isFirst = product.getImages().isEmpty();

        ProductImage image = new ProductImage(product, stored.url(), null, nextOrder, isFirst);
        return ProductMapper.toResponse(imageRepository.save(image));
    }

    @Transactional
    public void removeImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        fileStorageService.delete(image.getImageUrl());
        imageRepository.delete(image);
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        Product product = image.getProduct();
        for (ProductImage other : product.getImages()) {
            other.setPrimary(false);
        }
        image.setPrimary(true);
        imageRepository.save(image);
        return ProductMapper.toResponse(image);
    }

    private Product loadWithDetails(Long id) {
        return productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Pageable buildPageable(ProductSearchRequest request) {
        int page = request.getPage() == null ? 0 : request.getPage();
        int size = request.getSize() == null ? 20 : Math.min(request.getSize(), 100);
        Sort sort = parseSort(request.getSort());
        return PageRequest.of(page, size, sort);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",");
        if (parts.length != 2) {
            throw new BusinessException("Invalid sort format. Use 'field,direction'", HttpStatus.BAD_REQUEST);
        }
        ProductSortField field;
        try {
            field = ProductSortField.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unknown sort field: " + parts[0], HttpStatus.BAD_REQUEST);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid sort direction: " + parts[1], HttpStatus.BAD_REQUEST);
        }
        return Sort.by(direction, field.getProperty());
    }

    private String ensureUniqueSlug(String base) {
        String slug = base;
        int attempts = 0;
        while (productRepository.existsBySlug(slug)) {
            attempts++;
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (attempts > 5) {
                throw new BusinessException("Could not generate unique slug", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return slug;
    }
}