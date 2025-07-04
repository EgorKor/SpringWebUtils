package io.github.egorkor.model;

import com.github.javafaker.Faker;
import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    private static Faker faker = new Faker();
    @Id
    private Long id;
    private String firstName;
    private String password;
    private String email;
    private String phone;
    private String address;
    @ElementCollection
    private List<String> roles;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @SoftDeleteFlag
    private LocalDateTime deletedAt;
    @OneToMany(mappedBy = "user")
    private List<Order> orders = new ArrayList<>();

    public static User generateUser(long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName(faker.name().firstName());
        user.setPassword(faker.regexify("[a-z0-9!+-]{8,16}"));
        user.setEmail(faker.internet().emailAddress());
        user.setPhone(faker.phoneNumber().phoneNumber());
        user.setAddress(faker.address().fullAddress());
        return user;
    }

    public static List<User> generateUsers(int startId, int count) {
        return IntStream.range(startId, startId + count)
                .boxed()
                .map(id -> generateUser((long) id)).toList();
    }
}
