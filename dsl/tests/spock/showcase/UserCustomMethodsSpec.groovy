package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserCustomMethodsSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    def "signUp creates user with BRONZE tier, zero loyaltyPoints, and active=true"() {
        given:
            def tag = "${System.nanoTime()}"
        when:
            def uow = unitOfWorkService.createUnitOfWork("signup-$tag")
            def dto = userService.signUp("signup-user-$tag", "signup-${tag}@example.com", uow)
            unitOfWorkService.commit(uow)
        then:
            dto.username == "signup-user-$tag"
            dto.email == "signup-${tag}@example.com"
            dto.loyaltyPoints == 0
            dto.tier == "BRONZE"
            dto.active == true
    }

    def "signUp persists and is readable by getUserById"() {
        given:
            def tag = "${System.nanoTime()}"
            def uow = unitOfWorkService.createUnitOfWork("signup-persist-$tag")
            def created = userService.signUp("sp-$tag", "sp-${tag}@example.com", uow)
            unitOfWorkService.commit(uow)
        when:
            def uowR = unitOfWorkService.createUnitOfWork("signup-persist-read-$tag")
            def found = userService.getUserById(created.aggregateId, uowR)
        then:
            found.username == "sp-$tag"
            found.tier == "BRONZE"
            found.loyaltyPoints == 0
            found.active == true
    }

    def "awardLoyaltyPoints updates loyaltyPoints"() {
        given:
            def tag = "${System.nanoTime()}"
            def uow = unitOfWorkService.createUnitOfWork("alp-signup-$tag")
            def created = userService.signUp("alp-$tag", "alp-${tag}@example.com", uow)
            unitOfWorkService.commit(uow)
        when:
            def uowA = unitOfWorkService.createUnitOfWork("alp-award-$tag")
            userService.awardLoyaltyPoints(created.aggregateId, 25, uowA)
            unitOfWorkService.commit(uowA)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("alp-read-$tag")
            def found = userService.getUserById(created.aggregateId, uowR)
            found.loyaltyPoints == 25
    }

    def "awardLoyaltyPoints with zero points throws ShowcaseException"() {
        given:
            def tag = "${System.nanoTime()}"
            def uow = unitOfWorkService.createUnitOfWork("alp-zero-signup-$tag")
            def created = userService.signUp("alpz-$tag", "alpz-${tag}@example.com", uow)
            unitOfWorkService.commit(uow)
        when:
            def uowA = unitOfWorkService.createUnitOfWork("alp-zero-award-$tag")
            userService.awardLoyaltyPoints(created.aggregateId, 0, uowA)
        then:
            thrown(ShowcaseException)
    }

    def "awardLoyaltyPoints with negative points throws ShowcaseException"() {
        given:
            def tag = "${System.nanoTime()}"
            def uow = unitOfWorkService.createUnitOfWork("alp-neg-signup-$tag")
            def created = userService.signUp("alpn-$tag", "alpn-${tag}@example.com", uow)
            unitOfWorkService.commit(uow)
        when:
            def uowA = unitOfWorkService.createUnitOfWork("alp-neg-award-$tag")
            userService.awardLoyaltyPoints(created.aggregateId, -5, uowA)
        then:
            thrown(ShowcaseException)
    }
}
