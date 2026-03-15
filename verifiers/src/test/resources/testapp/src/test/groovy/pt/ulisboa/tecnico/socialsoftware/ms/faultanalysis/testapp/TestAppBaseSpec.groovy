package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp

abstract class TestAppBaseSpec {
    static final String COURSE_NAME_1 = "Test Course"
    static final String COURSE_ACRONYM_1 = "TC"
    static final Integer COURSE_AGGREGATE_ID_1 = 1

    protected createCourse(String name) {
        def dto = new CourseDto()
        dto.setName(name)
        return dto
    }
}
