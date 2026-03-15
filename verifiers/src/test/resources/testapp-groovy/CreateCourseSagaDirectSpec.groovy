package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp

class CreateCourseSagaDirectSpec extends TestAppBaseSpec {
    def courseDto
    def courseIds

    def setup() {
        courseDto = createCourse(COURSE_NAME_1)
        courseDto.setAcronym(COURSE_ACRONYM_1)
        courseIds = [courseDto.aggregateId, COURSE_AGGREGATE_ID_1]
    }

    def "direct saga construction with domain args"() {
        given:
        def saga = new CreateCourseSaga(unitOfWorkService, courseDto.aggregateId, courseDto, unitOfWork)

        when:
        saga.executeWorkflow(unitOfWork)

        then:
        true
    }
}
