# Step 2 · Entities & Generated Java Basics

> **Goal:** add capabilities incrementally—first just root fields, then one-to-one, one-to-many, and finally many-to-many relations—so you can see exactly what each change produces.

[← Back to Step 1](01-setup-and-cli.md) · [Next → Step 3](03-next-steps.md)

---

## 2.1 Root Fields Only

```nebula
Aggregate User {
    Root Entity User {
        String email;
        Boolean active;
        LocalDateTime createdAt;
        Integer age;
    }
}
```

```java
@Entity
public abstract class User extends Aggregate {
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private Integer age;

    protected User() { }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setEmail(userDto.getEmail());
        setActive(userDto.getActive());
        setCreatedAt(userDto.getCreatedAt());
        setAge(userDto.getAge());
    }

    public User(User other) {
        super(other);
        setEmail(other.getEmail());
        setActive(other.getActive());
        setCreatedAt(other.getCreatedAt());
        setAge(other.getAge());
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
```

At this point the aggregate is nothing more than a bag of primitive fields (`String`, `Boolean`, `Integer`, `LocalDateTime`), and that’s enough for Nebula to emit a fully formed JPA entity: constructors, getters/setters, and the base `Aggregate` ancestry that already knows how to deal with IDs, versions, and states. There are more primitive and built-in types available (enums, numeric flavors, temporal types, collections of primitives) and we’ll introduce them later. For now, don’t worry about DTO constructors—Nebula auto-generates DTO classes that mirror these fields, and we’ll revisit how to customize them in a future step.

> ℹ️ The constructor pattern shown here (protected no-arg, DTO-based constructor, copy constructor) is what the generator already produces in real projects—see `applications/answers/.../execution/aggregate/Execution.java` for a concrete example.

---

## 2.2 Add a One-to-One Supporting Entity

```nebula
Aggregate User {
    Root Entity User {
        String email;
        Boolean active;
        Profile profile;
    }

    Entity Profile {
        String bio;
        String avatarUrl;
    }
}
```

```java
// User.java excerpt
@OneToOne(cascade = CascadeType.ALL)
private Profile profile;

public Profile getProfile() { return profile; }
public void setProfile(Profile profile) { this.profile = profile; }

// Profile.java
@Entity
public class Profile {
    @Id @GeneratedValue private Integer id;
    private String bio;
    private String avatarUrl;
    @OneToOne(mappedBy = "profile")
    private User user;
    // constructors + getters/setters
}
```

Notice how simply referencing `Profile profile` was enough for the generator to produce the annotated relationship plus the mirrored `Profile` class with its back-reference. We’re still keeping things minimal: no manual DTO constructors required; the generated DTOs follow the field list automatically, and we’ll learn how to fine-tune them in a later step.

---

## 2.3 Add a One-to-Many Supporting Entity (List or Set)

```nebula
Aggregate User {
    Root Entity User {
        List<Car> cars;
    }

    Entity Car {
        String model;
        String licensePlate;
    }
}
```

```java
// User.java excerpt
@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Car> cars = new ArrayList<>();

public List<Car> getCars() { return cars; }
public void addCar(Car car) {
    cars.add(car);
    car.setOwner(this);
}

// Car.java
@Entity
public class Car {
    @Id @GeneratedValue private Integer id;
    private String model;
    private String licensePlate;
    @ManyToOne
    private User owner;
    // constructors + getters/setters
}
```

The story evolves naturally: by adding `List<Car>` the DSL now describes a one-to-many collection, and Nebula injects the correct `@OneToMany`/`@ManyToOne` pair, complete with helper methods to keep both sides in sync. Again, DTOs are regenerated with the new list, and we’ll explain how to shape those DTO constructors when we start mapping them explicitly.

---

## 2.4 Many-to-Many Example

```nebula
Aggregate User {
    Root Entity User {
        String email;
        List<Role> roles;
    }

    Entity Role {
        String name;
        List<User> members;
    }
}
```

```java
// User.java excerpt
@ManyToMany
@JoinTable(name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id"))
private List<Role> roles = new ArrayList<>();

// Role.java excerpt
@ManyToMany(mappedBy = "roles")
private List<User> members = new ArrayList<>();
```

By mirroring references (`List<Role> roles`, `List<User> members`), Nebula automatically chooses a sensible join table and sets up both sides of the many-to-many relationship. The generated DTOs and factories keep pace, and while they don’t yet have bespoke constructors, that detail is deferred to the DTO-focused step coming up.

---

## Recap

Each iteration mirrors the story you’d tell about the domain: first define the root’s basic data, then give it a profile, then give it cars, and finally connect it to roles in a many-to-many fashion. Nebula keeps generating constructors, getters/setters, relationships, DTOs, factories, repositories, and services along the way. So far we’ve relied on the default DTO shapes (no constructor customization needed); the next steps will dig into DTO mappings, repositories, and services to show how you can take control when you need it.

[← Back to Step 1](01-setup-and-cli.md) · [Next → Step 3](03-next-steps.md)

