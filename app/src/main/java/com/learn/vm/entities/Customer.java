package com.learn.vm.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Entity
@Table(name = "customer")
@Data
@Builder()
@NoArgsConstructor
@AllArgsConstructor
public class Customer implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String name;
    private boolean active;
}
