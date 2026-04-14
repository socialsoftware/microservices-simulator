package pt.ulisboa.tecnico.socialsoftware.teastore

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.webapi.requestDtos.CreateCategoryRequestDto
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CategoryCrudSpec extends Specification {

    @Autowired CategoryService categoryService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createCategory + getCategoryById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("tea-create")
            def req = new CreateCategoryRequestDto()
            req.name = "Black Tea"
            req.description = "fermented"

        when:
            def created = categoryService.createCategory(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("tea-read")
            def reloaded = categoryService.getCategoryById(created.aggregateId, uowR)
            reloaded.name == "Black Tea"
            reloaded.description == "fermented"
    }
}
