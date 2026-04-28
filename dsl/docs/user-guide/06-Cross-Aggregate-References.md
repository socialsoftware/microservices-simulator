# Cross-Aggregate References

Cross-aggregate references let one aggregate hold a local copy (projection) of data from another aggregate, with automatic type inference and event-driven referential integrity.

> **Tied example:** [`04-crossrefs`](../../abstractions/04-crossrefs/): Teacher, Course, and Enrollment aggregates with cross-references.

## Domain Overview

The `04-crossrefs` example models a university domain:

```
Teacher ──────────── Course ──────────── Enrollment
 (source)    1:N     (references      1:N  (references
                      Teacher)              Course + Teacher)
```

- A **Teacher** is a standalone aggregate
- A **Course** references a Teacher (the instructor)
- An **Enrollment** references both a Course and its Teachers

## The `from` Keyword

To reference data from another aggregate, define a non-root entity using `Entity ... from ...` with a source block:

```nebula
Entity CourseTeacher {
    from Teacher { name as teacherName, email as teacherEmail, department as teacherDepartment }
}
```

This creates a local entity (`CourseTeacher`) that holds a snapshot of selected fields from the `Teacher` aggregate.

### How It Works

1. Nebula finds the `Teacher` aggregate (even across file boundaries)
2. Gets its root entity's properties
3. For each field in the source block, infers the type from the source property
4. Generates a JPA entity with the mapped fields

### Field Mapping Syntax

Inside the `from` block, list source fields with optional aliases:

```nebula
from Teacher { name as teacherName, email as teacherEmail }
```

- `name`: the property name in the referenced aggregate's root entity
- `as teacherName`: the local field name (optional — if omitted, uses the source name directly)

Without aliases:

```nebula
from User { username, email }
```

This keeps the source field names as-is: `username` and `email`.

### Auto-Generated Base Fields

All cross-aggregate entities automatically include three fields:

```java
private Integer teacherAggregateId;    // ID of the referenced aggregate
private Integer teacherVersion;        // Version for optimistic concurrency
private AggregateState teacherState;   // State of the referenced aggregate
```

The naming convention is `<lowercase-aggregate>AggregateId`, `<lowercase-aggregate>Version`, and `<lowercase-aggregate>State`.

### Type Inference

You never need to declare types in cross-aggregate mappings. Nebula resolves them automatically:

```nebula
// Teacher has: String name, String email, String department
Entity CourseTeacher {
    from Teacher { name as teacherName, email as teacherEmail, department as teacherDepartment }
}
// teacherName: String (inferred), teacherEmail: String (inferred), etc.
```

### Cross-File Type Inference

Type inference works across file boundaries. A model registry tracks all aggregates during generation:

```nebula
// teacher.nebula
Aggregate Teacher {
    Root Entity Teacher {
        String name
        String email
    }
}
```

```nebula
// course.nebula (different file)
Entity CourseTeacher {
    from Teacher { name as teacherName, email as teacherEmail }
}
// Types resolved from teacher.nebula
```

### Generated Code

```java
@Entity
public class CourseTeacher {
    @Id
    @GeneratedValue
    private Long id;
    private String teacherName;
    private String teacherEmail;
    private String teacherDepartment;
    private Integer teacherAggregateId;
    private Integer teacherVersion;
    private AggregateState teacherState;
    @OneToOne
    private Course course;  // Back-reference to parent
}
```

### Empty Mappings

If you only need the base fields (ID, version, state), leave the source block empty:

```nebula
Entity EnrollmentTeacher {
    from Teacher {  }
}
```

This generates an entity with just `teacherAggregateId`, `teacherVersion`, and `teacherState`.

### Collection References

Cross-aggregate references can be used as collections:

```nebula
Root Entity Enrollment {
    EnrollmentCourse course              // Single reference
    Set<EnrollmentTeacher> teachers      // Collection of references
}
```

Collection references generate additional service methods: `addEnrollmentTeacher`, `addEnrollmentTeachers` (bulk), `getEnrollmentTeacher`, `updateEnrollmentTeacher`, and `removeEnrollmentTeacher`.

## Referential Integrity via Event Subscription

When a referenced aggregate is deleted, the subscriber must react. This is handled by subscribing to the source's delete event with a `when` condition and an explicit `action`:

```nebula
Events {
    subscribe TeacherDeletedEvent from Teacher {
        when teacher.teacherAggregateId == event.aggregateId
        action {
            this.state = INACTIVE
        }
    }
}
```

This means:
- **`subscribe TeacherDeletedEvent from Teacher`**: listen for teacher deletion events
- **`when teacher.teacherAggregateId == event.aggregateId`**: only react if the deleted teacher is the one this aggregate references
- **`action { this.state = INACTIVE }`**: mark this aggregate as INACTIVE (cascade)

This is the simulator's native referential integrity model: event-driven cascades. The delete always succeeds on the source side; the subscriber reacts asynchronously.

See [Chapter 07](07-Events-Reactive-Patterns.md) for a deep dive into the event system.

## Complete Example: Course Aggregate

Here's the full `course.nebula` from `04-crossrefs`:

```nebula
Aggregate Course {

    Entity CourseTeacher {
        from Teacher { name as teacherName, email as teacherEmail, department as teacherDepartment }
    }

    Root Entity Course {
        String title
        String description
        Integer maxStudents
        CourseTeacher teacher

        invariants {
            title.length() > 0 : "Course title cannot be blank"
            maxStudents > 0 : "Max students must be positive"
            teacher != null : "Course must have a teacher"
        }
    }

    Events {
        subscribe TeacherDeletedEvent from Teacher {
            when teacher.teacherAggregateId == event.aggregateId
            action {
                this.state = INACTIVE
            }
        }
    }
}
```

This aggregate demonstrates:
- **Cross-aggregate reference**: `CourseTeacher from Teacher` with three aliased fields
- **Entity reference property**: `CourseTeacher teacher` in the root entity
- **Invariants**: including null check on the reference
- **Referential integrity**: event-driven cascade via `subscribe` + `when` + `action`

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/04-crossrefs/
```

Explore the generated code:
- `CourseTeacher.java`: non-root entity with inferred types
- `Course.java`: root entity with `CourseTeacher` reference
- `CourseEventProcessing.java`: event handling for delete cascade

---

**Previous:** [05-Business-Rules-Repositories](05-Business-Rules-Repositories.md) | **Next:** [07-Events-Reactive-Patterns](07-Events-Reactive-Patterns.md)
