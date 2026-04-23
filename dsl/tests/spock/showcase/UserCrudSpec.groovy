package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.MembershipTier
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserCrudSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createUser(String username, String email, Integer loyaltyPoints, MembershipTier tier, Boolean active) {
        def uow = unitOfWorkService.createUnitOfWork("user-create-$username")
        def req = new CreateUserRequestDto()
        req.username = username
        req.email = email
        req.loyaltyPoints = loyaltyPoints
        req.tier = tier
        req.active = active
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "createUser persists and getUserById reads back"() {
        given:
            def tag = "${System.nanoTime()}"
        when:
            def user = createUser("alice-$tag", "alice-${tag}@example.com", 10, MembershipTier.SILVER, true)
        then:
            def uow = unitOfWorkService.createUnitOfWork("user-read-$tag")
            def found = userService.getUserById(user.aggregateId, uow)
            found.username == "alice-$tag"
            found.email == "alice-${tag}@example.com"
            found.loyaltyPoints == 10
            found.tier == "SILVER"
            found.active == true
    }

    def "createUser persists all membership tiers"() {
        given:
            def tag = "${System.nanoTime()}"
        when:
            def bronze = createUser("b-$tag", "b-${tag}@e.com", 0, MembershipTier.BRONZE, true)
            def gold = createUser("g-$tag", "g-${tag}@e.com", 100, MembershipTier.GOLD, true)
            def platinum = createUser("p-$tag", "p-${tag}@e.com", 500, MembershipTier.PLATINUM, true)
        then:
            def uow = unitOfWorkService.createUnitOfWork("user-tiers-read-$tag")
            userService.getUserById(bronze.aggregateId, uow).tier == "BRONZE"
            def uow2 = unitOfWorkService.createUnitOfWork("user-tiers-read2-$tag")
            userService.getUserById(gold.aggregateId, uow2).tier == "GOLD"
            def uow3 = unitOfWorkService.createUnitOfWork("user-tiers-read3-$tag")
            userService.getUserById(platinum.aggregateId, uow3).tier == "PLATINUM"
    }

    def "getAllUsers returns created users"() {
        given:
            def tag = "${System.nanoTime()}"
            createUser("bob-$tag", "bob-${tag}@e.com", 5, MembershipTier.BRONZE, true)
            createUser("carol-$tag", "carol-${tag}@e.com", 5, MembershipTier.SILVER, true)
        when:
            def uow = unitOfWorkService.createUnitOfWork("user-list-$tag")
            def all = userService.getAllUsers(uow)
        then:
            all.any { it.username == "bob-$tag" }
            all.any { it.username == "carol-$tag" }
    }

    def "updateUser mutates email and loyaltyPoints"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = createUser("dave-$tag", "dave-${tag}@e.com", 0, MembershipTier.BRONZE, true)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("user-upd-get-$tag")
            def existing = userService.getUserById(user.aggregateId, uowGet)
            existing.email = "dave-new-${tag}@example.com"
            existing.loyaltyPoints = 42
            def uowPut = unitOfWorkService.createUnitOfWork("user-upd-put-$tag")
            userService.updateUser(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-upd-read-$tag")
            def reloaded = userService.getUserById(user.aggregateId, uowR)
            reloaded.email == "dave-new-${tag}@example.com"
            reloaded.loyaltyPoints == 42
    }

    def "updateUser can change tier"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = createUser("tier-$tag", "tier-${tag}@e.com", 0, MembershipTier.BRONZE, true)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("user-tier-upd-get-$tag")
            def existing = userService.getUserById(user.aggregateId, uowGet)
            existing.tier = "PLATINUM"
            def uowPut = unitOfWorkService.createUnitOfWork("user-tier-upd-put-$tag")
            userService.updateUser(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-tier-upd-read-$tag")
            def reloaded = userService.getUserById(user.aggregateId, uowR)
            reloaded.tier == "PLATINUM"
    }

    def "updateUser can flip active flag to false"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = createUser("act-$tag", "act-${tag}@e.com", 0, MembershipTier.BRONZE, true)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("user-act-upd-get-$tag")
            def existing = userService.getUserById(user.aggregateId, uowGet)
            existing.active = false
            def uowPut = unitOfWorkService.createUnitOfWork("user-act-upd-put-$tag")
            userService.updateUser(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-act-upd-read-$tag")
            def reloaded = userService.getUserById(user.aggregateId, uowR)
            reloaded.active == false
    }

    def "getActiveUsers returns only active users via query method"() {
        given:
            def tag = "${System.nanoTime()}"
            def active = createUser("active-$tag", "active-${tag}@e.com", 0, MembershipTier.BRONZE, true)
            def inactive = createUser("inactive-$tag", "inactive-${tag}@e.com", 0, MembershipTier.BRONZE, false)
        when:
            def uow = unitOfWorkService.createUnitOfWork("user-active-$tag")
            def result = userService.getActiveUsers(uow)
        then:
            result.any { it.aggregateId == active.aggregateId }
            !result.any { it.aggregateId == inactive.aggregateId }
    }

    def "deleteUser hides the row from subsequent reads"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = createUser("eve-$tag", "eve-${tag}@e.com", 0, MembershipTier.BRONZE, true)
        when:
            def uowDel = unitOfWorkService.createUnitOfWork("user-del-$tag")
            userService.deleteUser(user.aggregateId, uowDel)
            unitOfWorkService.commit(uowDel)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-del-read-$tag")
            try {
                userService.getUserById(user.aggregateId, uowR)
                assert false : "expected to throw"
            } catch (Exception ignored) {}
    }
}
