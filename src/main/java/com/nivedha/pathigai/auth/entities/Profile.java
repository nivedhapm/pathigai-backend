package com.nivedha.pathigai.auth.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "profiles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Integer profileId;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    // Many-to-Many with Users
    @ManyToMany(mappedBy = "profiles", fetch = FetchType.LAZY)
    private Set<User> users;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return Objects.equals(profileId, profile.profileId) && Objects.equals(name, profile.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId, name);
    }
}