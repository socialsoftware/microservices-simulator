package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing.BookingEventProcessing
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.MembershipTier
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class EventReactionSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService
    @Autowired BookingEventProcessing bookingEventProcessing

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("er-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "er-user-$tag"
        req.email = "er-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "BookingEventProcessing bean is wired and autowired"() {
        expect:
            bookingEventProcessing != null
    }

    def "awardLoyaltyPoints on User commits successfully (publishes UserLoyaltyAwardedEvent)"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("er-award-$tag")
            userService.awardLoyaltyPoints(user.aggregateId, 15, uow)
            unitOfWorkService.commit(uow)
        then:
            noExceptionThrown()
            def uowR = unitOfWorkService.createUnitOfWork("er-award-read-$tag")
            def found = userService.getUserById(user.aggregateId, uowR)
            found.loyaltyPoints == 15
    }
}
