package com.learn.vm.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ua_blacklist")
@Data
public class UABlacklist {
    @Id
    private String ua;
}
