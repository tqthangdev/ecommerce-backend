package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.CategoryRequest;
import com.dev.ecommerce.dto.response.CategoryResponse;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.entity.Category;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.mapper.CategoryMapper;
import com.dev.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SlugService slugService;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Electronics", "electronics", "Tech products", null);
        category.setId(1L);
    }

    @Test
    void create_shouldSaveCategoryWithUniqueSlug() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");
        request.setDescription("Tech products");

        when(slugService.slugify("Electronics")).thenReturn("electronics");
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.getName()).isEqualTo("Electronics");
        assertThat(response.getSlug()).isEqualTo("electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_shouldRejectDuplicateSlug() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        when(slugService.slugify("Electronics")).thenReturn("electronics");
        // Stub ALL existsBySlug calls to true so ensureUniqueSlug exhausts retries
        when(categoryRepository.existsBySlug(anyString())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Could not generate unique slug")
                .extracting("status")
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void update_shouldUpdateCategoryAndSlug() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Electronics");
        request.setDescription("Updated description");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(slugService.slugify("Updated Electronics")).thenReturn("updated-electronics");
        when(categoryRepository.existsBySlug("updated-electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.update(1L, request);

        verify(categoryRepository).save(any(Category.class));
        assertThat(category.getName()).isEqualTo("Updated Electronics");
    }

    @Test
    void update_shouldThrowWhenCategoryNotFound() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Test");

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldRemoveCategory() {
        category.setProducts(new HashSet<>());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_shouldRejectWhenCategoryHasProducts() {
        category.getProducts().add(new com.dev.ecommerce.entity.Product()); // non-empty set
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete category with existing products");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void getById_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Electronics");
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBySlug_shouldReturnCategory() {
        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getBySlug("electronics");

        assertThat(response.getSlug()).isEqualTo("electronics");
    }

    @Test
    void list_shouldReturnPaginatedCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(category));

        when(categoryRepository.findAll(pageable)).thenReturn(page);

        PageResponse<CategoryResponse> response = categoryService.list(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }
}
