package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception;

public final class CrossrefsErrorMessage {
    private CrossrefsErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String COURSE_MISSING_TITLE = "Course requires a title.";

    public static final String COURSE_MISSING_DESCRIPTION = "Course requires a description.";

    public static final String ENROLLMENT_MISSING_TEACHERS = "Enrollment requires a teachers.";

    public static final String TEACHER_MISSING_NAME = "Teacher requires a name.";

    public static final String TEACHER_MISSING_EMAIL = "Teacher requires a email.";

    public static final String TEACHER_MISSING_DEPARTMENT = "Teacher requires a department.";

}