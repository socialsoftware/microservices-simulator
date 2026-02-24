# Cross-Aggregate References

Cross-aggregate references are the most powerful feature of Nebula DSL. They let one aggregate hold a local copy of data from another aggregate, with automatic type inference, referential integrity, and delete handling.

> **Tied example:** [`04-crossrefs`](../examples/abstractions/04-crossrefs/) — Teacher, Course, and Enrollment aggregates with cross-references.

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

To reference data from another aggregate, define a non-root entity using `Entity ... from ...`:

```nebula
Entity CourseTeacher from Teacher {
    map name as teacherName
    map email as teacherEmail
    map department as teacherDepartment
}
```

This creates a local entity (`CourseTeacher`) that holds a snapshot of selected fields from the `Teacher` aggregate.

### How It Works

1. Nebula finds the `Teacher` aggregate
2. Gets its root entity's properties
3. For each `map` statement, infers the type from the source property
4. Generates a JPA embeddable with the mapped fields

### Field Mapping Syntax

```nebula
map <sourceField> as <localField>
```

- `sourceField` — the property name in the referenced aggregate's root entity
- `localField` — the name used in the local entity

Example: `map name as teacherName` means "take the `name` field from `Teacher` (which is a `String`) and call it `teacherName` locally."

### Auto-Generated Base Fields

All cross-aggregate entities automatically include two fields:

```java
private Integer teacherAggregateId;  // ID of the referenced aggregate
private Integer teacherVersion;      // Version for optimistic concurrency
```

The naming convention is `<lowercase-aggregate>AggregateId` and `<lowercase-aggregate>Version`.

### Type Inference

You never need to declare types in cross-aggregate mappings. Nebula resolves them automatically:

```nebula
// Teacher has: String name, String email, String department
Entity CourseTeacher from Teacher {
    map name as teacherName        // String (inferred)
    map email as teacherEmail      // String (inferred)
    map department as teacherDepartment  // String (inferred)
}
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
Entity CourseTeacher from Teacher {
    map name as teacherName      // String (resolved from teacher.nebula)
    map email as teacherEmail    // String (resolved from teacher.nebula)
}
```

### Generated Code

```java
@Embeddable
public class CourseTeacher {
    private Integer teacherAggregateId;    // Auto-added
    private Integer teacherVersion;        // Auto-added
    private String teacherName;            // Inferred: String
    private String teacherEmail;           // Inferred: String
    private String teacherDepartment;      // Inferred: String
}
```

Non-root entities are simple JPA entities (no version chain, no invariants).

### Empty Mappings

If you only need the base fields (ID and version), leave the mapping block empty:

```nebula
Entity EnrollmentTeacher from Teacher {
}
```

This generates an entity with just `teacherAggregateId` and `teacherVersion`.

### Collection References

Cross-aggregate references can be used as collections:

```nebula
Root Entity Enrollment {
    EnrollmentCourse course              // Single reference
    Set<EnrollmentTeacher> teachers      // Collection of references
}
```

Collection references generate additional `addTeacherToEnrollment` and `removeTeacherFromEnrollment` service methods.

## References Block

The `References` block defines what happens when a referenced aggregate is deleted:

```nebula
References {
    teacher -> Teacher {
        onDelete: prevent
        message: "Cannot delete teacher with active courses"
    }
}
```

### Delete Actions

| Action | Behavior |
|--------|----------|
| `prevent` | Throw an exception — the referenced aggregate cannot be deleted while this reference exists |
| `cascade` | Delete this aggregate when the referenced aggregate is deleted |
| `setNull` | Set the reference to null when the referenced aggregate is deleted |

### Multiple References

An aggregate can reference multiple other aggregates:

```nebula
References {
    course -> Course {
        onDelete: cascade
        message: "Course deleted, removing enrollments"
    }
    // Additional references would go here
}
```

## Inter-Invariants (Introduction)

Inter-invariants enforce referential integrity across aggregates using event subscriptions. They combine an event subscription with a matching condition to detect when a referenced aggregate is deleted or modified.

```nebula
Events {
    interInvariant TEACHER_EXISTS {
        subscribe TeacherDeletedEvent from Teacher {
            teacher.teacherAggregateId == event.aggregateId
        }
    }
}
```

This means: "When a `TeacherDeletedEvent` arrives, check if our local `teacher.teacherAggregateId` matches `event.aggregateId`. If it does, this aggregate is affected."

Inter-invariants work together with the `References` block — the inter-invariant detects the event, and the reference action (`prevent`, `cascade`, `setNull`) determines the response. See [Chapter 07](07-Events-Reactive-Patterns.md) for a deep dive.

## Complete Example: Course Aggregate

Here's the full `course.nebula` from `04-crossrefs`:

```nebula
Aggregate Course {
    @GenerateCrud

    Entity CourseTeacher from Teacher {
        map name as teacherName
        map email as teacherEmail
        map department as teacherDepartment
    }

    Root Entity Course {
        String title
        String description
        Integer maxStudents
        CourseTeacher teacher

        invariants {
            check titleNotBlank { title.length() > 0 } error "Course title cannot be blank"
            check maxStudentsPositive { maxStudents > 0 } error "Max students must be positive"
            check teacherNotNull { teacher != null } error "Course must have a teacher"
        }
    }

    References {
        teacher -> Teacher {
            onDelete: prevent
            message: "Cannot delete teacher with active courses"
        }
    }

    Events {
        interInvariant TEACHER_EXISTS {
            subscribe TeacherDeletedEvent from Teacher {
                teacher.teacherAggregateId == event.aggregateId
            }
        }
    }
}
```

This aggregate demonstrates:
- **Cross-aggregate reference** — `CourseTeacher from Teacher` with three mapped fields
- **Entity reference property** — `CourseTeacher teacher` in the root entity
- **Invariants** — including `teacherNotNull` to ensure the reference is set
- **Referential integrity** — `prevent` delete action + `TEACHER_EXISTS` inter-invariant

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/04-crossrefs/ -o ../docs/examples/generated
```

Explore the generated code:
- `CourseTeacher.java` — embeddable entity with inferred types
- `Course.java` — root entity with `CourseTeacher` reference
- `LoanEventHandling.java` — inter-invariant event handling

---

**Previous:** [05-Business-Rules-Repositories](05-Business-Rules-Repositories.md) | **Next:** [07-Events-Reactive-Patterns](07-Events-Reactive-Patterns.md)
