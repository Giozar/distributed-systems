package com.giozar04.transactions.domain.interfaces;

import java.util.List;

import com.giozar04.transactions.domain.entities.Category;

public interface CategoryRepositoryInterface {
    Category createCategory(Category category);
    Category getCategoryById(long id);
    Category updateCategoryById(long id, Category category);
    Category deleteCategoryById(long id);
    List<Category> getAllCategories();
}
