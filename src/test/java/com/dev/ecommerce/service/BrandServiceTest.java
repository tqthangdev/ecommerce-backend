package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.BrandRequest;
import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.entity.Brand;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.mapper.BrandMapper;
import com.dev.ecommerce.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.anyString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SlugService slugService;

    @InjectMocks
    private BrandService brandService;

    private Brand brand;

    @BeforeEach
    void setUp() {
        brand = new Brand("Apple", "apple", "Apple Inc.", null);
        brand.setId(1L);
    }

    @Test
    void create_shouldSaveBrandWithUniqueSlug() {
        BrandRequest request = new BrandRequest();
        request.setName("Apple");
        request.setDescription("Apple Inc.");

        when(slugService.slugify("Apple")).thenReturn("apple");
        when(brandRepository.existsBySlug("apple")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> {
            Brand saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BrandResponse response = brandService.create(request);

        assertThat(response.getName()).isEqualTo("Apple");
        assertThat(response.getSlug()).isEqualTo("apple");
        verify(brandRepository).save(any(Brand.class));
    }

    @Test
    void create_shouldRejectDuplicateSlug() {
        BrandRequest request = new BrandRequest();
        request.setName("Apple");

        when(slugService.slugify("Apple")).thenReturn("apple");
        // Stub ALL existsBySlug calls to true so ensureUniqueSlug exhausts retries
        when(brandRepository.existsBySlug(anyString())).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Could not generate unique slug")
                .extracting("status")
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void update_shouldUpdateBrand() {
        BrandRequest request = new BrandRequest();
        request.setName("Updated Apple");
        request.setDescription("Updated description");

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(slugService.slugify("Updated Apple")).thenReturn("updated-apple");
        when(brandRepository.existsBySlug("updated-apple")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        BrandResponse response = brandService.update(1L, request);

        verify(brandRepository).save(any(Brand.class));
        assertThat(brand.getName()).isEqualTo("Updated Apple");
    }

    @Test
    void update_shouldThrowWhenBrandNotFound() {
        BrandRequest request = new BrandRequest();
        request.setName("Test");

        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldRemoveBrand() {
        brand.setProducts(new HashSet<>());
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        brandService.delete(1L);

        verify(brandRepository).delete(brand);
    }

    @Test
    void delete_shouldRejectWhenBrandHasProducts() {
        brand.getProducts().add(new com.dev.ecommerce.entity.Product()); // non-empty set
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThatThrownBy(() -> brandService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete brand with existing products");

        verify(brandRepository, never()).delete(any(Brand.class));
    }

    @Test
    void getById_shouldReturnBrand() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        BrandResponse response = brandService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Apple");
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBySlug_shouldReturnBrand() {
        when(brandRepository.findBySlug("apple")).thenReturn(Optional.of(brand));

        BrandResponse response = brandService.getBySlug("apple");

        assertThat(response.getSlug()).isEqualTo("apple");
    }

    @Test
    void list_shouldReturnPaginatedBrands() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Brand> page = new PageImpl<>(List.of(brand));

        when(brandRepository.findAll(pageable)).thenReturn(page);

        PageResponse<BrandResponse> response = brandService.list(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }
}
