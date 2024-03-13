package com.learn.vm.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ip_blacklist")
@Data
public class IPBlacklist {
    @Id
    private long ip;
}
