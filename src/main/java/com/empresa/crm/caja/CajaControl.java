package com.empresa.crm.caja;

import jakarta.persistence.*;

@Entity
@Table(name = "caja_control")
public class CajaControl {

	@Id
	@Column(length = 20)
	String empresa;
}