package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities.CategoryFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateCategoryRequestDto;

@RestController
public class CategoryController {
    @Autowired
    private CategoryFunctionalities categoryFunctionalities;

    @PostMapping("/categorys/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody CreateCategoryRequestDto createRequest) {
        return categoryFunctionalities.createCategory(createRequest);
    }

    @GetMapping("/categorys/{categoryAggregateId}")
    public CategoryDto getCategoryById(@PathVariable Integer categoryAggregateId) {
        return categoryFunctionalities.getCategoryById(categoryAggregateId);
    }

    @PutMapping("/categorys")
    public CategoryDto updateCategory(@RequestBody CategoryDto categoryDto) {
        return categoryFunctionalities.updateCategory(categoryDto);
    }

    @DeleteMapping("/categorys/{categoryAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Integer categoryAggregateId) {
        categoryFunctionalities.deleteCategory(categoryAggregateId);
    }

    @GetMapping("/categorys")
    public List<CategoryDto> getAllCategorys() {
        return categoryFunctionalities.getAllCategorys();
    }
}
