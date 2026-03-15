package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp

class UpdateCourseSagaIndirectSpec extends TestAppBaseSpec {
    CourseFunctionalities courseFunctionalities
    def courseDto

    def setup() {
        courseDto = new CourseDto(name: COURSE_NAME_1)
    }

    def "indirect saga creation via functionalities"() {
        given:
        def saga = courseFunctionalities.createCourse(courseDto.aggregateId, courseDto, unitOfWork)

        when:
        saga.executeWorkflow(unitOfWork)

        then:
        true
    }
}
